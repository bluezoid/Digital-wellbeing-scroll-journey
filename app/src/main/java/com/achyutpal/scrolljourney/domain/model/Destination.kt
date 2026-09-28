package com.achyutpal.scrolljourney.domain.model

/**
 * The rocket's route, in order, with the cumulative fuel needed to reach each stop.
 * Approved by the user on 2026-09-28: a quick first win (Moon), then growing gaps, with
 * small stops between planets and real stars beyond the solar system.
 *
 * Fuel is cumulative progress: 1 fuel ≈ 1 green day at best. Yellow days add nothing and
 * red days take one away, so real journeys take longer. Enum names are stored in the database
 * (user_settings.highestReachedPlanet): never rename an existing entry; only append or insert new ones.
 */
enum class Destination(val fuelRequired: Int, val kind: Kind) {
    EARTH(0, Kind.PLANET),
    MOON(3, Kind.MOON),
    MARS(12, Kind.PLANET),
    CERES(20, Kind.STOP), // asteroid belt
    JUPITER(30, Kind.PLANET),
    EUROPA(38, Kind.MOON),
    SATURN(50, Kind.PLANET),
    TITAN(60, Kind.MOON),
    URANUS(80, Kind.PLANET),
    NEPTUNE(105, Kind.PLANET),
    PLUTO(135, Kind.DWARF_PLANET),
    KUIPER_BELT(170, Kind.STOP), // Eris
    OORT_CLOUD(220, Kind.STOP), // edge of the solar system
    PROXIMA_CENTAURI(300, Kind.STAR), // nearest star
    ALPHA_CENTAURI(365, Kind.STAR), // ~1 year of green days
    SIRIUS(500, Kind.STAR),
    VEGA(650, Kind.STAR),
    GALACTIC_CENTRE(1_000, Kind.GALAXY), // final goal
    ;

    enum class Kind { PLANET, MOON, STOP, DWARF_PLANET, STAR, GALAXY }

    /** The next stop, or null at the final destination. */
    val next: Destination? get() = entries.getOrNull(ordinal + 1)

    /** True for everything up to the Oort Cloud (drawn in the solar-system view). */
    val inSolarSystem: Boolean get() = this <= OORT_CLOUD

    companion object {
        val FINAL: Destination = entries.last()

        /** Furthest destination whose requirement [fuel] meets. */
        fun forFuel(fuel: Int): Destination = entries.last { it.fuelRequired <= fuel }
    }
}
