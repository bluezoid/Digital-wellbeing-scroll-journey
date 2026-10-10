package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.calculator.LimitReminder
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.fakes.FakeHistoryRepository
import com.achyutpal.scrolljourney.fakes.FakeReminderGateway
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CheckLimitRemindersUseCaseTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val settings = FakeSettingsRepository(UserSettings(dailyLimitMinutes = 120, onboardingComplete = true))
    private val history = FakeHistoryRepository()
    private val gateway = FakeReminderGateway()
    private val check = CheckLimitRemindersUseCase(settings, history, gateway, today = { today })

    private suspend fun used(minutes: Int) = history.saveDay(DailyRecord(today, minutes), emptyList())

    @Test
    fun `sends near then reached, each once a day`() = runTest {
        used(50); check()
        used(100); check(); check()
        used(125); check(); check()

        assertEquals(listOf(Triple(LimitReminder.NEAR, 100, 120), Triple(LimitReminder.REACHED, 125, 120)), gateway.shown)
    }

    @Test
    fun `a new day starts fresh`() = runTest {
        used(130); check()
        val tomorrow = CheckLimitRemindersUseCase(settings, history, gateway, today = { today.plusDays(1) })
        history.saveDay(DailyRecord(today.plusDays(1), 130), emptyList())
        tomorrow()
        assertEquals(2, gateway.shown.size)
    }

    @Test
    fun `nothing when reminders are off, notifications blocked, or not set up`() = runTest {
        used(130)
        settings.state.value = settings.state.value.copy(remindersEnabled = false); check()
        settings.state.value = settings.state.value.copy(remindersEnabled = true); gateway.allowed = false; check()
        gateway.allowed = true; settings.state.value = settings.state.value.copy(onboardingComplete = false); check()
        assertEquals(emptyList<Any>(), gateway.shown)
    }

    @Test
    fun `nothing before today's usage is known`() = runTest {
        check()
        assertEquals(emptyList<Any>(), gateway.shown)
    }
}
