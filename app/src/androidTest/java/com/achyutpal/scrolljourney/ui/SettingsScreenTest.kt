package com.achyutpal.scrolljourney.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.ui.settings.SettingsContent
import com.achyutpal.scrolljourney.ui.settings.SettingsUiState
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings screen with fake state: reset dialog, 3D switch, credits (spec §41–43, §69). */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val calls = Calls()
    private val switchChanges = mutableListOf<Boolean>()

    private fun show(state: SettingsUiState = SettingsUiState(trackedAppCount = 4, versionName = "0.1.0")) {
        rule.setContent {
            ScrollJourneyTheme(darkTheme = true) {
                SettingsContent(
                    state = state,
                    onBack = calls.record("back"),
                    onChangeApps = calls.record("apps"),
                    onChangeLimit = calls.record("limit"),
                    onOpenUsageAccess = calls.record("usageAccess"),
                    onOpenAppSettings = calls.record("appSettings"),
                    onResetClick = calls.record("resetClick"),
                    onResetConfirm = calls.record("resetConfirm"),
                    onResetDismiss = calls.record("resetDismiss"),
                    onJourney3dChanged = { switchChanges += it },
                )
            }
        }
    }

    @Test
    fun tappingResetJourneyAsksForConfirmation() {
        show()
        rule.onNodeWithText(str(R.string.settings_reset_journey)).performScrollTo().performClick()
        assertEquals(listOf("resetClick"), calls.log)
    }

    @Test
    fun resetDialogCancelAndConfirmCallTheRightActions() {
        show(SettingsUiState(showResetConfirm = true))
        rule.onNodeWithText(str(R.string.settings_reset_confirm_title)).assertIsDisplayed()

        rule.onNodeWithText(str(R.string.action_cancel)).performClick()
        rule.onNodeWithText(str(R.string.settings_reset_confirm_action)).performClick()

        assertEquals(listOf("resetDismiss", "resetConfirm"), calls.log)
    }

    @Test
    fun resetDoneMessageIsShown() {
        show(SettingsUiState(resetDone = true))
        rule.onNodeWithText(str(R.string.settings_reset_done)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun switchIsOnByDefaultAndTurnsOff() {
        show()
        val row = rule.onNodeWithText(str(R.string.settings_journey_3d)).performScrollTo()
        rule.onNodeWithText(str(R.string.settings_journey_3d_on)).assertIsDisplayed()
        row.performClick()
        assertEquals(listOf(false), switchChanges)
    }

    @Test
    fun switchOffShowsTheTwoDDescription() {
        show(SettingsUiState(journey3dEnabled = false))
        rule.onNodeWithText(str(R.string.settings_journey_3d_off)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unsupportedPhoneShowsTheReasonAndCannotToggle() {
        show(SettingsUiState(device3dSupported = false))
        rule.onNodeWithText(str(R.string.settings_journey_3d_unsupported)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(str(R.string.settings_journey_3d)).performClick()
        assertEquals(emptyList<Boolean>(), switchChanges)
    }

    @Test
    fun creditsAreListed() {
        show()
        listOf(R.string.settings_credits_textures, R.string.settings_credits_rocket, R.string.settings_credits_engine).forEach {
            rule.onNodeWithText(str(it)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun trackingRowsOpenTheirScreens() {
        show()
        rule.onNodeWithText(str(R.string.settings_tracked_apps)).performClick()
        rule.onNodeWithText(str(R.string.settings_daily_limit)).performClick()
        assertEquals(listOf("apps", "limit"), calls.log)
    }
}
