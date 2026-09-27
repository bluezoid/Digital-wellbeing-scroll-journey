package com.achyutpal.scrolljourney.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.achyutpal.scrolljourney.domain.model.UsageEvent
import com.achyutpal.scrolljourney.domain.model.UsageEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The only place that talks to [UsageStatsManager]. Everything above sees plain [UsageEvent]s. */
interface UsageDataSource {
    suspend fun queryEvents(startMillis: Long, endMillis: Long): List<UsageEvent>
}

/**
 * Reads Android's usage event log.
 *
 * queryEvents() returns a cursor-like [UsageEvents]: we reuse one Event object and copy
 * the fields we need. Android keeps these detailed events only for a few days.
 * If Usage Access is off it silently returns nothing, so callers check access first.
 */
class AndroidUsageDataSource(context: Context) : UsageDataSource {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override suspend fun queryEvents(startMillis: Long, endMillis: Long): List<UsageEvent> =
        withContext(Dispatchers.IO) {
            val result = ArrayList<UsageEvent>()
            val events = usageStatsManager.queryEvents(startMillis, endMillis) ?: return@withContext result
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val type = event.eventType.toUsageEventType() ?: continue
                result += UsageEvent(
                    packageName = event.packageName,
                    className = event.className.orEmpty(),
                    timestamp = event.timeStamp,
                    type = type,
                )
            }
            result
        }

    @Suppress("DEPRECATION")
    private fun Int.toUsageEventType(): UsageEventType? = when (this) {
        // ACTIVITY_RESUMED/PAUSED (API 29) share values with MOVE_TO_FOREGROUND/BACKGROUND (API 21).
        UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.RESUMED
        UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventType.PAUSED
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_OFF
        UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.SHUTDOWN
        else -> null
    }
}
