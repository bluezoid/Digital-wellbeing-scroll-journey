package com.achyutpal.scrolljourney.ui.history

import com.achyutpal.scrolljourney.domain.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthCalendarTest {

    private val today = LocalDate.of(2026, 9, 27)

    @Test
    fun `September 2026 starts on Tuesday so one blank leads`() {
        val weeks = MonthCalendar.build(YearMonth.of(2026, 9), emptyMap(), today)
        assertNull(weeks[0][0].date) // Monday 31 Aug is padding
        assertEquals(LocalDate.of(2026, 9, 1), weeks[0][1].date)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(30, weeks.flatten().count { it.date != null })
    }

    @Test
    fun `month starting on Monday has no leading blanks`() {
        val weeks = MonthCalendar.build(YearMonth.of(2026, 6), emptyMap(), today) // 1 June 2026 = Monday
        assertEquals(LocalDate.of(2026, 6, 1), weeks[0][0].date)
    }

    @Test
    fun `cells carry status, record presence, today and future flags`() {
        val records = mapOf(LocalDate.of(2026, 9, 25) to DayStatus.GREEN, LocalDate.of(2026, 9, 26) to null)
        val cells = MonthCalendar.build(YearMonth.of(2026, 9), records, today).flatten().filter { it.date != null }
            .associateBy { it.date!! }

        assertEquals(DayStatus.GREEN, cells.getValue(LocalDate.of(2026, 9, 25)).status)
        assertTrue(cells.getValue(LocalDate.of(2026, 9, 26)).hasRecord)
        assertFalse(cells.getValue(LocalDate.of(2026, 9, 23)).hasRecord)
        assertTrue(cells.getValue(today).isToday)
        assertTrue(cells.getValue(LocalDate.of(2026, 9, 28)).isFuture)
        assertFalse(cells.getValue(today).isFuture)
    }
}
