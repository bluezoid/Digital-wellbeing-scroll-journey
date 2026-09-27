package com.achyutpal.scrolljourney.ui.dashboard

import com.achyutpal.scrolljourney.domain.calculator.StreakCalculator
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import java.time.LocalDate

data class WeekDay(val date: LocalDate, val status: DayStatus?, val isToday: Boolean)

/**
 * Everything the dashboard shows, in one immutable object (spec §34). The screen only
 * renders this: no calculations in the UI.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val accessDenied: Boolean = false,
    val errorMessage: String? = null,
    /** Null until the first sync finishes. */
    val today: DailyUsageSummary? = null,
    val limitMinutes: Int = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
    val status: DayStatus? = null,
    val distanceCentimetres: Long = 0,
    val rocket: RocketState = RocketState(),
    val journeyStartDate: LocalDate? = null,
    val currentStreak: Int = 0,
    val week: List<WeekDay> = emptyList(),
    val updatedAtMillis: Long? = null,
)

/** Pure mapping from the three data sources to [DashboardUiState]; unit-tested without Android. */
object DashboardStateBuilder {

    fun build(
        result: TodayUsageResult?,
        isLoading: Boolean,
        settings: UserSettings?,
        records: List<DailyRecord>,
        today: LocalDate,
        updatedAtMillis: Long?,
    ): DashboardUiState {
        val success = result as? TodayUsageResult.Success
        val statusByDate = records.associate { it.date to it.status }
        val week = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            WeekDay(date, statusByDate[date], isToday = daysAgo == 0)
        }
        val streak = StreakCalculator.calculate(StreakCalculator.contiguousStatuses(records, today))

        return DashboardUiState(
            isLoading = isLoading,
            accessDenied = result is TodayUsageResult.AccessDenied,
            errorMessage = (result as? TodayUsageResult.Failure)?.message,
            today = success?.summary,
            limitMinutes = success?.limitMinutes ?: settings?.dailyLimitMinutes ?: UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
            status = success?.status,
            distanceCentimetres = success?.distanceCentimetres ?: 0,
            rocket = settings?.rocket ?: RocketState(),
            journeyStartDate = settings?.journeyStartDate,
            currentStreak = streak.current,
            week = week,
            updatedAtMillis = updatedAtMillis,
        )
    }
}
