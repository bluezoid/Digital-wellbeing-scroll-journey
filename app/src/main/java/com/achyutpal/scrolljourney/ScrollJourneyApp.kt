package com.achyutpal.scrolljourney

import android.app.Application
import com.achyutpal.scrolljourney.di.AppContainer
import com.achyutpal.scrolljourney.worker.DailySyncScheduler

/**
 * Process-wide entry point. Owns the [AppContainer] so every screen shares the same dependencies,
 * and makes sure the daily background sync is scheduled.
 */
class ScrollJourneyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.crashLog.install(appVersion = container.deviceStatusRepository.appVersionName)
        DailySyncScheduler.schedule(this)
    }
}
