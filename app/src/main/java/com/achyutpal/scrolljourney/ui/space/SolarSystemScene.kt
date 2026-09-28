package com.achyutpal.scrolljourney.ui.space

import android.content.res.AssetManager
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.RenderableManager.PrimitiveType
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookTowards
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.texture.ImageTexture
import kotlin.math.sin

/**
 * Builds every 3D object once and animates them each frame:
 * - the solar system (Sun + point light, planets, moons, Saturn's ring, orbit paths, starfield)
 * - deep-space stops (Kuiper Belt / Oort Cloud rings and glowing star markers)
 * - the journey ("mission control" style): the rocket (beacon + comet tail from afar, real model
 *   up close) on a curved flight arc for the current leg only (solid behind the rocket, dashed
 *   ahead), a soft pulsing rim ring on the next stop and a faint green ring on reached stops.
 */
class SolarSystemScene(engine: Engine, materialLoader: MaterialLoader, modelLoader: ModelLoader, assets: AssetManager) {

    private fun texture(engine: Engine, assets: AssetManager, name: String) =
        ImageTexture.Builder().bitmap(assets, "textures/$name").build(engine)

    private val unlitTextured: Material = materialLoader.createMaterial("materials/space_unlit_textured.filamat")
    private val unlitColor: Material = materialLoader.createMaterial("materials/space_unlit_color.filamat")
    private val glowMaterial: Material = materialLoader.createMaterial("materials/space_glow.filamat")

    /** Soft additive glow (see materials/space_glow.mat): rim = false → bright core, true → bright edge. */
    private fun softGlow(materialLoader: MaterialLoader, r: Float, g: Float, b: Float, a: Float, power: Float, rim: Boolean) =
        materialLoader.createInstance(glowMaterial).apply {
            setParameter("color", r, g, b, a)
            setParameter("power", power)
            setParameter("rim", if (rim) 1f else 0f)
            setParameter("intensity", 1f)
        }

    private fun glowingTexture(materialLoader: MaterialLoader, tex: com.google.android.filament.Texture, intensity: Float) =
        materialLoader.createInstance(unlitTextured).apply {
            setTexture("baseTexture", tex)
            setParameter("intensity", intensity)
        }

    private fun flatColor(materialLoader: MaterialLoader, r: Float, g: Float, b: Float, a: Float) =
        materialLoader.createInstance(unlitColor).apply {
            setParameter("color", r, g, b, a)
            setParameter("intensity", 1f)
        }

    private fun <T : Node> T.decorative(): T = apply {
        isTouchable = false
        isHittable = false
    }

    private val stars = SphereNode(
        engine = engine,
        radius = SpaceBody.STARFIELD_RADIUS,
        stacks = 32,
        slices = 64,
        materialInstance = glowingTexture(materialLoader, texture(engine, assets, "stars.jpg"), 0.8f),
    ).decorative() // the camera is inside this sphere: it must never catch taps

    val bodies: Map<SpaceBody, SphereNode> = SpaceBody.entries.associateWith { body ->
        val tex = texture(engine, assets, "${body.texture}.jpg")
        SphereNode(
            engine = engine,
            radius = body.radius,
            stacks = if (body.radius > 1f) 48 else 32,
            slices = if (body.radius > 1f) 96 else 64,
            // One materialInstance for the whole sphere (a list would only cover the first stack).
            materialInstance = if (body == SpaceBody.SUN) {
                glowingTexture(materialLoader, tex, 1.4f)
            } else {
                materialLoader.createTextureInstance(tex, roughness = 1f)
            },
        )
    }

    private val saturnRing = GeometryNode(
        engine = engine,
        geometry = SpaceGeometry.ring(engine, SpaceBody.SATURN_RING_INNER, SpaceBody.SATURN_RING_OUTER),
        materialInstance = materialLoader.createTextureInstance(texture(engine, assets, "saturn_ring.png"), isOpaque = false).apply {
            setCullingMode(Material.CullingMode.NONE) // visible from above and below
        },
    )

