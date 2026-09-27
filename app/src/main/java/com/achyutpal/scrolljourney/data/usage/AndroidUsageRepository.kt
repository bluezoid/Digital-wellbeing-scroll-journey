package com.achyutpal.scrolljourney.data.usage

import android.util.Log
import com.achyutpal.scrolljourney.domain.calculator.UsageCalculator
import com.achyutpal.scrolljourney.domain.repository.UsageRepository

class AndroidUsageRepository(
    private val dataSource: UsageDataSource,
) : UsageRepository {

    override suspend fun getForegroundMillis(
        startMillis: Long,
        endMillis: Long,
        packages: Set<String>,
    ): Map<String, Long> {
        val events = dataSource.queryEvents(startMillis, endMillis)
        val result = UsageCalculator.foregroundMillis(events, startMillis, endMillis, packages)
        Log.d(TAG, "events=${events.size} window=$startMillis..$endMillis usage(ms)=$result")
        return result
    }

    private companion object {
        const val TAG = "UsageTracking"
    }
}
