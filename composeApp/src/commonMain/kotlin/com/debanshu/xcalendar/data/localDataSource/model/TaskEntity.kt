package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.TaskType

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignedToPersonId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("id", unique = true),
        Index("assignedToPersonId"),
        Index("projectId"),
        Index("routineId"),
        Index("status"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val energy: TaskEnergy,
    val type: TaskType,
    val scheduledStart: Long?,
    val scheduledEnd: Long?,
    val dueAt: Long?,
    val durationMinutes: Int,
    val assignedToPersonId: String?,
    val affectedPersonIds: List<String>,
    val projectId: String?,
    val routineId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
