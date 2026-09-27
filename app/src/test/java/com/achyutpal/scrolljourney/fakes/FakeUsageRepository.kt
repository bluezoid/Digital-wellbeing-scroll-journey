package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.repository.UsageRepository

/** Returns fixed usage for every installed app; the real repository filters to the requested packages. */
class FakeUsageRepository(
    var usageByPackage: Map<String, Long> = emptyMap(),
    var error: Exception? = null,
) : UsageRepository {
    var lastWindow: Pair<Long, Long>? = null
    var lastPackages: Set<String>? = null

    override suspend fun getForegroundMillis(startMillis: Long, endMillis: Long, packages: Set<String>): Map<String, Long> {
        error?.let { throw it }
        lastWindow = startMillis to endMillis
        lastPackages = packages
        return usageByPackage.filterKeys { it in packages }
    }
}
