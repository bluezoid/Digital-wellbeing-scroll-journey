package com.achyutpal.scrolljourney.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.achyutpal.scrolljourney.data.local.entity.SelectedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): the SQL for one table. Room generates the implementation
 * at compile time and fails the build if a query doesn't match the schema.
 */
@Dao
interface SelectedAppDao {

    @Query("SELECT * FROM selected_apps")
    suspend fun getAll(): List<SelectedAppEntity>

    /** A Flow re-emits automatically whenever the table changes. */
    @Query("SELECT * FROM selected_apps WHERE enabled = 1 ORDER BY appName COLLATE NOCASE")
    fun observeEnabled(): Flow<List<SelectedAppEntity>>

    /** Insert new rows, update existing ones (matched by primary key). */
    @Upsert
    suspend fun upsertAll(apps: List<SelectedAppEntity>)
}
