package com.achyutpal.scrolljourney.data.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.achyutpal.scrolljourney.data.local.dao.SelectedAppDao
import com.achyutpal.scrolljourney.data.local.entity.toDomain
import com.achyutpal.scrolljourney.data.local.entity.toEntity
import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Installed apps come from [PackageManager]; the selection lives in Room.
 *
 * We ask for "activities that appear in the launcher" instead of every installed package.
 * On Android 11+ this only works because the manifest declares a matching <queries> block,
 * which is the narrow alternative to the Play-restricted QUERY_ALL_PACKAGES permission.
 */
class AndroidAppRepository(
    private val packageManager: PackageManager,
    private val ownPackageName: String,
    private val selectedAppDao: SelectedAppDao,
) : AppRepository {

    override suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }
        activities
            .map { it.activityInfo }
            .filter { it.packageName != ownPackageName }
            .distinctBy { it.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = info.applicationInfo.loadLabel(packageManager).toString(),
                )
            }
    }

    override suspend fun getSavedSelections(): List<SelectedApp> =
        selectedAppDao.getAll().map { it.toDomain() }

    override fun observeEnabledApps(): Flow<List<SelectedApp>> =
        selectedAppDao.observeEnabled().map { list -> list.map { it.toDomain() } }

    override suspend fun saveSelections(records: List<SelectedApp>) {
        selectedAppDao.upsertAll(records.map { it.toEntity() })
    }

    /**
     * Asks for one package by name (cheap). Tracked apps are launcher apps, so the manifest's
     * <queries> block makes them visible here; an uninstalled app throws NameNotFoundException.
     */
    override suspend fun isInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                packageManager.getApplicationInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
