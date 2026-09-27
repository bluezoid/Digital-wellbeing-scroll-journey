package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.achyutpal.scrolljourney.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun get(date: String): DailyUsageEntity?

    @Upsert
    suspend fun upsert(day: DailyUsageEntity)

    @Query("UPDATE daily_usage SET finalized = 1 WHERE date = :date")
    suspend fun markFinalized(date: String)

    /** Dates are "YYYY-MM-DD" text, so string comparison = date comparison. */
    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getRange(start: String, end: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyUsageEntity>>
}
