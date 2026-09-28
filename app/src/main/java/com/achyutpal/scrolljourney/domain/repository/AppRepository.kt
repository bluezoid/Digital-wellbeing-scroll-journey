package com.achyutpal.scrolljourney.domain.repository

import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import kotlinx.coroutines.flow.Flow

/** Installed apps (from the system) and the user's tracked-app selection (from the database). */
interface AppRepository {
    suspend fun getInstalledApps(): List<InstalledApp>
    suspend fun getSavedSelections(): List<SelectedApp>
    fun observeEnabledApps(): Flow<List<SelectedApp>>
    suspend fun saveSelections(records: List<SelectedApp>)

    /** True if [packageName] is currently installed for this user. */
    suspend fun isInstalled(packageName: String): Boolean
}
