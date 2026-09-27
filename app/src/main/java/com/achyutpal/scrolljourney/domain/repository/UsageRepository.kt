package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.ForegroundUsage

/** Reads foreground time from the system's usage log. */
interface UsageRepository {
    /** Foreground time per package within [startMillis, endMillis]. Packages with no usage may be missing. */
    suspend fun getForegroundUsage(
        startMillis: Long,
        endMillis: Long,
        packages: Set<String>,
    ): ForegroundUsage
}
