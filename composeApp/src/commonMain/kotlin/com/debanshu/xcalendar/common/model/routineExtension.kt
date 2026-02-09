package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.RoutineEntity
import com.debanshu.xcalendar.domain.model.Routine

fun RoutineEntity.asRoutine(): Routine =
    Routine(
        id = id,
        title = title,
        notes = notes,
        timeOfDay = timeOfDay,
        recurrenceRule = recurrenceRule,
        assignedToPersonId = assignedToPersonId,
        isActive = isActive,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Routine.asRoutineEntity(): RoutineEntity =
    RoutineEntity(
        id = id,
        title = title,
        notes = notes,
        timeOfDay = timeOfDay,
        recurrenceRule = recurrenceRule,
        assignedToPersonId = assignedToPersonId,
        isActive = isActive,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
