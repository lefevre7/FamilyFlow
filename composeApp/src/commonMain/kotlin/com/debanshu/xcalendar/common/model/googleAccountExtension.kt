package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.GoogleAccountEntity
import com.debanshu.xcalendar.domain.model.GoogleAccountLink

fun GoogleAccountEntity.asLink(): GoogleAccountLink =
    GoogleAccountLink(
        id = id,
        email = email,
        displayName = displayName,
        personId = personId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun GoogleAccountLink.asEntity(): GoogleAccountEntity =
    GoogleAccountEntity(
        id = id,
        email = email,
        displayName = displayName,
        personId = personId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
