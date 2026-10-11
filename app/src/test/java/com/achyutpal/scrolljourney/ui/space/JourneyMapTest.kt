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
    fun `rocket sits on the flight arc at its fuel fraction`() {
        val rocket = RocketState(currentFuel = 25, highestReachedPlanet = Destination.CERES) // Ceres 20 → Jupiter 30
        val arc = JourneyMap.leg(rocket, t)!!
        assertEquals(0f, arc.pointAt(0.5f).distanceTo(JourneyMap.rocketPosition(rocket, t)), 0.001f)
        // A symmetric arc: the half-way point is equally far from both ends.
        assertEquals(arc.start.distanceTo(arc.pointAt(0.5f)), arc.end.distanceTo(arc.pointAt(0.5f)), 0.01f)
    }

    @Test
    fun `flight arc bows upward above the straight line`() {
        val arc = JourneyMap.leg(RocketState(currentFuel = 20, highestReachedPlanet = Destination.CERES), t)!!
        val chordMid = (arc.start + arc.end) * 0.5f
        assertTrue(arc.pointAt(0.5f).y > chordMid.y + 0.1f)
    }

    @Test
    fun `heading follows the arc`() {
        val rocket = RocketState(currentFuel = 20, highestReachedPlanet = Destination.CERES)
        val arc = JourneyMap.leg(rocket, t)!!
        val h = JourneyMap.rocketHeading(rocket, t)
        val towardControl = JourneyMap.direction(arc.start, arc.control)
        assertEquals(1f, h.x * towardControl.x + h.y * towardControl.y + h.z * towardControl.z, 0.001f) // at u = 0
    }

    @Test
    fun `no leg after the final destination`() {
        val done = RocketState(currentFuel = 1_000, highestReachedPlanet = Destination.GALACTIC_CENTRE)
        assertEquals(null, JourneyMap.leg(done, t))
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

    @Test
    fun `just launched - no tail behind the rocket`() {
        val arc = JourneyMap.leg(RocketState(), t)!!
        assertTrue(arc.pointsBehind(from = 0f, spacing = 0.1f, count = 12).isEmpty())
    }

    @Test
    fun `tail points are evenly spaced along the flown arc and stop at its start`() {
        val rocket = RocketState(currentFuel = 25, highestReachedPlanet = Destination.CERES)
        val arc = JourneyMap.leg(rocket, t)!!
        val flown = arc.sample(0f, rocket.legProgress, 400).zipWithNext().sumOf { (a, b) -> a.distanceTo(b).toDouble() }.toFloat()
        val spacing = flown / 4.5f // room for 4 points, not 5
        val points = arc.pointsBehind(rocket.legProgress, spacing, count = 12)
        assertEquals(4, points.size)
        val all = listOf(arc.pointAt(rocket.legProgress)) + points
        all.zipWithNext().forEach { (a, b) -> assertEquals(spacing, a.distanceTo(b), spacing * 0.05f) }
    }

    @Test
    fun `tail never has more points than asked`() {
        val rocket = RocketState(currentFuel = 29, highestReachedPlanet = Destination.CERES)
        assertEquals(3, JourneyMap.leg(rocket, t)!!.pointsBehind(rocket.legProgress, 0.01f, count = 3).size)
    }

    @Test
    fun `clearance is the gap to the nearer surface of the leg's stops`() {
        val rocket = RocketState() // Earth → Moon
        val p = JourneyMap.rocketPosition(rocket, t)
        val toEarth = p.distanceTo(JourneyMap.anchor(Destination.EARTH, t)) - JourneyMap.radius(Destination.EARTH)
        val toMoon = p.distanceTo(JourneyMap.anchor(Destination.MOON, t)) - JourneyMap.radius(Destination.MOON)
        assertEquals(minOf(toEarth, toMoon), JourneyMap.clearance(rocket, t, p), 1e-4f)
        assertTrue(JourneyMap.clearance(rocket, t, p) > 0f)
        assertEquals(0f, JourneyMap.clearance(rocket, t, JourneyMap.anchor(Destination.EARTH, t)), 0f)
    }

    @Test
    fun `the camera around the furthest stop stays well inside the star sphere and draw range`() {
        JourneyMap.deepSpace.forEach { m ->
            val farthestEye = m.position.distanceTo(SpacePoint.ORIGIN) +
                JourneyMap.visualExtent(m.destination) * OrbitCameraState.EXTENT_MAX_RADII
            assertTrue("${m.destination} camera would leave the stars", farthestEye < SpaceBody.STARFIELD_RADIUS)
            assertTrue(SpaceBody.STARFIELD_RADIUS * 2 < SpaceBody.CAMERA_FAR) // the whole sky is drawn from anywhere inside
        }
    }

    @Test
    fun `the galaxy's stars are the same every time and fit inside its framed size`() {
        val r = JourneyMap.radius(Destination.GALACTIC_CENTRE)
        val (arms, bulge) = JourneyMap.galaxyParticles(r)
        assertEquals(arms, JourneyMap.galaxyParticles(r).first) // fixed seed: no reshuffle between launches
        assertEquals(JourneyMap.GALAXY_ARM_DOTS * 2, arms.size)
        assertEquals(JourneyMap.GALAXY_BULGE_DOTS, bulge.size)
        val extent = JourneyMap.visualExtent(Destination.GALACTIC_CENTRE)
        (arms + bulge).forEach { (p, size) ->
            assertTrue(p.distanceTo(SpacePoint.ORIGIN) + size < extent)
            assertTrue(kotlin.math.abs(p.y) < r * 0.3f) // a flat disc, not a ball
        }
    }
}

