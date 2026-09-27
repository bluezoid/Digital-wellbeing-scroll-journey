package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.RocketState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RocketProgressCalculatorTest {

    @Test
    fun `spec 58 - planet for fuel`() {
        val expected = mapOf(
            0 to Planet.EARTH, 2 to Planet.EARTH,
            3 to Planet.MOON, 6 to Planet.MOON,
            7 to Planet.MARS, 13 to Planet.MARS,
            14 to Planet.JUPITER, 20 to Planet.JUPITER,
            21 to Planet.SATURN, 29 to Planet.SATURN,
            30 to Planet.URANUS, 44 to Planet.URANUS,
            45 to Planet.NEPTUNE, 59 to Planet.NEPTUNE,
            60 to Planet.PLUTO, 500 to Planet.PLUTO,
        )
        expected.forEach { (fuel, planet) -> assertEquals("fuel $fuel", planet, Planet.forFuel(fuel)) }
    }

    @Test
    fun `spec 59 - fuel deltas`() {
        assertEquals(1, RocketProgressCalculator.fuelDelta(GREEN))
        assertEquals(0, RocketProgressCalculator.fuelDelta(YELLOW))
        assertEquals(-1, RocketProgressCalculator.fuelDelta(RED))
    }

    @Test
    fun `spec 38 - green, yellow, red from 10`() {
        val start = RocketState(currentFuel = 10, lifetimeFuel = 10, highestReachedPlanet = Planet.MARS)
        assertEquals(11, RocketProgressCalculator.apply(start, GREEN).currentFuel)
        assertEquals(10, RocketProgressCalculator.apply(start, YELLOW).currentFuel)
        assertEquals(9, RocketProgressCalculator.apply(start, RED).currentFuel)
    }

    @Test
    fun `spec 60 - fuel never goes negative`() {
        val result = RocketProgressCalculator.apply(RocketState(currentFuel = 0), RED)
        assertEquals(0, result.currentFuel)
        assertEquals(Planet.EARTH, result.highestReachedPlanet)
    }

    @Test
    fun `spec 61 and 40 - red day never loses a reached planet`() {
        val atMars = RocketState(currentFuel = 7, lifetimeFuel = 7, highestReachedPlanet = Planet.MARS)
        val afterRed = RocketProgressCalculator.apply(atMars, RED)
        assertEquals(6, afterRed.currentFuel)
        assertEquals(Planet.MARS, afterRed.highestReachedPlanet)

        val eight = RocketState(currentFuel = 8, lifetimeFuel = 8, highestReachedPlanet = Planet.MARS)
        assertEquals(Planet.MARS, RocketProgressCalculator.apply(eight, RED).highestReachedPlanet)
    }

    @Test
    fun `three green days from zero reach the Moon`() {
        var rocket = RocketState()
        repeat(3) { rocket = RocketProgressCalculator.apply(rocket, GREEN) }
        assertEquals(RocketState(currentFuel = 3, lifetimeFuel = 3, highestReachedPlanet = Planet.MOON), rocket)
    }

    @Test
    fun `after losing fuel the next planet still needs its full threshold`() {
        var rocket = RocketState(currentFuel = 7, lifetimeFuel = 7, highestReachedPlanet = Planet.MARS)
        rocket = RocketProgressCalculator.apply(rocket, RED) // 6, still Mars
        repeat(7) { rocket = RocketProgressCalculator.apply(rocket, GREEN) } // 13
        assertEquals(Planet.MARS, rocket.highestReachedPlanet)
        rocket = RocketProgressCalculator.apply(rocket, GREEN) // 14
        assertEquals(Planet.JUPITER, rocket.highestReachedPlanet)
    }

    @Test
    fun `lifetime fuel only counts earned fuel`() {
        var rocket = RocketState()
        listOf(GREEN, GREEN, RED, YELLOW, GREEN).forEach { rocket = RocketProgressCalculator.apply(rocket, it) }
        assertEquals(2, rocket.currentFuel)
        assertEquals(3, rocket.lifetimeFuel)
    }

    @Test
    fun `leg progress and fuel to next`() {
        val rocket = RocketState(currentFuel = 5, highestReachedPlanet = Planet.MOON) // Moon 3 → Mars 7
        assertEquals(Planet.MARS, rocket.nextPlanet)
        assertEquals(2, rocket.fuelToNext)
        assertEquals(0.5f, rocket.legProgress, 0.001f)

        val belowMilestone = RocketState(currentFuel = 6, highestReachedPlanet = Planet.MARS) // after a red day
        assertEquals(0f, belowMilestone.legProgress, 0.001f)
        assertEquals(8, belowMilestone.fuelToNext)
    }

    @Test
    fun `Pluto is the end of the line`() {
        val rocket = RocketProgressCalculator.apply(RocketState(currentFuel = 59, highestReachedPlanet = Planet.NEPTUNE), GREEN)
        assertEquals(Planet.PLUTO, rocket.highestReachedPlanet)
        assertNull(rocket.nextPlanet)
        assertEquals(1f, rocket.legProgress, 0.001f)
        assertEquals(0, rocket.fuelToNext)
    }
}
