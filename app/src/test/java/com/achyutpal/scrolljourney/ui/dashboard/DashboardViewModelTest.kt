package com.achyutpal.scrolljourney.ui.dashboard

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.domain.usecase.ApplyJourneyFuelUseCase
import com.achyutpal.scrolljourney.domain.usecase.SyncUsageHistoryUseCase
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
import com.achyutpal.scrolljourney.fakes.FakeJourneyRepository
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Spec §85: fake repositories in, correct DashboardUiState out. */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val min = 60_000L
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 9, 27)
    private val now = ZonedDateTime.of(2026, 9, 27, 14, 30, 0, 0, zone).toInstant()

    private val access = FakeUsageAccessRepository()
    private val apps = FakeAppRepository(saved = listOf(SelectedApp("com.instagram.android", "Instagram", true, 0L)))
    private val usage = FakeUsageRepository(usageByPackage = mapOf("com.instagram.android" to 45 * min))
    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository(
        UserSettings(
            dailyLimitMinutes = 60,
            onboardingComplete = true,
            currentFuel = 5,
            lifetimeFuel = 6,
            highestReachedPlanet = Planet.MOON,
            journeyStartDate = today.minusDays(30),
        ),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): DashboardViewModel {
        val sync = SyncUsageHistoryUseCase(
            usageAccessRepository = access,
            appRepository = apps,
            usageRepository = usage,
            historyRepository = history,
            settingsRepository = settings,
            applyJourneyFuel = ApplyJourneyFuelUseCase(FakeJourneyRepository(history, settings), settings),
            now = { now },
            zone = { zone },
        )
        return DashboardViewModel(sync, history, settings, today = { today }, clock = { 99L }).also { vm ->
            // Keep the WhileSubscribed flow active; the unconfined dispatcher makes collection run immediately.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }
    }

    @Test
    fun `refresh produces today's usage, limit, status, distance and time`() = runTest {
        val vm = viewModel()
        vm.refresh()
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertEquals(45, state.today?.totalMinutes)
        assertEquals(60, state.limitMinutes)
        assertEquals(DayStatus.GREEN, state.status)
        assertEquals(4_500L, state.distanceCentimetres)
        assertEquals(99L, state.updatedAtMillis)
    }

    @Test
    fun `rocket reflects fuel applied by the sync`() = runTest {
        val vm = viewModel()
        vm.refresh()
        // Journey started 30 days ago: the 7 backfilled days (45 min, green) each add +1 → 5 + 7 = 12 → Mars.
        assertEquals(12, vm.uiState.value.rocket.currentFuel)
        assertEquals(Planet.MARS, vm.uiState.value.rocket.highestReachedPlanet)
        assertEquals(today.minusDays(30), vm.uiState.value.journeyStartDate)
    }

    @Test
    fun `streak counts today and consecutive green days`() = runTest {
        // Backfill writes 7 past days of 45 min (green at 60). The pre-seeded yellow day 8 days ago ends the run.
        history.saveDay(DailyRecord(today.minusDays(8), 70, status = DayStatus.YELLOW, finalized = true), emptyList())
        val vm = viewModel()
        vm.refresh()
        assertEquals(8, vm.uiState.value.currentStreak) // 7 past days + today
    }

    @Test
    fun `week strip has the last 7 days ending today`() = runTest {
        val vm = viewModel()
        vm.refresh()
        val week = vm.uiState.value.week
        assertEquals(7, week.size)
        assertEquals(today, week.last().date)
        assertTrue(week.last().isToday)
        assertEquals(DayStatus.GREEN, week.last().status)
    }

    @Test
    fun `revoked access shows the warning and no usage`() = runTest {
        access.current = UsageAccessState.REVOKED
        val vm = viewModel()
        vm.refresh()
        assertTrue(vm.uiState.value.accessDenied)
        assertEquals(null, vm.uiState.value.today)
    }
}
