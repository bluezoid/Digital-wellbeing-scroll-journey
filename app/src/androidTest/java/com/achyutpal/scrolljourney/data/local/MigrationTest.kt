package com.achyutpal.scrolljourney.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrades a real v1 database file to v2 using the exported schema JSONs,
 * and checks nothing is lost. Add a test here for every future version bump.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ScrollJourneyDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsSelectedAppsAndMarksExistingUsersOnboarded() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO selected_apps (packageName, appName, enabled, addedAt) " +
                    "VALUES ('com.instagram.android', 'Instagram', 1, 100)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT appName, enabled FROM selected_apps").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("Instagram", it.getString(0))
            assertEquals(1, it.getInt(1))
        }
        db.query("SELECT onboardingComplete, dailyLimitMinutes, highestReachedPlanet FROM user_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(60, it.getInt(1))
            assertEquals("EARTH", it.getString(2))
        }
        db.query("SELECT COUNT(*) FROM daily_usage").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate1To2_userWithoutSavedAppsIsNotOnboarded() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT onboardingComplete FROM user_settings WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate2To3_backfillsLimitOnlyForColouredFinalizedDays() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO daily_usage (date, totalMinutes, status, fuelDelta, distance, finalized) VALUES " +
                    "('2026-09-25', 44, 'GREEN', 0, 0, 1), " + // coloured + finalized → 60
                    "('2026-09-26', 21, NULL, 0, 0, 1), " + // finalized but never coloured → NULL
                    "('2026-09-27', 137, 'RED', 0, 0, 0)", // live today → NULL (rewritten by next sync)
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        db.query("SELECT date, totalMinutes, status, limitMinutes FROM daily_usage ORDER BY date").use {
            it.moveToNext()
            assertEquals("2026-09-25", it.getString(0))
            assertEquals(44, it.getInt(1))
            assertEquals("GREEN", it.getString(2))
            assertEquals(60, it.getInt(3))
            it.moveToNext()
            assertTrue(it.isNull(3))
            it.moveToNext()
            assertTrue(it.isNull(3))
        }
    }

    @Test
    fun migrate3To4_addsFuelFlagAndJourneyStartWithoutTouchingData() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO daily_usage (date, totalMinutes, status, limitMinutes, fuelDelta, distance, finalized) " +
                    "VALUES ('2026-09-25', 44, 'GREEN', 60, 0, 4400, 1)",
            )
            execSQL(
                "INSERT INTO user_settings (id, dailyLimitMinutes, onboardingComplete, currentFuel, lifetimeFuel, " +
                    "highestReachedPlanet, journeyDistance) VALUES (1, 120, 1, 0, 0, 'EARTH', 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query("SELECT totalMinutes, distance, fuelApplied FROM daily_usage").use {
            it.moveToFirst()
            assertEquals(44, it.getInt(0))
            assertEquals(4400, it.getInt(1))
            assertEquals(0, it.getInt(2))
        }
        db.query("SELECT dailyLimitMinutes, journeyStartDate FROM user_settings").use {
            it.moveToFirst()
            assertEquals(120, it.getInt(0))
            assertTrue(it.isNull(1))
        }
    }

    @Test
    fun migrate4To5_addsJourney3dSwitchOnWithoutTouchingData() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO user_settings (id, dailyLimitMinutes, onboardingComplete, currentFuel, lifetimeFuel, " +
                    "highestReachedPlanet, journeyDistance, journeyStartDate) VALUES (1, 90, 1, 4, 4, 'MOON', 900, '2026-09-27')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)

        db.query("SELECT dailyLimitMinutes, currentFuel, highestReachedPlanet, journeyStartDate, journey3dEnabled FROM user_settings").use {
            it.moveToFirst()
            assertEquals(90, it.getInt(0))
            assertEquals(4, it.getInt(1))
            assertEquals("MOON", it.getString(2))
            assertEquals("2026-09-27", it.getString(3))
            assertEquals(1, it.getInt(4)) // existing users get 3D on
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
