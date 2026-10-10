package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.achyutpal.scrolljourney.data.local.entity.DailyAppUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.DailyUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity

/** Whole-database reads and replace for backup / restore. */
@Dao
abstract class BackupDao {

    data class Snapshot(
        val settings: UserSettingsEntity?,
        val apps: List<SelectedAppEntity>,
        val days: List<DailyUsageEntity>,
        val appMinutes: List<DailyAppUsageEntity>,
    )

    /** All tables read inside one transaction, so a sync running at the same time can't mix states. */
    @Transaction
    open suspend fun snapshot() = Snapshot(settings(), apps(), days(), appMinutes())

    /** Deletes everything and inserts the backup's rows; all or nothing. */
    @Transaction
    open suspend fun replaceAll(
        settings: UserSettingsEntity,
        apps: List<SelectedAppEntity>,
        days: List<DailyUsageEntity>,
        appMinutes: List<DailyAppUsageEntity>,
    ) {
        clearAppMinutes()
        clearDays()
        clearApps()
        clearSettings()
        insertSettings(settings)
        insertApps(apps)
        insertDays(days)
        insertAppMinutes(appMinutes)
    }

    @Query("SELECT * FROM user_settings WHERE id = 1") protected abstract suspend fun settings(): UserSettingsEntity?
    @Query("SELECT * FROM selected_apps ORDER BY packageName") protected abstract suspend fun apps(): List<SelectedAppEntity>
    @Query("SELECT * FROM daily_usage ORDER BY date") protected abstract suspend fun days(): List<DailyUsageEntity>
    @Query("SELECT * FROM daily_app_usage ORDER BY date, packageName")
    protected abstract suspend fun appMinutes(): List<DailyAppUsageEntity>

    @Query("DELETE FROM user_settings") protected abstract suspend fun clearSettings()
    @Query("DELETE FROM selected_apps") protected abstract suspend fun clearApps()
    @Query("DELETE FROM daily_usage") protected abstract suspend fun clearDays()
    @Query("DELETE FROM daily_app_usage") protected abstract suspend fun clearAppMinutes()

    @Insert protected abstract suspend fun insertSettings(settings: UserSettingsEntity)
    @Insert protected abstract suspend fun insertApps(apps: List<SelectedAppEntity>)
    @Insert protected abstract suspend fun insertDays(days: List<DailyUsageEntity>)
    @Insert protected abstract suspend fun insertAppMinutes(rows: List<DailyAppUsageEntity>)
}
