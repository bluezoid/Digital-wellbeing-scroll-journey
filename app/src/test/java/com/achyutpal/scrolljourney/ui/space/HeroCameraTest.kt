package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroCameraTest {

    private val t = 9f

    @Test
    fun `view centre sits between the rocket and its next stop`() {
        val rocket = RocketState(currentFuel = 25, highestReachedPlanet = Destination.CERES) // Ceres → Jupiter
        val framing = HeroCamera.frame(rocket, t)
        val position = JourneyMap.rocketPosition(rocket, t)
        val next = JourneyMap.anchor(Destination.JUPITER, t)
        val expected = position + (next - position) * HeroCamera.TOWARD_NEXT
        assertEquals(0f, framing.target.distanceTo(expected), 1e-4f)
    }

    @Test
    fun `longer legs are framed from farther away, within limits`() {
        val earthMoon = HeroCamera.frame(RocketState(), t)
        val plutoKuiper = HeroCamera.frame(RocketState(currentFuel = 140, highestReachedPlanet = Destination.PLUTO), t)
        assertEquals(HeroCamera.MIN_DISTANCE, earthMoon.distance, 0f) // the short Earth → Moon leg hits the minimum
        assertTrue(plutoKuiper.distance > earthMoon.distance)
        assertTrue(plutoKuiper.distance <= HeroCamera.MAX_DISTANCE)
    }

    @Test
    fun `after the final stop the camera looks at the parked rocket`() {
        val done = RocketState(currentFuel = 1_000, highestReachedPlanet = Destination.GALACTIC_CENTRE)
        val framing = HeroCamera.frame(done, t)
        assertEquals(0f, framing.target.distanceTo(JourneyMap.rocketPosition(done, t)), 1e-4f)
    }

    @Test
    fun `eye is at the framing distance from the target`() {
        val framing = HeroCamera.frame(RocketState(currentFuel = 5, highestReachedPlanet = Destination.MOON), t)
        listOf(0f, 90f, 200f).forEach { yaw ->
            val eye = HeroCamera.eye(framing, yaw)
            assertEquals(framing.distance, eye.distanceTo(framing.target), 1e-3f)
            assertTrue(eye.y > framing.target.y) // looking slightly down on the orbit plane
        }
    }
}
