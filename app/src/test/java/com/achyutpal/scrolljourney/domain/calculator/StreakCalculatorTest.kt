package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    @Test
    fun `spec 62 A - three greens`() {
        assertEquals(Streak(current = 3, best = 3), StreakCalculator.calculate(listOf(GREEN, GREEN, GREEN)))
    }

    @Test
    fun `spec 62 B - yellow breaks the streak`() {
        assertEquals(Streak(current = 1, best = 2), StreakCalculator.calculate(listOf(GREEN, GREEN, YELLOW, GREEN)))
    }

    @Test
    fun `spec 62 C - reds only`() {
        assertEquals(Streak(current = 0, best = 0), StreakCalculator.calculate(listOf(RED, RED)))
    }

    @Test
    fun `spec 63 - day before yesterday yellow, yesterday and today green`() {
        assertEquals(2, StreakCalculator.calculate(listOf(YELLOW, GREEN, GREEN)).current)
    }

    @Test
    fun `spec 36 example`() {
        // Mon G, Tue G, Wed G, Thu Y, Fri G
        assertEquals(Streak(current = 1, best = 3), StreakCalculator.calculate(listOf(GREEN, GREEN, GREEN, YELLOW, GREEN)))
    }

    @Test
    fun `a day without data breaks the streak`() {
        assertEquals(Streak(current = 1, best = 2), StreakCalculator.calculate(listOf(GREEN, GREEN, null, GREEN)))
    }

    @Test
    fun `empty history has no streak`() {
        assertEquals(Streak(0, 0), StreakCalculator.calculate(emptyList()))
    }

    @Test
    fun `records are laid on a gap-free calendar ending today`() {
        val today = LocalDate.of(2026, 9, 27)
        val records = listOf(
            DailyRecord(today.minusDays(3), 10, status = GREEN),
            DailyRecord(today.minusDays(1), 10, status = GREEN),
            DailyRecord(today, 10, status = GREEN),
        )
        assertEquals(listOf(GREEN, null, GREEN, GREEN), StreakCalculator.contiguousStatuses(records, today))
        assertTrue(StreakCalculator.contiguousStatuses(emptyList(), today).isEmpty())
    }

    @Test
    fun `missing today means no current streak`() {
        val today = LocalDate.of(2026, 9, 27)
        val statuses = StreakCalculator.contiguousStatuses(listOf(DailyRecord(today.minusDays(1), 10, status = GREEN)), today)
        assertEquals(0, StreakCalculator.calculate(statuses).current)
    }
}
