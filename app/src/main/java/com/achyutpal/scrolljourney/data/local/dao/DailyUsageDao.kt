package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.achyutpal.scrolljourney.data.local.entity.DailyUsageEntity
import com.achyutpal.scrolljourney.domain.model.DayStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun get(date: String): DailyUsageEntity?

    @Upsert
    suspend fun upsert(day: DailyUsageEntity)

    @Query(
        "UPDATE daily_usage SET finalized = 1, status = :status, limitMinutes = :limitMinutes, distance = :distance " +
            "WHERE date = :date",
    )
    suspend fun markFinalized(date: String, status: DayStatus, limitMinutes: Int, distance: Long)

    @Query("UPDATE daily_usage SET status = :status, limitMinutes = :limitMinutes, distance = :distance WHERE date = :date")
    suspend fun updateEvaluation(date: String, status: DayStatus, limitMinutes: Int, distance: Long)

    /** Dates are "YYYY-MM-DD" text, so string comparison = date comparison. */
    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getRange(start: String, end: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE finalized = 1 AND fuelApplied = 0 AND date >= :from ORDER BY date ASC")
    suspend fun getAwaitingFuel(from: String): List<DailyUsageEntity>

    /** Returns 1 if this call applied the fuel, 0 if it had already been applied: the "exactly once" guard. */
    @Query("UPDATE daily_usage SET fuelApplied = 1, fuelDelta = :fuelDelta WHERE date = :date AND fuelApplied = 0")
    suspend fun markFuelApplied(date: String, fuelDelta: Int): Int

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyUsageEntity>>

    @Query("DELETE FROM daily_usage WHERE date > :today AND finalized = 0")
    suspend fun deleteLiveAfter(today: String): Int
}
