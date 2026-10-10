package com.achyutpal.scrolljourney.data.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.work.WorkManager
import com.achyutpal.scrolljourney.domain.calculator.JourneyViewRules
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository
import com.achyutpal.scrolljourney.domain.repository.DiagnosticsRepository
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.worker.DailySyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Collects the diagnostics report on the phone. */
class AndroidDiagnosticsRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    private val historyRepository: HistoryRepository,
    private val usageAccessRepository: UsageAccessRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val crashLog: CrashLog,
) : DiagnosticsRepository {

    override suspend fun report(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val graphics = deviceStatusRepository.graphicsCapability
        val today = LocalDate.now()
        DiagnosticsReport(
            generatedAt = "${Instant.now()} (${ZoneId.systemDefault()})",
            app = "${deviceStatusRepository.appVersionName} (${context.packageName})",
            device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            status = listOf(
                "Onboarding complete" to settings.onboardingComplete.toString(),
                "Daily limit" to "${settings.dailyLimitMinutes} min",
                "Tracked apps" to appRepository.observeEnabledApps().first().size.toString(),
                "Rocket" to "${settings.highestReachedPlanet} · fuel ${settings.currentFuel} · lifetime ${settings.lifetimeFuel}",
                "Journey since" to (settings.journeyStartDate?.toString() ?: "-"),
                "Usage Access" to (usageAccessRepository.checkUsageAccess() == UsageAccessState.GRANTED).toString(),
                "Notifications allowed" to deviceStatusRepository.areNotificationsEnabled().toString(),
                "Reminders on" to settings.remindersEnabled.toString(),
                "Background restricted" to deviceStatusRepository.isBackgroundRestricted().toString(),
                "Battery unrestricted" to deviceStatusRepository.isIgnoringBatteryOptimizations().toString(),
                "3D on / supported" to "${settings.journey3dEnabled} / ${JourneyViewRules.deviceSupports3d(graphics)}",
                "Low-RAM device" to graphics.isLowRamDevice.toString(),
                "OpenGL ES" to "%d.%d".format(graphics.glEsVersion shr 16, graphics.glEsVersion and 0xFFFF),
            ),
            jobs = jobs(),
            recentDays = historyRepository.getRange(today.minusDays(6), today).sortedBy { it.date }.map {
                "${it.date} · ${it.totalMinutes} · ${it.status ?: "-"} · ${if (it.finalized) "yes" else "live"} · ${it.fuelDelta}"
            },
            exitReasons = exitReasons(),
            crashes = crashLog.reports(),
        ).format()
    }

    private fun jobs(): List<String> = runCatching {
        val workManager = WorkManager.getInstance(context)
        listOf(DailySyncScheduler.NIGHTLY_WORK, DailySyncScheduler.REFRESH_WORK).flatMap { name ->
            workManager.getWorkInfosForUniqueWork(name).get().map { info ->
                val next = info.nextScheduleTimeMillis.takeIf { it in 1 until Long.MAX_VALUE }?.let { Instant.ofEpochMilli(it) }
                "$name: ${info.state} · attempts ${info.runAttemptCount} · next ${next ?: "-"}"
            }.ifEmpty { listOf("$name: not scheduled") }
        }
    }.getOrElse { listOf("couldn't read jobs: ${it.message}") }

    /** Android 11+: how the app's recent processes ended (crash, native crash, ANR, killed…). */
    private fun exitReasons(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return listOf("not available before Android 11")
        val manager = context.getSystemService(ActivityManager::class.java)
        return manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS).map { info ->
            "${Instant.ofEpochMilli(info.timestamp)} · ${reasonName(info.reason)} · ${info.description ?: ""}".trimEnd(' ', '·')
        }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
        ApplicationExitInfo.REASON_ANR -> "not responding (ANR)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
        ApplicationExitInfo.REASON_EXIT_SELF -> "closed normally"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "stopped by user"
        ApplicationExitInfo.REASON_USER_STOPPED -> "force stopped"
        ApplicationExitInfo.REASON_SIGNALED -> "killed by system"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission changed"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency died"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource use"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "start failure"
        else -> "other ($reason)"
    }

    private companion object {
        const val MAX_EXITS = 8
    }
}
