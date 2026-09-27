package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.UsageEvent
import com.achyutpal.scrolljourney.domain.model.UsageEventType

/**
 * Turns Android's raw RESUMED/PAUSED events into foreground time per app within a
 * time window [windowStart, windowEnd] (e.g. local midnight → now).
 *
 * Edge cases handled:
 * - An app has several activities: it's "in the foreground" while ANY of them is resumed,
 *   so switching screens inside Instagram isn't counted twice.
 * - App already open when the window starts (e.g. scrolling across midnight): its first
 *   event in the window is a PAUSED with no RESUMED before it → counted from windowStart.
 * - App still open at windowEnd → counted up to windowEnd.
 * - Screen off / shutdown → every open session ends there.
 */
object UsageCalculator {

    fun foregroundMillis(
        events: List<UsageEvent>,
        windowStart: Long,
        windowEnd: Long,
        trackedPackages: Set<String>,
    ): Map<String, Long> {
        val totals = HashMap<String, Long>()
        val resumedActivities = HashMap<String, MutableSet<String>>()
        val sessionStart = HashMap<String, Long>()
        val seenInWindow = HashSet<String>()

        fun closeSession(packageName: String, at: Long) {
            val start = sessionStart.remove(packageName) ?: return
            totals.merge(packageName, (at - start).coerceAtLeast(0), Long::plus)
        }

        fun closeAll(at: Long) {
            sessionStart.keys.toList().forEach { closeSession(it, at) }
            resumedActivities.clear()
        }

        events
            .asSequence()
            .filter { it.timestamp in windowStart..windowEnd }
            .sortedBy { it.timestamp }
            .forEach { event ->
                when (event.type) {
                    UsageEventType.SCREEN_OFF, UsageEventType.SHUTDOWN -> closeAll(event.timestamp)

                    UsageEventType.RESUMED -> {
                        if (event.packageName !in trackedPackages) return@forEach
                        seenInWindow += event.packageName
                        val activities = resumedActivities.getOrPut(event.packageName) { mutableSetOf() }
                        if (activities.isEmpty()) sessionStart[event.packageName] = event.timestamp
                        activities += event.className
                    }

                    UsageEventType.PAUSED -> {
                        if (event.packageName !in trackedPackages) return@forEach
                        val firstEventForApp = seenInWindow.add(event.packageName)
                        val activities = resumedActivities[event.packageName]
                        when {
                            activities.isNullOrEmpty() && firstEventForApp ->
                                // It was already in the foreground when the window began.
                                totals.merge(event.packageName, event.timestamp - windowStart, Long::plus)

                            activities != null && activities.remove(event.className) && activities.isEmpty() ->
                                closeSession(event.packageName, event.timestamp)
                        }
                    }
                }
            }

        closeAll(windowEnd)
        return totals
    }
}
