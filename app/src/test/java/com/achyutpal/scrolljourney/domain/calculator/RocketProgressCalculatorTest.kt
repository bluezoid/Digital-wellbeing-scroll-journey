package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rocket rules with the route approved on 2026-09-28 (replaces the spec §58 thresholds). */
class RocketProgressCalculatorTest {

    @Test
    fun `approved route thresholds`() {
        val expected = listOf(
            Destination.EARTH to 0, Destination.MOON to 3, Destination.MARS to 12, Destination.CERES to 20,
            Destination.JUPITER to 30, Destination.EUROPA to 38, Destination.SATURN to 50, Destination.TITAN to 60,
            Destination.URANUS to 80, Destination.NEPTUNE to 105, Destination.PLUTO to 135,
            Destination.KUIPER_BELT to 170, Destination.OORT_CLOUD to 220, Destination.PROXIMA_CENTAURI to 300,
            Destination.ALPHA_CENTAURI to 365, Destination.SIRIUS to 500, Destination.VEGA to 650,
            Destination.GALACTIC_CENTRE to 1_000,
        )
        assertEquals(expected.map { it.first }, Destination.entries.toList()) // route order
        expected.forEach { (dest, fuel) -> assertEquals(dest.name, fuel, dest.fuelRequired) }
    }

    @Test
    fun `thresholds strictly increase`() {
        Destination.entries.zipWithNext { a, b -> assertTrue("$a < $b", a.fuelRequired < b.fuelRequired) }
    }

    @Test
    fun `destination for fuel at and around each boundary`() {
        assertEquals(Destination.EARTH, Destination.forFuel(0))
        assertEquals(Destination.EARTH, Destination.forFuel(2))
        assertEquals(Destination.MOON, Destination.forFuel(3))
        assertEquals(Destination.MOON, Destination.forFuel(11))
        assertEquals(Destination.MARS, Destination.forFuel(12))
        assertEquals(Destination.PLUTO, Destination.forFuel(169))
        assertEquals(Destination.ALPHA_CENTAURI, Destination.forFuel(365))
        assertEquals(Destination.GALACTIC_CENTRE, Destination.forFuel(1_000))
        assertEquals(Destination.GALACTIC_CENTRE, Destination.forFuel(99_999))
    }

    @Test
    fun `solar system ends at the Oort Cloud`() {
        assertTrue(Destination.OORT_CLOUD.inSolarSystem)
        assertTrue(!Destination.PROXIMA_CENTAURI.inSolarSystem)
    }

    @Test
    fun `fuel deltas`() {
        assertEquals(1, RocketProgressCalculator.fuelDelta(GREEN))
        assertEquals(0, RocketProgressCalculator.fuelDelta(YELLOW))
        assertEquals(-1, RocketProgressCalculator.fuelDelta(RED))
    }

    @Test
    fun `green, yellow, red from 10`() {
        val start = RocketState(currentFuel = 10, lifetimeFuel = 10, highestReachedPlanet = Destination.MOON)
        assertEquals(11, RocketProgressCalculator.apply(start, GREEN).currentFuel)
        assertEquals(10, RocketProgressCalculator.apply(start, YELLOW).currentFuel)
        assertEquals(9, RocketProgressCalculator.apply(start, RED).currentFuel)
    }

    @Test
    fun `fuel never goes negative`() {
        val result = RocketProgressCalculator.apply(RocketState(currentFuel = 0), RED)
        assertEquals(0, result.currentFuel)
        assertEquals(Destination.EARTH, result.highestReachedPlanet)
    }

    @Test
    fun `red day never loses a reached destination`() {
        val atMars = RocketState(currentFuel = 12, lifetimeFuel = 12, highestReachedPlanet = Destination.MARS)
        val afterRed = RocketProgressCalculator.apply(atMars, RED)
        assertEquals(11, afterRed.currentFuel)
        assertEquals(Destination.MARS, afterRed.highestReachedPlanet)
    }

    @Test
    fun `three green days from zero reach the Moon`() {
        var rocket = RocketState()
        repeat(3) { rocket = RocketProgressCalculator.apply(rocket, GREEN) }
        assertEquals(RocketState(currentFuel = 3, lifetimeFuel = 3, highestReachedPlanet = Destination.MOON), rocket)
    }

    @Test
    fun `after losing fuel the next stop still needs its full threshold`() {
        var rocket = RocketState(currentFuel = 12, lifetimeFuel = 12, highestReachedPlanet = Destination.MARS)
        rocket = RocketProgressCalculator.apply(rocket, RED) // 11, still Mars
        repeat(8) { rocket = RocketProgressCalculator.apply(rocket, GREEN) } // 19
        assertEquals(Destination.MARS, rocket.highestReachedPlanet)
        rocket = RocketProgressCalculator.apply(rocket, GREEN) // 20
        assertEquals(Destination.CERES, rocket.highestReachedPlanet)
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
        val rocket = RocketState(currentFuel = 6, highestReachedPlanet = Destination.MOON) // Moon 3 → Mars 12
        assertEquals(Destination.MARS, rocket.nextPlanet)
        assertEquals(6, rocket.fuelToNext)
        assertEquals(3f / 9f, rocket.legProgress, 0.001f)

        val belowMilestone = RocketState(currentFuel = 11, highestReachedPlanet = Destination.MARS) // after a red day
        assertEquals(0f, belowMilestone.legProgress, 0.001f)
        assertEquals(9, belowMilestone.fuelToNext)
    }

    @Test
    fun `Galactic Centre is the end of the line`() {
        val rocket = RocketProgressCalculator.apply(
            RocketState(currentFuel = 999, highestReachedPlanet = Destination.VEGA),
            GREEN,
        )
        assertEquals(Destination.GALACTIC_CENTRE, rocket.highestReachedPlanet)
        assertNull(rocket.nextPlanet)
        assertEquals(1f, rocket.legProgress, 0.001f)
        assertEquals(0, rocket.fuelToNext)
    }
}
