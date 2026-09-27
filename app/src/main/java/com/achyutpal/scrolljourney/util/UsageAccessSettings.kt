package com.achyutpal.scrolljourney.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the system Usage Access screen.
 *
 * Tries, in order: our app's own toggle (supported on many Android 10+ devices),
 * the full Usage Access list, then plain Settings. Some manufacturers remove or
 * rename these screens, hence the fallbacks.
 */
fun Context.openUsageAccessSettings() {
    val candidates = listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")),
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
