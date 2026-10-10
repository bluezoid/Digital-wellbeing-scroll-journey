package com.achyutpal.scrolljourney.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Android 13+ asks the user before an app may post notifications (POST_NOTIFICATIONS, a runtime
 * permission). Older versions allow them by default. We ask once on the dashboard, and again
 * whenever the user turns reminders on in Settings.
 */
object NotificationPermission {
    @SuppressLint("InlinedApi") // a plain string; only requested on Android 13+ (see needsRequest)
    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    fun needsRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED

    /** True the first time only (per install), so the dashboard asks at most once by itself. */
    fun shouldAskOnce(context: Context): Boolean {
        if (!needsRequest(context)) return false
        val prefs = context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
        if (prefs.getBoolean("asked", false)) return false
        prefs.edit { putBoolean("asked", true) }
        return true
    }
}

/** The system screen for this app's notifications (to unblock them after "Don't allow"). */
fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }.onFailure { openAppDetailsSettings() }
}
