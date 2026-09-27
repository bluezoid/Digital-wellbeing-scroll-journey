package com.achyutpal.scrolljourney.ui.common

import java.util.Locale

/** 0 → "0 m", 13 700 cm → "137 m", 125 000 cm → "1.3 km". */
fun formatDistance(centimetres: Long, locale: Locale = Locale.getDefault()): String {
    val metres = centimetres / 100
    return if (metres < 1_000) "$metres m" else String.format(locale, "%.1f km", metres / 1_000.0)
}

/** 1.66 → "1.7", 12.0 → "12", 0.45 → "45" (as a percent, for fractions of a landmark). */
fun formatTimes(times: Double, locale: Locale = Locale.getDefault()): String =
    if (times >= 10 || times == Math.floor(times)) {
        String.format(locale, "%.0f", times)
    } else {
        String.format(locale, "%.1f", times)
    }
