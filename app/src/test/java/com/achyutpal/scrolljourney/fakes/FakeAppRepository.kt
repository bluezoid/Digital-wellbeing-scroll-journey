package com.achyutpal.scrolljourney.fakes

import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.domain.model.SelectedApp
import com.achyutpal.scrolljourney.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppRepository(
    var installed: List<InstalledApp> = emptyList(),
    saved: List<SelectedApp> = emptyList(),
    /** Packages treated as uninstalled by [isInstalled] (everything else counts as installed). */
    var missingPackages: Set<String> = emptySet(),
) : AppRepository {

    private val table = MutableStateFlow(saved.associateBy { it.packageName })
    val savedRows: Map<String, SelectedApp> get() = table.value

    override suspend fun getInstalledApps() = installed
    override suspend fun getSavedSelections() = table.value.values.toList()
    override fun observeEnabledApps(): Flow<List<SelectedApp>> =
        table.map { rows -> rows.values.filter { it.enabled } }

    override suspend fun saveSelections(records: List<SelectedApp>) {
        table.value = table.value + records.associateBy { it.packageName }
    }

    override suspend fun isInstalled(packageName: String) = packageName !in missingPackages
}
