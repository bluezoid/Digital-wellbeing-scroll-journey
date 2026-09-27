package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Saved daily history (Room): the app's long-term source of truth. */
interface HistoryRepository {
    suspend fun getDay(date: LocalDate): DailyRecord?

    /** Saves a day and replaces its per-app rows, atomically. Safe to call repeatedly. */
    suspend fun saveDay(record: DailyRecord, apps: List<AppUsage>)

    /** Freezes an already-saved day with its current totals (used when Android no longer has its events). */
    suspend fun markFinalized(date: LocalDate, status: DayStatus)

    /** Sets the status of a saved day without touching its minutes. */
    suspend fun updateStatus(date: LocalDate, status: DayStatus)

    /** Days in [start, end] inclusive, oldest first. */
    suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyRecord>

    /** Most recent [limit] saved days, newest first. */
    fun observeRecentDays(limit: Int): Flow<List<DailyRecord>>
}
