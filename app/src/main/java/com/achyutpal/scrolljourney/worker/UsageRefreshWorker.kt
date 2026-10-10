package com.achyutpal.scrolljourney.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.achyutpal.scrolljourney.ScrollJourneyApp

/**
 * Every 30 minutes: syncs yesterday + today (cheap: two usage queries), so today's numbers stay
 * fresh for the widget and the near-limit reminder, and yesterday is finalized soon after midnight.
 */
class UsageRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ScrollJourneyApp).container
        if (!container.settingsRepository.getSettings().onboardingComplete) return Result.success()

        val result = container.syncUsageHistoryUseCase(backfillDays = 1)
        Log.d(TAG, "refresh result=${result::class.simpleName}")
        // A failed refresh just waits for the next one in 30 minutes: no retry storm.
        return Result.success()
    }

    companion object {
        const val TAG = "UsageRefreshWorker"
    }
}
