package com.achyutpal.scrolljourney.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.achyutpal.scrolljourney.ScrollJourneyApp

/**
 * Background safety net for history (spec §49–51). Runs the SAME idempotent sync as opening the
 * app: finalizes finished days before Android deletes their events, applies rocket fuel once,
 * and saves today so far. Because the sync is idempotent, running it twice, or at the same
 * time as the app, changes nothing extra.
 *
 * CoroutineWorker = a Worker whose doWork() is a suspend function, off the main thread.
 */
class DailyUsageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ScrollJourneyApp).container
        if (!container.settingsRepository.getSettings().onboardingComplete) {
            Log.d(TAG, "onboarding not finished, nothing to sync")
            return Result.success()
        }

        val result = container.syncUsageHistoryUseCase()
        val outcome = DailySyncPolicy.outcomeFor(result)
        Log.d(TAG, "sync result=${result::class.simpleName} outcome=$outcome attempt=$runAttemptCount")
        return when (outcome) {
            DailySyncPolicy.Outcome.SUCCESS -> Result.success()
            DailySyncPolicy.Outcome.RETRY -> Result.retry()
        }
    }

    companion object {
        const val TAG = "DailyUsageWorker"
    }
}
