package com.achyutpal.scrolljourney.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.R
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.ui.history.CalendarCell
import com.achyutpal.scrolljourney.ui.history.DayAppLine
import com.achyutpal.scrolljourney.ui.history.DayDetail
import com.achyutpal.scrolljourney.ui.history.HistoryContent
import com.achyutpal.scrolljourney.ui.history.HistoryUiState
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** History calendar with fake state (spec §35–37). */
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val calls = Calls()
    private val selected = mutableListOf<LocalDate>()
    private val sep = YearMonth.of(2026, 9)

    private val week = (21..27).map { d ->
        CalendarCell(
            date = sep.atDay(d),
            status = if (d == 24) DayStatus.RED else DayStatus.GREEN,
            hasRecord = d != 23,
        )
    }

    private fun show(state: HistoryUiState) {
        rule.setContent {
            ScrollJourneyTheme(darkTheme = true) {
                HistoryContent(
                    state = state,
                    onBack = calls.record("back"),
                    onPrevious = calls.record("previous"),
                    onNext = calls.record("next"),
                    onSelect = { selected += it },
                )
            }
        }
    }

    @Test
    fun tappingADayOpensIt() {
        show(HistoryUiState(month = sep, weeks = listOf(week)))
        rule.onNodeWithText("25").performClick()
        assertEquals(listOf(sep.atDay(25)), selected)
    }

    @Test
    fun selectedDayShowsItsAppsAndMinutes() {
        val date = sep.atDay(25)
        show(
            HistoryUiState(
                month = sep,
                weeks = listOf(week),
                selectedDate = date,
                selected = DayDetail(
                    DailyRecord(date, 44, status = DayStatus.GREEN, limitMinutes = 60, finalized = true),
                    listOf(DayAppLine("Brave", 44)),
                ),
            ),
        )
        rule.onNodeWithText("Brave").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dayWithoutDataExplainsWhy() {
        show(HistoryUiState(month = sep, weeks = listOf(week), selectedDate = sep.atDay(23), selected = null))
        rule.onNodeWithText(str(R.string.history_no_data)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun streaksAndColourCountsAreShown() {
        show(HistoryUiState(month = sep, weeks = listOf(week), currentStreak = 3, bestStreak = 5, greenDays = 5, redDays = 1))
        rule.onNodeWithText(str(R.string.history_green_count, 5)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.history_red_count, 1)).assertIsDisplayed()
    }

    @Test
    fun monthArrowsFollowTheirLimits() {
        show(HistoryUiState(month = sep, weeks = listOf(week), canGoPrevious = true, canGoNext = false))
        rule.onNodeWithContentDescription(str(R.string.history_next_month)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(str(R.string.history_previous_month)).performClick()
        assertEquals(listOf("previous"), calls.log)
    }
}
