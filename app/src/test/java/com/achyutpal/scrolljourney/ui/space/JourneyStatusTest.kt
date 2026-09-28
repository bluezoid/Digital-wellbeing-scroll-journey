package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyStatusTest {

    private val atMoon = RocketState(currentFuel = 5, lifetimeFuel = 5, highestReachedPlanet = Destination.MOON)

    @Test
    fun `reached stops`() {
        assertEquals(JourneyStatus.Reached(1), JourneyStatus.of(Destination.EARTH, atMoon))
        assertEquals(JourneyStatus.Reached(2), JourneyStatus.of(Destination.MOON, atMoon))
    }

    @Test
    fun `next stop shows fuel still needed`() {
        assertEquals(JourneyStatus.Next(3, fuelToGo = 7, fuelRequired = 12), JourneyStatus.of(Destination.MARS, atMoon))
    }

    @Test
    fun `later stops are locked`() {
        assertEquals(JourneyStatus.Locked(18, fuelToGo = 995, fuelRequired = 1_000), JourneyStatus.of(Destination.GALACTIC_CENTRE, atMoon))
    }

    @Test
    fun `a reached stop stays reached after a red day drops fuel below it`() {
        val afterRed = atMoon.copy(currentFuel = 2) // below the Moon's 3, but the Moon was reached
        assertEquals(JourneyStatus.Reached(2), JourneyStatus.of(Destination.MOON, afterRed))
        assertEquals(JourneyStatus.Next(3, fuelToGo = 10, fuelRequired = 12), JourneyStatus.of(Destination.MARS, afterRed))
    }
}
