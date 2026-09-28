package com.achyutpal.scrolljourney.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.UserSettings
import java.time.LocalDate

/** Exactly one row, always id = 1. */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val dailyLimitMinutes: Int,
    val onboardingComplete: Boolean,
    val currentFuel: Int,
    val lifetimeFuel: Int,
    val highestReachedPlanet: Destination,
    val journeyDistance: Long,
    /** Added in DB v4, "YYYY-MM-DD". */
    val journeyStartDate: String?,
    /** Added in DB v5; existing users get 3D switched on. */
    @ColumnInfo(defaultValue = "1") val journey3dEnabled: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

fun UserSettingsEntity.toDomain() = UserSettings(
    dailyLimitMinutes = dailyLimitMinutes,
    onboardingComplete = onboardingComplete,
    currentFuel = currentFuel,
    lifetimeFuel = lifetimeFuel,
    highestReachedPlanet = highestReachedPlanet,
    journeyDistance = journeyDistance,
    journeyStartDate = journeyStartDate?.let(LocalDate::parse),
    journey3dEnabled = journey3dEnabled,
)

fun UserSettings.toEntity() = UserSettingsEntity(
    dailyLimitMinutes = dailyLimitMinutes,
    onboardingComplete = onboardingComplete,
    currentFuel = currentFuel,
    lifetimeFuel = lifetimeFuel,
    highestReachedPlanet = highestReachedPlanet,
    journeyDistance = journeyDistance,
    journeyStartDate = journeyStartDate?.toString(),
    journey3dEnabled = journey3dEnabled,
)
