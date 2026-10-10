package com.achyutpal.scrolljourney.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.data.backup.RoomBackupRepository
import com.achyutpal.scrolljourney.domain.calculator.JourneyViewRules
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.BackupRepository
import com.achyutpal.scrolljourney.domain.repository.BackupSummary
import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository
import com.achyutpal.scrolljourney.domain.repository.DiagnosticsRepository
import com.achyutpal.scrolljourney.domain.repository.DocumentStore
import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.ui.widget.WidgetRefresher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

data class DeviceStatus(
    val usageAccess: UsageAccessState = UsageAccessState.UNKNOWN,
    val backgroundRestricted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
    val notificationsAllowed: Boolean = true,
)

data class SettingsUiState(
    val trackedAppCount: Int = 0,
    val limitMinutes: Int = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
    val device: DeviceStatus = DeviceStatus(),
    val rocket: RocketState = RocketState(),
    val journeyStartDate: LocalDate? = null,
    val versionName: String = "",
    val journey3dEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    /** False on phones that can't run the 3D scene: the switch is shown disabled. */
    val device3dSupported: Boolean = true,
    val showResetConfirm: Boolean = false,
    val resetDone: Boolean = false,
    val backup: BackupState = BackupState(),
    /** Diagnostics text ready to hand to the system share sheet (consumed by the screen). */
    val diagnosticsToShare: String? = null,
)

/** Export / import progress and results. */
data class BackupState(
    val busy: Boolean = false,
    /** A checked backup waiting for the user to confirm the restore. */
    val pendingRestore: BackupSummary? = null,
    val message: BackupMessage? = null,
)

sealed interface BackupMessage {
    data class Exported(val days: Int) : BackupMessage
    data class Restored(val summary: BackupSummary) : BackupMessage
    /** [reason] is a short technical reason (e.g. "made by a newer version of the app"). */
    data class Failed(val reason: String) : BackupMessage
}

/** Settings (spec §41–43): apps, limit, permission/background status, reset journey, privacy. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    appRepository: AppRepository,
    private val usageAccessRepository: UsageAccessRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val today: () -> LocalDate = LocalDate::now,
    /** Called after data changed outside a sync (reset, restore): the app redraws the widgets. */
    private val onDataChanged: suspend () -> Unit = {},
) : ViewModel() {

    private data class Dialogs(
        val showResetConfirm: Boolean = false,
        val resetDone: Boolean = false,
        val diagnosticsToShare: String? = null,
    )

    private val device = MutableStateFlow(DeviceStatus())
    private val dialogs = MutableStateFlow(Dialogs())
    private val backup = MutableStateFlow(BackupState())

    /** The backup text read from the picked file, kept until the user confirms or cancels. */
    private var pendingRestoreText: String? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        appRepository.observeEnabledApps(),
        device,
        dialogs,
        backup,
    ) { settings, apps, deviceStatus, dialog, backupState ->
        SettingsUiState(
            trackedAppCount = apps.size,
            limitMinutes = settings.dailyLimitMinutes,
            device = deviceStatus,
            rocket = settings.rocket,
            journeyStartDate = settings.journeyStartDate,
            versionName = deviceStatusRepository.appVersionName,
            journey3dEnabled = settings.journey3dEnabled,
            remindersEnabled = settings.remindersEnabled,
            device3dSupported = JourneyViewRules.deviceSupports3d(deviceStatusRepository.graphicsCapability),
            showResetConfirm = dialog.showResetConfirm,
            resetDone = dialog.resetDone,
            backup = backupState,
            diagnosticsToShare = dialog.diagnosticsToShare,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Called on every resume: the user may have changed things in Android Settings. */
    fun refreshDeviceStatus() {
        device.value = DeviceStatus(
            usageAccess = usageAccessRepository.checkUsageAccess(),
            backgroundRestricted = deviceStatusRepository.isBackgroundRestricted(),
            ignoringBatteryOptimizations = deviceStatusRepository.isIgnoringBatteryOptimizations(),
            notificationsAllowed = deviceStatusRepository.areNotificationsEnabled(),
        )
    }

    fun onJourney3dChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setJourney3dEnabled(enabled) }
    }

    fun onRemindersChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(enabled) }
    }

    fun onShareDiagnostics() {
        viewModelScope.launch {
            val text = runCatching { diagnosticsRepository.report() }
                .getOrElse { "Scroll Journey diagnostics failed: ${it.message}" }
            dialogs.update { it.copy(diagnosticsToShare = text) }
        }
    }

    fun onDiagnosticsShared() = dialogs.update { it.copy(diagnosticsToShare = null) }

    fun onResetClick() = dialogs.update { it.copy(showResetConfirm = true, resetDone = false) }

    fun onResetDismiss() = dialogs.update { it.copy(showResetConfirm = false) }

    fun onResetConfirm() {
        dialogs.update { it.copy(showResetConfirm = false) }
        viewModelScope.launch {
            settingsRepository.resetJourney(journeyStart = today())
            onDataChanged()
            dialogs.update { it.copy(resetDone = true) }
        }
    }

    /** File name offered in the system "Save as" screen. */
    fun suggestedBackupName(): String = RoomBackupRepository.suggestedFileName(today())

    /** The user picked where to save: write the whole database there. */
    fun onExport(uri: String) = runBackup {
        val text = backupRepository.export()
        documentStore.write(uri, text)
        BackupMessage.Exported(days = backupRepository.inspect(text).days)
    }

    /** The user picked a file to restore: read and check it, then ask for confirmation. */
    fun onImportPicked(uri: String) = runBackup {
        val text = documentStore.read(uri)
        val summary = backupRepository.inspect(text)
        pendingRestoreText = text
        backup.update { it.copy(pendingRestore = summary) }
        null
    }

    fun onRestoreConfirm() {
        val text = pendingRestoreText ?: return
        pendingRestoreText = null
        backup.update { it.copy(pendingRestore = null) }
        runBackup {
            val summary = backupRepository.restore(text)
            onDataChanged()
            BackupMessage.Restored(summary)
        }
    }

    fun onRestoreDismiss() {
        pendingRestoreText = null
        backup.update { it.copy(pendingRestore = null) }
    }

    /** Runs a backup step with a busy flag; any failure becomes a message instead of a crash. */
    private fun runBackup(step: suspend () -> BackupMessage?) {
        backup.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val message = try {
                step()
            } catch (e: InvalidBackupException) {
                BackupMessage.Failed(e.message ?: "invalid file")
            } catch (e: IOException) {
                BackupMessage.Failed(e.message ?: "couldn't access the file")
            } catch (e: SecurityException) {
                BackupMessage.Failed("no permission for that file")
            }
            backup.update { it.copy(busy = false, message = message ?: it.message) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScrollJourneyApp
                val container = app.container
                SettingsViewModel(
                    container.settingsRepository,
                    container.appRepository,
                    container.usageAccessRepository,
                    container.deviceStatusRepository,
                    container.backupRepository,
                    container.documentStore,
                    container.diagnosticsRepository,
                    onDataChanged = { WidgetRefresher.refresh(app) },
                )
            }
        }
    }
}
