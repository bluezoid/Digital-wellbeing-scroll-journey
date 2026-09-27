package com.achyutpal.scrolljourney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.achyutpal.scrolljourney.data.local.dao.SelectedAppDao
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity

/**
 * The app's SQLite database. Phase 2 has only selected_apps; Phase 4 adds
 * daily_usage, daily_app_usage and user_settings with a version bump + migration
 * (never a destructive wipe; see spec).
 */
@Database(
    entities = [SelectedAppEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ScrollJourneyDatabase : RoomDatabase() {
    abstract fun selectedAppDao(): SelectedAppDao

    companion object {
        const val NAME = "scroll_journey.db"
    }
}
