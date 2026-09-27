package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary

sealed interface TodayUsageResult {
    data class Success(val summary: DailyUsageSummary) : TodayUsageResult

    /** Usage Access is off: numbers can't be trusted, so don't show zeros. */
    data object AccessDenied : TodayUsageResult

    data class Failure(val message: String) : TodayUsageResult
}
