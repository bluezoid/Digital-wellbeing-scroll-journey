package com.achyutpal.scrolljourney.ui.history

import com.achyutpal.scrolljourney.domain.model.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One square in the month grid. [date] is null for the padding before the 1st / after the last day. */
data class CalendarCell(
    val date: LocalDate?,
    val status: DayStatus? = null,
    val hasRecord: Boolean = false,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
)

/** Pure month-grid layout, weeks starting on Monday (spec §35). Unit-tested without Android. */
object MonthCalendar {

    fun build(month: YearMonth, recordsByDate: Map<LocalDate, DayStatus?>, today: LocalDate): List<List<CalendarCell>> {
        val first = month.atDay(1)
        val leadingBlanks = first.dayOfWeek.value - DayOfWeek.MONDAY.value // Monday = 0 … Sunday = 6
        val cells = MutableList(leadingBlanks) { CalendarCell(date = null) }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            cells += CalendarCell(
                date = date,
                status = recordsByDate[date],
                hasRecord = recordsByDate.containsKey(date),
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
        }
        while (cells.size % 7 != 0) cells += CalendarCell(date = null)
        return cells.chunked(7)
    }
}
