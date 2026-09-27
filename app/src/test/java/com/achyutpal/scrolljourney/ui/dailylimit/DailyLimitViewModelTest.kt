package com.achyutpal.scrolljourney.ui.dailylimit

import com.achyutpal.scrolljourney.domain.model.UserSettings
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
class DailyLimitViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the saved limit, default 60`() {
        val vm = DailyLimitViewModel(FakeSettingsRepository(), isOnboarding = true)
        assertEquals(60, vm.uiState.value.limitMinutes)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `slider values snap into the allowed range`() {
        val vm = DailyLimitViewModel(FakeSettingsRepository(), isOnboarding = true)
        vm.onLimitChange(7)
        assertEquals(15, vm.uiState.value.limitMinutes)
        vm.onLimitChange(500)
        assertEquals(240, vm.uiState.value.limitMinutes)
    }

    @Test
    fun `saving in onboarding stores the limit and completes onboarding`() {
        val settings = FakeSettingsRepository()
        val vm = DailyLimitViewModel(settings, isOnboarding = true)
        vm.onLimitChange(90)
        vm.onSave()

        assertTrue(vm.uiState.value.isDone)
        assertEquals(90, settings.state.value.dailyLimitMinutes)
        assertTrue(settings.state.value.onboardingComplete)
    }

    @Test
    fun `saving from home does not touch the onboarding flag`() {
        val settings = FakeSettingsRepository(UserSettings(onboardingComplete = false))
        val vm = DailyLimitViewModel(settings, isOnboarding = false)
        vm.onLimitChange(30)
        vm.onSave()

        assertEquals(30, settings.state.value.dailyLimitMinutes)
        assertFalse(settings.state.value.onboardingComplete)
    }
}
