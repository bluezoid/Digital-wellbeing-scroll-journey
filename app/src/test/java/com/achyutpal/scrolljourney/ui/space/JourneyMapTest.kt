package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyMapTest {

    private val t = 17f

    @Test
    fun `every destination has a place in the scene`() {
        Destination.entries.forEach { d ->
            assertNotNull(d.name, JourneyMap.anchor(d, t))
            assertTrue(d.name, JourneyMap.radius(d) > 0f)
        }
    }

    @Test
    fun `solar-system stops follow their body, deep-space stops are fixed markers`() {
        assertEquals(SpaceBody.MARS.positionAt(t), JourneyMap.anchor(Destination.MARS, t))
        assertEquals(SpaceBody.EUROPA, JourneyMap.bodyFor(Destination.EUROPA))
        Destination.entries.filter { !it.inSolarSystem || it == Destination.KUIPER_BELT || it == Destination.OORT_CLOUD }
            .forEach { d -> assertEquals(d.name, JourneyMap.anchor(d, 0f), JourneyMap.anchor(d, 999f)) }
    }

    @Test
    fun `deep-space stops get farther along the route and stay inside the starfield`() {
        val distances = JourneyMap.deepSpace.map { it.position.distanceTo(SpacePoint.ORIGIN) }
        distances.zipWithNext { a, b -> assertTrue(a < b) }
        assertTrue(distances.first() > SpaceBody.PLUTO.orbitRadius)
        assertTrue(distances.last() + 10f < SpaceBody.STARFIELD_RADIUS)
    }

    @Test
    fun `rocket at 0 fuel sits just outside Earth, on the way to the Moon`() {
        val p = JourneyMap.rocketPosition(RocketState(), t)
        val earth = SpaceBody.EARTH.positionAt(t)
        val moon = SpaceBody.MOON.positionAt(t)
        assertTrue(p.distanceTo(earth) > SpaceBody.EARTH.radius)
        assertTrue(p.distanceTo(moon) > SpaceBody.MOON.radius)
        assertTrue(p.distanceTo(earth) < earth.distanceTo(moon))
    }

    @Test
    fun `long legs keep the full gap from the surface`() {
        val atMars = RocketState(currentFuel = 12, highestReachedPlanet = Destination.MARS)
        val p = JourneyMap.rocketPosition(atMars, t)
        assertEquals(SpaceBody.MARS.radius + JourneyMap.GAP, p.distanceTo(SpaceBody.MARS.positionAt(t)), 0.01f)
    }

    @Test
    fun `rocket is never inside a body`() {
        (0..1_000 step 7).forEach { fuel ->
            val rocket = RocketState(currentFuel = fuel, highestReachedPlanet = Destination.forFuel(fuel))
            val p = JourneyMap.rocketPosition(rocket, t)
            SpaceBody.entries.forEach { body ->
                assertTrue("fuel $fuel inside $body", p.distanceTo(body.positionAt(t)) > body.radius)
            }
        }
    }

    @Test
    fun `half-way fuel puts the rocket half-way along the leg`() {
        val half = RocketState(currentFuel = 21, highestReachedPlanet = Destination.CERES) // Ceres 20 → Jupiter 30
        val start = JourneyMap.rocketPosition(half.copy(currentFuel = 20), t)
        val end = JourneyMap.rocketPosition(half.copy(currentFuel = 30), t)
        val mid = JourneyMap.rocketPosition(half.copy(currentFuel = 25), t)
        assertEquals(start.distanceTo(end) / 2f, start.distanceTo(mid), 0.05f)
    }

    @Test
    fun `routes split at the rocket`() {
        val rocket = RocketState(currentFuel = 14, highestReachedPlanet = Destination.MARS)
        assertEquals(listOf(Destination.EARTH, Destination.MOON, Destination.MARS), JourneyMap.reachedRoute(rocket))
        assertEquals(listOf(Destination.CERES, Destination.JUPITER, Destination.EUROPA), JourneyMap.upcomingRoute(rocket))
    }

    @Test
    fun `final destination parks the rocket beside it`() {
        val done = RocketState(currentFuel = 1_000, highestReachedPlanet = Destination.GALACTIC_CENTRE)
        val p = JourneyMap.rocketPosition(done, t)
        assertTrue(p.distanceTo(JourneyMap.anchor(Destination.GALACTIC_CENTRE, t)) > JourneyMap.radius(Destination.GALACTIC_CENTRE))
    }

    @Test
    fun `body and destination mapping round-trips`() {
        SpaceBody.entries.forEach { body ->
            JourneyMap.destinationFor(body)?.let { assertEquals(body, JourneyMap.bodyFor(it)) }
        }
    }
}
