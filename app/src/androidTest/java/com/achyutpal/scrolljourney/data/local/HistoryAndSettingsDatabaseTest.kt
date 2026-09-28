package com.achyutpal.scrolljourney.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.data.history.RoomHistoryRepository
import com.achyutpal.scrolljourney.data.journey.RoomJourneyRepository
import com.achyutpal.scrolljourney.data.settings.RoomSettingsRepository
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Planet
import com.achyutpal.scrolljourney.domain.model.RocketState
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
        history.markFinalized(day, DayStatus.GREEN, limitMinutes = 60, distance = 4_400)
        assertEquals(
            DailyRecord(day, 44, status = DayStatus.GREEN, limitMinutes = 60, distance = 4_400, finalized = true),
            history.getDay(day),
        )
    }

    @Test
    fun updateEvaluationKeepsMinutes() = runTest {
        history.saveDay(DailyRecord(day, 80, finalized = true), emptyList())
        history.updateEvaluation(day, DayStatus.RED, limitMinutes = 30, distance = 8_000)
        assertEquals(
            DailyRecord(day, 80, status = DayStatus.RED, limitMinutes = 30, distance = 8_000, finalized = true),
            history.getDay(day),
        )
    }

    @Test
    fun dailyLimitPersists() = runTest {
        settings.setDailyLimit(90)
        assertEquals(90, settings.getSettings().dailyLimitMinutes)
    }

    @Test
    fun resetJourneyRestartsRocketButKeepsHistoryAndLimit() = runTest {
        val journey = RoomJourneyRepository(db)
        settings.setOnboardingComplete(journeyStart = day.minusDays(10))
        settings.setDailyLimit(90)
        history.saveDay(
            DailyRecord(day, 20, status = DayStatus.GREEN, limitMinutes = 90, distance = 2_000, finalized = true),
            listOf(AppUsage("insta", "Instagram", 20 * min)),
        )
        journey.applyDay(history.getDay(day)!!, 1) { it.copy(currentFuel = 5, lifetimeFuel = 5, highestReachedPlanet = Planet.MOON) }

        settings.resetJourney(journeyStart = day.plusDays(1))

        val s = settings.getSettings()
        assertEquals(0, s.currentFuel)
        assertEquals(0, s.lifetimeFuel)
        assertEquals(Planet.EARTH, s.highestReachedPlanet)
        assertEquals(0L, s.journeyDistance)
        assertEquals(day.plusDays(1), s.journeyStartDate)
        assertEquals(90, s.dailyLimitMinutes)
        assertTrue(s.onboardingComplete)
        val kept = history.getDay(day)!!
        assertEquals(20, kept.totalMinutes)
        assertTrue(kept.fuelApplied)
        assertEquals(1, db.dailyAppUsageDao().getForDate(day.toString()).size)
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
    fun onboardingFlagPersistsAndStartsTheJourneyOnce() = runTest {
        settings.setOnboardingComplete(journeyStart = day)
        assertTrue(settings.getSettings().onboardingComplete)
        assertTrue(settings.observeSettings().first().onboardingComplete)
        assertEquals(day, settings.getSettings().journeyStartDate)

        settings.ensureJourneyStarted(day.plusDays(5)) // must not move an existing start
        assertEquals(day, settings.getSettings().journeyStartDate)
    }

    @Test
    fun fuelIsAppliedExactlyOnceAndRocketSavedInSameTransaction() = runTest {
        val journey = RoomJourneyRepository(db)
        settings.ensureJourneyStarted(day)
        history.saveDay(
            DailyRecord(day, 20, status = DayStatus.GREEN, limitMinutes = 60, distance = 2_000, finalized = true),
            emptyList(),
        )
        history.saveDay(DailyRecord(day.plusDays(1), 5, status = DayStatus.GREEN, finalized = false), emptyList())

        val awaiting = journey.daysAwaitingFuel(from = day)
        assertEquals(listOf(day), awaiting.map { it.date }) // unfinalized day excluded

        val update = { r: RocketState -> r.copy(currentFuel = r.currentFuel + 1, lifetimeFuel = r.lifetimeFuel + 1) }
        assertTrue(journey.applyDay(awaiting.single(), fuelDelta = 1, update = update))
        assertFalse(journey.applyDay(awaiting.single(), fuelDelta = 1, update = update)) // second time: no-op

        val saved = settings.getSettings()
        assertEquals(1, saved.currentFuel)
        assertEquals(1, saved.lifetimeFuel)
        assertEquals(2_000L, saved.journeyDistance)
        assertTrue(history.getDay(day)!!.fuelApplied)
        assertEquals(1, history.getDay(day)!!.fuelDelta)
        assertTrue(journey.daysAwaitingFuel(from = day).isEmpty())
    }
}
