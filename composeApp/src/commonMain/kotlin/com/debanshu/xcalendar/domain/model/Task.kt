package com.debanshu.xcalendar.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Task(
    val id: String,
    val title: String,
    val notes: String? = null,
    val status: TaskStatus = TaskStatus.OPEN,
    val priority: TaskPriority = TaskPriority.SHOULD,
    val energy: TaskEnergy = TaskEnergy.MEDIUM,
    val type: TaskType = TaskType.FLEXIBLE,
    val scheduledStart: Long? = null,
    val scheduledEnd: Long? = null,
    val dueAt: Long? = null,
    val durationMinutes: Int = 30,
    val assignedToPersonId: String? = null,
    val affectedPersonIds: List<String> = emptyList(),
    val projectId: String? = null,
    val routineId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class TaskStatus {
    OPEN,
    DONE,
    ARCHIVED,
}

enum class TaskPriority {
    MUST,
    SHOULD,
    NICE,
}

enum class TaskEnergy {
    LOW,
    MEDIUM,
    HIGH,
}

enum class TaskType {
    FLEXIBLE,
    ROUTINE,
    PROJECT,
}
