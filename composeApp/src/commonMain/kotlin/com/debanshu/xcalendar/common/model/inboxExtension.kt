package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.InboxItemEntity
import com.debanshu.xcalendar.domain.model.InboxItem

fun InboxItemEntity.asInboxItem(): InboxItem =
    InboxItem(
        id = id,
        rawText = rawText,
        source = source,
        status = status,
        createdAt = createdAt,
        personId = personId,
        linkedTaskId = linkedTaskId,
    )

fun InboxItem.asInboxItemEntity(): InboxItemEntity =
    InboxItemEntity(
        id = id,
        rawText = rawText,
        source = source,
        status = status,
        createdAt = createdAt,
        personId = personId,
        linkedTaskId = linkedTaskId,
    )
