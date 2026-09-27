package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec §57 and §59 cases, plus boundaries. */
class LimitEvaluatorTest {

    @Test
    fun `spec cases for a 60 minute limit`() {
        assertEquals(GREEN, LimitEvaluator.evaluate(0, 60))
        assertEquals(GREEN, LimitEvaluator.evaluate(30, 60))
        assertEquals(GREEN, LimitEvaluator.evaluate(60, 60))
        assertEquals(YELLOW, LimitEvaluator.evaluate(61, 60))
        assertEquals(YELLOW, LimitEvaluator.evaluate(75, 60))
        assertEquals(RED, LimitEvaluator.evaluate(76, 60))
        assertEquals(RED, LimitEvaluator.evaluate(100, 60))
    }

    @Test
    fun `boundary is exact when 125 percent is not a whole number`() {
        // limit 15 → yellow up to 18.75, so 18 is yellow and 19 is red
        assertEquals(YELLOW, LimitEvaluator.evaluate(18, 15))
        assertEquals(RED, LimitEvaluator.evaluate(19, 15))
    }

    @Test
    fun `max limit boundaries`() {
        assertEquals(GREEN, LimitEvaluator.evaluate(240, 240))
        assertEquals(YELLOW, LimitEvaluator.evaluate(300, 240))
        assertEquals(RED, LimitEvaluator.evaluate(301, 240))
    }

    @Test
    fun `huge usage does not overflow`() {
        assertEquals(RED, LimitEvaluator.evaluate(Int.MAX_VALUE, 240))
    }

    @Test
    fun `yellow max matches the evaluator`() {
        assertEquals(75, LimitEvaluator.yellowMaxMinutes(60))
        assertEquals(18, LimitEvaluator.yellowMaxMinutes(15))
        assertEquals(300, LimitEvaluator.yellowMaxMinutes(240))
        listOf(15, 37, 60, 99, 240).forEach { limit ->
            assertEquals(YELLOW, LimitEvaluator.evaluate(LimitEvaluator.yellowMaxMinutes(limit), limit))
            assertEquals(RED, LimitEvaluator.evaluate(LimitEvaluator.yellowMaxMinutes(limit) + 1, limit))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero limit is rejected`() {
        LimitEvaluator.evaluate(10, 0)
    }
}
