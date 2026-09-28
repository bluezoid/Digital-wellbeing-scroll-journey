package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where each journey destination sits in the 3D scene, and where the rocket is.
 * Solar-system stops follow their (moving) body; stops beyond Pluto are fixed markers out in
 * deep space (still inside the starfield). Pure Kotlin: unit-tested.
 */
object JourneyMap {

    /** A deep-space stop drawn as a glowing marker (no texture). */
    data class DeepSpaceMarker(
        val destination: Destination,
        val position: SpacePoint,
        val radius: Float,
        /** Linear RGB, 0..1. */
        val color: Triple<Float, Float, Float>,
    )

    /** The solar-system body that represents a destination, or null for deep-space stops. */
    fun bodyFor(destination: Destination): SpaceBody? = when (destination) {
        Destination.EARTH -> SpaceBody.EARTH
        Destination.MOON -> SpaceBody.MOON
        Destination.MARS -> SpaceBody.MARS
        Destination.CERES -> SpaceBody.CERES
        Destination.JUPITER -> SpaceBody.JUPITER
        Destination.EUROPA -> SpaceBody.EUROPA
        Destination.SATURN -> SpaceBody.SATURN
        Destination.TITAN -> SpaceBody.TITAN
        Destination.URANUS -> SpaceBody.URANUS
        Destination.NEPTUNE -> SpaceBody.NEPTUNE
        Destination.PLUTO -> SpaceBody.PLUTO
        else -> null
    }

    fun destinationFor(body: SpaceBody): Destination? = Destination.entries.firstOrNull { bodyFor(it) == body }

    const val KUIPER_BELT_RADIUS = 54f
    const val OORT_CLOUD_RADIUS = 95f

    /** Fixed deep-space markers, angles spread so the route fans outward. */
    val deepSpace: List<DeepSpaceMarker> = listOf(
        marker(Destination.KUIPER_BELT, KUIPER_BELT_RADIUS, 120f, 0f, 0.35f, Triple(0.75f, 0.85f, 1f)),
        marker(Destination.OORT_CLOUD, OORT_CLOUD_RADIUS, 150f, 8f, 0.6f, Triple(0.6f, 0.8f, 1f)),
        marker(Destination.PROXIMA_CENTAURI, 165f, 165f, 18f, 1.6f, Triple(1f, 0.45f, 0.3f)),
        marker(Destination.ALPHA_CENTAURI, 180f, 172f, 22f, 2.2f, Triple(1f, 0.9f, 0.6f)),
        marker(Destination.SIRIUS, 250f, 200f, -30f, 2.8f, Triple(0.75f, 0.85f, 1f)),
        marker(Destination.VEGA, 320f, 228f, 55f, 2.8f, Triple(0.85f, 0.9f, 1f)),
        marker(Destination.GALACTIC_CENTRE, 420f, 255f, 25f, 9f, Triple(0.85f, 0.6f, 1f)),
    )

    private fun marker(d: Destination, dist: Float, angleDeg: Float, y: Float, radius: Float, color: Triple<Float, Float, Float>) =
        DeepSpaceMarker(d, polar(dist, angleDeg, y), radius, color)

    /** Centre of a destination at scene time [seconds]. */
    fun anchor(destination: Destination, seconds: Float): SpacePoint =
        bodyFor(destination)?.positionAt(seconds)
            ?: deepSpace.first { it.destination == destination }.position

    /** Visual radius of a destination (to keep the rocket and markers off its surface). */
    fun radius(destination: Destination): Float =
        bodyFor(destination)?.radius ?: deepSpace.first { it.destination == destination }.radius

    /**
     * The current leg as a smooth flight arc: a quadratic Bézier from just off the last reached
     * stop to just off the next one, lifted above the orbit plane by [ARC_LIFT] × its length.
     * Ends are pushed past the bodies' surfaces (plus a gap; short planet→moon legs shrink the
     * gap to a third of the free space) so nothing is drawn inside a planet.
     * Returns null at the final destination (no leg left).
     */
    fun leg(rocket: RocketState, seconds: Float): FlightArc? {
        val from = rocket.highestReachedPlanet
        val to = rocket.nextPlanet ?: return null
        val a = anchor(from, seconds)
        val b = anchor(to, seconds)
        val dir = direction(a, b)
        val free = a.distanceTo(b) - radius(from) - radius(to)
        val gap = minOf(GAP, free / 3f)
        val start = a + dir * (radius(from) + gap)
        val end = b - dir * (radius(to) + gap)
        val lift = start.distanceTo(end) * ARC_LIFT
        val control = lerp(start, end, 0.5f) + SpacePoint(0f, lift, 0f)
        return FlightArc(start, control, end)
    }

