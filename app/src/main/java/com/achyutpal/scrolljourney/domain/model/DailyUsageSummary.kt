package com.achyutpal.scrolljourney.domain.model

import java.time.LocalDate

/** Time spent in one tracked app during a day. */
data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundMillis: Long,
) {
    /** Whole minutes (rounded down), the unit the limit and history use. */
    val minutes: Int get() = (foregroundMillis / MILLIS_PER_MINUTE).toInt()

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/** Usage of the selected apps for one calendar day, most-used first. */
data class DailyUsageSummary(
    val date: LocalDate,
    val apps: List<AppUsage>,
) {
    /** Sum of the per-app minutes, so the breakdown on screen always adds up to the total. */
    val totalMinutes: Int get() = apps.sumOf { it.minutes }
    val hasAnyUsage: Boolean get() = apps.any { it.foregroundMillis > 0 }
}
