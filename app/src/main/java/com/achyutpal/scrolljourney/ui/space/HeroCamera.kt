package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.RocketState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the dashboard hero's camera looks: a point between the rocket and its next stop, from a
 * distance that fits the whole current leg. Pure Kotlin: unit-tested.
 */
object HeroCamera {

    data class Framing(val target: SpacePoint, val distance: Float)

    fun frame(rocket: RocketState, seconds: Float): Framing {
        val position = JourneyMap.rocketPosition(rocket, seconds)
        val next = rocket.nextPlanet
            ?: return Framing(position, (JourneyMap.radius(rocket.highestReachedPlanet) * FINAL_RADII).coerceIn(MIN_DISTANCE, MAX_DISTANCE))
        val nextAnchor = JourneyMap.anchor(next, seconds)
        val legLength = JourneyMap.anchor(rocket.highestReachedPlanet, seconds).distanceTo(nextAnchor)
        val target = position + (nextAnchor - position) * TOWARD_NEXT
        return Framing(target, (legLength * LEG_FIT).coerceIn(MIN_DISTANCE, MAX_DISTANCE))
    }

    /** Camera position: on a sphere of the framing's distance around its target, at [yawDegrees] and a fixed pitch. */
    fun eye(framing: Framing, yawDegrees: Float, pitchDegrees: Float = PITCH_DEGREES): SpacePoint {
        val yaw = yawDegrees * DEG
        val pitch = pitchDegrees * DEG
        val d = framing.distance
        return framing.target + SpacePoint(d * cos(pitch) * sin(yaw), d * sin(pitch), d * cos(pitch) * cos(yaw))
    }

    /** How far the view centre leans from the rocket toward the next stop (0 = rocket, 1 = stop). */
    const val TOWARD_NEXT = 0.35f
    const val LEG_FIT = 1.6f
    const val MIN_DISTANCE = 3.4f
    const val MAX_DISTANCE = 60f
    private const val FINAL_RADII = 8f

    /** Slow drift around the scene, degrees per second. */
    const val DRIFT_DEGREES_PER_SECOND = 4f
    const val PITCH_DEGREES = 18f
    const val START_YAW_DEGREES = 20f
    private const val DEG = (PI / 180.0).toFloat()
}
