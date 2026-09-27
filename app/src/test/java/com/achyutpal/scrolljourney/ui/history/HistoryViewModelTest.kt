package com.achyutpal.scrolljourney.ui.history

import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val today = LocalDate.of(2026, 9, 27)
    private val history = FakeHistoryRepository()
    private val apps = FakeAppRepository(saved = listOf(SelectedApp("com.instagram.android", "Instagram", false, 0L)))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun seed() {
        // Aug 30 G, Aug 31 G, Sep 1 G (best run 3 across months), Sep 2 Y, Sep 25 G, Sep 26 G, Sep 27 R (today)
        listOf(
            today.withMonth(8).withDayOfMonth(30) to GREEN,
            today.withMonth(8).withDayOfMonth(31) to GREEN,
            today.withDayOfMonth(1) to GREEN,
            today.withDayOfMonth(2) to YELLOW,
            today.minusDays(2) to GREEN,
            today.minusDays(1) to GREEN,
            today to RED,
        ).forEach { (date, status) ->
            history.saveDay(DailyRecord(date, 30, status = status, limitMinutes = 60, finalized = date != today), emptyList())
        }
        history.saveDay(
            DailyRecord(today.minusDays(1), 21, status = GREEN, limitMinutes = 60, distance = 2_100, finalized = true),
            listOf(AppUsage("com.instagram.android", "Instagram", 21 * 60_000L)),
        )
    }

    private fun TestScope.viewModel() = HistoryViewModel(history, apps, today = { today }).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `current and best streak`() = runTest {
        seed()
        val state = viewModel().uiState.value
        assertEquals(0, state.currentStreak) // today red
        assertEquals(3, state.bestStreak) // Aug 30 – Sep 1
    }

    @Test
    fun `month summary counts only the shown month`() = runTest {
        seed()
        val state = viewModel().uiState.value
        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(3, state.greenDays)
        assertEquals(1, state.yellowDays)
        assertEquals(1, state.redDays)
    }

    @Test
    fun `month navigation is bounded by first record and current month`() = runTest {
        seed()
        val vm = viewModel()
        assertTrue(vm.uiState.value.canGoPrevious)
        assertFalse(vm.uiState.value.canGoNext)

        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 8), vm.uiState.value.month)
        assertFalse(vm.uiState.value.canGoPrevious) // August is the earliest
        assertTrue(vm.uiState.value.canGoNext)
        assertEquals(2, vm.uiState.value.greenDays)
    }

    @Test
    fun `selecting a day shows its details with app names`() = runTest {
        seed()
        val vm = viewModel()
        vm.selectDay(today.minusDays(1))

        val detail = vm.uiState.value.selected!!
        assertEquals(21, detail.record.totalMinutes)
        assertEquals(listOf(DayAppLine("Instagram", 21)), detail.apps) // name from the (disabled) selection row
    }

    @Test
    fun `selecting a day without data shows no detail`() = runTest {
        seed()
        val vm = viewModel()
        vm.selectDay(today.minusDays(10))
        assertEquals(today.minusDays(10), vm.uiState.value.selectedDate)
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun `today is selected on open`() = runTest {
        seed()
        val state = viewModel().uiState.value
        assertEquals(today, state.selectedDate)
        assertEquals(RED, state.selected?.record?.status)
    }
}
