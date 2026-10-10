package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.GraphicsCapability

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

    /** False if notifications are blocked for the app (or not yet allowed on Android 13+). */
    fun areNotificationsEnabled(): Boolean

    /** Low-RAM flag and OpenGL ES version: decides whether the 3D journey can run here. */
    val graphicsCapability: GraphicsCapability
}
