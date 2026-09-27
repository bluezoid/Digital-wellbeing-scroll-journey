package com.achyutpal.scrolljourney.ui.dailylimit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.calculator.DailyLimitRules
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyLimitUiState(
    val isLoading: Boolean = true,
    val limitMinutes: Int = DailyLimitRules.DEFAULT_MINUTES,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
)

/**
 * Picks the combined daily limit (onboarding, or from the dashboard menu). During onboarding, saving it is the final step
 * and marks onboarding complete.
 */
class DailyLimitViewModel(
    private val settingsRepository: SettingsRepository,
    private val isOnboarding: Boolean,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyLimitUiState())
    val uiState: StateFlow<DailyLimitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settingsRepository.getSettings().dailyLimitMinutes
            _uiState.update { it.copy(isLoading = false, limitMinutes = DailyLimitRules.snap(saved)) }
        }
    }

    fun onLimitChange(minutes: Int) {
        _uiState.update { it.copy(limitMinutes = DailyLimitRules.snap(minutes)) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settingsRepository.setDailyLimit(state.limitMinutes)
            // Finishing onboarding starts the rocket journey: today is the first day that can earn fuel.
            if (isOnboarding) settingsRepository.setOnboardingComplete(journeyStart = today())
            _uiState.update { it.copy(isSaving = false, isDone = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                // Navigation puts the route's arguments into the SavedStateHandle.
                val handle: SavedStateHandle = createSavedStateHandle()
                DailyLimitViewModel(
                    settingsRepository = container.settingsRepository,
                    isOnboarding = handle.get<Boolean>(Routes.ARG_ONBOARDING) ?: true,
                )
            }
        }
    }
}
