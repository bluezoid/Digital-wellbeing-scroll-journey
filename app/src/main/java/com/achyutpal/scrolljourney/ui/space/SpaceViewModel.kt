package com.achyutpal.scrolljourney.ui.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.achyutpal.scrolljourney.ScrollJourneyApp
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Feeds the 3D scene with the user's real rocket state (from the settings row).
 * [debugFuel] (debug builds only, via `--ei debug_fuel N`) previews any point of the journey.
 */
class SpaceViewModel(
    settingsRepository: SettingsRepository,
    private val debugFuel: Int? = null,
) : ViewModel() {

    val rocket: StateFlow<RocketState> = settingsRepository.observeSettings()
        .map { settings -> debugFuel?.let(::previewRocket) ?: settings.rocket }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), debugFuel?.let(::previewRocket) ?: RocketState())

    companion object {
        /** Set by MainActivity in debug builds from the `debug_fuel` intent extra. */
        @Volatile
        var debugFuelOverride: Int? = null

        fun previewRocket(fuel: Int) = RocketState(
            currentFuel = fuel,
            lifetimeFuel = fuel,
            highestReachedPlanet = Destination.forFuel(fuel),
        )

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ScrollJourneyApp).container
                SpaceViewModel(container.settingsRepository, debugFuelOverride)
            }
        }
    }
}
