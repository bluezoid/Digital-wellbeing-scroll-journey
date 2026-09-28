package com.achyutpal.scrolljourney.ui.common

import androidx.annotation.StringRes
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.Destination

fun Destination.emoji(): String = when (this) {
    Destination.EARTH -> "🌍"
    Destination.MOON -> "🌙"
    Destination.MARS -> "🔴"
    Destination.CERES -> "🪨"
    Destination.JUPITER -> "🟠"
    Destination.EUROPA -> "🧊"
    Destination.SATURN -> "🪐"
    Destination.TITAN -> "🌫️"
    Destination.URANUS -> "🔵"
    Destination.NEPTUNE -> "🌀"
    Destination.PLUTO -> "⚪"
    Destination.KUIPER_BELT -> "❄️"
    Destination.OORT_CLOUD -> "☄️"
    Destination.PROXIMA_CENTAURI -> "⭐"
    Destination.ALPHA_CENTAURI -> "✨"
    Destination.SIRIUS -> "💎"
    Destination.VEGA -> "🔷"
    Destination.GALACTIC_CENTRE -> "🌌"
}

@StringRes
fun Destination.nameRes(): Int = when (this) {
    Destination.EARTH -> R.string.planet_earth
    Destination.MOON -> R.string.planet_moon
    Destination.MARS -> R.string.planet_mars
    Destination.CERES -> R.string.dest_ceres
    Destination.JUPITER -> R.string.planet_jupiter
    Destination.EUROPA -> R.string.dest_europa
    Destination.SATURN -> R.string.planet_saturn
    Destination.TITAN -> R.string.dest_titan
    Destination.URANUS -> R.string.planet_uranus
    Destination.NEPTUNE -> R.string.planet_neptune
    Destination.PLUTO -> R.string.planet_pluto
    Destination.KUIPER_BELT -> R.string.dest_kuiper_belt
    Destination.OORT_CLOUD -> R.string.dest_oort_cloud
    Destination.PROXIMA_CENTAURI -> R.string.dest_proxima_centauri
    Destination.ALPHA_CENTAURI -> R.string.dest_alpha_centauri
    Destination.SIRIUS -> R.string.dest_sirius
    Destination.VEGA -> R.string.dest_vega
    Destination.GALACTIC_CENTRE -> R.string.dest_galactic_centre
}