    private val orbits: List<Node> = run {
        val orbitMaterial = flatColor(materialLoader, 0.75f, 0.8f, 1f, 0.22f)
        val beltMaterial = flatColor(materialLoader, 0.7f, 0.85f, 1f, 0.12f)
        SpaceBody.sunOrbiters.map { GeometryNode(engine, SpaceGeometry.orbitCircle(engine, it.orbitRadius), orbitMaterial).decorative() } +
            GeometryNode(engine, SpaceGeometry.orbitCircle(engine, JourneyMap.KUIPER_BELT_RADIUS, 220), beltMaterial).decorative() +
            GeometryNode(engine, SpaceGeometry.orbitCircle(engine, JourneyMap.OORT_CLOUD_RADIUS, 260), beltMaterial).decorative()
    }

    /** Deep-space stops: glowing spheres (no texture) at fixed spots, each with a soft light halo. */
    val markers: Map<Destination, SphereNode> = JourneyMap.deepSpace.associate { m ->
        val (r, g, b) = m.color
        m.destination to SphereNode(engine, radius = m.radius, materialInstance = flatColor(materialLoader, r, g, b, 1f)).apply {
            position = Position(m.position.x, m.position.y, m.position.z)
        }
    }
    private val markerHalos: List<Node> = JourneyMap.deepSpace.map { m ->
        val (r, g, b) = m.color
        SphereNode(engine, radius = m.radius * 3.5f, materialInstance = softGlow(materialLoader, r, g, b, 0.9f, power = 4f, rim = false)).apply {
            position = Position(m.position.x, m.position.y, m.position.z)
        }.decorative()
    }

    private val sunlight = LightNode(engine, LightManager.Type.POINT) {
        color(1f, 0.96f, 0.9f)
        intensity(3.0e8f) // lumens (tuned for SceneView's "sunny 16" camera exposure)
        falloff(200f)
        castShadows(false)
    }

    // --- Journey layer ---------------------------------------------------------------------

    val rocket = RocketModel(engine, modelLoader, materialLoader, glowMaterial)

    /** Faint green rim ring around every reached stop. */
    private val reachedRings: Map<Destination, Node> = Destination.entries.associateWith { d ->
        SphereNode(
            engine = engine,
            radius = JourneyMap.radius(d) * 1.06f,
            materialInstance = softGlow(materialLoader, 0.35f, 1f, 0.6f, 0.22f, power = 12f, rim = true),
        ).decorative()
    }

    /** Thin pulsing rim ring around the next stop (transparent in the middle: the planet stays visible). */
    private val nextGlow = SphereNode(
        engine = engine,
        radius = 1f,
        materialInstance = softGlow(materialLoader, 1f, 0.6f, 0.2f, 0.85f, power = 12f, rim = true),
    ).decorative()

    /** The flight arc of the current leg: solid behind the rocket, dashed and faint ahead of it. */
    private val arcTravelled = lineNode(engine, flatColor(materialLoader, 1f, 0.62f, 0.25f, 0.95f), segments = ARC_SEGMENTS)
    private val arcAhead = lineNode(engine, flatColor(materialLoader, 1f, 0.85f, 0.65f, 0.5f), segments = ARC_DASHES)

    /**
     * The initial points span the whole scene: Filament refuses an empty bounding box, and the box
     * from creation is what culling uses, so it must cover everywhere the route can go.
     */
    private fun lineNode(engine: Engine, material: com.google.android.filament.MaterialInstance, segments: Int): GeometryNode {
        val extent = SpaceBody.STARFIELD_RADIUS
        val vertices = List(segments * 2) { i ->
            val s = if (i % 2 == 0) -extent else extent
            Geometry.Vertex(position = Position(s, s, s))
        }
        val indices = List(segments * 2) { it }
        val geometry = Geometry.Builder(PrimitiveType.LINES).vertices(vertices).primitivesIndices(listOf(indices)).build(engine)
        return GeometryNode(engine, geometry, material).decorative()
    }

    val all: List<Node> = listOf(stars, sunlight) + orbits + bodies.values + saturnRing + markers.values + markerHalos +
        reachedRings.values + nextGlow + arcTravelled + arcAhead + rocket.nodes

    private val bodyByNode: Map<Node, SpaceBody> = bodies.entries.associate { (body, node) -> node to body } +
        (saturnRing to SpaceBody.SATURN)
    private val markerByNode: Map<Node, Destination> = markers.entries.associate { (d, node) -> node to d }

