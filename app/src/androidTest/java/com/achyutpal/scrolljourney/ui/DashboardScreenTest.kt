package com.achyutpal.scrolljourney.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyUsageSummary
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.dashboard.DashboardContent
import com.achyutpal.scrolljourney.ui.dashboard.DashboardUiState
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Dashboard with fake state. The live 3D hero isn't covered here (show3d = false): a robot can't
 * judge a 3D picture, it was checked by eye on the phone; its show/hide rule is unit-tested.
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val calls = Calls()
    private val today = LocalDate.of(2026, 9, 28)
    private val min = 60_000L

    private fun show(state: DashboardUiState) {
        rule.setContent {
            ScrollJourneyTheme(darkTheme = true) {
                DashboardContent(
                    state = state,
                    onRefresh = calls.record("refresh"),
                    onOpenUsageAccess = calls.record("usageAccess"),
                    onOpenHistory = calls.record("history"),
                    onOpenSettings = calls.record("settings"),
                    onOpenJourney = calls.record("journey"),
                )
            }
        }
    }

    private val loaded = DashboardUiState(
        isLoading = false,
        today = DailyUsageSummary(
            today,
            listOf(AppUsage("com.facebook.katana", "Facebook", 11 * min), AppUsage("com.instagram.android", "Instagram", 2 * min)),
        ),
        limitMinutes = 120,
        status = DayStatus.GREEN,
        show3d = false,
    )

    @Test
    fun showsTodaysMinutesAgainstTheLimitAndPerApp() {
        show(loaded)
        rule.onNodeWithText("13m").assertIsDisplayed()
        rule.onNodeWithText(str(R.string.home_limit_of, "2h 00m")).assertIsDisplayed()
        rule.onNodeWithText("Facebook").assertIsDisplayed()
        rule.onNodeWithText("Instagram").assertIsDisplayed()
    }

    @Test
    fun twoDJourneyCardWhenThreeDIsOff() {
        show(loaded)
        rule.onNodeWithText(str(R.string.rocket_title)).assertIsDisplayed()
    }

    @Test
    fun menuHasOnlyHistoryAndSettings() {
        show(loaded)
        rule.onNodeWithContentDescription(str(R.string.action_more)).performClick()
        rule.onNodeWithText(str(R.string.history_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.settings_title)).performClick()
        assertEquals(listOf("settings"), calls.log)
        rule.onNodeWithText("3D solar system (preview)").assertDoesNotExist()
    }

    @Test
    fun refreshButtonTriggersASync() {
        show(loaded)
        rule.onNodeWithContentDescription(str(R.string.action_refresh)).performClick()
        assertEquals(listOf("refresh"), calls.log)
    }

    @Test
    fun usageAccessOffShowsTheWarning() {
        show(loaded.copy(accessDenied = true, today = null))
        rule.onNodeWithText(str(R.string.home_revoked_title), substring = true).assertIsDisplayed() // title has a ⚠️ prefix
        rule.onNodeWithText(str(R.string.action_open_settings)).performClick()
        assertEquals(listOf("usageAccess"), calls.log)
    }
}
