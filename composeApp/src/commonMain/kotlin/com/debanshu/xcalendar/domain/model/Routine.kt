package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Routine(
    val id: String,
    val title: String,
    val notes: String? = null,
    val timeOfDay: RoutineTimeOfDay = RoutineTimeOfDay.MORNING,
    val recurrenceRule: String? = null,
    val assignedToPersonId: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class RoutineTimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    ANYTIME,
}
