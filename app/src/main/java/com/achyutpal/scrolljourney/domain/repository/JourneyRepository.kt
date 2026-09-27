package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.RocketState
import java.time.LocalDate

/** Applies finished days to the rocket. Owns the "exactly once" guarantee. */
interface JourneyRepository {

    /** Finalized days on/after [from] whose fuel hasn't been applied yet, oldest first. */
    suspend fun daysAwaitingFuel(from: LocalDate): List<DailyRecord>

    /**
     * In ONE database transaction: marks [day] as fuel-applied (only if it wasn't already),
     * then saves the rocket state returned by [update] and adds the day's distance to the journey.
     * Returns false and changes nothing if the day was already applied, so running twice
     * (or from two places at once) can never double-count.
     */
    suspend fun applyDay(day: DailyRecord, fuelDelta: Int, update: (RocketState) -> RocketState): Boolean
}
