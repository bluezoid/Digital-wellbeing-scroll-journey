package com.achyutpal.scrolljourney.domain.usecase

import com.achyutpal.scrolljourney.domain.calculator.ReminderPolicy
import com.achyutpal.scrolljourney.domain.repository.HistoryRepository
import com.achyutpal.scrolljourney.domain.repository.ReminderGateway
import com.achyutpal.scrolljourney.domain.repository.SettingsRepository
import java.time.LocalDate

/** After a sync: send the near-limit / limit-reached reminder if one is due today (once each). */
class CheckLimitRemindersUseCase(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val gateway: ReminderGateway,
    private val today: () -> LocalDate = LocalDate::now,
) {
    suspend operator fun invoke() {
        val settings = settingsRepository.getSettings()
        if (!settings.onboardingComplete || !settings.remindersEnabled || !gateway.canNotify()) return
        val date = today()
        val minutes = historyRepository.getDay(date)?.totalMinutes ?: return
        val reminder = ReminderPolicy.due(minutes, settings.dailyLimitMinutes, gateway.sentOn(date)) ?: return
        gateway.show(reminder, minutes, settings.dailyLimitMinutes)
        gateway.markSent(date, reminder)
    }
}
