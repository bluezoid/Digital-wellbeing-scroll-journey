package com.achyutpal.scrolljourney.domain.calculator

/**
 * Minutes of app use → estimated scroll distance, in centimetres.
 * Long arithmetic: even 240 years of minutes at 1 m/min stays far below Long.MAX_VALUE.
 */
class DistanceCalculator(
    private val config: DistanceConfig = DistanceConfig(),
) {
    fun centimetres(minutes: Int): Long = minutes.coerceAtLeast(0).toLong() * config.centimetresPerMinute
}
