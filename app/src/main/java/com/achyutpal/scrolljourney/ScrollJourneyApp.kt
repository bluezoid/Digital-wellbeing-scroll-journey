package com.achyutpal.scrolljourney

import android.app.Application
import com.achyutpal.scrolljourney.di.AppContainer

/** Process-wide entry point. Owns the [AppContainer] so every screen shares the same dependencies. */
class ScrollJourneyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
