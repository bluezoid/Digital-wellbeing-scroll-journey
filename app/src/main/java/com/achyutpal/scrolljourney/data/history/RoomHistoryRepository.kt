package com.achyutpal.scrolljourney.data.history

import androidx.room.withTransaction
import com.achyutpal.scrolljourney.data.local.ScrollJourneyDatabase
import com.achyutpal.scrolljourney.data.local.entity.DailyAppUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.toDomain
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomHistoryRepository(
    private val database: ScrollJourneyDatabase,
) : HistoryRepository {

    private val dailyUsageDao = database.dailyUsageDao()
    private val dailyAppUsageDao = database.dailyAppUsageDao()

    override suspend fun getDay(date: LocalDate): DailyRecord? =
        dailyUsageDao.get(date.toString())?.toDomain()

    /**
     * A transaction: either every write below succeeds or none does, so a crash
     * mid-save can't leave a day total without its per-app rows. Per-app rows are
     * replaced (not merged) so an app removed from the selection disappears from that day.
     */
    override suspend fun saveDay(record: DailyRecord, apps: List<AppUsage>) {
        val date = record.date.toString()
        database.withTransaction {
            dailyUsageDao.upsert(record.toEntity())
            dailyAppUsageDao.deleteForDate(date)
            dailyAppUsageDao.upsertAll(apps.map { DailyAppUsageEntity(date, it.packageName, it.minutes) })
        }
    }

    override suspend fun markFinalized(date: LocalDate) {
        dailyUsageDao.markFinalized(date.toString())
    }

    override suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyRecord> =
        dailyUsageDao.getRange(start.toString(), end.toString()).map { it.toDomain() }

    override fun observeRecentDays(limit: Int): Flow<List<DailyRecord>> =
        dailyUsageDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }
}
