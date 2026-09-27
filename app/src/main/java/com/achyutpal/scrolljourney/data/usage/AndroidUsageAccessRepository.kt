package com.achyutpal.scrolljourney.data.usage

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.repository.UsageAccessRepository

/**
 * Reads the Usage Access toggle through [AppOpsManager].
 *
 * Usage Access is not a normal runtime permission, so `checkSelfPermission` alone is not
 * enough: the real switch is the "app op" OPSTR_GET_USAGE_STATS. Its mode is:
 * - MODE_ALLOWED: user switched it on
 * - MODE_DEFAULT: never touched; fall back to the manifest permission check
 * - anything else: off
 */
class AndroidUsageAccessRepository(
    private val context: Context,
) : UsageAccessRepository {

    override fun checkUsageAccess(): UsageAccessState {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }

        val granted = when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> context.checkCallingOrSelfPermission(
                Manifest.permission.PACKAGE_USAGE_STATS,
            ) == PackageManager.PERMISSION_GRANTED
            else -> false
        }
        Log.d(TAG, "usage access check: appOpsMode=$mode granted=$granted")
        return if (granted) UsageAccessState.GRANTED else UsageAccessState.REVOKED
    }

    private companion object {
        const val TAG = "UsageAccess"
    }
}
