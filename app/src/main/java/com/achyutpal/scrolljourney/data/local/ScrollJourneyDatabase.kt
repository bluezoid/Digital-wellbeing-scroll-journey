package com.achyutpal.scrolljourney.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.achyutpal.scrolljourney.data.local.dao.DailyAppUsageDao
import com.achyutpal.scrolljourney.data.local.dao.DailyUsageDao
import com.achyutpal.scrolljourney.data.local.dao.SelectedAppDao
import com.achyutpal.scrolljourney.data.local.dao.UserSettingsDao
import com.achyutpal.scrolljourney.data.local.entity.DailyAppUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.DailyUsageEntity
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity
import com.achyutpal.scrolljourney.data.local.entity.UserSettingsEntity

/**
 * The app's SQLite database.
 *
 * Version history (schemas in app/schemas/, never delete them):
 * - v1: selected_apps
 * - v2: + daily_usage, daily_app_usage, user_settings
 *
 * Upgrades use migrations, never a destructive wipe. Room generates "auto migrations"
 * for simple changes like new tables by comparing the two schema JSON files.
 */
@Database(
    entities = [
        SelectedAppEntity::class,
        DailyUsageEntity::class,
        DailyAppUsageEntity::class,
        UserSettingsEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = ScrollJourneyDatabase.Migration1To2::class),
    ],
)
abstract class ScrollJourneyDatabase : RoomDatabase() {
    abstract fun selectedAppDao(): SelectedAppDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun dailyAppUsageDao(): DailyAppUsageDao
    abstract fun userSettingsDao(): UserSettingsDao

    /**
     * v1 users already finished onboarding if they saved at least one app, so create
     * their settings row with onboardingComplete = true instead of sending them back to the intro.
     */
    class Migration1To2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO user_settings
                    (id, dailyLimitMinutes, onboardingComplete, currentFuel, lifetimeFuel, highestReachedPlanet, journeyDistance)
                SELECT 1, 60, EXISTS(SELECT 1 FROM selected_apps WHERE enabled = 1), 0, 0, 'EARTH', 0
                """.trimIndent(),
            )
        }
    }

    companion object {
        const val NAME = "scroll_journey.db"
    }
}
