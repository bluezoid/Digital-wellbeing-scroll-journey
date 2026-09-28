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
     * The rocket sits on the straight path from the last reached stop to the next one, at the
     * fraction of the leg its fuel has covered. Both ends are pushed out past the bodies' surfaces
     * (plus a gap) so the rocket is never drawn inside a planet. Short legs (a planet to its own moon)
     * shrink the gap to a third of the free space. At the final stop it parks beside it.
     */
    fun rocketPosition(rocket: RocketState, seconds: Float): SpacePoint {
        val from = rocket.highestReachedPlanet
        val to = rocket.nextPlanet ?: return anchor(from, seconds) + SpacePoint(0f, radius(from) + GAP, 0f)
        val a = anchor(from, seconds)
        val b = anchor(to, seconds)
        val dir = direction(a, b)
        val free = a.distanceTo(b) - radius(from) - radius(to)
        val gap = minOf(GAP, free / 3f)
        val start = a + dir * (radius(from) + gap)
        val end = b - dir * (radius(to) + gap)
        return lerp(start, end, rocket.legProgress)
    }

    /** Unit direction the rocket is heading (toward the next stop). */
    fun rocketHeading(rocket: RocketState, seconds: Float): SpacePoint {
        val to = rocket.nextPlanet ?: return SpacePoint(0f, 1f, 0f)
        return direction(anchor(rocket.highestReachedPlanet, seconds), anchor(to, seconds))
    }

    /** Stops already reached, in order: the travelled route drawn behind the rocket. */
    fun reachedRoute(rocket: RocketState): List<Destination> =
        Destination.entries.filter { it <= rocket.highestReachedPlanet }

    /** The next few stops ahead of the rocket: the upcoming route drawn faintly. */
    fun upcomingRoute(rocket: RocketState, count: Int = UPCOMING_STOPS): List<Destination> =
        Destination.entries.filter { it > rocket.highestReachedPlanet }.take(count)

    const val GAP = 0.6f
    const val UPCOMING_STOPS = 3

    private fun polar(dist: Float, angleDeg: Float, y: Float): SpacePoint {
        val a = Math.toRadians(angleDeg.toDouble())
        return SpacePoint((dist * cos(a)).toFloat(), y, (dist * sin(a)).toFloat())
    }

    private fun direction(a: SpacePoint, b: SpacePoint): SpacePoint {
        val d = b - a
        val len = sqrt(d.x * d.x + d.y * d.y + d.z * d.z)
        return if (len == 0f) SpacePoint(1f, 0f, 0f) else d * (1f / len)
    }

    private fun lerp(a: SpacePoint, b: SpacePoint, t: Float) = a + (b - a) * t
}
