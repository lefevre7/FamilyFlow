package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.TaskEntity
import com.debanshu.xcalendar.domain.model.Task

fun TaskEntity.asTask(): Task =
    Task(
        id = id,
        title = title,
        notes = notes,
        status = status,
        priority = priority,
        energy = energy,
        type = type,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        dueAt = dueAt,
        durationMinutes = durationMinutes,
        assignedToPersonId = assignedToPersonId,
        affectedPersonIds = affectedPersonIds,
        projectId = projectId,
        routineId = routineId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Task.asTaskEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        notes = notes,
        status = status,
        priority = priority,
        energy = energy,
        type = type,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        dueAt = dueAt,
        durationMinutes = durationMinutes,
        assignedToPersonId = assignedToPersonId,
        affectedPersonIds = affectedPersonIds,
        projectId = projectId,
        routineId = routineId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
