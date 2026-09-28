package com.achyutpal.scrolljourney.ui.space

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Camera that orbits a focus point (the Sun, or a planet the user tapped) and follows it as it moves.
 *
 * - yaw/pitch: angles around the focus (drag to change); pitch is clamped so you can't flip over.
 * - distance: how far from the focus (pinch to change), clamped per focus so you can't enter a planet.
 * - Focusing a new body flies there smoothly: target and distance ease toward their goals each frame.
 *
 * Pure Kotlin so the maths is unit-tested; FocusOrbitManipulator adapts it to SceneView gestures.
 */
class OrbitCameraState(
    var yawDegrees: Float = 0f,
    var pitchDegrees: Float = OVERVIEW_PITCH,
    distance: Float = OVERVIEW_DISTANCE,
) {
    /** Current (animated) values. */
    var distance: Float = distance
        private set
    var target: SpacePoint = SpacePoint.ORIGIN
        private set

    /** Goal values the animation eases toward. */
    var desiredDistance: Float = distance
        private set
    var minDistance: Float = SUN_MIN_DISTANCE
        private set
    var maxDistance: Float = MAX_DISTANCE
        private set

    /** Extra shift from two-finger panning, relative to the focus. */
    private var panOffset: SpacePoint = SpacePoint.ORIGIN

    /** Drag: rotate around the focus. Pixel deltas; y grows upward. */
    fun orbit(dxPixels: Float, dyPixels: Float) {
        yawDegrees -= dxPixels * ORBIT_DEGREES_PER_PIXEL
        pitchDegrees = (pitchDegrees - dyPixels * ORBIT_DEGREES_PER_PIXEL).coerceIn(-MAX_PITCH, MAX_PITCH)
    }

    /** Pinch: fingers apart (curr > prev) zooms in. */
    fun zoom(prevSeparation: Float, currSeparation: Float) {
        if (prevSeparation <= 0f || currSeparation <= 0f) return
        desiredDistance = (desiredDistance * prevSeparation / currSeparation).coerceIn(minDistance, maxDistance)
    }

    /** Two-finger drag: slide the view sideways/up-down, scaled so it feels the same at any zoom. */
    fun pan(dxPixels: Float, dyPixels: Float) {
        val (right, up) = basis()
        val k = distance * PAN_PER_PIXEL
        panOffset = SpacePoint(
            panOffset.x - (right.x * dxPixels + up.x * dyPixels) * k,
            panOffset.y - (right.y * dxPixels + up.y * dyPixels) * k,
            panOffset.z - (right.z * dxPixels + up.z * dyPixels) * k,
        )
    }

    /** Fly to a body of [radius]; [overview] = the whole-system view around the Sun. */
    fun focus(radius: Float, overview: Boolean) {
        panOffset = SpacePoint.ORIGIN
        if (overview) {
            minDistance = SUN_MIN_DISTANCE
            desiredDistance = OVERVIEW_DISTANCE
            pitchDegrees = OVERVIEW_PITCH
        } else {
            minDistance = radius * MIN_DISTANCE_RADII
            desiredDistance = (radius * FOCUS_DISTANCE_RADII).coerceAtLeast(MIN_FOCUS_DISTANCE)
        }
        maxDistance = MAX_DISTANCE
    }

    /** Called every frame: ease toward the (possibly moving) focus centre and the wanted distance. */
    fun advance(deltaSeconds: Float, focusCentre: SpacePoint) {
        val t = 1f - exp(-EASE_PER_SECOND * deltaSeconds.coerceIn(0f, 0.1f))
        val goal = focusCentre + panOffset
        target = SpacePoint(
            target.x + (goal.x - target.x) * t,
            target.y + (goal.y - target.y) * t,
            target.z + (goal.z - target.z) * t,
        )
        distance += (desiredDistance - distance) * t
    }

    /** Where the camera sits: on a sphere of [distance] around [target]. */
    fun eye(): SpacePoint {
        val yaw = yawDegrees * DEG
        val pitch = pitchDegrees * DEG
        return SpacePoint(
            target.x + distance * cos(pitch) * sin(yaw),
            target.y + distance * sin(pitch),
            target.z + distance * cos(pitch) * cos(yaw),
        )
    }

    /** Camera right and up directions (for panning). */
    private fun basis(): Pair<SpacePoint, SpacePoint> {
        val yaw = yawDegrees * DEG
        val pitch = pitchDegrees * DEG
        val right = SpacePoint(cos(yaw), 0f, -sin(yaw))
        val up = SpacePoint(-sin(pitch) * sin(yaw), cos(pitch), -sin(pitch) * cos(yaw))
        return right to up
    }

    companion object {
        const val OVERVIEW_DISTANCE = 67f
        const val OVERVIEW_PITCH = 23f
        const val SUN_MIN_DISTANCE = 7f
        const val MAX_DISTANCE = 140f
        const val MAX_PITCH = 85f
        const val MIN_DISTANCE_RADII = 1.8f
        const val FOCUS_DISTANCE_RADII = 6f
        const val MIN_FOCUS_DISTANCE = 1.6f
        const val ORBIT_DEGREES_PER_PIXEL = 0.25f
        const val PAN_PER_PIXEL = 0.0015f
        const val EASE_PER_SECOND = 5f
        private const val DEG = (PI / 180.0).toFloat()

        fun length(p: SpacePoint) = sqrt(p.x * p.x + p.y * p.y + p.z * p.z)
    }
}
