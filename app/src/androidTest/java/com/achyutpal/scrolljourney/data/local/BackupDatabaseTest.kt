package com.achyutpal.scrolljourney.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.achyutpal.scrolljourney.data.backup.RoomBackupRepository
import com.achyutpal.scrolljourney.data.history.RoomHistoryRepository
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity
import com.achyutpal.scrolljourney.data.settings.RoomSettingsRepository
import com.achyutpal.scrolljourney.domain.model.AppUsage
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import com.achyutpal.scrolljourney.domain.model.Destination
import com.achyutpal.scrolljourney.domain.repository.InvalidBackupException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Export → wipe/change → restore against a real (in-memory) SQLite database. */
@RunWith(AndroidJUnit4::class)
class BackupDatabaseTest {

    private lateinit var db: ScrollJourneyDatabase
    private lateinit var backups: RoomBackupRepository
    private lateinit var history: RoomHistoryRepository
    private lateinit var settings: RoomSettingsRepository

    private val day = LocalDate.of(2026, 9, 28)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollJourneyDatabase::class.java).build()
        backups = RoomBackupRepository(db.backupDao(), "1.0.0", now = { Instant.parse("2026-10-10T08:00:00Z") }, zone = { ZoneId.of("Asia/Kolkata") })
        history = RoomHistoryRepository(db)
        settings = RoomSettingsRepository(db.userSettingsDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun everything() = db.backupDao().snapshot()

    @Test
    fun exportThenRestoreBringsBackEveryRow() = runTest {
        settings.setOnboardingComplete(day)
        settings.setDailyLimit(120)
        db.selectedAppDao().upsertAll(listOf(SelectedAppEntity("com.google.android.youtube", "YouTube", true, 1L)))
        history.saveDay(
            DailyRecord(day, 62, status = DayStatus.GREEN, limitMinutes = 120, distance = 6_200, finalized = true),
            listOf(AppUsage("com.google.android.youtube", "YouTube", 62 * 60_000L)),
        )
        val before = everything()
        val text = backups.export()

        // Change everything after the export.
        settings.setDailyLimit(30)
        settings.resetJourney(day.plusDays(5))
        history.saveDay(DailyRecord(day.plusDays(1), 300, status = DayStatus.RED, finalized = true), emptyList())

        val summary = backups.restore(text)

        assertEquals(before, everything())
        assertEquals(1, summary.days)
        assertEquals(Destination.EARTH, summary.destination)
        assertEquals("2026-10-10", summary.exportedOn)
    }

    @Test
    fun invalidBackupLeavesTheDatabaseUntouched() = runTest {
        settings.setDailyLimit(90)
        val before = everything()
        try {
            backups.restore("{\"format\":\"scroll-journey-backup\",\"version\":99}")
            fail("expected InvalidBackupException")
        } catch (e: InvalidBackupException) {
            // expected
        }
        assertEquals(before, everything())
    }
}
