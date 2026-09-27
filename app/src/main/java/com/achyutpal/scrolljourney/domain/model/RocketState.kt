package com.achyutpal.scrolljourney.domain.model

/**
 * - [currentFuel]: drives the rocket toward the next planet; red days lower it (never below 0).
 * - [lifetimeFuel]: every fuel point ever earned (green days); only ever goes up.
 * - [highestReachedPlanet]: permanent milestone. A red day can cost progress toward the
 *   next planet but never moves the rocket back past a planet it already reached.
 */
data class RocketState(
    val currentFuel: Int = 0,
    val lifetimeFuel: Int = 0,
    val highestReachedPlanet: Planet = Planet.EARTH,
) {
    val nextPlanet: Planet? get() = highestReachedPlanet.next

    /** Fuel still needed for the next planet (0 at Pluto). */
    val fuelToNext: Int get() = nextPlanet?.let { (it.fuelRequired - currentFuel).coerceAtLeast(0) } ?: 0

    /** 0..1 progress along the current leg (reached planet → next planet); 1 at Pluto. */
    val legProgress: Float
        get() {
            val next = nextPlanet ?: return 1f
            val from = highestReachedPlanet.fuelRequired
            val span = next.fuelRequired - from
            return ((currentFuel - from).toFloat() / span).coerceIn(0f, 1f)
        }
}
