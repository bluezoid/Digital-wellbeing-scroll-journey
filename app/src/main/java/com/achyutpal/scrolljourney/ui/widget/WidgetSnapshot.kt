package com.achyutpal.scrolljourney.ui.widget

import com.achyutpal.scrolljourney.domain.calculator.LimitEvaluator
import com.achyutpal.scrolljourney.domain.calculator.StreakCalculator
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.RocketState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import java.time.LocalDate
import java.time.LocalTime

/** Everything the home-screen widgets show, computed from saved data (no usage query of its own). */
data class WidgetSnapshot(
    val state: State,
    val todayMinutes: Int = 0,
    val limitMinutes: Int = UserSettings.DEFAULT_DAILY_LIMIT_MINUTES,
    val status: DayStatus = DayStatus.GREEN,
    val rocket: RocketState = RocketState(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** Estimated scroll distance today, in centimetres. */
    val todayDistance: Long = 0,
    /** The last 7 days, oldest first, ending today (today = its live status). */
    val week: List<WeekDay> = emptyList(),
    /** When this snapshot was made (right after the latest sync). */
    val updatedAt: LocalTime? = null,
) {
    /** One day of the 7-day strip; [status] null = no data saved for that day. */
    data class WeekDay(val date: LocalDate, val status: DayStatus?)

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

    /** Minutes still allowed today (0 once the limit is reached). */
    val minutesLeft: Int get() = (limitMinutes - todayMinutes).coerceAtLeast(0)

    /** Minutes over the limit (0 while under it). */
    val minutesOver: Int get() = (todayMinutes - limitMinutes).coerceAtLeast(0)

    companion object {
        const val WEEK_DAYS = 7

        /** Pure: unit-tested. [records] are recent saved days (any order). */
        fun build(
            settings: UserSettings,
            records: List<DailyRecord>,
            today: LocalDate,
            hasUsageAccess: Boolean,
            now: LocalTime? = null,
        ): WidgetSnapshot {
            if (!settings.onboardingComplete) return WidgetSnapshot(State.NOT_SET_UP)
            val todayRecord = records.firstOrNull { it.date == today }
            val minutes = todayRecord?.totalMinutes ?: 0
            val limit = settings.dailyLimitMinutes
            val status = LimitEvaluator.evaluate(minutes, limit) // always against today's current limit
            val byDate = records.associateBy { it.date }
            val week = (WEEK_DAYS - 1 downTo 0).map { ago ->
                val date = today.minusDays(ago.toLong())
                val record = byDate[date]
                val dayStatus = when {
                    date == today -> status
                    record == null -> null
                    // A past day not finalized yet (nightly job pending): judge it like the job will.
                    else -> record.status ?: LimitEvaluator.evaluate(record.totalMinutes, record.limitMinutes ?: limit)
                }
                WeekDay(date, dayStatus)
            }
            val streak = StreakCalculator.calculate(StreakCalculator.contiguousStatuses(records, today))
            return WidgetSnapshot(
                state = if (hasUsageAccess) State.READY else State.NO_ACCESS,
                todayMinutes = minutes,
                limitMinutes = limit,
                status = status,
                rocket = settings.rocket,
                streak = streak.current,
                bestStreak = streak.best,
                todayDistance = todayRecord?.distance ?: 0,
                week = week,
                updatedAt = now,
            )
        }
    }
}
