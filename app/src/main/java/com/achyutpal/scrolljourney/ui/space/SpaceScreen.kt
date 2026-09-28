package com.achyutpal.scrolljourney.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

/**
 * 3D solar system + the user's journey. Drag = orbit 360°, pinch = zoom, two fingers = pan.
 * Tap a planet / star = fly to it (card shows its journey status); double-tap = back to the Sun;
 * "Find my rocket" = fly to the rocket.
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

    var fps by remember { mutableIntStateOf(0) }
    val frameStats = remember { FrameStats() }

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
            onGestureListener = rememberOnGestureListener(
                // Try the 3D hit first, then fall back to screen-space picking (small far objects).
                onSingleTapConfirmed = { e, node -> (scene.focusFor(node) ?: focusAt(e.x, e.y))?.let(::focusOn) },
                onDoubleTap = { _, _ -> focusOn(SpaceFocus.Overview) },
            ),
            onFrame = { frameTimeNanos ->
                val t = clock.seconds(frameTimeNanos)
                scene.update(t, rocketState, camera.eye())
                // Follow the focus as it moves, easing the camera there after a tap.
                camera.advance(clock.delta(frameTimeNanos), focusCentre(t, rocketState))
                cameraNode.transform = FocusOrbitManipulator.cameraTransform(camera)
                frameStats.onFrame(frameTimeNanos)?.let { fps = it }
            },
        )

        Row(
            modifier = Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
            Text("FPS $fps", color = Color(0x994ADE80), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 12.dp))
        }

        FocusCard(
            focus = focus,
            rocket = rocketState,
            onFindRocket = { focusOn(SpaceFocus.Rocket) },
            onOverview = { focusOn(SpaceFocus.Overview) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Treated as the rocket's "size" when focusing: the camera settles about 6× this away. */
private const val ROCKET_FOCUS_RADIUS = RocketModel.LENGTH

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (focus != SpaceFocus.Overview) {
                OutlinedButton(onClick = onOverview) { Text(stringResource(R.string.space_back_to_sun)) }
            }
            if (focus != SpaceFocus.Rocket) {
                Button(onClick = onFindRocket, colors = ButtonDefaults.buttonColors()) { Text(stringResource(R.string.space_find_rocket)) }
            }
        }
    }
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

/** Seconds since the first frame, and the time since the previous frame, from the frame clock. */
private class SceneClock {
    private var start = 0L
    private var last = 0L

    /** The scene time of the most recent frame (used when handling taps between frames). */
    var lastSeconds = 0f
        private set

    fun seconds(frameTimeNanos: Long): Float {
        if (start == 0L) start = frameTimeNanos
        lastSeconds = (frameTimeNanos - start) / 1_000_000_000f
        return lastSeconds
    }

    fun delta(frameTimeNanos: Long): Float {
        val d = if (last == 0L) 0f else (frameTimeNanos - last) / 1_000_000_000f
        last = frameTimeNanos
        return d
    }
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

/** Counts frames per one-second window; returns the FPS when a window completes. */
private class FrameStats {
    private var windowStart = 0L
    private var frames = 0

    fun onFrame(nowNanos: Long): Int? {
        if (windowStart == 0L) windowStart = nowNanos
        frames++
        val elapsed = nowNanos - windowStart
        if (elapsed < 1_000_000_000L) return null
        val result = (frames * 1_000_000_000L / elapsed).toInt()
        windowStart = nowNanos
        frames = 0
        return result
    }
}
