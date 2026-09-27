package com.achyutpal.scrolljourney.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec §64 (distancePerMinute = 100). */
class DistanceCalculatorTest {

    private val calculator = DistanceCalculator(DistanceConfig(centimetresPerMinute = 100))

    @Test
    fun `spec cases`() {
        assertEquals(0L, calculator.centimetres(0))
        assertEquals(100L, calculator.centimetres(1))
        assertEquals(1_000L, calculator.centimetres(10))
        assertEquals(6_000L, calculator.centimetres(60))
    }

    @Test
    fun `large values do not overflow`() {
        assertEquals(Int.MAX_VALUE.toLong() * 100, calculator.centimetres(Int.MAX_VALUE))
    }

    @Test
    fun `negative minutes count as zero`() {
        assertEquals(0L, calculator.centimetres(-5))
    }

    @Test
    fun `default is one metre per minute`() {
        assertEquals(13_700L, DistanceCalculator().centimetres(137))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive speed is rejected`() {
        DistanceConfig(centimetresPerMinute = 0)
    }
}
