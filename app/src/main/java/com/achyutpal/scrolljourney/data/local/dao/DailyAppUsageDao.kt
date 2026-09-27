package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.achyutpal.scrolljourney.data.local.entity.DailyAppUsageEntity

@Dao
interface DailyAppUsageDao {

    @Query("SELECT * FROM daily_app_usage WHERE date = :date ORDER BY minutes DESC")
    suspend fun getForDate(date: String): List<DailyAppUsageEntity>

    @Query("DELETE FROM daily_app_usage WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Upsert
    suspend fun upsertAll(rows: List<DailyAppUsageEntity>)
}
