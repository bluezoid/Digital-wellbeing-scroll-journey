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
 * - v3: + daily_usage.limitMinutes (the limit each day's colour was judged against)
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
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = ScrollJourneyDatabase.Migration1To2::class),
        AutoMigration(from = 2, to = 3, spec = ScrollJourneyDatabase.Migration2To3::class),
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

    /**
     * Room adds the new nullable column itself. Before v3 the only way a day got a status was the
     * first Phase-5 sync, which always used the default 60-minute limit (the limit screen shipped
     * in that same release), so already-coloured finalized days are back-filled with 60.
     * Unevaluated days stay NULL; today's live row is rewritten on the next sync anyway.
     */
    class Migration2To3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE daily_usage SET limitMinutes = 60 WHERE status IS NOT NULL AND finalized = 1")
        }
    }

    companion object {
        const val NAME = "scroll_journey.db"
    }
}
