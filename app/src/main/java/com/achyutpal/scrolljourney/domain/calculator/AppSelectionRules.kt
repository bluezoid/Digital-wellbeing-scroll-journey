package com.achyutpal.scrolljourney.domain.calculator

import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp

/** Pure business rules for the app picker. No Android, no Room: unit-tested directly. */
object AppSelectionRules {

    /** Pre-ticked on first run, but only if actually installed. */
    val DEFAULT_PACKAGES: Set<String> = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.twitter.android",
        "com.snapchat.android",
    )

    /**
     * Which installed apps start ticked.
     * - Nothing saved yet (first run): installed default social apps.
     * - Otherwise: exactly what the user saved as enabled (so a reinstalled app stays unticked).
     */
    fun initialSelection(installed: List<InstalledApp>, saved: List<SelectedApp>): Set<String> {
        val installedPackages = installed.map { it.packageName }.toSet()
        val wanted = if (saved.isEmpty()) {
            DEFAULT_PACKAGES
        } else {
            saved.filter { it.enabled }.map { it.packageName }.toSet()
        }
        return wanted intersect installedPackages
    }

    /** Initially ticked apps first, then alphabetical. */
    fun sortForDisplay(installed: List<InstalledApp>, preselected: Set<String>): List<InstalledApp> =
        installed.sortedWith(
            compareByDescending<InstalledApp> { it.packageName in preselected }
                .thenBy { it.label.lowercase() },
        )

    /**
     * Turns the picker's final ticks into records to upsert.
     * - Ticked: enabled, keeping the original addedAt if it existed.
     * - Unticked but saved before: disabled (history is kept).
     * - Saved + enabled but no longer installed: disabled, so a reinstall isn't auto re-enabled.
     */
    fun buildSelectionRecords(
        installed: List<InstalledApp>,
        saved: List<SelectedApp>,
        selected: Set<String>,
        now: Long,
    ): List<SelectedApp> {
        val savedByPackage = saved.associateBy { it.packageName }
        val installedPackages = installed.map { it.packageName }.toSet()

        val forInstalled = installed.mapNotNull { app ->
            val previous = savedByPackage[app.packageName]
            when {
                app.packageName in selected -> SelectedApp(
                    packageName = app.packageName,
                    appName = app.label,
                    enabled = true,
                    addedAt = previous?.addedAt ?: now,
                )
                previous != null -> previous.copy(appName = app.label, enabled = false)
                else -> null
            }
        }
        val forUninstalled = saved
            .filter { it.enabled && it.packageName !in installedPackages }
            .map { it.copy(enabled = false) }

        return forInstalled + forUninstalled
    }
}
