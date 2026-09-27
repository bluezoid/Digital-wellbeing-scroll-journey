package com.achyutpal.scrolljourney.data.local.entity

import androidx.room.Entity

/**
 * Minutes per app per day. The primary key is the pair (date, packageName), so the
 * database itself guarantees one row per app per day: saving twice can't duplicate.
 */
@Entity(
    tableName = "daily_app_usage",
    primaryKeys = ["date", "packageName"],
)
data class DailyAppUsageEntity(
    val date: String,
    val packageName: String,
    val minutes: Int,
)
