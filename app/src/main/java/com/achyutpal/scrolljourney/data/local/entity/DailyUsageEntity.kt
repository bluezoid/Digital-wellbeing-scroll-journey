package com.achyutpal.scrolljourney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.achyutpal.scrolljourney.domain.model.DailyRecord
import com.achyutpal.scrolljourney.domain.model.DayStatus
import java.time.LocalDate

/** One row per calendar day. date is "YYYY-MM-DD" (sorts correctly as text). Enums are stored by name. */
@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey val date: String,
    val totalMinutes: Int,
    val status: DayStatus?,
    /** Added in DB v3. Null for days that were never evaluated. */
    val limitMinutes: Int?,
    val fuelDelta: Int,
    val distance: Long,
    val finalized: Boolean,
)

fun DailyUsageEntity.toDomain() = DailyRecord(
    date = LocalDate.parse(date),
    totalMinutes = totalMinutes,
    status = status,
    limitMinutes = limitMinutes,
    fuelDelta = fuelDelta,
    distance = distance,
    finalized = finalized,
)

fun DailyRecord.toEntity() = DailyUsageEntity(
    date = date.toString(),
    totalMinutes = totalMinutes,
    status = status,
    limitMinutes = limitMinutes,
    fuelDelta = fuelDelta,
    distance = distance,
    finalized = finalized,
)
