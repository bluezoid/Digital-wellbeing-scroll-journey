package com.achyutpal.scrolljourney.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.GraphicsCapability
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.usecase.SyncUsageHistoryUseCase
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Combines three sources into one [DashboardUiState]:
 * 1. the latest sync result (today's live usage), refreshed on every resume,
 * 2. the settings row (limit, rocket, journey start), a Room Flow,
 * 3. saved history (streak and week strip), a Room Flow.
 * Room Flows re-emit by themselves whenever a sync writes, so the screen stays in step.
 */
class DashboardViewModel(
    private val syncUsageHistory: SyncUsageHistoryUseCase,
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    private val graphics: GraphicsCapability = GraphicsCapability(),
    private val today: () -> LocalDate = LocalDate::now,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private data class SyncState(
        val isLoading: Boolean = true,
        val result: TodayUsageResult? = null,
        val updatedAtMillis: Long? = null,
    )

    private val syncState = MutableStateFlow(SyncState())

    val uiState: StateFlow<DashboardUiState> = combine(
        syncState,
        settingsRepository.observeSettings(),
        historyRepository.observeRecentDays(STREAK_LOOKBACK_DAYS),
    ) { sync, settings, records ->
        DashboardStateBuilder.build(
            result = sync.result,
            isLoading = sync.isLoading,
            settings = settings,
            records = records,
            today = today(),
            updatedAtMillis = sync.updatedAtMillis,
            device = graphics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun refresh() {
        syncState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = syncUsageHistory()
            syncState.value = SyncState(isLoading = false, result = result, updatedAtMillis = clock())
        }
    }

    companion object {
        /** Enough saved days for any realistic streak. */
        const val STREAK_LOOKBACK_DAYS = 400

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                DashboardViewModel(
                    container.syncUsageHistoryUseCase,
                    container.historyRepository,
                    container.settingsRepository,
                    container.deviceStatusRepository.graphicsCapability,
                )
            }
        }
    }
}
