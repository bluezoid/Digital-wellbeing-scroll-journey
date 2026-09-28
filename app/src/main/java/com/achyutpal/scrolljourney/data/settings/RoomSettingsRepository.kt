package com.achyutpal.scrolljourney.data.settings

import com.achyutpal.scrolljourney.data.local.dao.UserSettingsDao
import com.achyutpal.scrolljourney.data.local.entity.toDomain
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.calculator.DailyLimitRules
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate

/** The settings row is created lazily with defaults the first time anyone asks for it. */
class RoomSettingsRepository(
    private val dao: UserSettingsDao,
) : SettingsRepository {

    override suspend fun getSettings(): UserSettings {
        ensureRow()
        return checkNotNull(dao.get()).toDomain()
    }

    override fun observeSettings(): Flow<UserSettings> =
        dao.observe()
            .onStart { ensureRow() }
            .filterNotNull()
            .map { it.toDomain() }

    override suspend fun setOnboardingComplete(journeyStart: LocalDate) {
        ensureRow()
        dao.setOnboardingComplete(journeyStart.toString())
    }

    override suspend fun ensureJourneyStarted(journeyStart: LocalDate) {
        ensureRow()
        dao.ensureJourneyStarted(journeyStart.toString())
    }

    override suspend fun setDailyLimit(minutes: Int) {
        require(DailyLimitRules.isValid(minutes)) {
            "daily limit must be ${DailyLimitRules.MIN_MINUTES}–${DailyLimitRules.MAX_MINUTES}, was $minutes"
        }
        ensureRow()
        dao.setDailyLimit(minutes)
    }

    override suspend fun setJourney3dEnabled(enabled: Boolean) {
        ensureRow()
        dao.setJourney3dEnabled(enabled)
    }

    override suspend fun resetJourney(journeyStart: LocalDate) {
        ensureRow()
        dao.resetJourney(journeyStart.toString())
    }

    private suspend fun ensureRow() {
        dao.insertIfMissing(UserSettings().toEntity())
    }
}
