package com.achyutpal.scrolljourney.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.achyutpal.scrolljourney.MainActivity
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.calculator.LimitReminder
import com.achyutpal.scrolljourney.domain.calculator.ReminderPolicy
import com.achyutpal.scrolljourney.domain.repository.ReminderGateway
import com.achyutpal.scrolljourney.ui.common.formatMinutes
import java.time.LocalDate

/**
 * Notifications go through a **channel** (Android 8+): the user can mute or change "Limit
 * reminders" separately in the system settings. "Already sent today" is kept in small device-only
 * preferences (not in the backup: it's meaningless on another phone).
 */
class AndroidReminderGateway(private val context: Context) : ReminderGateway {

    private val prefs = context.getSharedPreferences("limit_reminders", Context.MODE_PRIVATE)

    override fun sentOn(date: LocalDate): Set<LimitReminder> =
        if (prefs.getString(KEY_DATE, null) != date.toString()) emptySet()
        else prefs.getStringSet(KEY_SENT, emptySet()).orEmpty().mapNotNull { name ->
            LimitReminder.entries.firstOrNull { it.name == name }
        }.toSet()

    override fun markSent(date: LocalDate, reminder: LimitReminder) {
        val sent = sentOn(date) + reminder
        prefs.edit {
            putString(KEY_DATE, date.toString())
            putStringSet(KEY_SENT, sent.map { it.name }.toSet())
        }
    }

    override fun canNotify(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @Suppress("MissingPermission") // canNotify() is checked by the caller right before
    override fun show(reminder: LimitReminder, minutes: Int, limitMinutes: Int) {
        ensureChannel(context)
        val (title, text) = when (reminder) {
            LimitReminder.NEAR -> context.getString(R.string.reminder_near_title) to context.getString(
                R.string.reminder_near_text,
                formatMinutes(minutes),
                formatMinutes(limitMinutes),
                formatMinutes((limitMinutes - minutes).coerceAtLeast(0)),
            )
            LimitReminder.REACHED -> context.getString(R.string.reminder_reached_title) to context.getString(
                R.string.reminder_reached_text,
                formatMinutes(minutes),
                formatMinutes(ReminderPolicy.yellowUntilMinutes(limitMinutes)),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        // One id per reminder kind: "reached" replaces "almost there" instead of stacking.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "limit_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val KEY_DATE = "date"
        private const val KEY_SENT = "sent"

        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
