package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.PersonEntity
import com.debanshu.xcalendar.domain.model.Person

fun PersonEntity.asPerson(): Person =
    Person(
        id = id,
        name = name,
        role = role,
        ageYears = ageYears,
        color = color,
        avatarUrl = avatarUrl,
        isAdmin = isAdmin,
        isDefault = isDefault,
        sortOrder = sortOrder,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Person.asPersonEntity(): PersonEntity =
    PersonEntity(
        id = id,
        name = name,
        role = role,
        ageYears = ageYears,
        color = color,
        avatarUrl = avatarUrl,
        isAdmin = isAdmin,
        isDefault = isDefault,
        sortOrder = sortOrder,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
