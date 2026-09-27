package com.achyutpal.scrolljourney.ui.apppicker

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.data.apps.AppIconSource
import com.achyutpal.scrolljourney.domain.calculator.AppSelectionRules
import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppRow(
    val packageName: String,
    val label: String,
    val selected: Boolean,
)

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val rows: List<AppRow> = emptyList(),
    val query: String = "",
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
) {
    val selectedCount: Int get() = rows.count { it.selected }
    val canContinue: Boolean get() = selectedCount > 0 && !isSaving
    val visibleRows: List<AppRow>
        get() = if (query.isBlank()) rows else rows.filter { it.label.contains(query.trim(), ignoreCase = true) }
}

class AppPickerViewModel(
    private val appRepository: AppRepository,
    private val iconSource: AppIconSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    private var installed: List<InstalledApp> = emptyList()
    private var saved: List<SelectedApp> = emptyList()

    init {
        viewModelScope.launch {
            installed = appRepository.getInstalledApps()
            saved = appRepository.getSavedSelections()
            val preselected = AppSelectionRules.initialSelection(installed, saved)
            val rows = AppSelectionRules.sortForDisplay(installed, preselected).map {
                AppRow(it.packageName, it.label, selected = it.packageName in preselected)
            }
            _uiState.update { it.copy(isLoading = false, rows = rows) }
        }
    }

    fun onToggle(packageName: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.packageName == packageName) row.copy(selected = !row.selected) else row
                },
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canContinue) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val selected = state.rows.filter { it.selected }.map { it.packageName }.toSet()
            val records = AppSelectionRules.buildSelectionRecords(installed, saved, selected, clock())
            appRepository.saveSelections(records)
            _uiState.update { it.copy(isSaving = false, isDone = true) }
        }
    }

    suspend fun loadIcon(packageName: String): ImageBitmap? = iconSource.loadIcon(packageName)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                AppPickerViewModel(container.appRepository, container.appIconSource)
            }
        }
    }
}
