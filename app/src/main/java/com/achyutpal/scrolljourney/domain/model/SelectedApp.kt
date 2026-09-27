package com.achyutpal.scrolljourney.domain.model

/**
 * A tracked-app record. Kept even when [enabled] is false, so history survives
 * unticking or uninstalling the app, and a reinstall is not silently re-enabled.
 */
data class SelectedApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean,
    val addedAt: Long,
)
