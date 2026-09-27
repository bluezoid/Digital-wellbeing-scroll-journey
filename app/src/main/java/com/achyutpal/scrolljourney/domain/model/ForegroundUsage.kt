package com.achyutpal.scrolljourney.domain.model

/**
 * Result of reading one time window from the usage log.
 * [eventCount] = how many raw events Android still had for the window, for any app.
 * Zero means "no data available" (e.g. too old), which is different from "0 minutes used".
 */
data class ForegroundUsage(
    val millisByPackage: Map<String, Long>,
    val eventCount: Int,
) {
    val hasData: Boolean get() = eventCount > 0
}
