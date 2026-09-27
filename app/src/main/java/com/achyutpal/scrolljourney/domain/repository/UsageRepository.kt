package com.achyutpal.scrolljourney.domain.repository

/** Reads foreground time from the system's usage log. */
interface UsageRepository {
    /**
     * Foreground milliseconds per package within [startMillis, endMillis].
     * Packages with no usage may be missing from the map.
     */
    suspend fun getForegroundMillis(
        startMillis: Long,
        endMillis: Long,
        packages: Set<String>,
    ): Map<String, Long>
}
