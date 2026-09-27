package com.achyutpal.scrolljourney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.usecase.SyncUsageHistoryUseCase
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val result: TodayUsageResult? = null,
    val updatedAtMillis: Long? = null,
)

/**
 * TEMPORARY (Phases 3–4): syncs history and shows today's minutes plus the last 7 saved days,
 * so both can be checked on the phone. Grows into DashboardViewModel in Phase 8.
 */
class HomePlaceholderViewModel(
    private val syncUsageHistory: SyncUsageHistoryUseCase,
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Re-emits automatically whenever a sync writes to the database. */
    val recentDays: StateFlow<List<DailyRecord>> = historyRepository.observeRecentDays(RECENT_DAYS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Rocket + journey start, straight from the settings row (updates when fuel is applied). */
    val settings: StateFlow<UserSettings?> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = syncUsageHistory()
            _uiState.value = HomeUiState(isLoading = false, result = result, updatedAtMillis = clock())
        }
    }

    companion object {
        const val RECENT_DAYS = 7

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                HomePlaceholderViewModel(
                    container.syncUsageHistoryUseCase,
                    container.historyRepository,
                    container.settingsRepository,
                )
            }
        }
    }
}
