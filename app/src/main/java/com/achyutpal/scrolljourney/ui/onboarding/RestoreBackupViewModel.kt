package com.achyutpal.scrolljourney.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.BackupRepository
import com.achyutpal.scrolljourney.domain.repository.BackupSummary
import com.achyutpal.scrolljourney.domain.repository.DocumentStore
import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import com.achyutpal.scrolljourney.ui.widget.WidgetRefresher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Where to go after a successful restore on a fresh install. */
enum class AfterRestore {
    /** The backup finished onboarding, but this phone still needs Usage Access. */
    USAGE_ACCESS,
    DASHBOARD,

    /** The backup is from an unfinished onboarding: continue onboarding normally. */
    ONBOARDING,
}

data class RestoreUiState(
    val busy: Boolean = false,
    val pending: BackupSummary? = null,
    /** Short reason when the picked file can't be used. */
    val error: String? = null,
    val done: AfterRestore? = null,
)

/** "Restore from a backup" on the intro screen (new phone or reinstall). */
class RestoreBackupViewModel(
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
    private val settingsRepository: SettingsRepository,
    private val usageAccessRepository: UsageAccessRepository,
    private val onDataChanged: suspend () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreUiState())
    val state: StateFlow<RestoreUiState> = _state.asStateFlow()
    private var pendingText: String? = null

    fun onPicked(uri: String) = run {
        val text = documentStore.read(uri)
        val summary = backupRepository.inspect(text)
        pendingText = text
        _state.update { it.copy(pending = summary) }
    }

    fun onConfirm() {
        val text = pendingText ?: return
        pendingText = null
        _state.update { it.copy(pending = null) }
        run {
            backupRepository.restore(text)
            onDataChanged()
            val next = when {
                !settingsRepository.getSettings().onboardingComplete -> AfterRestore.ONBOARDING
                usageAccessRepository.checkUsageAccess() != UsageAccessState.GRANTED -> AfterRestore.USAGE_ACCESS
                else -> AfterRestore.DASHBOARD
            }
            _state.update { it.copy(done = next) }
        }
    }

    fun onDismiss() {
        pendingText = null
        _state.update { it.copy(pending = null) }
    }

    /** The screen navigated away: don't navigate again on recomposition. */
    fun onNavigated() = _state.update { it.copy(done = null) }

    private fun run(step: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val error = try {
                step()
                null
            } catch (e: InvalidBackupException) {
                e.message ?: "invalid file"
            } catch (e: IOException) {
                e.message ?: "couldn't access the file"
            } catch (e: SecurityException) {
                "no permission for that file"
            }
            _state.update { it.copy(busy = false, error = error) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScrollJourneyApp
                val c = app.container
                RestoreBackupViewModel(c.backupRepository, c.documentStore, c.settingsRepository, c.usageAccessRepository) {
                    WidgetRefresher.refresh(app)
                }
            }
        }
    }
}
