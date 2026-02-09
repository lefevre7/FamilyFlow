package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.ProjectEntity
import com.debanshu.xcalendar.domain.model.Project

fun ProjectEntity.asProject(): Project =
    Project(
        id = id,
        title = title,
        notes = notes,
        status = status,
        seasonLabel = seasonLabel,
        startAt = startAt,
        endAt = endAt,
        ownerPersonId = ownerPersonId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Project.asProjectEntity(): ProjectEntity =
    ProjectEntity(
        id = id,
        title = title,
        notes = notes,
        status = status,
        seasonLabel = seasonLabel,
        startAt = startAt,
        endAt = endAt,
        ownerPersonId = ownerPersonId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