    /** What a tapped node is (the ring counts as Saturn). */
    fun focusFor(node: Node?): SpaceFocus? = node?.let {
        bodyByNode[it]?.let(SpaceFocus::Body) ?: markerByNode[it]?.let(SpaceFocus::Marker)
    }

    fun update(seconds: Float, journey: RocketState, cameraEye: SpacePoint) {
        bodies.forEach { (body, node) ->
            val p = body.positionAt(seconds)
            node.position = Position(p.x, p.y, p.z)
            node.rotation = Rotation(y = body.spinAt(seconds))
        }
        val saturn = SpaceBody.SATURN.positionAt(seconds)
        saturnRing.position = Position(saturn.x, saturn.y, saturn.z)
        saturnRing.rotation = Rotation(x = SpaceBody.SATURN_RING_TILT_DEGREES)

        updateJourney(seconds, journey, cameraEye)
    }

    private fun updateJourney(seconds: Float, journey: RocketState, cameraEye: SpacePoint) {
        // Rocket: placed on the current leg, pointing at the next stop.
        val p = JourneyMap.rocketPosition(journey, seconds)
        val h = JourneyMap.rocketHeading(journey, seconds)
        rocket.root.transform = lookTowards(Float3(p.x, p.y, p.z), Float3(h.x, h.y, h.z), Float3(0f, 1f, 0f))
        val arc = JourneyMap.leg(journey, seconds)
        rocket.animate(
            seconds,
            cameraDistance = cameraEye.distanceTo(p),
            clearance = JourneyMap.clearance(journey, seconds, p),
        ) { spacing, count ->
            arc?.pointsBehind(journey.legProgress, spacing, count)
                ?.map { it to JourneyMap.clearance(journey, seconds, it) }
                .orEmpty()
        }

        // Faint rings on reached stops; a soft pulsing rim glow around the next stop.
        reachedRings.forEach { (d, node) ->
            node.isVisible = d <= journey.highestReachedPlanet
            if (node.isVisible) node.position = JourneyMap.anchor(d, seconds).toPosition()
        }
        val next = journey.nextPlanet
        nextGlow.isVisible = next != null
        if (next != null) {
            val pulse = 1f + 0.03f * sin(seconds * 3f)
            val s = JourneyMap.radius(next) * 1.08f * pulse
            nextGlow.position = JourneyMap.anchor(next, seconds).toPosition()
            nextGlow.scale = Scale(s, s, s)
        }

        // Flight arc for the current leg only (nothing is drawn for past legs).
        arcTravelled.isVisible = arc != null
        arcAhead.isVisible = arc != null
        if (arc != null) {
            val u = journey.legProgress
            arcTravelled.updateGeometry(vertices = polyline(arc.sample(0f, u, ARC_SEGMENTS)))
            arcAhead.updateGeometry(vertices = dashes(arc.sample(u, 1f, ARC_DASHES * 2)))
        }
    }

    /** Consecutive points as LINES pairs: a continuous line. */
    private fun polyline(points: List<SpacePoint>): List<Geometry.Vertex> =
        points.zipWithNext().flatMap { (a, b) -> listOf(Geometry.Vertex(position = a.toPosition()), Geometry.Vertex(position = b.toPosition())) }

    /** Every other segment only: a dashed line (2 × dashes + 1 points → dashes pairs). */
    private fun dashes(points: List<SpacePoint>): List<Geometry.Vertex> =
        (0 until points.size / 2).flatMap { i ->
            listOf(Geometry.Vertex(position = points[2 * i].toPosition()), Geometry.Vertex(position = points[2 * i + 1].toPosition()))
        }

    private fun SpacePoint.toPosition() = Position(x, y, z)

    private companion object {
        const val ARC_SEGMENTS = 40
        const val ARC_DASHES = 20
    }
}
/** What the camera is centred on. */
sealed interface SpaceFocus {
    data object Overview : SpaceFocus
    data class Body(val body: SpaceBody) : SpaceFocus
    data class Marker(val destination: Destination) : SpaceFocus
    data object Rocket : SpaceFocus
}
