package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.calculator.RocketProgressCalculator
import com.achyutpal.scrolljourney.domain.repository.JourneyRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository

/**
 * Feeds every finished day since the journey started into the rocket, oldest first, each
 * exactly once. Today never counts until it's finalized (i.e. tomorrow). Days before the
 * journey start (backfilled history) never give fuel.
 */
class ApplyJourneyFuelUseCase(
    private val journeyRepository: JourneyRepository,
    private val settingsRepository: SettingsRepository,
) {
    /** @return how many days were applied by this call. */
    suspend operator fun invoke(): Int {
        val start = settingsRepository.getSettings().journeyStartDate ?: return 0
        var applied = 0
        for (day in journeyRepository.daysAwaitingFuel(from = start)) {
            val status = day.status ?: continue // every finalized day has a status; stay safe anyway
            val wasApplied = journeyRepository.applyDay(day, RocketProgressCalculator.fuelDelta(status)) { rocket ->
                RocketProgressCalculator.apply(rocket, status)
            }
            if (wasApplied) applied++
        }
        return applied
    }
}
