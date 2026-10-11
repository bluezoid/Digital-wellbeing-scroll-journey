package com.achyutpal.scrolljourney.ui.space

import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager.PrimitiveType
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.UvCoordinate
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Custom meshes for the solar system, built from vertices (points) and indices (which points connect). */
object SpaceGeometry {

    /** A flat circle drawn as line segments in the XZ plane: an orbit path. */
    fun orbitCircle(engine: Engine, radius: Float, segments: Int = 160): Geometry {
        val vertices = (0 until segments).map { i ->
            val a = 2 * PI * i / segments
            Geometry.Vertex(position = Position((radius * cos(a)).toFloat(), 0f, (radius * sin(a)).toFloat()))
        }
        val indices = (0 until segments).flatMap { i -> listOf(i, (i + 1) % segments) }
        return Geometry.Builder(PrimitiveType.LINES)
            .vertices(vertices)
            .primitivesIndices(listOf(indices))
            .build(engine)
    }

    /**
     * A flat ring (annulus) in the XZ plane for Saturn. The ring texture is a radial strip,
     * so u runs from the inner edge (0) to the outer edge (1) and v runs around the ring.
     */
    fun ring(engine: Engine, inner: Float, outer: Float, segments: Int = 160): Geometry {
        val up = Direction(0f, 1f, 0f)
        val vertices = (0..segments).flatMap { i ->
            val a = 2 * PI * i / segments
            val c = cos(a).toFloat()
            val s = sin(a).toFloat()
            val v = i.toFloat() / segments
            listOf(
                Geometry.Vertex(position = Position(inner * c, 0f, inner * s), normal = up, uvCoordinate = UvCoordinate(0f, v)),
                Geometry.Vertex(position = Position(outer * c, 0f, outer * s), normal = up, uvCoordinate = UvCoordinate(1f, v)),
            )
        }
        val indices = (0 until segments).flatMap { i ->
            val a = i * 2
            listOf(a, a + 1, a + 2, a + 1, a + 3, a + 2)
        }
        return Geometry.Builder(PrimitiveType.TRIANGLES)
            .vertices(vertices)
            .primitivesIndices(listOf(indices))
            .build(engine)
    }

    /**
     * Many small spheres merged into one mesh (one draw call): with the additive glow material each
     * one is a soft dot, and together they read as a cloud of stars (the galaxy's arms and bulge).
     * [particles] = centre + radius of each dot.
     */
    fun glowCloud(engine: Engine, particles: List<Pair<SpacePoint, Float>>, stacks: Int = 5, slices: Int = 8): Geometry {
        val perParticle = (stacks + 1) * (slices + 1)
        val vertices = particles.flatMap { (c, size) ->
            (0..stacks).flatMap { i ->
                val phi = PI.toFloat() * i / stacks
                (0..slices).map { j ->
                    val theta = 2 * PI.toFloat() * j / slices
                    val n = Direction(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta))
                    Geometry.Vertex(position = Position(c.x + n.x * size, c.y + n.y * size, c.z + n.z * size), normal = n)
                }
            }
        }
        val indices = particles.indices.flatMap { p ->
            val base = p * perParticle
            (0 until stacks).flatMap { i ->
                (0 until slices).flatMap { j ->
                    val a = base + i * (slices + 1) + j
                    val b = a + slices + 1
                    listOf(a, a + 1, b, a + 1, b + 1, b)
                }
            }
        }
        return Geometry.Builder(PrimitiveType.TRIANGLES)
            .vertices(vertices)
            .primitivesIndices(listOf(indices))
            .build(engine)
    }
}
