package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.calculator.LimitReminder
import com.achyutpal.scrolljourney.domain.repository.ReminderGateway
import java.time.LocalDate

class FakeReminderGateway(var allowed: Boolean = true) : ReminderGateway {
    val sent = mutableMapOf<LocalDate, MutableSet<LimitReminder>>()
    val shown = mutableListOf<Triple<LimitReminder, Int, Int>>()

    override fun sentOn(date: LocalDate): Set<LimitReminder> = sent[date].orEmpty()
    override fun markSent(date: LocalDate, reminder: LimitReminder) {
        sent.getOrPut(date) { mutableSetOf() } += reminder
    }
    override fun canNotify() = allowed
    override fun show(reminder: LimitReminder, minutes: Int, limitMinutes: Int) {
        shown += Triple(reminder, minutes, limitMinutes)
    }
}
