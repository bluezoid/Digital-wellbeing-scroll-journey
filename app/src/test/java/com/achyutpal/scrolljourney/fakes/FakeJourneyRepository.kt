package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.repository.JourneyRepository
import java.time.LocalDate

/** Works on the same fake history + settings, mirroring the real "apply once" transaction. */
class FakeJourneyRepository(
    private val history: FakeHistoryRepository,
    private val settings: FakeSettingsRepository,
) : JourneyRepository {

    override suspend fun daysAwaitingFuel(from: LocalDate): List<DailyRecord> =
        history.records.values.filter { it.finalized && !it.fuelApplied && it.date >= from }.sortedBy { it.date }

    override suspend fun applyDay(day: DailyRecord, fuelDelta: Int, update: (RocketState) -> RocketState): Boolean {
        if (!history.markFuelApplied(day.date, fuelDelta)) return false
        val current = settings.state.value
        val next = update(current.rocket)
        settings.state.value = current.copy(
            currentFuel = next.currentFuel,
            lifetimeFuel = next.lifetimeFuel,
            highestReachedPlanet = next.highestReachedPlanet,
            journeyDistance = current.journeyDistance + day.distance,
        )
        return true
    }
}
