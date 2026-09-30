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
 * - From afar: a small warm **beacon** that keeps about the same size on screen at any zoom,
 *   with a fading **comet tail** that follows the path already flown. Both are capped by the
 *   free space around them, so they never spill over a planet.
 * - Up close: a real low-poly rocket (Kenney "Space Kit", CC0) at a believable scale
 *   ([LENGTH] ≈ a quarter of the Moon's diameter, 1/15 of Earth's), with a small engine glow.
 * The two cross-fade with zoom: you see either the model or the beacon, never both at full.
 *
 * The root is driven by a lookTowards transform, so "forward" is −Z. The tail dots are placed
 * in world space (they bend along the arc), so they are separate nodes: add all of [nodes].
 */
class RocketModel(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    glowMaterial: Material,
) {
    val root = Node(engine).decorative()

    private val body: Node = Node(engine).decorative()
    private val parts = mutableListOf<ModelNode>()
    private val engineGlow: Node
    private val beacon: Node
    private val beaconMaterial: MaterialInstance
    private val tail: List<Node>
    private val tailMaterial: MaterialInstance

    /** Everything to add to the scene: the rocket itself plus its world-space tail dots. */
    val nodes: List<Node>

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
            parts += part
        }

        engineGlow = SphereNode(engine, radius = LENGTH * 0.14f, materialInstance = glow(1f, 0.45f, 0.1f, 0.9f, power = 3f)).apply {
            position = Position(z = LENGTH / 2f + LENGTH * 0.075f)
        }.decorative()

        beaconMaterial = glow(BEACON_COLOR.first, BEACON_COLOR.second, BEACON_COLOR.third, 1f, power = 3f)
        beacon = SphereNode(engine, radius = 1f, materialInstance = beaconMaterial).decorative()
        tailMaterial = glow(TAIL_COLOR.first, TAIL_COLOR.second, TAIL_COLOR.third, TAIL_ALPHA, power = 2.5f)
        tail = List(TAIL_SEGMENTS) { SphereNode(engine, radius = 1f, materialInstance = tailMaterial).decorative() }

        listOf(body, engineGlow, beacon).forEach(root::addChildNode)
        nodes = listOf(root) + tail
    }

    /**
     * Cross-fades between the close-up model and the far-away beacon, and keeps the beacon and
     * tail a steady size on screen by scaling them with the camera distance.
     *
     * @param clearance free space around the rocket (the beacon never grows past it).
     * @param trail points behind the rocket along the flown arc, each with its own clearance,
     *   given the wanted spacing and count (see [JourneyMap.FlightArc.pointsBehind]).
     */
    fun animate(
        seconds: Float,
        cameraDistance: Float,
        clearance: Float,
        trail: (spacing: Float, count: Int) -> List<Pair<SpacePoint, Float>>,
    ) {
        body.isVisible = cameraDistance < MODEL_HIDE_DISTANCE
        engineGlow.isVisible = body.isVisible

        // 0 up close → 1 far away (smoothstep), so the beacon fades in as the model gets tiny.
        val x = ((cameraDistance - BEACON_FADE_START) / (BEACON_FADE_END - BEACON_FADE_START)).coerceIn(0f, 1f)
        val fade = x * x * (3f - 2f * x)
        beacon.isVisible = fade > 0f
        val wanted = cameraDistance * BEACON_SCREEN_SIZE * (1f + 0.1f * kotlin.math.sin(seconds * 3f))
        val beaconRadius = minOf(wanted, clearance)
        beacon.scale = Scale(beaconRadius)
        beaconMaterial.setParameter("color", BEACON_COLOR.first, BEACON_COLOR.second, BEACON_COLOR.third, fade)
        tailMaterial.setParameter("color", TAIL_COLOR.first, TAIL_COLOR.second, TAIL_COLOR.third, TAIL_ALPHA * fade)

        val points = if (beacon.isVisible) trail(cameraDistance * TAIL_SPACING, TAIL_SEGMENTS) else emptyList()
        tail.forEachIndexed { i, node ->
            val point = points.getOrNull(i)
            node.isVisible = point != null
            if (point != null) {
                val (p, room) = point
                val shrink = 1f - i.toFloat() / TAIL_SEGMENTS
                node.position = Position(p.x, p.y, p.z)
                node.scale = Scale(minOf(wanted * 0.5f * shrink, room))
            }
        }
    }

    /**
     * Detaches the glTF model parts from the scene. Call it when the scene is closing, before the
     * model loader frees the models: a frame that still runs afterwards then can't reach them
     * (ModelNode.onFrame touches the native model and would crash on freed memory).
     */
    fun releaseModel() {
        parts.forEach(body::removeChildNode)
        parts.clear()
    }

    private fun <T : Node> T.decorative(): T = apply {
        isTouchable = false
        isHittable = false
    }

    companion object {
        /**
         * Rocket length in scene units: clearly smaller than every moon (Moon 0.32 across, Europa
         * 0.30, Earth 1.2). Real scale would be invisible; this reads as "tiny craft, huge worlds".
         */
        const val LENGTH = 0.08f

        private const val MODEL_HEIGHT = 4.1f
        private val PARTS = listOf(
            "rocket_baseA.glb" to 0f,
            "rocket_fuelB.glb" to 1f,
            "rocket_sidesB.glb" to 2f,
            "rocket_topB.glb" to 3f,
        )

        /** Camera distances: the model hides past MODEL_HIDE; the beacon fades in between FADE_START..END. */
        // Scaled with LENGTH: the model stays until it's only a few pixels long.
        private const val MODEL_HIDE_DISTANCE = 4.5f
        private const val BEACON_FADE_START = 3.5f
        private const val BEACON_FADE_END = 5.5f

        /** Beacon radius per unit of camera distance: about Moon-sized in the overview. */
        private const val BEACON_SCREEN_SIZE = 0.0025f
        private val BEACON_COLOR = Triple(1f, 0.62f, 0.28f)

        private const val TAIL_SEGMENTS = 12
        private const val TAIL_SPACING = 0.004f
        private const val TAIL_ALPHA = 0.3f
        private val TAIL_COLOR = Triple(1f, 0.5f, 0.15f)
    }
}
