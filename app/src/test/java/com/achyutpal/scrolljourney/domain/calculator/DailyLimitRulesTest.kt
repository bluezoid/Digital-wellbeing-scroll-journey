package com.achyutpal.scrolljourney.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §41 and §68. */
class DailyLimitRulesTest {

    @Test
    fun `valid limits`() {
        listOf(15, 30, 60, 120, 240).forEach { assertTrue("$it", DailyLimitRules.isValid(it)) }
    }

    @Test
    fun `invalid limits`() {
        listOf(14, 241, 0, -10).forEach { assertFalse("$it", DailyLimitRules.isValid(it)) }
    }

    @Test
    fun `default is 60`() {
        assertEquals(60, DailyLimitRules.DEFAULT_MINUTES)
    }

    @Test
    fun `snap clamps and rounds to 5 minute steps`() {
        assertEquals(15, DailyLimitRules.snap(3))
        assertEquals(240, DailyLimitRules.snap(999))
        assertEquals(60, DailyLimitRules.snap(61))
        assertEquals(65, DailyLimitRules.snap(63))
        assertEquals(15, DailyLimitRules.snap(15))
        assertEquals(240, DailyLimitRules.snap(238))
    }
}
