package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user-supplied annotations for a holiday.
 * [affectedPersonIds] is JSON-encoded (same pattern as other entities in this package).
 */
@Entity(tableName = "holiday_annotations")
data class HolidayAnnotationEntity(
    @PrimaryKey val holidayId: String,
    val description: String?,
    val location: String?,
    /** Null means no reminder is set. */
    val reminderMinutes: Int?,
    val affectedPersonIds: String, // JSON-encoded List<String>
    val updatedAt: Long,
)
