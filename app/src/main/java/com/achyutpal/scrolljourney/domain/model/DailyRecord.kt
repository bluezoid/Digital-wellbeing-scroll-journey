package com.achyutpal.scrolljourney.domain.model

import java.time.LocalDate

/**
 * One saved day of history.
 *
 * [finalized] = the day is over and its numbers are frozen. Android deletes old usage
 * events after a few days, so a finalized day must never be recalculated (it would
 * overwrite real history with zeros).
 *
 * [status] is judged against [limitMinutes], the daily limit in force when the day was
 * evaluated. It's stored so a past colour can be explained even after the user changes
 * their limit. [distance] is the estimated scroll distance in centimetres.
 * [fuelApplied] = this day's [fuelDelta] has been added to the rocket (exactly once, ever).
 */
data class DailyRecord(
    val date: LocalDate,
    val totalMinutes: Int,
    val status: DayStatus? = null,
    val limitMinutes: Int? = null,
    val fuelDelta: Int = 0,
    val distance: Long = 0,
    val finalized: Boolean = false,
    val fuelApplied: Boolean = false,
)
