package com.achyutpal.scrolljourney.di

import android.content.Context
import androidx.room.Room
import com.achyutpal.scrolljourney.data.apps.AndroidAppIconSource
import com.achyutpal.scrolljourney.data.apps.AndroidAppRepository
import com.achyutpal.scrolljourney.data.apps.AppIconSource
import com.achyutpal.scrolljourney.data.local.ScrollJourneyDatabase
import com.achyutpal.scrolljourney.data.usage.AndroidUsageAccessRepository
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository

/**
 * Manual dependency injection: the single place that decides which concrete class
 * backs each interface. Created once in [com.achyutpal.scrolljourney.ScrollJourneyApp].
 * (Hilt can replace this later without touching ViewModels.)
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: ScrollJourneyDatabase by lazy {
        Room.databaseBuilder(appContext, ScrollJourneyDatabase::class.java, ScrollJourneyDatabase.NAME)
            .build()
    }

    val usageAccessRepository: UsageAccessRepository by lazy {
        AndroidUsageAccessRepository(appContext)
    }

    val appRepository: AppRepository by lazy {
        AndroidAppRepository(
            packageManager = appContext.packageManager,
            ownPackageName = appContext.packageName,
            selectedAppDao = database.selectedAppDao(),
        )
    }

    val appIconSource: AppIconSource by lazy {
        AndroidAppIconSource(appContext.packageManager)
    }
}
