package com.achyutpal.scrolljourney.ui.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitCameraStateTest {

    private fun settle(state: OrbitCameraState, centre: SpacePoint, seconds: Float = 5f) {
        repeat((seconds / 0.016f).toInt()) { state.advance(0.016f, centre) }
    }

    @Test
    fun `starts in the overview looking at the Sun from above`() {
        val c = OrbitCameraState()
        assertEquals(SpacePoint.ORIGIN, c.target)
        assertEquals(OrbitCameraState.OVERVIEW_DISTANCE, c.eye().distanceTo(c.target), 0.01f)
        assertTrue(c.eye().y > 0f)
    }

    @Test
    fun `focusing a planet flies to it and follows it`() {
        val c = OrbitCameraState()
        val earth = SpaceBody.EARTH
        c.focus(earth.radius, overview = false)
        settle(c, earth.positionAt(0f))

        assertEquals(0f, c.target.distanceTo(earth.positionAt(0f)), 0.05f)
        assertEquals(earth.radius * OrbitCameraState.FOCUS_DISTANCE_RADII, c.distance, 0.05f)

        // Earth moves along its orbit: the camera keeps it centred.
        settle(c, earth.positionAt(10f))
        assertEquals(0f, c.target.distanceTo(earth.positionAt(10f)), 0.05f)
    }

    @Test
    fun `zoom is clamped so the camera never enters the focused planet`() {
        val c = OrbitCameraState()
        val jupiter = SpaceBody.JUPITER
        c.focus(jupiter.radius, overview = false)
        repeat(50) { c.zoom(prevSeparation = 100f, currSeparation = 200f) } // pinch out hard = zoom in
        settle(c, jupiter.positionAt(0f))
        assertEquals(jupiter.radius * OrbitCameraState.MIN_DISTANCE_RADII, c.distance, 0.05f)
        assertTrue(c.distance > jupiter.radius)
    }

    @Test
    fun `zoom out is clamped to the maximum distance`() {
        val c = OrbitCameraState()
        repeat(50) { c.zoom(prevSeparation = 200f, currSeparation = 100f) }
        settle(c, SpacePoint.ORIGIN)
        assertEquals(OrbitCameraState.MAX_DISTANCE, c.distance, 0.1f)
    }

    @Test
    fun `back to overview restores Sun distance limits`() {
        val c = OrbitCameraState()
        c.focus(SpaceBody.MOON.radius, overview = false)
        c.focus(SpaceBody.SUN.radius, overview = true)
        assertEquals(OrbitCameraState.SUN_MIN_DISTANCE, c.minDistance, 0.001f)
        assertEquals(OrbitCameraState.OVERVIEW_DISTANCE, c.desiredDistance, 0.001f)
    }

    @Test
    fun `pitch is clamped so the view never flips`() {
        val c = OrbitCameraState()
        c.orbit(0f, -100_000f)
        assertEquals(OrbitCameraState.MAX_PITCH, c.pitchDegrees, 0.001f)
        c.orbit(0f, 100_000f)
        assertEquals(-OrbitCameraState.MAX_PITCH, c.pitchDegrees, 0.001f)
    }

    @Test
    fun `yaw rotates freely all the way around`() {
        val c = OrbitCameraState()
        val before = c.eye()
        c.orbit(-360f / OrbitCameraState.ORBIT_DEGREES_PER_PIXEL, 0f) // one full turn
        assertEquals(0f, before.distanceTo(c.eye()), 0.01f)
    }

    @Test
    fun `viewFrom places the camera in that direction from the target`() {
        val c = OrbitCameraState()
        c.focus(radius = 0.2f, overview = false, viewFrom = SpacePoint(1f, 0f, 0f))
        settle(c, SpacePoint.ORIGIN)
        val eye = c.eye()
        assertTrue(eye.x > 0.9f * c.distance)
        assertEquals(0f, eye.y, 0.01f)
        assertEquals(0f, eye.z, 0.01f)
    }

    @Test
    fun `a tiny target can settle closer than the planet minimum`() {
        val c = OrbitCameraState()
        c.focus(radius = 0.12f, overview = false)
        assertEquals(OrbitCameraState.MIN_FOCUS_DISTANCE, c.desiredDistance, 1e-4f) // planets: default minimum

        c.focus(radius = 0.12f, overview = false, minFocusDistance = 0.72f)
        assertEquals(0.72f, c.desiredDistance, 1e-4f)
    }

    @Test
    fun `deep-space stops are viewed from outside their glow and can't be pinched inside`() {
        val c = OrbitCameraState()
        val extent = JourneyMap.visualExtent(com.achyutpal.scrolljourney.domain.model.Destination.GALACTIC_CENTRE)
        c.focusOnExtent(extent)
        assertTrue(c.desiredDistance > extent * 2f)
        assertTrue(c.minDistance > extent) // pinch-zoom stops before entering the glow
        assertTrue(c.maxDistance >= OrbitCameraState.MAX_DISTANCE)
    }
}

