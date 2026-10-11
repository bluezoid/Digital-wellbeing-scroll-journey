package com.achyutpal.scrolljourney.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.ui.apppicker.AppPickerContent
import com.achyutpal.scrolljourney.ui.apppicker.AppPickerUiState
import com.achyutpal.scrolljourney.ui.apppicker.AppRow
import com.achyutpal.scrolljourney.ui.dailylimit.DailyLimitContent
import com.achyutpal.scrolljourney.ui.dailylimit.DailyLimitUiState
import com.achyutpal.scrolljourney.ui.onboarding.IntroScreen
import com.achyutpal.scrolljourney.ui.onboarding.UsageAccessContent
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Onboarding screens (intro → usage access → app picker → daily limit) with fake state. */
@RunWith(AndroidJUnit4::class)
class OnboardingScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private val calls = Calls()

    private fun themed(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { ScrollJourneyTheme(darkTheme = true) { content() } }

    // --- Intro ---------------------------------------------------------------------------

    @Test
    fun introNextThenContinueFinishes() {
        themed { IntroScreen(onFinished = calls.record("finished")) }
        rule.onNodeWithText(str(R.string.intro1_title)).assertIsDisplayed()

        rule.onNodeWithText(str(R.string.action_next)).performClick()
        rule.onNodeWithText(str(R.string.intro2_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.action_continue)).performClick()

        assertEquals(listOf("finished"), calls.log)
    }

    // --- Usage access --------------------------------------------------------------------

    @Test
    fun usageAccessButtonOpensAndroidSettings() {
        themed { UsageAccessContent(state = UsageAccessState.REVOKED, onOpenSettings = calls.record("open")) }
        rule.onNodeWithText(str(R.string.usage_access_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.action_open_settings)).performClick()
        assertEquals(listOf("open"), calls.log)
    }

    @Test
    fun restrictedPhoneShowsTheGuidedStepsWithBothButtons() {
        themed {
            UsageAccessContent(
                state = UsageAccessState.REVOKED,
                restricted = true,
                onOpenSettings = calls.record("settings"),
                onOpenAppInfo = calls.record("appInfo"),
            )
        }
        rule.onNodeWithText(str(R.string.restricted_title), substring = true).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.action_open_settings)).performScrollTo().performClick()
        rule.onNodeWithText(str(R.string.restricted_open_app_info)).performScrollTo().performClick()
        assertEquals(listOf("settings", "appInfo"), calls.log)
    }

    @Test
    fun helpLinkRevealsTheRestrictedStepsWhenNotDetected() {
        themed { UsageAccessContent(state = UsageAccessState.REVOKED, onOpenSettings = {}, restricted = false) }
        rule.onNodeWithText(str(R.string.restricted_title), substring = true).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.restricted_help_link)).performClick()
        rule.onNodeWithText(str(R.string.restricted_title), substring = true).assertIsDisplayed()
    }

    // --- App picker ----------------------------------------------------------------------

    private val toggled = mutableListOf<String>()

    private fun picker(rows: List<AppRow>) = themed {
        AppPickerContent(
            state = AppPickerUiState(isLoading = false, rows = rows),
            onBack = null,
            onToggle = { toggled += it },
            onQueryChange = {},
            onSave = calls.record("save"),
            loadIcon = { null },
        )
    }

    @Test
    fun pickerCannotContinueWithNoAppsSelected() {
        picker(listOf(AppRow("com.instagram.android", "Instagram", false), AppRow("com.brave.browser", "Brave", false)))
        rule.onNodeWithText(str(R.string.app_picker_continue, 0)).assertIsNotEnabled()
    }

    @Test
    fun tappingAnAppTogglesIt() {
        picker(listOf(AppRow("com.instagram.android", "Instagram", false)))
        rule.onNodeWithText("Instagram").performClick()
        assertEquals(listOf("com.instagram.android"), toggled)
    }

    @Test
    fun pickerContinuesWithSelectedApps() {
        picker(listOf(AppRow("com.instagram.android", "Instagram", true), AppRow("com.google.android.youtube", "YouTube", true)))
        rule.onNodeWithText(str(R.string.app_picker_continue, 2)).assertIsEnabled().performClick()
        assertEquals(listOf("save"), calls.log)
    }

    // --- Daily limit ---------------------------------------------------------------------

    private val limits = mutableListOf<Int>()

    private fun limit(onboarding: Boolean) = themed {
        DailyLimitContent(
            state = DailyLimitUiState(isLoading = false, limitMinutes = 60),
            isOnboarding = onboarding,
            onBack = null,
            onLimitChange = { limits += it },
            onSave = calls.record("save"),
        )
    }

    @Test
    fun presetsSetTheLimit() {
        limit(onboarding = true)
        rule.onNodeWithText("30m").performClick()
        rule.onNodeWithText("2h").performClick()
        assertEquals(listOf(30, 120), limits)
    }

    @Test
    fun onboardingLimitStartsTheJourney() {
        limit(onboarding = true)
        rule.onNodeWithText(str(R.string.limit_start_journey)).performClick()
        assertEquals(listOf("save"), calls.log)
    }

    @Test
    fun changingTheLimitLaterSaysSave() {
        limit(onboarding = false)
        rule.onNodeWithText(str(R.string.limit_save)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.limit_start_journey)).assertDoesNotExist()
    }
}
