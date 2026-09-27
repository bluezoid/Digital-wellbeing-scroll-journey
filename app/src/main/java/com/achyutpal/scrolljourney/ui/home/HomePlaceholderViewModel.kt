package com.achyutpal.scrolljourney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.usecase.GetTodayUsageUseCase
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val result: TodayUsageResult? = null,
    val updatedAtMillis: Long? = null,
)

/**
 * TEMPORARY (Phase 3): today's minutes per tracked app, so the numbers can be compared
 * with Digital Wellbeing on the phone. Grows into DashboardViewModel in Phase 8.
 */
class HomePlaceholderViewModel(
    private val getTodayUsage: GetTodayUsageUseCase,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getTodayUsage()
            _uiState.value = HomeUiState(isLoading = false, result = result, updatedAtMillis = clock())
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                HomePlaceholderViewModel(container.getTodayUsageUseCase)
            }
        }
    }
}
