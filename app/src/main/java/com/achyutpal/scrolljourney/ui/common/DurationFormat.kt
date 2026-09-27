package com.achyutpal.scrolljourney.ui.common

/** 30 → "30m", 60 → "1h", 90 → "1h 30m": short labels for chips. */
fun formatMinutesCompact(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** 7 → "7m", 60 → "1h 00m", 95 → "1h 35m". */
fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    return "%dh %02dm".format(minutes / 60, minutes % 60)
}
