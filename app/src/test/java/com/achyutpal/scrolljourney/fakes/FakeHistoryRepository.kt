package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** In-memory stand-in for the Room tables, keyed by date like the real primary key. */
class FakeHistoryRepository(initial: List<DailyRecord> = emptyList()) : HistoryRepository {

    private val days = MutableStateFlow(initial.associateBy { it.date })
    val appRows = mutableMapOf<LocalDate, List<AppUsage>>()
    var saveCount = 0

    val records: Map<LocalDate, DailyRecord> get() = days.value

    override suspend fun getDay(date: LocalDate) = days.value[date]

    override suspend fun saveDay(record: DailyRecord, apps: List<AppUsage>) {
        saveCount++
        days.value = days.value + (record.date to record)
        appRows[record.date] = apps
    }

    override suspend fun markFinalized(date: LocalDate, status: DayStatus) {
        val day = days.value[date] ?: return
        days.value = days.value + (date to day.copy(finalized = true, status = status))
    }

    override suspend fun updateStatus(date: LocalDate, status: DayStatus) {
        val day = days.value[date] ?: return
        days.value = days.value + (date to day.copy(status = status))
    }

    override suspend fun getRange(start: LocalDate, end: LocalDate) =
        days.value.values.filter { it.date in start..end }.sortedBy { it.date }

    override fun observeRecentDays(limit: Int): Flow<List<DailyRecord>> =
        days.map { it.values.sortedByDescending { d -> d.date }.take(limit) }
}
