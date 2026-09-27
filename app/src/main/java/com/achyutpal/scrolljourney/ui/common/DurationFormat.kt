package com.achyutpal.scrolljourney.ui.common

/** 7 → "7m", 60 → "1h 00m", 95 → "1h 35m". */
fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    return "%dh %02dm".format(minutes / 60, minutes % 60)
}
