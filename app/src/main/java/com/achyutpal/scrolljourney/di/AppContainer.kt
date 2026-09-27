package com.achyutpal.scrolljourney.di

import android.content.Context
import com.achyutpal.scrolljourney.data.usage.AndroidUsageAccessRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository

/**
 * Manual dependency injection: the single place that decides which concrete class
 * backs each interface. Created once in [com.achyutpal.scrolljourney.ScrollJourneyApp].
 * (Hilt can replace this later without touching ViewModels.)
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val usageAccessRepository: UsageAccessRepository by lazy {
        AndroidUsageAccessRepository(appContext)
    }
}
