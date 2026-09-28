package com.achyutpal.scrolljourney.domain.model

import java.time.LocalDate

/** The single settings row: user preferences plus the rocket journey state. */
data class UserSettings(
    val dailyLimitMinutes: Int = DEFAULT_DAILY_LIMIT_MINUTES,
    val onboardingComplete: Boolean = false,
    val currentFuel: Int = 0,
    val lifetimeFuel: Int = 0,
    val highestReachedPlanet: Destination = Destination.EARTH,
    /** Estimated scroll distance (cm) of the days that fuelled this journey. */
    val journeyDistance: Long = 0,
    /** First day that counts for fuel. Days before it (e.g. backfilled history) never give fuel. */
    val journeyStartDate: LocalDate? = null,
) {
    val rocket: RocketState get() = RocketState(currentFuel, lifetimeFuel, highestReachedPlanet)

    companion object {
        const val DEFAULT_DAILY_LIMIT_MINUTES = 60
    }
}
