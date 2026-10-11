package com.achyutpal.scrolljourney.ui.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current [UsageAccessState] for a screen.
 *
 * Screens call [refresh] every time they resume, because the user can flip the
 * toggle in Settings at any moment. Never assume a past grant still holds.
 */
class UsageAccessViewModel(
    private val repository: UsageAccessRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UsageAccessState.UNKNOWN)
    val state: StateFlow<UsageAccessState> = _state.asStateFlow()

    /** Android blocks the switch as a "restricted setting" (app installed from a file). */
    private val _restricted = MutableStateFlow(false)
    val restricted: StateFlow<Boolean> = _restricted.asStateFlow()

    fun refresh() {
        val access = repository.checkUsageAccess()
        _state.value = access
        _restricted.value = access != UsageAccessState.GRANTED && repository.isBlockedByRestrictedSettings()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScrollJourneyApp
                UsageAccessViewModel(app.container.usageAccessRepository)
            }
        }
    }
}
