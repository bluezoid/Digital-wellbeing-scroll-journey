package com.achyutpal.scrolljourney.worker

import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** Pure rules behind the background job, kept out of the Worker so they're unit-testable. */
object DailySyncPolicy {

    /** Aim for just after midnight so the finished day is saved and fuelled early. Not guaranteed (Doze), and not needed. */
    val PREFERRED_RUN_TIME: LocalTime = LocalTime.of(0, 30)

    enum class Outcome { SUCCESS, RETRY }

    /**
     * - Synced: done.
     * - Usage Access off: retrying won't help until the user fixes it; the next app open shows the warning.
     * - Unexpected failure: retry later with backoff.
     */
    fun outcomeFor(result: TodayUsageResult): Outcome = when (result) {
        is TodayUsageResult.Success -> Outcome.SUCCESS
        TodayUsageResult.AccessDenied -> Outcome.SUCCESS
        is TodayUsageResult.Failure -> Outcome.RETRY
    }

    /** Delay from [now] until the next [PREFERRED_RUN_TIME] (today's if still ahead, else tomorrow's). */
    fun initialDelay(now: ZonedDateTime, runAt: LocalTime = PREFERRED_RUN_TIME): Duration {
        var next = now.with(runAt).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
