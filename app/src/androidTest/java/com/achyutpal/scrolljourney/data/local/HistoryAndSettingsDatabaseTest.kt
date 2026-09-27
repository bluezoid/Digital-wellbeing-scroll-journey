package com.achyutpal.scrolljourney.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.data.history.RoomHistoryRepository
import com.achyutpal.scrolljourney.data.settings.RoomSettingsRepository
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Planet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs on the phone against a real (in-memory) SQLite database. */
@RunWith(AndroidJUnit4::class)
class HistoryAndSettingsDatabaseTest {

    private lateinit var db: ScrollJourneyDatabase
    private lateinit var history: RoomHistoryRepository
    private lateinit var settings: RoomSettingsRepository

    private val day = LocalDate.of(2026, 9, 27)
    private val min = 60_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollJourneyDatabase::class.java)
            .build()
        history = RoomHistoryRepository(db)
        settings = RoomSettingsRepository(db.userSettingsDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun saveAndReadDay() = runTest {
        history.saveDay(DailyRecord(day, 30), listOf(AppUsage("insta", "Instagram", 30 * min)))
        assertEquals(DailyRecord(day, 30), history.getDay(day))
    }

    @Test
    fun savingSameDayTwiceUpdatesInsteadOfDuplicating() = runTest {
        history.saveDay(DailyRecord(day, 10), listOf(AppUsage("insta", "Instagram", 10 * min)))
        history.saveDay(DailyRecord(day, 25), listOf(AppUsage("insta", "Instagram", 25 * min)))

        assertEquals(1, history.getRange(day, day).size)
        assertEquals(25, history.getDay(day)?.totalMinutes)
        val rows = db.dailyAppUsageDao().getForDate(day.toString())
        assertEquals(1, rows.size)
        assertEquals(25, rows.single().minutes)
    }

    @Test
    fun perAppRowsAreReplacedWhenSelectionChanges() = runTest {
        history.saveDay(
            DailyRecord(day, 30),
            listOf(AppUsage("insta", "Instagram", 20 * min), AppUsage("yt", "YouTube", 10 * min)),
        )
        history.saveDay(DailyRecord(day, 20), listOf(AppUsage("insta", "Instagram", 20 * min)))

        assertEquals(listOf("insta"), db.dailyAppUsageDao().getForDate(day.toString()).map { it.packageName })
    }

    @Test
    fun rangeQueryIsInclusiveAndOrdered() = runTest {
        listOf(day.minusDays(3), day, day.minusDays(1), day.minusDays(10)).forEach {
            history.saveDay(DailyRecord(it, 1), emptyList())
        }
        val range = history.getRange(day.minusDays(3), day).map { it.date }
        assertEquals(listOf(day.minusDays(3), day.minusDays(1), day), range)
    }

    @Test
    fun markFinalizedFreezesExistingDay() = runTest {
        history.saveDay(DailyRecord(day, 44, finalized = false), emptyList())
        history.markFinalized(day, DayStatus.GREEN)
        assertEquals(DailyRecord(day, 44, status = DayStatus.GREEN, finalized = true), history.getDay(day))
    }

    @Test
    fun updateStatusKeepsMinutes() = runTest {
        history.saveDay(DailyRecord(day, 80, finalized = true), emptyList())
        history.updateStatus(day, DayStatus.RED)
        assertEquals(DailyRecord(day, 80, status = DayStatus.RED, finalized = true), history.getDay(day))
    }

    @Test
    fun dailyLimitPersists() = runTest {
        settings.setDailyLimit(90)
        assertEquals(90, settings.getSettings().dailyLimitMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDailyLimitIsRejected() = runTest {
        settings.setDailyLimit(241)
    }

    @Test
    fun recentDaysAreNewestFirstAndLimited() = runTest {
        (0L..9L).forEach { history.saveDay(DailyRecord(day.minusDays(it), it.toInt()), emptyList()) }
        val recent = history.observeRecentDays(7).first()
        assertEquals(7, recent.size)
        assertEquals(day, recent.first().date)
    }

    @Test
    fun settingsRowIsCreatedWithDefaults() = runTest {
        val s = settings.getSettings()
        assertEquals(60, s.dailyLimitMinutes)
        assertFalse(s.onboardingComplete)
        assertEquals(0, s.currentFuel)
        assertEquals(Planet.EARTH, s.highestReachedPlanet)
    }

    @Test
    fun onboardingFlagPersistsAndIsNotResetByLaterReads() = runTest {
        settings.setOnboardingComplete()
        assertTrue(settings.getSettings().onboardingComplete)
        assertTrue(settings.observeSettings().first().onboardingComplete)
    }
}
