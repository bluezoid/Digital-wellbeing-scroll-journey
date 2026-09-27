package com.achyutpal.scrolljourney.domain.model

import java.time.LocalDate

/**
 * One saved day of history.
 *
 * [finalized] = the day is over and its numbers are frozen. Android deletes old usage
 * events after a few days, so a finalized day must never be recalculated (it would
 * overwrite real history with zeros). [status], [fuelDelta] and [distance] are filled
 * in when the day-status, rocket and distance engines arrive (Phases 5–7).
 */
data class DailyRecord(
    val date: LocalDate,
    val totalMinutes: Int,
    val status: DayStatus? = null,
    val fuelDelta: Int = 0,
    val distance: Long = 0,
    val finalized: Boolean = false,
)
