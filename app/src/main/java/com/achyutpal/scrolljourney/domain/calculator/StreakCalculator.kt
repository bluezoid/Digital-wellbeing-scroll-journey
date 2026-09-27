package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import java.time.LocalDate

data class Streak(val current: Int, val best: Int)

/**
 * Green-day streaks (spec §36–37). Only GREEN extends a streak; YELLOW, RED and days with
 * no data (null) break it. The current streak counts back from the LAST element (today).
 *
 * Counting today while it's still live is safe: usage only grows, so a yellow/red today can't
 * turn green again, and a green today simply stops counting if it's exceeded later.
 */
object StreakCalculator {

    /** [days] = one entry per consecutive calendar day, oldest first, ending today. */
    fun calculate(days: List<DayStatus?>): Streak {
        var best = 0
        var run = 0
        for (status in days) {
            run = if (status == DayStatus.GREEN) run + 1 else 0
            best = maxOf(best, run)
        }
        return Streak(current = run, best = best)
    }

    /** Lays saved records onto a gap-free calendar from the earliest record to [today]; missing days → null. */
    fun contiguousStatuses(records: List<DailyRecord>, today: LocalDate): List<DayStatus?> {
        val byDate = records.associateBy { it.date }
        val first = records.minOfOrNull { it.date } ?: return emptyList()
        if (first.isAfter(today)) return emptyList()
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { byDate[it]?.status }
            .toList()
    }
}
