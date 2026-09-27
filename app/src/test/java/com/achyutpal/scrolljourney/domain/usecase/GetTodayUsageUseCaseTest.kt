package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GetTodayUsageUseCaseTest {

    private val min = 60_000L
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 9, 27, 14, 30, 0, 0, zone).toInstant()

    private fun app(pkg: String, name: String, enabled: Boolean = true) = SelectedApp(pkg, name, enabled, 0L)

    private val selected = listOf(
        app("com.instagram.android", "Instagram"),
        app("com.google.android.youtube", "YouTube"),
        app("com.facebook.katana", "Facebook", enabled = false),
    )

    private val usage = FakeUsageRepository(
        usageByPackage = mapOf(
            "com.instagram.android" to 20 * min,
            "com.google.android.youtube" to 10 * min,
            "com.facebook.katana" to 50 * min,
            "com.android.chrome" to 30 * min,
        ),
    )

    private fun useCase(access: UsageAccessState = UsageAccessState.GRANTED) = GetTodayUsageUseCase(
        usageAccessRepository = FakeUsageAccessRepository(access),
        appRepository = FakeAppRepository(saved = selected),
        usageRepository = usage,
        now = { now },
        zone = { zone },
    )

    @Test
    fun `total counts only enabled apps`() = runTest {
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(30, result.summary.totalMinutes) // 20 + 10; Facebook (disabled) and Chrome ignored
        assertEquals(listOf("Instagram", "YouTube"), result.summary.apps.map { it.appName })
    }

    @Test
    fun `window runs from local midnight to now`() = runTest {
        useCase()()
        val midnight = ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight to now.toEpochMilli(), usage.lastWindow)
    }

    @Test
    fun `summary carries today's date`() = runTest {
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(LocalDate.of(2026, 9, 27), result.summary.date)
    }

    @Test
    fun `tracked app with no usage shows zero and no-usage flag`() = runTest {
        usage.usageByPackage = emptyMap()
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(listOf(0, 0), result.summary.apps.map { it.minutes })
        assertFalse(result.summary.hasAnyUsage)
    }

    @Test
    fun `apps sorted by most used`() = runTest {
        usage.usageByPackage = mapOf("com.instagram.android" to 2 * min, "com.google.android.youtube" to 9 * min)
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(listOf("YouTube", "Instagram"), result.summary.apps.map { it.appName })
    }

    @Test
    fun `access off returns AccessDenied instead of zeros`() = runTest {
        assertEquals(TodayUsageResult.AccessDenied, useCase(UsageAccessState.REVOKED)())
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
    fun `minutes round down per app and total adds up`() = runTest {
        usage.usageByPackage = mapOf(
            "com.instagram.android" to 20 * min + 59_000,
            "com.google.android.youtube" to 10 * min + 59_000,
        )
        val result = useCase()() as TodayUsageResult.Success
        assertEquals(listOf(20, 10), result.summary.apps.map { it.minutes })
        assertEquals(30, result.summary.totalMinutes)
    }
}
