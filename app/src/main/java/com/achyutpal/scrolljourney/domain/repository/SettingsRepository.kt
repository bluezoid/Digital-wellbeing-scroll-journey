package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SettingsRepository {
    suspend fun getSettings(): UserSettings
    fun observeSettings(): Flow<UserSettings>
    /** Marks onboarding done and starts the journey on [journeyStart] (if it hasn't started). */
    suspend fun setOnboardingComplete(journeyStart: LocalDate)

    /** Sets the journey start date only if it's still empty. */
    suspend fun ensureJourneyStarted(journeyStart: LocalDate)

    /** @throws IllegalArgumentException if outside DailyLimitRules' range. */
    suspend fun setDailyLimit(minutes: Int)
}
