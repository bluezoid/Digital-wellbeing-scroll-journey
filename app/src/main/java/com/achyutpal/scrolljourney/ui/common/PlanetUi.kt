package com.achyutpal.scrolljourney.ui.common

import androidx.annotation.StringRes
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.Planet

fun Planet.emoji(): String = when (this) {
    Planet.EARTH -> "🌍"
    Planet.MOON -> "🌙"
    Planet.MARS -> "🔴"
    Planet.JUPITER -> "🟠"
    Planet.SATURN -> "🪐"
    Planet.URANUS -> "🔵"
    Planet.NEPTUNE -> "🌀"
    Planet.PLUTO -> "⚪"
}

@StringRes
fun Planet.nameRes(): Int = when (this) {
    Planet.EARTH -> R.string.planet_earth
    Planet.MOON -> R.string.planet_moon
    Planet.MARS -> R.string.planet_mars
    Planet.JUPITER -> R.string.planet_jupiter
    Planet.SATURN -> R.string.planet_saturn
    Planet.URANUS -> R.string.planet_uranus
    Planet.NEPTUNE -> R.string.planet_neptune
    Planet.PLUTO -> R.string.planet_pluto
}
