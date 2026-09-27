package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DayStatus

/**
 * Colours a day against the daily limit:
 * - GREEN:  usage ≤ limit
 * - YELLOW: limit < usage ≤ 125% of limit
 * - RED:    usage > 125% of limit
 *
 * Integer maths only (`usage × 100 ≤ limit × 125`), so boundaries like 75 of 60 are exact:
 * floating-point 1.25 can land a hair off and flip a boundary day.
 */
object LimitEvaluator {

    private const val YELLOW_PERCENT = 125L

    fun evaluate(usageMinutes: Int, limitMinutes: Int): DayStatus {
        require(limitMinutes > 0) { "limit must be positive, was $limitMinutes" }
        return when {
            usageMinutes <= limitMinutes -> DayStatus.GREEN
            usageMinutes.toLong() * 100 <= limitMinutes.toLong() * YELLOW_PERCENT -> DayStatus.YELLOW
            else -> DayStatus.RED
        }
    }

    /** Highest usage that is still YELLOW, e.g. 75 for a 60-minute limit. */
    fun yellowMaxMinutes(limitMinutes: Int): Int = (limitMinutes.toLong() * YELLOW_PERCENT / 100).toInt()
}
