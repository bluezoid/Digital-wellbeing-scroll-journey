package com.achyutpal.scrolljourney.data.usage

import android.util.Log
import com.achyutpal.scrolljourney.domain.calculator.UsageCalculator
import com.achyutpal.scrolljourney.domain.model.ForegroundUsage
import com.achyutpal.scrolljourney.domain.repository.UsageRepository

class AndroidUsageRepository(
    private val dataSource: UsageDataSource,
) : UsageRepository {

    override suspend fun getForegroundUsage(
        startMillis: Long,
        endMillis: Long,
        packages: Set<String>,
    ): ForegroundUsage {
        val events = dataSource.queryEvents(startMillis, endMillis)
        val millis = UsageCalculator.foregroundMillis(events, startMillis, endMillis, packages)
        Log.d(TAG, "events=${events.size} window=$startMillis..$endMillis usage(ms)=$millis")
        return ForegroundUsage(millis, events.size)
    }

    private companion object {
        const val TAG = "UsageTracking"
    }
}
