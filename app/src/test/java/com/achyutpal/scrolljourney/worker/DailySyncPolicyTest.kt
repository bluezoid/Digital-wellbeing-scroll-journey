package com.achyutpal.scrolljourney.worker

import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.usecase.TodayUsageResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailySyncPolicyTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `success and access denied finish, failures retry`() {
        val success = TodayUsageResult.Success(DailyUsageSummary(LocalDate.of(2026, 9, 27), emptyList()), 60, DayStatus.GREEN, 0)
        assertEquals(DailySyncPolicy.Outcome.SUCCESS, DailySyncPolicy.outcomeFor(success))
        assertEquals(DailySyncPolicy.Outcome.SUCCESS, DailySyncPolicy.outcomeFor(TodayUsageResult.AccessDenied))
        assertEquals(DailySyncPolicy.Outcome.RETRY, DailySyncPolicy.outcomeFor(TodayUsageResult.Failure("boom")))
    }

    @Test
    fun `evening schedules for 00-30 tonight`() {
        val now = ZonedDateTime.of(2026, 9, 27, 21, 30, 0, 0, zone)
        assertEquals(Duration.ofHours(3), DailySyncPolicy.initialDelay(now))
    }

    @Test
    fun `just after midnight but before 00-30 runs the same night`() {
        val now = ZonedDateTime.of(2026, 9, 28, 0, 10, 0, 0, zone)
        assertEquals(Duration.ofMinutes(20), DailySyncPolicy.initialDelay(now))
    }

    @Test
    fun `exactly 00-30 and later go to the next night`() {
        assertEquals(Duration.ofHours(24), DailySyncPolicy.initialDelay(ZonedDateTime.of(2026, 9, 28, 0, 30, 0, 0, zone)))
        assertEquals(Duration.ofHours(23).plusMinutes(30), DailySyncPolicy.initialDelay(ZonedDateTime.of(2026, 9, 28, 1, 0, 0, 0, zone)))
    }
}
