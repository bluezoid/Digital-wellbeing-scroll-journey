package com.achyutpal.scrolljourney.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.achyutpal.scrolljourney.R
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import io.github.sceneview.Scene
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.texture.ImageTexture
import kotlin.math.hypot

/**
 * 3D-2: the stylised solar system. Sun (self-lit, with a point light inside that lights the
 * planets: real day/night sides), 9 planets incl. Pluto, the Moon, Europa, Titan, the Ceres
 * asteroid-belt stop, Saturn's rings, orbit paths and a Milky Way starfield all around.
 * Drag = orbit 360°, pinch = zoom (clamped), two fingers = pan.
 * Tap a planet = fly to it and follow it (pinch then zooms on that planet); double-tap = back to the Sun.
 *
 * Debug: adb shell am start -n com.achyutpal.scrolljourney/.MainActivity --es debug_route space
 */
@Composable
fun SpaceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    // Keep only a little ambient light so planets have a dark night side.
    val environment = rememberEnvironment(environmentLoader).also { it.indirectLight?.intensity = 6_000f }

    val scene = remember(engine) {
        fun texture(name: String) = ImageTexture.Builder()
            .bitmap(context.assets, "textures/$name")
            .build(engine)

        val unlitTextured = materialLoader.createMaterial("materials/space_unlit_textured.filamat")
        val unlitColor = materialLoader.createMaterial("materials/space_unlit_color.filamat")

        fun glowing(textureName: String, intensity: Float) = materialLoader.createInstance(unlitTextured).apply {
            setTexture("baseTexture", texture(textureName))
            setParameter("intensity", intensity)
        }

        val stars = SphereNode(
            engine = engine,
            radius = SpaceBody.STARFIELD_RADIUS,
            stacks = 32,
            slices = 64,
            materialInstance = glowing("stars.jpg", intensity = 0.8f),
        ).apply {
            isTouchable = false
            isHittable = false // the camera is inside this sphere: it must never catch taps
        }

        val bodies = SpaceBody.entries.associateWith { body ->
            val material = if (body == SpaceBody.SUN) {
                glowing("sun.jpg", intensity = 1.4f)
            } else {
                materialLoader.createTextureInstance(texture("${body.texture}.jpg"), roughness = 1f)
            }
            SphereNode(
                engine = engine,
                radius = body.radius,
                stacks = if (body.radius > 1f) 48 else 32,
                slices = if (body.radius > 1f) 96 else 64,
                // One materialInstance for the whole sphere (a list would only cover the first stack).
                materialInstance = material,
            )
        }

        val saturnRing = GeometryNode(
            engine = engine,
            geometry = SpaceGeometry.ring(engine, SpaceBody.SATURN_RING_INNER, SpaceBody.SATURN_RING_OUTER),
            materialInstance = materialLoader.createTextureInstance(texture("saturn_ring.png"), isOpaque = false).apply {
                setCullingMode(Material.CullingMode.NONE) // visible from above and below
            },
        )

        val orbitMaterial = materialLoader.createInstance(unlitColor).apply {
            setParameter("color", 0.75f, 0.8f, 1f, 0.22f)
            setParameter("intensity", 1f)
        }
        val orbits = SpaceBody.sunOrbiters.map { body ->
            GeometryNode(engine, SpaceGeometry.orbitCircle(engine, body.orbitRadius), orbitMaterial)
                .apply {
                    isTouchable = false
                    isHittable = false
                }
        }

        // The Sun's light: a point light at the centre, so each planet is lit on the side facing the Sun.
        val sunlight = LightNode(engine, LightManager.Type.POINT) {
            color(1f, 0.96f, 0.9f)
            intensity(3.0e8f) // lumens (tuned for SceneView's "sunny 16" camera exposure)
            falloff(200f)
            castShadows(false)
        }

        SolarSystemNodes(stars, bodies, saturnRing, orbits, sunlight)
    }

    val clock = remember { SceneClock() }
    val camera = remember { OrbitCameraState() }
    var focused by remember { mutableStateOf(SpaceBody.SUN) }
    val cameraNode = rememberCameraNode(engine) { transform = FocusOrbitManipulator.cameraTransform(camera) }
    val cameraManipulator = rememberCameraManipulator(creator = { FocusOrbitManipulator(camera) })

    fun focusOn(body: SpaceBody) {
        focused = body
        camera.focus(radius = body.radius, overview = body == SpaceBody.SUN)
    }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val touchPx = with(density) { 36.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    /** Screen-space picking: project every body and take the one nearest the finger. */
    fun bodyAt(x: Float, y: Float): SpaceBody? {
        if (viewSize == IntSize.Zero) return null
        val w = viewSize.width.toFloat()
        val h = viewSize.height.toFloat()
        val t = clock.lastSeconds
        val eye = camera.eye()
        val target = camera.target
        val forward = SpacePoint(target.x - eye.x, target.y - eye.y, target.z - eye.z)
        val screenBodies = SpaceBody.entries.map { body ->
            val p = body.positionAt(t)
            val toBody = SpacePoint(p.x - eye.x, p.y - eye.y, p.z - eye.z)
            val inFront = toBody.x * forward.x + toBody.y * forward.y + toBody.z * forward.z > 0f
            val centre = cameraNode.worldToView(Position(p.x, p.y, p.z))
            val edge = cameraNode.worldToView(Position(p.x, p.y + body.radius, p.z))
            val cx = centre.x * w
            val cy = (1f - centre.y) * h
            val radiusPx = hypot(edge.x * w - cx, (1f - edge.y) * h - cy)
            ScreenBody(body, cx, cy, radiusPx, inFront)
        }
        return SpacePicking.pick(x, y, screenBodies, minTouchPx = touchPx, marginPx = marginPx)
    }

    var fps by remember { mutableIntStateOf(0) }
    val frameStats = remember { FrameStats() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Scene(
            modifier = Modifier.fillMaxSize().onSizeChanged { viewSize = it },
            engine = engine,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            mainLightNode = null, // the Sun's point light replaces the default overhead light
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = rememberNodes { addAll(scene.all) },
            onGestureListener = rememberOnGestureListener(
                // Try the 3D hit first, then fall back to screen-space picking (small far planets).
                onSingleTapConfirmed = { e, node -> (scene.bodyFor(node) ?: bodyAt(e.x, e.y))?.let(::focusOn) },
                onDoubleTap = { _, _ -> focusOn(SpaceBody.SUN) },
            ),
            onFrame = { frameTimeNanos ->
                val t = clock.seconds(frameTimeNanos)
                scene.update(t)
                // Follow the focused body as it orbits, easing the camera there after a tap.
                camera.advance(clock.delta(frameTimeNanos), focused.positionAt(t))
                cameraNode.transform = FocusOrbitManipulator.cameraTransform(camera)
                frameStats.onFrame(frameTimeNanos)?.let { fps = it }
            },
        )

        Column(Modifier.statusBarsPadding().padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (focused == SpaceBody.SUN) stringResource(R.string.space_title) else stringResource(focused.nameRes()),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(if (focused == SpaceBody.SUN) R.string.space_hint_overview else R.string.space_hint_focused),
                color = Color(0xFFB4B8D6),
                style = MaterialTheme.typography.bodySmall,
            )
            Text("FPS: $fps", color = Color(0xFF4ADE80), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** All scene nodes plus the per-frame animation (orbits and spins). */
private class SolarSystemNodes(
    private val stars: Node,
    private val bodies: Map<SpaceBody, SphereNode>,
    private val saturnRing: Node,
    private val orbits: List<Node>,
    private val sunlight: Node,
) {
    val all: List<Node> = listOf(stars, sunlight) + orbits + bodies.values + saturnRing

    private val bodyByNode: Map<Node, SpaceBody> = bodies.entries.associate { (body, node) -> node to body } +
        (saturnRing to SpaceBody.SATURN)

    /** Which body a tapped node belongs to (the ring counts as Saturn). */
    fun bodyFor(node: Node?): SpaceBody? = node?.let(bodyByNode::get)

    fun update(seconds: Float) {
        bodies.forEach { (body, node) ->
            val p = body.positionAt(seconds)
            node.position = Position(p.x, p.y, p.z)
            node.rotation = Rotation(y = body.spinAt(seconds))
        }
        val saturn = SpaceBody.SATURN.positionAt(seconds)
        saturnRing.position = Position(saturn.x, saturn.y, saturn.z)
        saturnRing.rotation = Rotation(x = SpaceBody.SATURN_RING_TILT_DEGREES)
    }
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
