package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayAppMinutes
import com.achyutpal.scrolljourney.domain.model.DayStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Saved daily history (Room): the app's long-term source of truth. */
interface HistoryRepository {
    suspend fun getDay(date: LocalDate): DailyRecord?

    /** Saves a day and replaces its per-app rows, atomically. Safe to call repeatedly. */
    suspend fun saveDay(record: DailyRecord, apps: List<AppUsage>)

    /** Freezes an already-saved day with its current minutes (used when Android no longer has its events). */
    suspend fun markFinalized(date: LocalDate, status: DayStatus, limitMinutes: Int, distance: Long)

    /** Fills in values derived from a day's minutes (status, judged limit, distance) without touching the minutes. */
    suspend fun updateEvaluation(date: LocalDate, status: DayStatus, limitMinutes: Int, distance: Long)

    /** Saved per-app minutes for one day, most used first. */
    suspend fun getAppMinutes(date: LocalDate): List<DayAppMinutes>

    /** Days in [start, end] inclusive, oldest first. */
    suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyRecord>

    /** Most recent [limit] saved days, newest first. */
    fun observeRecentDays(limit: Int): Flow<List<DailyRecord>>
}