    /** Where the rocket is: on the flight arc at the fraction of the leg its fuel has covered. */
    fun rocketPosition(rocket: RocketState, seconds: Float): SpacePoint {
        val arc = leg(rocket, seconds)
            ?: return anchor(rocket.highestReachedPlanet, seconds) + SpacePoint(0f, radius(rocket.highestReachedPlanet) + GAP, 0f)
        return arc.pointAt(rocket.legProgress)
    }

    /** Unit direction the rocket is heading: along the arc (its tangent). */
    fun rocketHeading(rocket: RocketState, seconds: Float): SpacePoint =
        leg(rocket, seconds)?.tangentAt(rocket.legProgress) ?: SpacePoint(0f, 1f, 0f)

    /**
     * How much empty space is around [point]: its distance to the nearer surface of the current
     * leg's two stops (never negative). The rocket's glow is capped by this so it never spills
     * over the planet it just left or the one it's heading to.
     */
    fun clearance(rocket: RocketState, seconds: Float, point: SpacePoint): Float {
        val stops = listOfNotNull(rocket.highestReachedPlanet, rocket.nextPlanet)
        return stops.minOf { point.distanceTo(anchor(it, seconds)) - radius(it) }.coerceAtLeast(0f)
    }

    /** A quadratic Bézier curve: start → (pulled toward control) → end. */
    data class FlightArc(val start: SpacePoint, val control: SpacePoint, val end: SpacePoint) {
        fun pointAt(u: Float): SpacePoint {
            val v = 1f - u
            return start * (v * v) + control * (2f * v * u) + end * (u * u)
        }

        fun tangentAt(u: Float): SpacePoint = direction(
            SpacePoint.ORIGIN,
            (control - start) * (2f * (1f - u)) + (end - control) * (2f * u),
        )

        /** [count] + 1 evenly spaced points from u = [from] to u = [to]. */
        fun sample(from: Float, to: Float, count: Int): List<SpacePoint> =
            (0..count).map { i -> pointAt(from + (to - from) * i / count) }

        /**
         * Up to [count] points behind u = [from], each [spacing] further back *along the curve*
         * (for the comet tail). Stops at the start of the arc, so the tail is never longer than
         * the distance flown and never reaches back into the launch planet.
         */
        fun pointsBehind(from: Float, spacing: Float, count: Int): List<SpacePoint> {
            val result = mutableListOf<SpacePoint>()
            if (spacing <= 0f) return result
            var u = from
            var prev = pointAt(u)
            var travelled = 0f
            while (u > 0f && result.size < count) {
                val nextU = maxOf(0f, u - STEP)
                val next = pointAt(nextU)
                val step = prev.distanceTo(next)
                // Drop a point at each multiple of spacing that falls inside this small step.
                while (result.size < count && travelled + step >= spacing * (result.size + 1)) {
                    val k = if (step == 0f) 0f else (spacing * (result.size + 1) - travelled) / step
                    result += prev + (next - prev) * k
                }
                travelled += step
                prev = next
                u = nextU
            }
            return result
        }

        private companion object {
            const val STEP = 1f / 256
        }
    }

    const val GAP = 0.6f
    const val ARC_LIFT = 0.18f

    private fun polar(dist: Float, angleDeg: Float, y: Float): SpacePoint {
        val a = Math.toRadians(angleDeg.toDouble())
        return SpacePoint((dist * cos(a)).toFloat(), y, (dist * sin(a)).toFloat())
    }

    internal fun direction(a: SpacePoint, b: SpacePoint): SpacePoint {
        val d = b - a
        val len = sqrt(d.x * d.x + d.y * d.y + d.z * d.z)
        return if (len == 0f) SpacePoint(1f, 0f, 0f) else d * (1f / len)
    }

    private fun lerp(a: SpacePoint, b: SpacePoint, t: Float) = a + (b - a) * t
}
