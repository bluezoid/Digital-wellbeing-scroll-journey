package com.achyutpal.scrolljourney.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartDestination { ONBOARDING, APP_PICKER, DASHBOARD }

/**
 * Decides the first screen:
 * - Onboarding finished → DASHBOARD (which itself warns if Usage Access was revoked later).
 * - Not finished but access already granted → APP_PICKER (resume where the user left off).
 * - Otherwise → ONBOARDING.
 */
class StartupViewModel(
    private val usageAccessRepository: UsageAccessRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _start = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            _start.value = when {
                settingsRepository.getSettings().onboardingComplete -> StartDestination.DASHBOARD
                usageAccessRepository.checkUsageAccess() == UsageAccessState.GRANTED -> StartDestination.APP_PICKER
                else -> StartDestination.ONBOARDING
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                StartupViewModel(container.usageAccessRepository, container.settingsRepository)
            }
        }
    }
}
