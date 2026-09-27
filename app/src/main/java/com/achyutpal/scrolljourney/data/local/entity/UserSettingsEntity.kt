package com.achyutpal.scrolljourney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.achyutpal.scrolljourney.domain.model.Planet
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
    val highestReachedPlanet: Planet,
    val journeyDistance: Long,
    /** Added in DB v4, "YYYY-MM-DD". */
    val journeyStartDate: String?,
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
)

fun UserSettings.toEntity() = UserSettingsEntity(
    dailyLimitMinutes = dailyLimitMinutes,
    onboardingComplete = onboardingComplete,
    currentFuel = currentFuel,
    lifetimeFuel = lifetimeFuel,
    highestReachedPlanet = highestReachedPlanet,
    journeyDistance = journeyDistance,
    journeyStartDate = journeyStartDate?.toString(),
)
