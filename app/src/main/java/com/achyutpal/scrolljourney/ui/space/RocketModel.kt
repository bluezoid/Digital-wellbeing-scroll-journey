package com.achyutpal.scrolljourney.ui.space

import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.Material
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

/**
 * A small rocket built from simple shapes (no downloaded model, so no licensing or size cost).
 * It points along -Z (the "forward" of a lookTowards transform): white body, red nose,
 * orange fins, a flickering flame behind, and a soft glow that only appears from far away
 * (so the rocket stays findable as a dot in the overview, but looks clean up close).
 */
class RocketModel(
    engine: Engine,
    materialLoader: MaterialLoader,
    unlitColor: Material,
) {
    val root = Node(engine).apply {
        isTouchable = false
        isHittable = false
    }

    private val flame: Node
    private val glow: Node

    init {
        val white = materialLoader.createColorInstance(Color(0xFFF4F4FA), roughness = 0.4f)
        val red = materialLoader.createColorInstance(Color(0xFFE53935), roughness = 0.5f)
        val orange = materialLoader.createColorInstance(Color(0xFFFF8A3D), roughness = 0.6f)

        fun glowing(r: Float, g: Float, b: Float, a: Float) = materialLoader.createInstance(unlitColor).apply {
            setParameter("color", r, g, b, a)
            setParameter("intensity", 1f)
        }

        // Body: a cylinder along Y by default, turned to lie along Z.
        val body = CylinderNode(engine, radius = 0.12f, height = 0.5f, materialInstance = white).apply {
            rotation = Rotation(x = 90f)
        }
        // Nose: a stretched sphere at the front (-Z).
        val nose = SphereNode(engine, radius = 0.12f, materialInstance = red).apply {
            position = Position(z = -0.25f)
            scale = Scale(1f, 1f, 1.9f)
        }
        // Three fins at the back (+Z), 120° apart.
        val fins = (0 until 3).map { i ->
            CubeNode(engine, size = Size(0.02f, 0.16f, 0.16f), materialInstance = orange).apply {
                val angle = i * 120f
                rotation = Rotation(z = angle)
                val rad = Math.toRadians(angle.toDouble())
                position = Position((0.14f * -kotlin.math.sin(rad)).toFloat(), (0.14f * kotlin.math.cos(rad)).toFloat(), 0.2f)
            }
        }
        flame = SphereNode(engine, radius = 0.06f, materialInstance = glowing(1f, 0.45f, 0.1f, 0.85f)).apply {
            position = Position(z = 0.34f)
            scale = Scale(1f, 1f, 2f)
        }
        glow = SphereNode(engine, radius = 0.5f, materialInstance = glowing(1f, 0.55f, 0.2f, 0.10f))

        listOf(body, nose, flame, glow).plus(fins).forEach {
            it.isTouchable = false
            it.isHittable = false
            root.addChildNode(it)
        }
    }

    /**
     * Flame flicker, plus the far-away glow: hidden when the camera is close, and growing with
     * distance so it stays a visible dot however far out the user zooms.
     */
    fun animate(seconds: Float, cameraDistance: Float) {
        val flicker = 1.6f + 0.5f * kotlin.math.sin(seconds * 23f) * kotlin.math.sin(seconds * 7f)
        flame.scale = Scale(1f, 1f, flicker)
        glow.isVisible = cameraDistance > GLOW_MIN_DISTANCE
        if (glow.isVisible) {
            val pulse = 1f + 0.12f * kotlin.math.sin(seconds * 3f)
            val s = (cameraDistance / GLOW_REFERENCE_DISTANCE).coerceAtLeast(0.6f) * pulse
            glow.scale = Scale(s, s, s)
        }
    }

    private companion object {
        const val GLOW_MIN_DISTANCE = 10f
        const val GLOW_REFERENCE_DISTANCE = 30f
    }
}
