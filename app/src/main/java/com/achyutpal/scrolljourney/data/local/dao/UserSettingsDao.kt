package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): UserSettingsEntity?

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<UserSettingsEntity?>

    /** Creates the row with defaults only if it doesn't exist yet. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET onboardingComplete = 1 WHERE id = 1")
    suspend fun setOnboardingComplete()

    @Query("UPDATE user_settings SET dailyLimitMinutes = :minutes WHERE id = 1")
    suspend fun setDailyLimit(minutes: Int)
}
