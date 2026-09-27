package com.achyutpal.scrolljourney.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartDestination { ONBOARDING, APP_PICKER, HOME }

/**
 * Decides the first screen. Until the real onboardingComplete flag arrives (Phase 4),
 * "has at least one tracked app saved" counts as onboarding done.
 * - Apps saved → HOME (which itself warns if Usage Access was revoked).
 * - Access granted, no apps yet → APP_PICKER.
 * - Otherwise → ONBOARDING.
 */
class StartupViewModel(
    private val usageAccessRepository: UsageAccessRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _start = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val hasTrackedApps = appRepository.getSavedSelections().any { it.enabled }
            _start.value = when {
                hasTrackedApps -> StartDestination.HOME
                usageAccessRepository.checkUsageAccess() == UsageAccessState.GRANTED -> StartDestination.APP_PICKER
                else -> StartDestination.ONBOARDING
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                StartupViewModel(container.usageAccessRepository, container.appRepository)
            }
        }
    }
}
