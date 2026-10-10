package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity
import com.achyutpal.scrolljourney.domain.model.Destination
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
    suspend fun saveRocket(currentFuel: Int, lifetimeFuel: Int, highestReachedPlanet: Destination, addDistance: Long)

    @Query("UPDATE user_settings SET dailyLimitMinutes = :minutes WHERE id = 1")
    suspend fun setDailyLimit(minutes: Int)

    @Query("UPDATE user_settings SET journey3dEnabled = :enabled WHERE id = 1")
    suspend fun setJourney3dEnabled(enabled: Boolean)

    @Query("UPDATE user_settings SET remindersEnabled = :enabled WHERE id = 1")
    suspend fun setRemindersEnabled(enabled: Boolean)

    @Query(
        "UPDATE user_settings SET currentFuel = 0, lifetimeFuel = 0, highestReachedPlanet = 'EARTH', " +
            "journeyDistance = 0, journeyStartDate = :journeyStart WHERE id = 1",
    )
    suspend fun resetJourney(journeyStart: String)
}
