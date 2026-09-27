package com.achyutpal.scrolljourney.domain.model

/**
 * Stops on the rocket's route, in order, with the fuel needed to reach each one.
 * Fuel is cumulative progress: 60 fuel points reach Pluto (not "60 days": yellow days
 * add nothing and red days take one away).
 */
enum class Planet(val fuelRequired: Int) {
    EARTH(0),
    MOON(3),
    MARS(7),
    JUPITER(14),
    SATURN(21),
    URANUS(30),
    NEPTUNE(45),
    PLUTO(60),
    ;

    /** The next stop, or null at Pluto. */
    val next: Planet? get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** Furthest planet whose requirement [fuel] meets. */
        fun forFuel(fuel: Int): Planet = entries.last { it.fuelRequired <= fuel }
    }
}
