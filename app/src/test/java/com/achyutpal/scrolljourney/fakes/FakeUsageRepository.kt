package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.ForegroundUsage
import com.achyutpal.scrolljourney.domain.repository.UsageRepository

/**
 * Answers each window query with [provider]; by default every window returns [usageByPackage]
 * (filtered to the requested packages) and counts as having data.
 */
class FakeUsageRepository(
    var usageByPackage: Map<String, Long> = emptyMap(),
    var error: Exception? = null,
) : UsageRepository {

    var provider: (start: Long, end: Long) -> ForegroundUsage = { _, _ -> ForegroundUsage(usageByPackage, eventCount = 1) }
    val calls = mutableListOf<Pair<Long, Long>>()
    var lastPackages: Set<String>? = null

    override suspend fun getForegroundUsage(startMillis: Long, endMillis: Long, packages: Set<String>): ForegroundUsage {
        error?.let { throw it }
        calls += startMillis to endMillis
        lastPackages = packages
        val usage = provider(startMillis, endMillis)
        return usage.copy(millisByPackage = usage.millisByPackage.filterKeys { it in packages })
    }
}
