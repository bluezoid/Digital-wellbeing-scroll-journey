package com.achyutpal.scrolljourney.ui.space

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.ui.common.emoji
import com.achyutpal.scrolljourney.ui.common.nameRes
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookTowards
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.roundToInt

private val HeroHeight = 280.dp

/**
 * The dashboard hero: a live, watch-only 3D view of the current leg of the journey, with a slim
 * progress strip underneath. Tapping anywhere opens the full-screen Explore view.
 *
 * Watch-only on purpose: the dashboard scrolls vertically, and a drag-to-rotate scene inside it
 * would fight the scroll. So the scene takes no touches; a transparent layer on top handles the
 * tap and lets vertical drags reach the scroll.
 *
 * @param active false while the hero is scrolled off screen: 3D drawing pauses (see [rememberSceneLifecycle]).
 * @param onFailed called if the 3D scene can't be built on this phone; the caller shows the 2D card instead.
 */
@Composable
fun JourneyHero(
    rocket: RocketState,
    active: Boolean,
    onExplore: () -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .background(Color.Black),
        ) {
            var rocketOnScreen by remember { mutableStateOf<Pair<Float, Float>?>(null) }
            HeroScene(rocket, active, onFailed, onRocketMoved = { rocketOnScreen = it })

            rocketOnScreen?.let { (x, y) -> YouLabel(x, y) }
            HeroPill(
                text = legText(rocket),
                color = Color(0xFFFFC247),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
            HeroPill(
                text = stringResource(R.string.hero_explore),
                color = Color(0xFFFF8A3D),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            )
            // On top of the scene: takes the tap, lets drags through to the dashboard scroll.
            val exploreLabel = stringResource(R.string.hero_explore_description)
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = exploreLabel, role = Role.Button, onClick = onExplore),
            )
        }
        ProgressStrip(rocket)
    }
}

/** The 3D scene itself; draws only while [active]. */
@Composable
private fun HeroScene(
    rocket: RocketState,
    active: Boolean,
    onFailed: () -> Unit,
    onRocketMoved: (Pair<Float, Float>?) -> Unit,
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader).also { it.indirectLight?.intensity = 6_000f }
    // Building loads materials, textures and the rocket model: if anything fails, fall back to 2D.
    val scene = remember(engine) {
        runCatching { SolarSystemScene(engine, materialLoader, modelLoader, context.assets) }
            .onFailure { Log.w("JourneyHero", "3D scene unavailable, showing the 2D card", it) }
            .getOrNull()
    }
    if (scene == null) {
        LaunchedEffect(Unit) { onFailed() }
        return
    }

    val clock = remember { SceneClock() }
    val cameraNode = rememberCameraNode(engine)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var yaw by remember { mutableStateOf(HeroCamera.START_YAW_DEGREES) }

    Scene(
        modifier = Modifier.fillMaxWidth().height(HeroHeight).onSizeChanged { size = it },
        engine = engine,
        materialLoader = materialLoader,
        modelLoader = modelLoader,
        environmentLoader = environmentLoader,
        environment = environment,
        mainLightNode = null, // the Sun's point light replaces the default overhead light
        cameraNode = cameraNode,
        cameraManipulator = null, // watch-only: no drag / pinch
        childNodes = rememberNodes { addAll(scene.all) },
        // SceneView keeps the screen awake for its activity and never undoes it; with no activity it skips that.
        activity = null,
        lifecycle = rememberSceneLifecycle(active),
        onFrame = frame@{ frameTimeNanos ->
            if (!engine.isValid) return@frame // a frame arriving after the scene was torn down
            val t = clock.seconds(frameTimeNanos)
            yaw += clock.delta(frameTimeNanos) * HeroCamera.DRIFT_DEGREES_PER_SECOND
            val framing = HeroCamera.frame(rocket, t)
            val eye = HeroCamera.eye(framing, yaw)
            scene.update(t, rocket, eye)
            cameraNode.transform = lookTowards(
                eye = Position(eye.x, eye.y, eye.z),
                forward = Float3(framing.target.x - eye.x, framing.target.y - eye.y, framing.target.z - eye.z),
                up = Float3(0f, 1f, 0f),
            )
            onRocketMoved(projectAbove(cameraNode, JourneyMap.rocketPosition(rocket, t), eye, framing.target, size))
        },
    )
    // Declared after the Scene, so disposed before it (Compose disposes in reverse order): the
    // rocket model leaves the scene while the scene still exists, and before the model loader
    // frees it (see RocketModel.releaseModel).
    DisposableEffect(scene) { onDispose { scene.rocket.releaseModel() } }
}

/** Screen position just above a point, or null when it's behind the camera or off screen. */
private fun projectAbove(
    cameraNode: io.github.sceneview.node.CameraNode,
    p: SpacePoint,
    eye: SpacePoint,
    target: SpacePoint,
    size: IntSize,
): Pair<Float, Float>? {
    if (size == IntSize.Zero) return null
    val forward = target - eye
    val toP = p - eye
    if (toP.x * forward.x + toP.y * forward.y + toP.z * forward.z <= 0f) return null
    val v = cameraNode.worldToView(Position(p.x, p.y + RocketModel.LENGTH, p.z))
    val x = v.x * size.width
    val y = (1f - v.y) * size.height
    return if (x in 0f..size.width.toFloat() && y in 0f..size.height.toFloat()) x to y else null
}

@Composable
private fun YouLabel(x: Float, y: Float) {
    val color = Color(0xFFFFB066)
    Text(
        text = stringResource(R.string.space_label_you),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height - 4.dp.toPx()
            }
            .background(Color(0xB30B1026), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun HeroPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(Color(0xCC0B1026), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun legText(rocket: RocketState): String {
    val from = rocket.highestReachedPlanet
    val to = rocket.nextPlanet ?: return "${from.emoji()} ${stringResource(R.string.rocket_journey_complete)}"
    return "${from.emoji()} ${stringResource(from.nameRes())} → ${to.emoji()} ${stringResource(to.nameRes())}"
}

/** Fuel toward the next stop, under the 3D view. */
@Composable
private fun ProgressStrip(rocket: RocketState) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val next = rocket.nextPlanet
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (next != null) {
                    stringResource(R.string.rocket_fuel_to_next, rocket.currentFuel, next.fuelRequired, stringResource(next.nameRes()))
                } else {
                    stringResource(R.string.rocket_journey_complete)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.rocket_fuel_label, rocket.currentFuel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (next != null) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { rocket.legProgress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.rocket_rules),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Same size as the hero, shown for the moment before the settings load (avoids a jump). */
@Composable
fun JourneyHeroPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(HeroHeight).background(Color.Black))
}
