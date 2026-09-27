package com.achyutpal.scrolljourney.ui.apppicker

import com.achyutpal.scrolljourney.data.apps.AppIconSource
import com.achyutpal.scrolljourney.domain.model.InstalledApp
import com.achyutpal.scrolljourney.fakes.FakeAppRepository
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPickerViewModelTest {

    private val noIcons = object : AppIconSource {
        override suspend fun loadIcon(packageName: String) = null
    }
    private val instagram = InstalledApp("com.instagram.android", "Instagram")
    private val chrome = InstalledApp("com.android.chrome", "Chrome")

    // viewModelScope runs on Dispatchers.Main, which doesn't exist on the JVM: swap in a test dispatcher.
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads installed apps with defaults pre-selected`() {
        val vm = AppPickerViewModel(FakeAppRepository(installed = listOf(chrome, instagram)), FakeSettingsRepository(), noIcons)
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf("Instagram", "Chrome"), state.rows.map { it.label })
        assertEquals(1, state.selectedCount)
        assertTrue(state.canContinue)
    }

    @Test
    fun `cannot continue with nothing selected`() {
        val vm = AppPickerViewModel(FakeAppRepository(installed = listOf(instagram)), FakeSettingsRepository(), noIcons)
        vm.onToggle(instagram.packageName)

        assertEquals(0, vm.uiState.value.selectedCount)
        assertFalse(vm.uiState.value.canContinue)
    }

    @Test
    fun `search filters visible rows but keeps selection`() {
        val vm = AppPickerViewModel(FakeAppRepository(installed = listOf(chrome, instagram)), FakeSettingsRepository(), noIcons)
        vm.onQueryChange("chr")

        assertEquals(listOf("Chrome"), vm.uiState.value.visibleRows.map { it.label })
        assertEquals(1, vm.uiState.value.selectedCount)
    }

    @Test
    fun `save persists enabled and disabled records and signals done`() {
        val repo = FakeAppRepository(installed = listOf(chrome, instagram))
        val settings = FakeSettingsRepository()
        val vm = AppPickerViewModel(repo, settings, noIcons, clock = { 42L })
        vm.onToggle(chrome.packageName)
        vm.onSave()

        assertTrue(vm.uiState.value.isDone)
        assertTrue(settings.state.value.onboardingComplete)
        assertEquals(setOf(instagram.packageName, chrome.packageName), repo.savedRows.keys)
        assertTrue(repo.savedRows.getValue(chrome.packageName).enabled)
        assertEquals(42L, repo.savedRows.getValue(chrome.packageName).addedAt)
    }

    @Test
    fun `reopening the picker shows the saved selection`() {
        val repo = FakeAppRepository(installed = listOf(chrome, instagram))
        AppPickerViewModel(repo, FakeSettingsRepository(), noIcons).apply {
            onToggle(instagram.packageName) // untick default
            onToggle(chrome.packageName)
            onSave()
        }

        val reopened = AppPickerViewModel(repo, FakeSettingsRepository(), noIcons).uiState.value
        assertEquals(setOf(chrome.packageName), reopened.rows.filter { it.selected }.map { it.packageName }.toSet())
    }
}
