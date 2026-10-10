package com.achyutpal.scrolljourney.data.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.achyutpal.scrolljourney.domain.model.GraphicsCapability
import com.achyutpal.scrolljourney.domain.repository.DeviceStatusRepository

class AndroidDeviceStatusRepository(
    private val context: Context,
) : DeviceStatusRepository {

    /** ActivityManager.isBackgroundRestricted exists from Android 9 (API 28); before that there's no such switch. */
    override fun isBackgroundRestricted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted

    override fun isIgnoringBatteryOptimizations(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    /** Read once: neither value can change while the app runs. */
    override val graphicsCapability: GraphicsCapability by lazy {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        GraphicsCapability(
            isLowRamDevice = activityManager.isLowRamDevice,
            glEsVersion = activityManager.deviceConfigurationInfo.reqGlEsVersion,
        )
    }

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override val appVersionName: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}
