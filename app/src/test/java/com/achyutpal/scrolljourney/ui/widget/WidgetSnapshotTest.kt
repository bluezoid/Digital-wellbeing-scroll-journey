package com.achyutpal.scrolljourney.ui.widget

import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WidgetSnapshotTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val settings = UserSettings(
        dailyLimitMinutes = 120, onboardingComplete = true, currentFuel = 6, lifetimeFuel = 8, highestReachedPlanet = Destination.MOON,
    )

    private fun day(daysAgo: Long, minutes: Int, status: DayStatus, finalized: Boolean = true) =
        DailyRecord(today.minusDays(daysAgo), minutes, status = status, limitMinutes = 120, finalized = finalized)

    @Test
    fun `shows today's minutes, status against the limit, rocket and streak`() {
        val records = listOf(day(0, 45, DayStatus.GREEN, finalized = false), day(1, 60, DayStatus.GREEN), day(2, 30, DayStatus.GREEN), day(3, 200, DayStatus.RED))
        val s = WidgetSnapshot.build(settings, records, today, hasUsageAccess = true)

        assertEquals(WidgetSnapshot.State.READY, s.state)
        assertEquals(45, s.todayMinutes)
        assertEquals(DayStatus.GREEN, s.status)
        assertEquals(Destination.MOON, s.rocket.highestReachedPlanet)
        assertEquals(3, s.streak)
        assertEquals(45f / 120f, s.limitFraction, 1e-4f)
    }

    @Test
    fun `no saved day yet means 0 minutes and green`() {
        val s = WidgetSnapshot.build(settings, emptyList(), today, hasUsageAccess = true)
        assertEquals(0, s.todayMinutes)
        assertEquals(DayStatus.GREEN, s.status)
    }

    @Test
    fun `status follows the limit and the bar stops at full`() {
        assertEquals(DayStatus.YELLOW, WidgetSnapshot.build(settings, listOf(day(0, 140, DayStatus.YELLOW, false)), today, true).status)
        val red = WidgetSnapshot.build(settings, listOf(day(0, 400, DayStatus.RED, false)), today, true)
        assertEquals(DayStatus.RED, red.status)
        assertEquals(1f, red.limitFraction, 0f)
    }

    @Test
    fun `not set up and usage access off have their own states`() {
        assertEquals(WidgetSnapshot.State.NOT_SET_UP, WidgetSnapshot.build(UserSettings(), emptyList(), today, true).state)
        assertEquals(WidgetSnapshot.State.NO_ACCESS, WidgetSnapshot.build(settings, emptyList(), today, false).state)
    }
}
