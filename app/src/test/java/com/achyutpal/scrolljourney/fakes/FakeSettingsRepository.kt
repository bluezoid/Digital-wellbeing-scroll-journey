package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.calculator.DailyLimitRules
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)

    override suspend fun getSettings() = state.value
    override fun observeSettings(): Flow<UserSettings> = state
    override suspend fun setOnboardingComplete(journeyStart: LocalDate) {
        state.value = state.value.copy(
            onboardingComplete = true,
            journeyStartDate = state.value.journeyStartDate ?: journeyStart,
        )
    }

    override suspend fun ensureJourneyStarted(journeyStart: LocalDate) {
        state.value = state.value.copy(journeyStartDate = state.value.journeyStartDate ?: journeyStart)
    }

    override suspend fun resetJourney(journeyStart: LocalDate) {
        state.value = state.value.copy(
            currentFuel = 0,
            lifetimeFuel = 0,
            highestReachedPlanet = Planet.EARTH,
            journeyDistance = 0,
            journeyStartDate = journeyStart,
        )
    }

    override suspend fun setDailyLimit(minutes: Int) {
        require(DailyLimitRules.isValid(minutes))
        state.value = state.value.copy(dailyLimitMinutes = minutes)
    }
}
