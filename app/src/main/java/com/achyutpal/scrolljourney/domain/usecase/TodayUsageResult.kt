package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.DayStatus

sealed interface TodayUsageResult {
    data class Success(
        val summary: DailyUsageSummary,
        val limitMinutes: Int,
        /** Today's colour so far. It can still get worse until midnight. */
        val status: DayStatus,
        /** Estimated scroll distance so far today, in centimetres. */
        val distanceCentimetres: Long,
    ) : TodayUsageResult

    /** Usage Access is off: numbers can't be trusted, so don't show zeros. */
    data object AccessDenied : TodayUsageResult

    data class Failure(val message: String) : TodayUsageResult
}
