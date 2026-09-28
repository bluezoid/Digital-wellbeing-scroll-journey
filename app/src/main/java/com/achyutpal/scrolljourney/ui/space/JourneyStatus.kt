package com.achyutpal.scrolljourney.ui.space

import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.RocketState

/** How a destination relates to the user's journey, for the 3D info card. */
sealed interface JourneyStatus {
    val stopNumber: Int

    data class Reached(override val stopNumber: Int) : JourneyStatus
    data class Next(override val stopNumber: Int, val fuelToGo: Int, val fuelRequired: Int) : JourneyStatus
    data class Locked(override val stopNumber: Int, val fuelToGo: Int, val fuelRequired: Int) : JourneyStatus

    companion object {
        val TOTAL_STOPS = Destination.entries.size

        fun of(destination: Destination, rocket: RocketState): JourneyStatus {
            val number = destination.ordinal + 1
            val toGo = (destination.fuelRequired - rocket.currentFuel).coerceAtLeast(0)
            return when {
                destination <= rocket.highestReachedPlanet -> Reached(number)
                destination == rocket.nextPlanet -> Next(number, toGo, destination.fuelRequired)
                else -> Locked(number, toGo, destination.fuelRequired)
            }
        }
    }
}
