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
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.texture.ImageTexture

/**
 * 3D-1 SPIKE (temporary): one textured, lit, slowly spinning Earth with an orbit camera.
 * Goal: prove SceneView/Filament runs smoothly on the test phone. The FPS counter is for that check.
 * (Diagnosis 2026-09-28: lit colour + lit texture render correctly; SceneView's unlit
 *  createImageInstance showed stripe artefacts on spheres, so it's not used.)
 * NOTE: SceneView's Sphere has one primitive per stack. Pass a single `materialInstance` (applied to
 * the whole sphere), NOT `materialInstances = listOf(x)`, which only colours the first stack (top cap)
 * and leaves the rest on Filament's grey default material.
 * Open from ⋮ → "3D test", or in debug builds:
 *   adb shell am start -n com.achyutpal.scrolljourney/.MainActivity --es debug_route space_spike
 */
@Composable
fun EarthSpikeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val earth = remember(engine) {
        val texture = ImageTexture.Builder()
            .bitmap(context.assets, "textures/earth_2k.jpg")
            .build(engine)
        SphereNode(
            engine = engine,
            radius = 1f,
            stacks = 48,
            slices = 96,
            // A single materialInstance covers the whole sphere (see note above).
            materialInstance = materialLoader.createTextureInstance(texture, roughness = 0.9f),
        )
    }
    val cameraNode = rememberCameraNode(engine) { position = Position(z = 3.5f) }

    var fps by remember { mutableIntStateOf(0) }
    val frameStats = remember { FrameStats() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = Position(z = 3.5f),
                targetPosition = Position(0f, 0f, 0f),
            ),
            childNodes = rememberNodes { add(earth) },
            onFrame = { frameTimeNanos ->
                earth.rotation = Rotation(y = earth.rotation.y + 0.15f) // slow spin, degrees per frame
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
            Text(stringResource(R.string.spike_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.spike_hint), color = Color(0xFFB4B8D6), style = MaterialTheme.typography.bodySmall)
            Text("FPS: $fps", color = Color(0xFF4ADE80), style = MaterialTheme.typography.labelLarge)
        }
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
