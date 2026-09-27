package com.achyutpal.scrolljourney.data.journey

import androidx.room.withTransaction
import com.achyutpal.scrolljourney.data.local.ScrollJourneyDatabase
import com.achyutpal.scrolljourney.data.local.entity.toDomain
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.JourneyRepository
import java.time.LocalDate

class RoomJourneyRepository(
    private val database: ScrollJourneyDatabase,
) : JourneyRepository {

    private val dailyUsageDao = database.dailyUsageDao()
    private val settingsDao = database.userSettingsDao()

    override suspend fun daysAwaitingFuel(from: LocalDate): List<DailyRecord> =
        dailyUsageDao.getAwaitingFuel(from.toString()).map { it.toDomain() }

    /**
     * The conditional UPDATE (`... WHERE fuelApplied = 0`) and the rocket save happen in one
     * transaction: if the day was already applied, nothing else is written; if the app dies
     * mid-way, neither write survives. That's what makes fuel "exactly once".
     */
    override suspend fun applyDay(day: DailyRecord, fuelDelta: Int, update: (RocketState) -> RocketState): Boolean =
        database.withTransaction {
            val applied = dailyUsageDao.markFuelApplied(day.date.toString(), fuelDelta) == 1
            if (applied) {
                settingsDao.insertIfMissing(UserSettings().toEntity())
                val current = checkNotNull(settingsDao.get()).toDomain().rocket
                val next = update(current)
                settingsDao.saveRocket(next.currentFuel, next.lifetimeFuel, next.highestReachedPlanet, day.distance)
            }
            applied
        }
}
