package com.achyutpal.scrolljourney.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Two background jobs, both stored by WorkManager in its own database (they survive app restarts
 * and phone reboots with no boot receiver of our own):
 *
 * - **Nightly** ([NIGHTLY_WORK]): a one-time job aimed at ~00:30 that finalizes yesterday and the
 *   last week. Each run appends the next one, re-aimed at 00:30, so it can't drift (a 24 h
 *   periodic job drifted from 00:30 to 23:32 in real use).
 * - **Refresh** ([REFRESH_WORK]): every 15 min, syncs just yesterday + today, so the widget and the
 *   limit reminders stay current without opening the app.
 */
object DailySyncScheduler {

    const val NIGHTLY_WORK = "nightly-sync"
    const val REFRESH_WORK = "usage-refresh"

    /** The earlier 24 h periodic job (drifted); cancelled on start. */
    private const val LEGACY_PERIODIC_WORK = "daily-usage-sync"

    const val REFRESH_MINUTES = 15L // Android's minimum for periodic work

    /** On every app start: make sure both jobs exist, without disturbing their current timing. */
    fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK)
        workManager.enqueueUniqueWork(NIGHTLY_WORK, ExistingWorkPolicy.KEEP, nightlyRequest(now))
        workManager.enqueueUniquePeriodicWork(
            REFRESH_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UsageRefreshWorker>(REFRESH_MINUTES, TimeUnit.MINUTES)
                .addTag(UsageRefreshWorker.TAG)
                .build(),
        )
    }

    /**
     * Called by the nightly job as it finishes. APPEND makes the next run wait for this one to
     * end; WorkManager starts its delay at that moment, so it lands on the next 00:30.
     */
    fun scheduleNextNight(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NIGHTLY_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, nightlyRequest(now))
    }

    private fun nightlyRequest(now: ZonedDateTime): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<DailyUsageWorker>()
            .setInitialDelay(DailySyncPolicy.initialDelay(now).toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(DailyUsageWorker.TAG)
            .build()
}
