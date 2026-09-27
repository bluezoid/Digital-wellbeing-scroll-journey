package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getSettings(): UserSettings
    fun observeSettings(): Flow<UserSettings>
    suspend fun setOnboardingComplete()

    /** @throws IllegalArgumentException if outside DailyLimitRules' range. */
    suspend fun setDailyLimit(minutes: Int)
}
