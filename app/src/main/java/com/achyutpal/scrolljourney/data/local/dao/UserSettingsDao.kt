package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity
import com.achyutpal.scrolljourney.domain.model.Planet
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

    @Query(
        "UPDATE user_settings SET onboardingComplete = 1, " +
            "journeyStartDate = COALESCE(journeyStartDate, :journeyStart) WHERE id = 1",
    )
    suspend fun setOnboardingComplete(journeyStart: String)

    @Query("UPDATE user_settings SET journeyStartDate = COALESCE(journeyStartDate, :journeyStart) WHERE id = 1")
    suspend fun ensureJourneyStarted(journeyStart: String)

    @Query(
        "UPDATE user_settings SET currentFuel = :currentFuel, lifetimeFuel = :lifetimeFuel, " +
            "highestReachedPlanet = :highestReachedPlanet, journeyDistance = journeyDistance + :addDistance WHERE id = 1",
    )
    suspend fun saveRocket(currentFuel: Int, lifetimeFuel: Int, highestReachedPlanet: Planet, addDistance: Long)

    @Query("UPDATE user_settings SET dailyLimitMinutes = :minutes WHERE id = 1")
    suspend fun setDailyLimit(minutes: Int)
}
