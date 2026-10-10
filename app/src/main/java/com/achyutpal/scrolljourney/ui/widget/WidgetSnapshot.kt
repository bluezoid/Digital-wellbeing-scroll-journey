package com.achyutpal.scrolljourney.ui.widget

import com.achyutpal.scrolljourney.domain.calculator.LimitEvaluator
import com.achyutpal.scrolljourney.domain.calculator.StreakCalculator
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import java.time.LocalDate

/** Everything the home-screen widgets show, computed from saved data (no usage query of its own). */
data class WidgetSnapshot(
    val state: State,
    val todayMinutes: Int = 0,
    val limitMinutes: Int = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
    val status: DayStatus = DayStatus.GREEN,
    val rocket: RocketState = RocketState(),
    val streak: Int = 0,
) {
    enum class State {
        /** Normal: today's numbers and the journey. */
        READY,

        /** Onboarding isn't finished yet. */
        NOT_SET_UP,

        /** Usage Access was switched off: the numbers would be stale. */
        NO_ACCESS,
    }

    /** 0..1 of today's limit used (the bar is full at or over the limit). */
    val limitFraction: Float
        get() = if (limitMinutes <= 0) 0f else (todayMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)

    companion object {
        /** Pure: unit-tested. [records] are recent saved days (any order). */
        fun build(settings: UserSettings, records: List<DailyRecord>, today: LocalDate, hasUsageAccess: Boolean): WidgetSnapshot {
            if (!settings.onboardingComplete) return WidgetSnapshot(State.NOT_SET_UP)
            val todayRecord = records.firstOrNull { it.date == today }
            val minutes = todayRecord?.totalMinutes ?: 0
            val limit = settings.dailyLimitMinutes
            return WidgetSnapshot(
                state = if (hasUsageAccess) State.READY else State.NO_ACCESS,
                todayMinutes = minutes,
                limitMinutes = limit,
                status = LimitEvaluator.evaluate(minutes, limit), // always against today's current limit
                rocket = settings.rocket,
                streak = StreakCalculator.calculate(StreakCalculator.contiguousStatuses(records, today)).current,
            )
        }
    }
}
