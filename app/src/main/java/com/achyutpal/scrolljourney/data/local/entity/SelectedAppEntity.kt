package com.achyutpal.scrolljourney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.achyutpal.scrolljourney.domain.model.SelectedApp

/**
 * Room entity = one database table. Each property becomes a column.
 * packageName is the primary key, so saving the same app twice updates the row instead of duplicating it.
 */
@Entity(tableName = "selected_apps")
data class SelectedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val enabled: Boolean,
    val addedAt: Long,
)

fun SelectedAppEntity.toDomain() = SelectedApp(
    packageName = packageName,
    appName = appName,
    enabled = enabled,
    addedAt = addedAt,
)

fun SelectedApp.toEntity() = SelectedAppEntity(
    packageName = packageName,
    appName = appName,
    enabled = enabled,
    addedAt = addedAt,
)
