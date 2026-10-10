package com.achyutpal.scrolljourney.di

import android.content.Context
import androidx.room.Room
import com.achyutpal.scrolljourney.data.apps.AndroidAppIconSource
import com.achyutpal.scrolljourney.data.apps.AndroidAppRepository
import com.achyutpal.scrolljourney.data.apps.AppIconSource
import com.achyutpal.scrolljourney.data.backup.AndroidDocumentStore
import com.achyutpal.scrolljourney.data.backup.RoomBackupRepository
import com.achyutpal.scrolljourney.data.diagnostics.AndroidDiagnosticsRepository
import com.achyutpal.scrolljourney.data.diagnostics.CrashLog
import com.achyutpal.scrolljourney.data.history.RoomHistoryRepository
import com.achyutpal.scrolljourney.data.journey.RoomJourneyRepository
import com.achyutpal.scrolljourney.data.local.ScrollJourneyDatabase
import com.achyutpal.scrolljourney.data.reminder.AndroidReminderGateway
import com.achyutpal.scrolljourney.data.settings.RoomSettingsRepository
import com.achyutpal.scrolljourney.data.system.AndroidDeviceStatusRepository
import com.achyutpal.scrolljourney.data.usage.AndroidUsageAccessRepository
import com.achyutpal.scrolljourney.data.usage.AndroidUsageDataSource
import com.achyutpal.scrolljourney.data.usage.AndroidUsageRepository
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.BackupRepository
import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository
import com.achyutpal.scrolljourney.domain.repository.DiagnosticsRepository
import com.achyutpal.scrolljourney.domain.repository.DocumentStore
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.JourneyRepository
import com.achyutpal.scrolljourney.domain.repository.ReminderGateway
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.domain.repository.UsageRepository
import com.achyutpal.scrolljourney.domain.usecase.ApplyJourneyFuelUseCase
import com.achyutpal.scrolljourney.domain.usecase.CheckLimitRemindersUseCase
import com.achyutpal.scrolljourney.domain.usecase.SyncUsageHistoryUseCase
import com.achyutpal.scrolljourney.ui.widget.WidgetRefresher

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

    val usageRepository: UsageRepository by lazy {
        AndroidUsageRepository(AndroidUsageDataSource(appContext))
    }

    val historyRepository: HistoryRepository by lazy {
        RoomHistoryRepository(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        RoomSettingsRepository(database.userSettingsDao())
    }

    val deviceStatusRepository: DeviceStatusRepository by lazy {
        AndroidDeviceStatusRepository(appContext)
    }

    val crashLog: CrashLog by lazy { CrashLog(appContext) }

    val diagnosticsRepository: DiagnosticsRepository by lazy {
        AndroidDiagnosticsRepository(
            appContext, settingsRepository, appRepository, historyRepository,
            usageAccessRepository, deviceStatusRepository, crashLog,
        )
    }

    val reminderGateway: ReminderGateway by lazy { AndroidReminderGateway(appContext) }

    val checkLimitReminders: CheckLimitRemindersUseCase by lazy {
        CheckLimitRemindersUseCase(settingsRepository, historyRepository, reminderGateway)
    }

    val backupRepository: BackupRepository by lazy {
        RoomBackupRepository(database.backupDao(), appVersion = deviceStatusRepository.appVersionName)
    }

    val documentStore: DocumentStore by lazy { AndroidDocumentStore(appContext.contentResolver) }

    val journeyRepository: JourneyRepository by lazy {
        RoomJourneyRepository(database)
    }

    val syncUsageHistoryUseCase: SyncUsageHistoryUseCase by lazy {
        SyncUsageHistoryUseCase(
            usageAccessRepository = usageAccessRepository,
            appRepository = appRepository,
            usageRepository = usageRepository,
            historyRepository = historyRepository,
            settingsRepository = settingsRepository,
            applyJourneyFuel = ApplyJourneyFuelUseCase(journeyRepository, settingsRepository),
            onSynced = {
                WidgetRefresher.refresh(appContext)
                checkLimitReminders()
            },
        )
    }
}
