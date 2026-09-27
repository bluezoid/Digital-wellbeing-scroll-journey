package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.UserSettings

/** Allowed values for the combined daily limit. */
object DailyLimitRules {
    const val MIN_MINUTES = 15
    const val MAX_MINUTES = 240
    const val STEP_MINUTES = 5
    const val DEFAULT_MINUTES = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES

    fun isValid(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES

    /** Clamps into range and rounds to the nearest step (for the slider). */
    fun snap(minutes: Int): Int {
        val clamped = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        val steps = Math.round((clamped - MIN_MINUTES).toDouble() / STEP_MINUTES).toInt()
        return (MIN_MINUTES + steps * STEP_MINUTES).coerceAtMost(MAX_MINUTES)
    }
}
