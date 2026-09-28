package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.DayStatus.GREEN
import com.achyutpal.scrolljourney.domain.model.DayStatus.RED
import com.achyutpal.scrolljourney.domain.model.DayStatus.YELLOW
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
import com.achyutpal.scrolljourney.fakes.FakeJourneyRepository
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ApplyJourneyFuelUseCaseTest {

    private val start = LocalDate.of(2026, 9, 20)
    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository(UserSettings(journeyStartDate = start))
    private val useCase = ApplyJourneyFuelUseCase(FakeJourneyRepository(history, settings), settings)

    private suspend fun day(offset: Long, status: DayStatus, finalized: Boolean = true, distance: Long = 1_000) {
        history.saveDay(
            DailyRecord(start.plusDays(offset), 10, status = status, distance = distance, finalized = finalized),
            emptyList(),
        )
    }

    @Test
    fun `nothing happens before the journey has started`() = runTest {
        settings.state.value = UserSettings(journeyStartDate = null)
        day(0, GREEN)
        assertEquals(0, useCase())
        assertEquals(0, settings.state.value.currentFuel)
    }

    @Test
    fun `days are applied oldest first with the right deltas`() = runTest {
        day(2, RED); day(0, GREEN); day(1, GREEN); day(3, YELLOW)

        assertEquals(4, useCase())

        // G, G, R, Y → 1, 2, 1, 1
        assertEquals(1, settings.state.value.currentFuel)
        assertEquals(2, settings.state.value.lifetimeFuel)
        assertEquals(listOf(1, 1, -1, 0), (0L..3L).map { history.records.getValue(start.plusDays(it)).fuelDelta })
    }

    @Test
    fun `each day counts exactly once`() = runTest {
        day(0, GREEN); day(1, GREEN)
        useCase()
        assertEquals(0, useCase())
        assertEquals(2, settings.state.value.currentFuel)
    }

    @Test
    fun `days before the journey start never give fuel`() = runTest {
        day(-1, GREEN); day(-2, GREEN); day(0, GREEN)
        useCase()
        assertEquals(1, settings.state.value.currentFuel)
        assertFalse(history.records.getValue(start.minusDays(1)).fuelApplied)
    }

    @Test
    fun `unfinished today is not applied until finalized`() = runTest {
        day(0, GREEN, finalized = false)
        assertEquals(0, useCase())

        history.markFinalized(start, GREEN, limitMinutes = 60, distance = 1_000)
        assertEquals(1, useCase())
        assertTrue(history.records.getValue(start).fuelApplied)
    }

    @Test
    fun `journey distance adds up the applied days`() = runTest {
        day(0, GREEN, distance = 4_400); day(1, RED, distance = 24_400)
        useCase()
        assertEquals(28_800L, settings.state.value.journeyDistance)
    }

    @Test
    fun `three green days reach the Moon and a red day keeps it`() = runTest {
        day(0, GREEN); day(1, GREEN); day(2, GREEN); day(3, RED)
        useCase()
        assertEquals(2, settings.state.value.currentFuel)
        assertEquals(Destination.MOON, settings.state.value.highestReachedPlanet)
    }
}
