package com.achyutpal.scrolljourney.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.nameRes
import com.achyutpal.scrolljourney.ui.theme.StatusGreen
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * 3D solar system + the user's journey. Drag = orbit 360°, pinch = zoom, two fingers = pan.
 * Tap a planet / star = fly to it (card shows its journey status); double-tap = back to the Sun;
 * "Find my rocket" = fly to the rocket.
 *
 * Opened by tapping the dashboard's 3D hero ("Explore").
 *
 * Debug: adb shell am start -n com.achyutpal.scrolljourney/.MainActivity --es debug_route space [--ei debug_fuel 140]
 */
@Composable
fun SpaceScreen(
    onBack: () -> Unit,
    viewModel: SpaceViewModel = viewModel(factory = SpaceViewModel.Factory),
) {
    val context = LocalContext.current
    val rocketState by viewModel.rocket.collectAsStateWithLifecycle()

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    // Keep only a little ambient light so planets have a dark night side.
    val environment = rememberEnvironment(environmentLoader).also { it.indirectLight?.intensity = 6_000f }
    val scene = remember(engine) { SolarSystemScene(engine, materialLoader, modelLoader, context.assets) }

    val clock = remember { SceneClock() }
    val camera = remember { OrbitCameraState() }
    var focus by remember { mutableStateOf<SpaceFocus>(SpaceFocus.Overview) }
    val cameraNode = rememberCameraNode(engine) { transform = FocusOrbitManipulator.cameraTransform(camera) }
    val cameraManipulator = rememberCameraManipulator(creator = { FocusOrbitManipulator(camera) })

    fun focusOn(target: SpaceFocus) {
        focus = target
        when (target) {
            SpaceFocus.Overview -> camera.focus(radius = SpaceBody.SUN.radius, overview = true)
            is SpaceFocus.Body -> if (target.body == SpaceBody.SUN) {
                focus = SpaceFocus.Overview
                camera.focus(radius = SpaceBody.SUN.radius, overview = true)
            } else {
                camera.focus(radius = target.body.radius, overview = false)
            }
            is SpaceFocus.Marker -> camera.focus(radius = JourneyMap.radius(target.destination), overview = false)
            SpaceFocus.Rocket -> camera.focus(
                radius = ROCKET_FOCUS_RADIUS,
                overview = false,
                minFocusDistance = ROCKET_FOCUS_RADIUS * OrbitCameraState.FOCUS_DISTANCE_RADII,
                viewFrom = rocketViewDirection(rocketState, clock.lastSeconds),
            )
        }
    }

    fun focusCentre(t: Float, rocket: RocketState): SpacePoint = when (val f = focus) {
        SpaceFocus.Overview -> SpacePoint.ORIGIN
        is SpaceFocus.Body -> f.body.positionAt(t)
        is SpaceFocus.Marker -> JourneyMap.anchor(f.destination, t)
        SpaceFocus.Rocket -> JourneyMap.rocketPosition(rocket, t)
    }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val touchPx = with(density) { 36.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    /** Screen-space picking for small / far objects that the 3D hit test misses. */
    fun focusAt(x: Float, y: Float): SpaceFocus? {
        if (viewSize == IntSize.Zero) return null
        val w = viewSize.width.toFloat()
        val h = viewSize.height.toFloat()
        val t = clock.lastSeconds
        val eye = camera.eye()
        val forward = camera.target - eye
        fun project(p: SpacePoint, radius: Float): Triple<Float, Float, Float>? {
            val toP = p - eye
            if (toP.x * forward.x + toP.y * forward.y + toP.z * forward.z <= 0f) return null // behind the camera
            val c = cameraNode.worldToView(Position(p.x, p.y, p.z))
            val e = cameraNode.worldToView(Position(p.x, p.y + radius, p.z))
            val cx = c.x * w
            val cy = (1f - c.y) * h
            return Triple(cx, cy, hypot(e.x * w - cx, (1f - e.y) * h - cy))
        }
        val candidates = SpaceBody.entries.mapNotNull { b ->
            project(b.positionAt(t), b.radius)?.let { (cx, cy, r) -> Pickable(SpaceFocus.Body(b), cx, cy, r) }
        } + JourneyMap.deepSpace.mapNotNull { m ->
            project(m.position, m.radius)?.let { (cx, cy, r) -> Pickable(SpaceFocus.Marker(m.destination), cx, cy, r) }
        } + listOfNotNull(
            project(JourneyMap.rocketPosition(rocketState, t), 0.4f)?.let { (cx, cy, r) -> Pickable(SpaceFocus.Rocket, cx, cy, r) },
        )
        // Reuse the tested picking rule via ScreenBody-like scoring.
        return candidates
            .map { it to hypot(x - it.x, y - it.y) / maxOf(touchPx, it.radiusPx + marginPx) }
            .filter { it.second <= 1f }
            .minByOrNull { it.second }
            ?.first?.focus
    }

    var labels by remember { mutableStateOf(emptyList<LabelSpot>()) }
    val labelGapPx = with(density) { 30.dp.toPx() }

    /** Screen position just above a 3D point (null if behind the camera or off screen). */
    fun screenAbove(p: SpacePoint, radius: Float): Pair<Float, Float>? {
        if (viewSize == IntSize.Zero) return null
        val eye = camera.eye()
        val forward = camera.target - eye
        val toP = p - eye
        if (toP.x * forward.x + toP.y * forward.y + toP.z * forward.z <= 0f) return null
        val w = viewSize.width.toFloat()
        val h = viewSize.height.toFloat()
        val top = cameraNode.worldToView(Position(p.x, p.y + radius, p.z))
        val x = top.x * w
        val y = (1f - top.y) * h
        return if (x in 0f..w && y in 0f..h) x to y else null
    }

    /** The rocket, the next stop and the last reached stop, each with a floating label. */
    fun computeLabels(t: Float, rocket: RocketState): List<LabelSpot> {
        val spots = buildList {
            screenAbove(JourneyMap.rocketPosition(rocket, t), RocketModel.LENGTH)?.let { (x, y) -> add(LabelSpot(LabelKind.ROCKET, null, x, y)) }
            rocket.nextPlanet?.let { d ->
                screenAbove(JourneyMap.anchor(d, t), JourneyMap.radius(d) * 1.2f)?.let { (x, y) -> add(LabelSpot(LabelKind.NEXT, d, x, y)) }
            }
            val reached = rocket.highestReachedPlanet
            screenAbove(JourneyMap.anchor(reached, t), JourneyMap.radius(reached) * 1.15f)?.let { (x, y) ->
                add(LabelSpot(LabelKind.REACHED, reached, x, y))
            }
        }
        // Spots are in priority order (you → next stop → reached); a label that would overlap a more
        // important one is hidden rather than moved (moving it could place it next to the wrong object).
        // A planet's label that would sit on top of the rocket itself is hidden too.
        val rocketAt = screenAbove(JourneyMap.rocketPosition(rocket, t), 0f)
        val placed = mutableListOf<LabelSpot>()
        spots.forEach { spot ->
            val overlaps = placed.any { kotlin.math.abs(it.x - spot.x) < labelGapPx * 5 && kotlin.math.abs(it.y - spot.y) < labelGapPx * 1.4f }
            val coversRocket = spot.kind != LabelKind.ROCKET && rocketAt != null &&
                kotlin.math.abs(rocketAt.first - spot.x) < labelGapPx * 3 &&
                rocketAt.second - spot.y in -labelGapPx * 1.6f..labelGapPx * 0.8f
            if (!overlaps && !coversRocket) placed += spot
        }
        return placed
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Scene(
            modifier = Modifier.fillMaxSize().onSizeChanged { viewSize = it },
            engine = engine,
            materialLoader = materialLoader,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            mainLightNode = null, // the Sun's point light replaces the default overhead light
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = rememberNodes { addAll(scene.all) },
            // SceneView keeps the screen awake for its activity and never undoes it; with no activity it skips that.
            activity = null,
            // Pauses drawing before the model loader is destroyed when this screen closes (see rememberSceneLifecycle).
            lifecycle = rememberSceneLifecycle(active = true),
            onGestureListener = rememberOnGestureListener(
                // A single tap is confirmed ~300 ms later (in case it becomes a double tap). If the
                // screen closed meanwhile, the engine is already destroyed: ignore the late tap.
                // Try the 3D hit first, then fall back to screen-space picking (small far objects).
                onSingleTapConfirmed = { e, node ->
                    if (engine.isValid) (scene.focusFor(node) ?: focusAt(e.x, e.y))?.let(::focusOn)
                },
                onDoubleTap = { _, _ -> if (engine.isValid) focusOn(SpaceFocus.Overview) },
            ),
            onFrame = frame@{ frameTimeNanos ->
                if (!engine.isValid) return@frame
                val t = clock.seconds(frameTimeNanos)
                scene.update(t, rocketState, camera.eye())
                // Follow the focus as it moves, easing the camera there after a tap.
                camera.advance(clock.delta(frameTimeNanos), focusCentre(t, rocketState))
                cameraNode.transform = FocusOrbitManipulator.cameraTransform(camera)
                labels = computeLabels(t, rocketState)
            },
        )

        labels.forEach { spot -> FloatingLabel(spot, rocketState) }

        Row(
            modifier = Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
        }

        FocusCard(
            focus = focus,
            rocket = rocketState,
            onFindRocket = { focusOn(SpaceFocus.Rocket) },
            onOverview = { focusOn(SpaceFocus.Overview) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    // Declared after the Scene, so disposed before it: the rocket model leaves the scene while the
    // scene still exists, and before the model loader frees it (see RocketModel.releaseModel).
    DisposableEffect(scene) { onDispose { scene.rocket.releaseModel() } }
}

/**
 * Treated as the rocket's "size" when focusing: the camera settles 6× this away (1.2 units),
 * close enough to see the small rocket clearly, far enough to keep the big planets in view.
 */
private const val ROCKET_FOCUS_RADIUS = RocketModel.LENGTH * 2.5f

/**
 * Look at the rocket from the side facing away from the nearest body (plus a little height and a
 * sideways angle), so that body sits behind the rocket instead of blocking the camera.
 */
private fun rocketViewDirection(rocket: RocketState, seconds: Float): SpacePoint {
    val p = JourneyMap.rocketPosition(rocket, seconds)
    val nearest = SpaceBody.entries.minBy { it.positionAt(seconds).distanceTo(p) - it.radius }
    val away = p - nearest.positionAt(seconds)
    val heading = JourneyMap.rocketHeading(rocket, seconds)
    return away + heading * 0.6f + SpacePoint(0f, 0.5f * OrbitCameraState.length(away), 0f)
}

private enum class LabelKind { ROCKET, NEXT, REACHED }

private data class LabelSpot(val kind: LabelKind, val destination: Destination?, val x: Float, val y: Float)

/** A small pill label floating just above a 3D object (positioned in the layout phase each frame). */
@Composable
private fun FloatingLabel(spot: LabelSpot, rocket: RocketState) {
    val (text, color) = when (spot.kind) {
        LabelKind.ROCKET -> stringResource(R.string.space_label_you) to Color(0xFFFFB066)
        LabelKind.NEXT -> {
            val d = spot.destination ?: return
            stringResource(R.string.space_label_next, stringResource(d.nameRes()), (d.fuelRequired - rocket.currentFuel).coerceAtLeast(0)) to
                Color(0xFFFF8A3D)
        }
        LabelKind.REACHED -> stringResource(R.string.space_label_reached, stringResource((spot.destination ?: return).nameRes())) to
            StatusGreen
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .offset { IntOffset(spot.x.roundToInt(), spot.y.roundToInt()) }
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height - 6.dp.toPx()
            }
            .background(Color(0xB30B1026), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

private data class Pickable(val focus: SpaceFocus, val x: Float, val y: Float, val radiusPx: Float)

/** Bottom card: what's in focus and how it relates to the user's journey. */
@Composable
private fun FocusCard(
    focus: SpaceFocus,
    rocket: RocketState,
    onFindRocket: () -> Unit,
    onOverview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val destination: Destination? = when (focus) {
        is SpaceFocus.Body -> JourneyMap.destinationFor(focus.body)
        is SpaceFocus.Marker -> focus.destination
        else -> null
    }
    val title = when (focus) {
        SpaceFocus.Overview -> stringResource(R.string.space_title)
        is SpaceFocus.Body -> destination?.let { "${it.emoji()} ${stringResource(it.nameRes())}" } ?: stringResource(focus.body.nameRes())
        is SpaceFocus.Marker -> "${focus.destination.emoji()} ${stringResource(focus.destination.nameRes())}"
        SpaceFocus.Rocket -> stringResource(R.string.space_rocket_title)
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color(0xCC0B1026), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val (line, lineColor) = when {
            destination != null -> statusLine(JourneyStatus.of(destination, rocket))
            focus is SpaceFocus.Body -> stringResource(R.string.space_not_a_stop) to Color(0xFFB4B8D6)
            else -> rocketLine(rocket) to Color(0xFFFFC247)
        }
        Text(line, color = lineColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        Text(
            text = stringResource(if (focus == SpaceFocus.Overview) R.string.space_hint_overview else R.string.space_hint_focused),
            color = Color(0xFF8A8FB0),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        // Equal-width buttons with one centred line each (a lone button keeps its natural width).
        val both = focus != SpaceFocus.Overview && focus != SpaceFocus.Rocket
        val buttonModifier = if (both) Modifier.weight(1f) else Modifier
        val padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp).then(if (both) Modifier.fillMaxWidth() else Modifier),
        ) {
            if (focus != SpaceFocus.Overview) {
                OutlinedButton(onClick = onOverview, modifier = buttonModifier, contentPadding = padding) {
                    ButtonLabel(stringResource(R.string.space_back_to_sun))
                }
            }
            if (focus != SpaceFocus.Rocket) {
                Button(onClick = onFindRocket, modifier = buttonModifier, contentPadding = padding, colors = ButtonDefaults.buttonColors()) {
                    ButtonLabel(stringResource(R.string.space_find_rocket))
                }
            }
        }
    }
}

@Composable
private fun ButtonLabel(text: String) {
    Text(text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
}

@Composable
private fun statusLine(status: JourneyStatus): Pair<String, Color> = when (status) {
    is JourneyStatus.Reached ->
        stringResource(R.string.space_status_reached, status.stopNumber, JourneyStatus.TOTAL_STOPS) to StatusGreen
    is JourneyStatus.Next ->
        stringResource(R.string.space_status_next, status.stopNumber, JourneyStatus.TOTAL_STOPS, status.fuelToGo, status.fuelRequired) to
            Color(0xFFFF8A3D)
    is JourneyStatus.Locked ->
        stringResource(R.string.space_status_locked, status.stopNumber, JourneyStatus.TOTAL_STOPS, status.fuelRequired, status.fuelToGo) to
            Color(0xFFB4B8D6)
}

@Composable
private fun rocketLine(rocket: RocketState): String {
    val from = rocket.highestReachedPlanet
    val to = rocket.nextPlanet ?: return stringResource(R.string.rocket_journey_complete)
    return stringResource(
        R.string.space_rocket_line,
        "${from.emoji()} ${stringResource(from.nameRes())}",
        "${to.emoji()} ${stringResource(to.nameRes())}",
        rocket.currentFuel,
        to.fuelRequired,
    )
}

/** Display name for each body (reuses the journey destination names where they exist). */
private fun SpaceBody.nameRes(): Int = when (this) {
    SpaceBody.SUN -> R.string.body_sun
    SpaceBody.MERCURY -> R.string.body_mercury
    SpaceBody.VENUS -> R.string.body_venus
    SpaceBody.EARTH -> R.string.planet_earth
    SpaceBody.MOON -> R.string.planet_moon
    SpaceBody.MARS -> R.string.planet_mars
    SpaceBody.CERES -> R.string.body_ceres
    SpaceBody.JUPITER -> R.string.planet_jupiter
    SpaceBody.EUROPA -> R.string.dest_europa
    SpaceBody.SATURN -> R.string.planet_saturn
    SpaceBody.TITAN -> R.string.dest_titan
    SpaceBody.URANUS -> R.string.planet_uranus
    SpaceBody.NEPTUNE -> R.string.planet_neptune
    SpaceBody.PLUTO -> R.string.planet_pluto
}
