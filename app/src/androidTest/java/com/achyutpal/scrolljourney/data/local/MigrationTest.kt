package com.achyutpal.scrolljourney.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
