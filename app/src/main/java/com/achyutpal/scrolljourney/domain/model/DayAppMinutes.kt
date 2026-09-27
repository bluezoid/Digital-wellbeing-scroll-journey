package com.achyutpal.scrolljourney.domain.model

/** A saved per-app total for one day (names are looked up separately, from the selection). */
data class DayAppMinutes(
    val packageName: String,
    val minutes: Int,
)
