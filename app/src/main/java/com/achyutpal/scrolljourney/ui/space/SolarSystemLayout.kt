package com.achyutpal.scrolljourney.ui.space

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in scene units (1 unit ≈ Earth's radius × 1.7 in this stylised model). */
data class SpacePoint(val x: Float, val y: Float, val z: Float) {
    fun distanceTo(o: SpacePoint): Float {
        val dx = x - o.x
        val dy = y - o.y
        val dz = z - o.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(o: SpacePoint) = SpacePoint(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: SpacePoint) = SpacePoint(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = SpacePoint(x * k, y * k, z * k)

    companion object {
        val ORIGIN = SpacePoint(0f, 0f, 0f)
    }
}

/**
 * The stylised solar system (NOT to scale: at real scale planets would be invisible dots).
 * Distances are compressed and sizes balanced so everything fits on a phone screen.
 * Orbits are flat circles in the XZ plane; periods are in seconds so the scene gently moves.
 *
 * Pure Kotlin (no Android / 3D engine) so positions are unit-tested.
 */
enum class SpaceBody(
    val texture: String,
    val radius: Float,
    val orbitRadius: Float,
    val orbitPeriodSeconds: Float,
    val startAngleDegrees: Float,
    val spinPeriodSeconds: Float,
) {
    SUN("sun", 3.5f, 0f, 0f, 0f, 90f),
    MERCURY("mercury", 0.30f, 6.5f, 44f, 20f, 40f),
    VENUS("venus", 0.55f, 9.0f, 70f, 110f, 60f),
    EARTH("earth", 0.60f, 12.0f, 110f, 200f, 24f),
    MOON("moon", 0.16f, 1.3f, 16f, 0f, 16f),
    MARS("mars", 0.40f, 15.5f, 160f, 290f, 25f),
    CERES("ceres", 0.18f, 18.5f, 220f, 40f, 20f),
    JUPITER("jupiter", 1.70f, 24.0f, 320f, 150f, 12f),
    EUROPA("moon", 0.15f, 2.6f, 20f, 90f, 20f),
    SATURN("saturn", 1.40f, 31.0f, 420f, 250f, 13f),
    TITAN("venus", 0.20f, 4.0f, 30f, 200f, 30f),
    URANUS("uranus", 0.90f, 37.0f, 520f, 330f, 18f),
    NEPTUNE("neptune", 0.90f, 42.0f, 600f, 70f, 18f),
    PLUTO("pluto", 0.22f, 46.0f, 700f, 180f, 30f),
    ;

    /** Moons orbit their planet; everything else orbits the Sun. */
    val parent: SpaceBody?
        get() = when (this) {
            MOON -> EARTH
            EUROPA -> JUPITER
            TITAN -> SATURN
            else -> null
        }

    /** Where this body is [seconds] after the scene started. */
    fun positionAt(seconds: Float): SpacePoint {
        val centre = parent?.positionAt(seconds) ?: SpacePoint.ORIGIN
        if (orbitRadius == 0f) return centre
        val angle = Math.toRadians((startAngleDegrees + 360f * seconds / orbitPeriodSeconds).toDouble())
        return centre + SpacePoint(orbitRadius * cos(angle).toFloat(), 0f, orbitRadius * sin(angle).toFloat())
    }

    /** Spin angle in degrees around the body's own axis. */
    fun spinAt(seconds: Float): Float = (360f * seconds / spinPeriodSeconds) % 360f

    companion object {
        const val SATURN_RING_INNER = 1.8f
        const val SATURN_RING_OUTER = 3.3f
        const val SATURN_RING_TILT_DEGREES = 27f
        /** The star background sphere. Far bigger than the furthest stop (≈ 430) so the camera can
         *  never reach its edge, even when looking at the Galactic Centre from the far side. */
        const val STARFIELD_RADIUS = 3000f

        /** Camera draw range (SceneView's default far plane is only 1000). */
        const val CAMERA_NEAR = 0.02f
        const val CAMERA_FAR = 10_000f

        /** Bodies that orbit the Sun directly (get an orbit path drawn). */
        val sunOrbiters: List<SpaceBody> get() = entries.filter { it != SUN && it.parent == null }
    }
}
