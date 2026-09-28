package com.achyutpal.scrolljourney.ui.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarSystemLayoutTest {

    @Test
    fun `sun stays at the centre`() {
        listOf(0f, 12.5f, 999f).forEach { t -> assertEquals(SpacePoint.ORIGIN, SpaceBody.SUN.positionAt(t)) }
    }

    @Test
    fun `planets stay on their orbit circle`() {
        SpaceBody.sunOrbiters.forEach { body ->
            listOf(0f, 7.3f, 123f).forEach { t ->
                assertEquals("$body at $t", body.orbitRadius, body.positionAt(t).distanceTo(SpacePoint.ORIGIN), 0.001f)
            }
        }
    }

    @Test
    fun `moons circle their planet`() {
        val t = 42f
        assertEquals(SpaceBody.MOON.orbitRadius, SpaceBody.MOON.positionAt(t).distanceTo(SpaceBody.EARTH.positionAt(t)), 0.001f)
        assertEquals(SpaceBody.EUROPA.orbitRadius, SpaceBody.EUROPA.positionAt(t).distanceTo(SpaceBody.JUPITER.positionAt(t)), 0.001f)
        assertEquals(SpaceBody.TITAN.orbitRadius, SpaceBody.TITAN.positionAt(t).distanceTo(SpaceBody.SATURN.positionAt(t)), 0.001f)
    }

    @Test
    fun `a full orbit period returns to the start`() {
        val earth = SpaceBody.EARTH
        val start = earth.positionAt(0f)
        assertEquals(0f, start.distanceTo(earth.positionAt(earth.orbitPeriodSeconds)), 0.01f)
    }

    @Test
    fun `orbits are ordered outward and never overlap`() {
        val orbiters = SpaceBody.sunOrbiters
        orbiters.zipWithNext { inner, outer ->
            val innerReach = inner.radius + moonReach(inner)
            val outerReach = outer.radius + moonReach(outer)
            assertTrue("$inner vs $outer", outer.orbitRadius - inner.orbitRadius > innerReach + outerReach)
        }
        assertTrue(SpaceBody.MERCURY.orbitRadius - SpaceBody.MERCURY.radius > SpaceBody.SUN.radius)
    }

    @Test
    fun `everything fits inside the starfield`() {
        assertTrue(SpaceBody.PLUTO.orbitRadius * 3 < SpaceBody.STARFIELD_RADIUS)
    }

    private fun moonReach(planet: SpaceBody): Float {
        val ring = if (planet == SpaceBody.SATURN) SpaceBody.SATURN_RING_OUTER else 0f
        val moons = SpaceBody.entries.filter { it.parent == planet }.maxOfOrNull { it.orbitRadius + it.radius } ?: 0f
        return maxOf(ring, moons) - if (ring > 0 || moons > 0) planet.radius else 0f
    }
}
