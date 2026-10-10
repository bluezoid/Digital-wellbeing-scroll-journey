package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.calculator.LimitReminder
import java.time.LocalDate

/** Device-side reminder plumbing: what was sent today, and showing a notification. */
interface ReminderGateway {
    fun sentOn(date: LocalDate): Set<LimitReminder>
    fun markSent(date: LocalDate, reminder: LimitReminder)

    /** False if the user blocked notifications for the app (or hasn't allowed them on Android 13+). */
    fun canNotify(): Boolean
    fun show(reminder: LimitReminder, minutes: Int, limitMinutes: Int)
}
