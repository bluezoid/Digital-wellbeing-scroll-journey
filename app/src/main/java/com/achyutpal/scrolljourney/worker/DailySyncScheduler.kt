package com.achyutpal.scrolljourney.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Enqueues the daily job once. WorkManager stores it in its own database, so it survives
 * app restarts, force-stops being lifted, and phone reboots, with no boot receiver of our own.
 */
object DailySyncScheduler {

    /** Unique name: calling [schedule] on every app start never creates duplicates. */
    const val UNIQUE_WORK_NAME = "daily-usage-sync"

    fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val request = PeriodicWorkRequestBuilder<DailyUsageWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(DailySyncPolicy.initialDelay(now).toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(DailyUsageWorker.TAG)
            .build()

        // KEEP: if it's already scheduled, leave its timing alone (don't push it back on every launch).
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
