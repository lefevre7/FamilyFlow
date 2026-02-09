package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class ScheduleItem(
    val id: String,
    val title: String,
    val startTime: Long?,
    val endTime: Long?,
    val isAllDay: Boolean,
    val priority: TaskPriority?,
    val energy: TaskEnergy?,
    val personIds: List<String>,
    val source: ScheduleSource,
    val isFlexible: Boolean,
    val originalEvent: Event? = null,
    val originalTask: Task? = null,
)

enum class ScheduleSource {
    EVENT,
    TASK,
}

@Stable
data class ScheduleFilter(
    val personId: String? = null,
    val onlyMust: Boolean = false,
    val includeUnassignedEvents: Boolean = true,
    val nowWindowMinutes: Int? = null,
)

@Stable
data class ScheduleConflict(
    val itemIdA: String,
    val itemIdB: String,
)

@Stable
data class ScheduleSuggestion(
    val taskId: String,
    val startTime: Long,
    val endTime: Long,
)

@Stable
data class ScheduleAggregationResult(
    val items: List<ScheduleItem>,
    val conflicts: List<ScheduleConflict>,
    val suggestions: List<ScheduleSuggestion>,
)
