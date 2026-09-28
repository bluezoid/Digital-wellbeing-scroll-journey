package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState

/** Pure rocket rules (spec §15–17, §38–40). No Android, no database. */
object RocketProgressCalculator {

    fun fuelDelta(status: DayStatus): Int = when (status) {
        DayStatus.GREEN -> 1
        DayStatus.YELLOW -> 0
        DayStatus.RED -> -1
    }

    /** Applies one finished day to the rocket. */
    fun apply(state: RocketState, status: DayStatus): RocketState {
        val delta = fuelDelta(status)
        val fuel = (state.currentFuel + delta).coerceAtLeast(0)
        val reachedNow = Destination.forFuel(fuel)
        return RocketState(
            currentFuel = fuel,
            lifetimeFuel = state.lifetimeFuel + delta.coerceAtLeast(0),
            highestReachedPlanet = maxOf(state.highestReachedPlanet, reachedNow),
        )
    }
}
