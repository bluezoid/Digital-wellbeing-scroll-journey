package com.achyutpal.scrolljourney.domain.repository

/**
 * Whether the phone lets the app work in the background. Some manufacturers (realme, Xiaomi, …)
 * freeze background apps aggressively, which delays the daily sync job.
 */
interface DeviceStatusRepository {
    /** True if the user (or the system) has restricted this app's background activity. */
    fun isBackgroundRestricted(): Boolean

    /** True if the app is exempt from battery optimisation (Doze app standby). */
    fun isIgnoringBatteryOptimizations(): Boolean

    val appVersionName: String
}
