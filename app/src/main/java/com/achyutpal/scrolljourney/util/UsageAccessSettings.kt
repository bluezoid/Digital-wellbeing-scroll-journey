package com.achyutpal.scrolljourney.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Opens the system Usage Access screen.
 *
 * Tries, in order: our app's own toggle (supported on many Android 10+ devices),
 * the full Usage Access list, then plain Settings. Some manufacturers remove or
 * rename these screens, hence the fallbacks.
 */
/** Opens this app's page in Android Settings (battery / background activity live there). */
fun Context.openAppDetailsSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun Context.openUsageAccessSettings() {
    val candidates = listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:$packageName".toUri()),
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in candidates) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next, more generic screen.
        }
    }
}
