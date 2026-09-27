package com.achyutpal.scrolljourney.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceComparisonsTest {

    @Test
    fun `no comparison for zero distance`() {
        assertNull(DistanceComparisons.compare(0))
    }

    @Test
    fun `short distance is a fraction of the smallest landmark`() {
        val c = DistanceComparisons.compare(3_650)!! // 36.5 m
        assertEquals(Landmark.QUTUB_MINAR, c.landmark)
        assertEquals(0.5, c.times, 0.001)
    }

    @Test
    fun `uses the tallest landmark already passed`() {
        assertEquals(Landmark.GREAT_PYRAMID, DistanceComparisons.compare(13_900)!!.landmark) // exactly 139 m
        assertEquals(Landmark.GREAT_PYRAMID, DistanceComparisons.compare(13_700 * 2)!!.landmark) // 274 m
        val eiffel = DistanceComparisons.compare(55_000)!! // 550 m
        assertEquals(Landmark.EIFFEL_TOWER, eiffel.landmark)
        assertEquals(1.667, eiffel.times, 0.001)
    }

    @Test
    fun `beyond Everest keeps counting Everests`() {
        val c = DistanceComparisons.compare(884_900L * 3)!!
        assertEquals(Landmark.MOUNT_EVEREST, c.landmark)
        assertEquals(3.0, c.times, 0.001)
    }
}
