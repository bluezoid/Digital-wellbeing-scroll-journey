package com.achyutpal.scrolljourney.data.backup

import com.achyutpal.scrolljourney.data.local.entity.DailyAppUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.DailyUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import kotlinx.serialization.Serializable

/**
 * The exported backup: every table of the database, as plain JSON.
 * Enums are written by name and dates as "YYYY-MM-DD", the same as in the database.
 * [version] lets a future app read old backups (and refuse ones newer than it understands).
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: String,
    val appVersion: String,
    val settings: SettingsRow,
    val apps: List<AppRow>,
    val days: List<DayRow>,
    val appMinutes: List<AppMinutesRow>,
) {
    @Serializable
    data class SettingsRow(
        val dailyLimitMinutes: Int,
        val onboardingComplete: Boolean,
        val currentFuel: Int,
        val lifetimeFuel: Int,
        val highestReachedPlanet: String,
        val journeyDistance: Long,
        val journeyStartDate: String? = null,
        val journey3dEnabled: Boolean = true,
        val remindersEnabled: Boolean = true,
    )

    @Serializable
    data class AppRow(val packageName: String, val appName: String, val enabled: Boolean, val addedAt: Long)

    @Serializable
    data class DayRow(
        val date: String,
        val totalMinutes: Int,
        val status: String? = null,
        val limitMinutes: Int? = null,
        val fuelDelta: Int,
        val distance: Long,
        val finalized: Boolean,
        val fuelApplied: Boolean,
    )

    @Serializable
    data class AppMinutesRow(val date: String, val packageName: String, val minutes: Int)

    companion object {
        const val FORMAT = "scroll-journey-backup"
        const val VERSION = 1
    }
}

// --- entity ↔ backup row ----------------------------------------------------------------

fun UserSettingsEntity.toRow() = BackupFile.SettingsRow(
    dailyLimitMinutes, onboardingComplete, currentFuel, lifetimeFuel, highestReachedPlanet.name,
    journeyDistance, journeyStartDate, journey3dEnabled, remindersEnabled,
)

fun BackupFile.SettingsRow.toEntity() = UserSettingsEntity(
    dailyLimitMinutes = dailyLimitMinutes,
    onboardingComplete = onboardingComplete,
    currentFuel = currentFuel,
    lifetimeFuel = lifetimeFuel,
    highestReachedPlanet = Destination.valueOf(highestReachedPlanet),
    journeyDistance = journeyDistance,
    journeyStartDate = journeyStartDate,
    journey3dEnabled = journey3dEnabled,
    remindersEnabled = remindersEnabled,
)

fun SelectedAppEntity.toRow() = BackupFile.AppRow(packageName, appName, enabled, addedAt)
fun BackupFile.AppRow.toEntity() = SelectedAppEntity(packageName, appName, enabled, addedAt)

fun DailyUsageEntity.toRow() = BackupFile.DayRow(
    date, totalMinutes, status?.name, limitMinutes, fuelDelta, distance, finalized, fuelApplied,
)

fun BackupFile.DayRow.toEntity() = DailyUsageEntity(
    date = date,
    totalMinutes = totalMinutes,
    status = status?.let(DayStatus::valueOf),
    limitMinutes = limitMinutes,
    fuelDelta = fuelDelta,
    distance = distance,
    finalized = finalized,
    fuelApplied = fuelApplied,
)

fun DailyAppUsageEntity.toRow() = BackupFile.AppMinutesRow(date, packageName, minutes)
fun BackupFile.AppMinutesRow.toEntity() = DailyAppUsageEntity(date, packageName, minutes)
