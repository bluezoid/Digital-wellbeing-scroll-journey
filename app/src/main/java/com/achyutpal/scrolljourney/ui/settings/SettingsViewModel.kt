package com.achyutpal.scrolljourney.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.calculator.JourneyViewRules
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DeviceStatus(
    val usageAccess: UsageAccessState = UsageAccessState.UNKNOWN,
    val backgroundRestricted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
)

data class SettingsUiState(
    val trackedAppCount: Int = 0,
    val limitMinutes: Int = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
    val device: DeviceStatus = DeviceStatus(),
    val rocket: RocketState = RocketState(),
    val journeyStartDate: LocalDate? = null,
    val versionName: String = "",
    val journey3dEnabled: Boolean = true,
    /** False on phones that can't run the 3D scene: the switch is shown disabled. */
    val device3dSupported: Boolean = true,
    val showResetConfirm: Boolean = false,
    val resetDone: Boolean = false,
)

/** Settings (spec §41–43): apps, limit, permission/background status, reset journey, privacy. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    appRepository: AppRepository,
    private val usageAccessRepository: UsageAccessRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private data class Dialogs(val showResetConfirm: Boolean = false, val resetDone: Boolean = false)

    private val device = MutableStateFlow(DeviceStatus())
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        appRepository.observeEnabledApps(),
        device,
        dialogs,
    ) { settings, apps, deviceStatus, dialog ->
        SettingsUiState(
            trackedAppCount = apps.size,
            limitMinutes = settings.dailyLimitMinutes,
            device = deviceStatus,
            rocket = settings.rocket,
            journeyStartDate = settings.journeyStartDate,
            versionName = deviceStatusRepository.appVersionName,
            journey3dEnabled = settings.journey3dEnabled,
            device3dSupported = JourneyViewRules.deviceSupports3d(deviceStatusRepository.graphicsCapability),
            showResetConfirm = dialog.showResetConfirm,
            resetDone = dialog.resetDone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Called on every resume: the user may have changed things in Android Settings. */
    fun refreshDeviceStatus() {
        device.value = DeviceStatus(
            usageAccess = usageAccessRepository.checkUsageAccess(),
            backgroundRestricted = deviceStatusRepository.isBackgroundRestricted(),
            ignoringBatteryOptimizations = deviceStatusRepository.isIgnoringBatteryOptimizations(),
        )
    }

    fun onJourney3dChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setJourney3dEnabled(enabled) }
    }

    fun onResetClick() = dialogs.update { it.copy(showResetConfirm = true, resetDone = false) }

    fun onResetDismiss() = dialogs.update { it.copy(showResetConfirm = false) }

    fun onResetConfirm() {
        dialogs.update { it.copy(showResetConfirm = false) }
        viewModelScope.launch {
            settingsRepository.resetJourney(journeyStart = today())
            dialogs.update { it.copy(resetDone = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                SettingsViewModel(
                    container.settingsRepository,
                    container.appRepository,
                    container.usageAccessRepository,
                    container.deviceStatusRepository,
                )
            }
        }
    }
}
