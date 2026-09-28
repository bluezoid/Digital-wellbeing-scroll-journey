package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.model.ForegroundUsage
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
import com.achyutpal.scrolljourney.fakes.FakeJourneyRepository
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SyncUsageHistoryUseCaseTest {

    private val min = 60_000L
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 9, 27)
    private val now = ZonedDateTime.of(2026, 9, 27, 14, 30, 0, 0, zone).toInstant()

    private val insta = "com.instagram.android"
    private val yt = "com.google.android.youtube"
    private val fb = "com.facebook.katana"
    private val chrome = "com.android.chrome"

    private val apps = FakeAppRepository(
        saved = listOf(
            SelectedApp(insta, "Instagram", true, 0L),
            SelectedApp(yt, "YouTube", true, 0L),
            SelectedApp(fb, "Facebook", false, 0L),
        ),
    )
    private val usage = FakeUsageRepository(
        usageByPackage = mapOf(insta to 20 * min, yt to 10 * min, fb to 50 * min, chrome to 30 * min),
    )
    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository(UserSettings(dailyLimitMinutes = 60))

    private fun startOf(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun useCase(access: UsageAccessState = UsageAccessState.GRANTED) = SyncUsageHistoryUseCase(
        usageAccessRepository = FakeUsageAccessRepository(access),
        appRepository = apps,
        usageRepository = usage,
        historyRepository = history,
        settingsRepository = settings,
        applyJourneyFuel = ApplyJourneyFuelUseCase(FakeJourneyRepository(history, settings), settings),
        now = { now },
        zone = { zone },
    )

    @Test
    fun `today counts only enabled apps and is saved unfinalized`() = runTest {
        val result = useCase()() as TodayUsageResult.Success

        assertEquals(30, result.summary.totalMinutes) // Instagram 20 + YouTube 10; Facebook off, Chrome unselected
        assertEquals(listOf("Instagram", "YouTube"), result.summary.apps.map { it.appName })
        assertEquals(DailyRecord(today, 30, status = DayStatus.GREEN, limitMinutes = 60, distance = 3_000, finalized = false), history.records[today])
        assertEquals(listOf(20, 10), history.appRows.getValue(today).map { it.minutes })
    }

    @Test
    fun `today's window runs from local midnight to now`() = runTest {
        useCase()()
        assertEquals(startOf(today) to now.toEpochMilli(), usage.calls.last())
    }

    @Test
    fun `past days with data are backfilled and finalized, up to 7 days`() = runTest {
        useCase()()

        val pastDays = (1..7).map { today.minusDays(it.toLong()) }
        pastDays.forEach { day ->
            assertEquals(DailyRecord(day, 30, status = DayStatus.GREEN, limitMinutes = 60, distance = 3_000, finalized = true), history.records[day])
        }
        assertNull(history.records[today.minusDays(8)])
    }

    @Test
    fun `past day window covers the whole local day`() = runTest {
        useCase()()
        val yesterday = today.minusDays(1)
        assertTrue(startOf(yesterday) to startOf(today) - 1 in usage.calls)
    }

    @Test
    fun `day without any events is skipped, not saved as zero`() = runTest {
        val noDataDay = today.minusDays(3)
        usage.provider = { start, _ ->
            if (start == startOf(noDataDay)) ForegroundUsage(emptyMap(), eventCount = 0)
            else ForegroundUsage(usage.usageByPackage, eventCount = 5)
        }
        useCase()()

        assertNull(history.records[noDataDay])
        assertTrue(history.records.containsKey(today.minusDays(2)))
    }

    @Test
    fun `finalized day is never recalculated`() = runTest {
        val yesterday = today.minusDays(1)
        history.saveDay(DailyRecord(yesterday, 99, finalized = true), emptyList())

        useCase()()

        assertEquals(99, history.records.getValue(yesterday).totalMinutes)
        assertFalse(usage.calls.any { it.first == startOf(yesterday) })
    }

    @Test
    fun `yesterday saved live is finalized with the full day's usage`() = runTest {
        val yesterday = today.minusDays(1)
        history.saveDay(DailyRecord(yesterday, 5, finalized = false), emptyList()) // saved at 9am yesterday

        useCase()()

        assertEquals(DailyRecord(yesterday, 30, status = DayStatus.GREEN, limitMinutes = 60, distance = 3_000, finalized = true), history.records[yesterday])
    }

    @Test
    fun `live day whose events expired is frozen as it was`() = runTest {
        val old = today.minusDays(6)
        history.saveDay(DailyRecord(old, 44, finalized = false), emptyList())
        usage.provider = { start, _ ->
            if (start == startOf(old)) ForegroundUsage(emptyMap(), eventCount = 0)
            else ForegroundUsage(usage.usageByPackage, eventCount = 5)
        }

        useCase()()

        assertEquals(DailyRecord(old, 44, status = DayStatus.GREEN, limitMinutes = 60, distance = 4_400, finalized = true), history.records[old])
    }

    @Test
    fun `running sync twice gives the same history`() = runTest {
        useCase()()
        val first = history.records
        useCase()()

        assertEquals(first, history.records)
        assertEquals(8, history.records.size) // 7 past days + today, no duplicates
    }

    @Test
    fun `second run only re-reads today`() = runTest {
        useCase()()
        usage.calls.clear()
        useCase()()

        assertEquals(listOf(startOf(today) to now.toEpochMilli()), usage.calls)
    }

    @Test
    fun `today's status and limit are returned and saved`() = runTest {
        settings.setDailyLimit(25) // 30 minutes used: > 25, <= 31.25 → yellow
        val result = useCase()() as TodayUsageResult.Success

        assertEquals(25, result.limitMinutes)
        assertEquals(DayStatus.YELLOW, result.status)
        assertEquals(DayStatus.YELLOW, history.records.getValue(today).status)
    }

    @Test
    fun `finalized past day gets its status from the limit`() = runTest {
        settings.setDailyLimit(20) // 30 > 25 (125% of 20) → red
        useCase()()
        assertEquals(DayStatus.RED, history.records.getValue(today.minusDays(1)).status)
    }

    @Test
    fun `day frozen before the status engine gets a status without recalculating minutes`() = runTest {
        val yesterday = today.minusDays(1)
        history.saveDay(DailyRecord(yesterday, 90, status = null, finalized = true), emptyList())

        useCase()()

        assertEquals(DailyRecord(yesterday, 90, status = DayStatus.RED, limitMinutes = 60, distance = 9_000, finalized = true), history.records[yesterday])
        assertFalse(usage.calls.any { it.first == startOf(yesterday) })
    }

    @Test
    fun `status of a finalized day is not re-evaluated when the limit changes`() = runTest {
        useCase()() // yesterday finalized GREEN with limit 60
        settings.setDailyLimit(15)
        useCase()()

        val yesterday = history.records.getValue(today.minusDays(1))
        assertEquals(DayStatus.GREEN, yesterday.status)
        assertEquals(60, yesterday.limitMinutes) // keeps the limit it was judged against
        val live = history.records.getValue(today)
        assertEquals(DayStatus.RED, live.status) // today is live, re-evaluated
        assertEquals(15, live.limitMinutes)
    }

    @Test
    fun `today's estimated distance is returned`() = runTest {
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(3_000L, result.distanceCentimetres) // 30 min × 1 m/min
    }

    @Test
    fun `coloured day without distance gets distance but keeps status and judged limit`() = runTest {
        val yesterday = today.minusDays(1)
        history.saveDay(
            DailyRecord(yesterday, 44, status = DayStatus.GREEN, limitMinutes = 60, distance = 0, finalized = true),
            emptyList(),
        )
        settings.setDailyLimit(30)

        useCase()()

        assertEquals(
            DailyRecord(yesterday, 44, status = DayStatus.GREEN, limitMinutes = 60, distance = 4_400, finalized = true),
            history.records[yesterday],
        )
        assertFalse(usage.calls.any { it.first == startOf(yesterday) })
    }

    @Test
    fun `finalized zero-minute day is left alone`() = runTest {
        val yesterday = today.minusDays(1)
        val zeroDay = DailyRecord(yesterday, 0, status = DayStatus.GREEN, limitMinutes = 60, distance = 0, finalized = true)
        history.saveDay(zeroDay, emptyList())
        val saves = history.saveCount

        useCase()()

        assertEquals(zeroDay, history.records[yesterday])
        assertEquals(saves + 7, history.saveCount) // 6 other past days + today, nothing for yesterday
    }

    @Test
    fun `journey starts today for users without a start date, so backfilled days give no fuel`() = runTest {
        useCase()()

        assertEquals(today, settings.state.value.journeyStartDate)
        assertEquals(0, settings.state.value.currentFuel)
        assertTrue(history.records.values.none { it.fuelApplied })
    }

    @Test
    fun `finalized days since the journey start fuel the rocket once, today never`() = runTest {
        settings.ensureJourneyStarted(today.minusDays(3))

        useCase()()
        useCase()() // second run must not add fuel again

        // 3 past days since the start, each 30 min against a 60 min limit → green → +1
        assertEquals(3, settings.state.value.currentFuel)
        assertEquals(Destination.MOON, settings.state.value.highestReachedPlanet)
        assertFalse(history.records.getValue(today).fuelApplied)
        assertEquals(1, history.records.getValue(today.minusDays(1)).fuelDelta)
        assertFalse(history.records.getValue(today.minusDays(4)).fuelApplied)
    }

    @Test
    fun `access off returns AccessDenied and writes nothing`() = runTest {
        assertEquals(TodayUsageResult.AccessDenied, useCase(UsageAccessState.REVOKED)())
        assertEquals(0, history.saveCount)
    }

    @Test
    fun `security exception maps to AccessDenied`() = runTest {
        usage.error = SecurityException("no access")
        assertEquals(TodayUsageResult.AccessDenied, useCase()())
    }

    @Test
    fun `other errors map to Failure`() = runTest {
        usage.error = IllegalStateException("boom")
        assertTrue(useCase()() is TodayUsageResult.Failure)
    }

    @Test
    fun `tracked app with no usage today shows zero`() = runTest {
        usage.usageByPackage = emptyMap()
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(listOf(0, 0), result.summary.apps.map { it.minutes })
        assertFalse(result.summary.hasAnyUsage)
    }
}
