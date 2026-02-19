package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

/**
 * User-supplied annotations for a holiday (read-only from the API).
 * Keyed by [holidayId], so each calendar year's instance of a holiday is independent.
 * Title and date are immutable (those come from the [Holiday] model).
 */
@Stable
data class HolidayAnnotation(
    val holidayId: String,
    val description: String? = null,
    val location: String? = null,
    /** Null = no reminder. */
    val reminderMinutes: Int? = null,
    val affectedPersonIds: List<String> = emptyList(),
    val updatedAt: Long = 0L,
) {
    fun isEmpty(): Boolean =
        description.isNullOrBlank() &&
            location.isNullOrBlank() &&
            reminderMinutes == null &&
            affectedPersonIds.isEmpty()
}
