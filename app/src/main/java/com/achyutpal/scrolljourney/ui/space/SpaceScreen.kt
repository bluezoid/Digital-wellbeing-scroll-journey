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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.achyutpal.scrolljourney.R
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
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
import io.github.sceneview.texture.ImageTexture

/** Camera starts above and in front of the Sun, looking at it. */
private val CameraHome = Position(0f, 26f, 62f)

/**
 * 3D-2: the stylised solar system. Sun (self-lit, with a point light inside that lights the
 * planets: real day/night sides), 9 planets incl. Pluto, the Moon, Europa, Titan, the Ceres
 * asteroid-belt stop, Saturn's rings, orbit paths and a Milky Way starfield all around.
 * Drag = orbit 360°, pinch = zoom (clamped), two fingers = pan.
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
        ).apply { isTouchable = false }

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
                .apply { isTouchable = false }
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

    val cameraNode = rememberCameraNode(engine) {
        position = CameraHome
        lookAt(Position(0f, 0f, 0f))
    }
    val cameraManipulator = rememberCameraManipulator(
        creator = {
            ClampedOrbitManipulator(
                delegate = SceneView.createDefaultCameraManipulator(
                    orbitHomePosition = CameraHome,
                    targetPosition = Position(0f, 0f, 0f),
                ),
                target = Position(0f, 0f, 0f),
                minDistance = 7f,
                maxDistance = 140f,
            )
        },
    )

    var fps by remember { mutableIntStateOf(0) }
    val frameStats = remember { FrameStats() }
    val clock = remember { SceneClock() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            mainLightNode = null, // the Sun's point light replaces the default overhead light
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = rememberNodes { addAll(scene.all) },
            onFrame = { frameTimeNanos ->
                val t = clock.seconds(frameTimeNanos)
                scene.update(t)
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
            Text(stringResource(R.string.space_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.spike_hint), color = Color(0xFFB4B8D6), style = MaterialTheme.typography.bodySmall)
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

/** Seconds since the first frame, from the renderer's frame clock. */
private class SceneClock {
    private var start = 0L
    fun seconds(frameTimeNanos: Long): Float {
        if (start == 0L) start = frameTimeNanos
        return (frameTimeNanos - start) / 1_000_000_000f
    }
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
