package com.achyutpal.scrolljourney.ui.space

import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

/**
 * The user's rocket, in the "mission control" style:
 * - From afar: a soft glowing **beacon** that keeps about the same size on screen at any zoom,
 *   with a fading **comet tail** behind it. Never oversized next to planets.
 * - Up close: a real low-poly rocket (Kenney "Space Kit", CC0) at a believable scale
 *   ([LENGTH] ≈ a third of Earth's radius), with a small engine glow.
 *
 * The root is driven by a lookTowards transform, so "forward" is −Z.
 */
class RocketModel(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    glowMaterial: Material,
) {
    val root = Node(engine).decorative()

    private val body: Node = Node(engine).decorative()
    private val engineGlow: Node
    private val beacon: Node
    private val tail: List<Node>

    init {
        fun glow(r: Float, g: Float, b: Float, a: Float, power: Float): MaterialInstance =
            materialLoader.createInstance(glowMaterial).apply {
                setParameter("color", r, g, b, a)
                setParameter("power", power)
                setParameter("rim", 0f)
                setParameter("intensity", 1f)
            }

        // Kenney parts stack along +Y with their bottoms at the origin (body top of the base = 1.0).
        // Model units → scene: the stacked rocket is MODEL_HEIGHT tall; scale it to LENGTH and centre it.
        body.rotation = Rotation(x = -90f) // model +Y (nose) → scene −Z (forward)
        val s = LENGTH / MODEL_HEIGHT
        body.scale = Scale(s, s, s)
        PARTS.forEach { (file, y) ->
            val part = ModelNode(
                modelInstance = modelLoader.createModelInstance("models/rocket/$file"),
                autoAnimate = false,
                centerOrigin = Position(0f, -1f, 0f), // bottom-centre at the node origin
            ).decorative()
            part.position = Position(y = y - MODEL_HEIGHT / 2f)
            body.addChildNode(part)
        }

        engineGlow = SphereNode(engine, radius = 0.028f, materialInstance = glow(1f, 0.45f, 0.1f, 0.9f, power = 3f)).apply {
            position = Position(z = LENGTH / 2f + 0.015f)
        }.decorative()

        beacon = SphereNode(engine, radius = 1f, materialInstance = glow(1f, 0.85f, 0.6f, 1f, power = 3f)).decorative()
        val tailMaterial = glow(1f, 0.5f, 0.15f, 0.35f, power = 2.5f)
        tail = List(TAIL_SEGMENTS) { SphereNode(engine, radius = 1f, materialInstance = tailMaterial).decorative() }

        (listOf(body, engineGlow, beacon) + tail).forEach(root::addChildNode)
    }

    /**
     * Switches between the close-up model and the far-away beacon, and keeps the beacon and
     * tail a steady size on screen by scaling them with the camera distance.
     */
    fun animate(seconds: Float, cameraDistance: Float) {
        val far = cameraDistance > MODEL_VISIBLE_DISTANCE
        body.isVisible = !far || cameraDistance < MODEL_VISIBLE_DISTANCE * 1.5f
        engineGlow.isVisible = !far

        val beaconRadius = cameraDistance * BEACON_SCREEN_SIZE * (1f + 0.1f * kotlin.math.sin(seconds * 3f))
        beacon.isVisible = cameraDistance > BEACON_MIN_DISTANCE
        beacon.scale = Scale(beaconRadius)

        val spacing = cameraDistance * TAIL_SPACING
        tail.forEachIndexed { i, node ->
            node.isVisible = beacon.isVisible
            val fade = 1f - i.toFloat() / TAIL_SEGMENTS
            node.position = Position(z = LENGTH / 2f + spacing * (i + 1)) // behind the rocket (+Z)
            node.scale = Scale(beaconRadius * 0.55f * fade)
        }
    }

    private fun <T : Node> T.decorative(): T = apply {
        isTouchable = false
        isHittable = false
    }

    companion object {
        /** Rocket length in scene units (Earth's radius is 0.6). */
        const val LENGTH = 0.2f

        private const val MODEL_HEIGHT = 4.1f
        private val PARTS = listOf(
            "rocket_baseA.glb" to 0f,
            "rocket_fuelB.glb" to 1f,
            "rocket_sidesB.glb" to 2f,
            "rocket_topB.glb" to 3f,
        )

        private const val MODEL_VISIBLE_DISTANCE = 8f
        private const val BEACON_MIN_DISTANCE = 4f
        private const val BEACON_SCREEN_SIZE = 0.008f
        private const val TAIL_SEGMENTS = 16
        private const val TAIL_SPACING = 0.0055f
    }
}
