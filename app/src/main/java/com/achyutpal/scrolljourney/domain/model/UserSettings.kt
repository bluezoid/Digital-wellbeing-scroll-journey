package com.achyutpal.scrolljourney.domain.model

/** The single settings row: user preferences plus the rocket journey state. */
data class UserSettings(
    val dailyLimitMinutes: Int = DEFAULT_DAILY_LIMIT_MINUTES,
    val onboardingComplete: Boolean = false,
    val currentFuel: Int = 0,
    val lifetimeFuel: Int = 0,
    val highestReachedPlanet: Planet = Planet.EARTH,
    val journeyDistance: Long = 0,
) {
    companion object {
        const val DEFAULT_DAILY_LIMIT_MINUTES = 60
    }
}
