package com.achyutpal.scrolljourney.ui.startup

import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.domain.model.UserSettings
import com.achyutpal.scrolljourney.fakes.FakeSettingsRepository
import com.achyutpal.scrolljourney.fakes.FakeUsageAccessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun start(access: UsageAccessState, onboarded: Boolean) = StartupViewModel(
        FakeUsageAccessRepository(access),
        FakeSettingsRepository(UserSettings(onboardingComplete = onboarded)),
    ).start.value

    @Test
    fun `fresh install starts onboarding`() {
        assertEquals(StartDestination.ONBOARDING, start(UsageAccessState.REVOKED, onboarded = false))
    }

    @Test
    fun `access granted but onboarding unfinished resumes at app picker`() {
        assertEquals(StartDestination.APP_PICKER, start(UsageAccessState.GRANTED, onboarded = false))
    }

    @Test
    fun `onboarded user goes to the dashboard even if access was revoked`() {
        assertEquals(StartDestination.DASHBOARD, start(UsageAccessState.REVOKED, onboarded = true))
    }
}
