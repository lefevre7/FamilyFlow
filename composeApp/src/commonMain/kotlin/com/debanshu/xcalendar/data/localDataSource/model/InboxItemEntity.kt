package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus

@Entity(
    tableName = "inbox_items",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTaskId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("id", unique = true),
        Index("personId"),
        Index("linkedTaskId"),
        Index("status"),
    ],
)
data class InboxItemEntity(
    @PrimaryKey val id: String,
    val rawText: String,
    val source: InboxSource,
    val status: InboxStatus,
    val createdAt: Long,
    val personId: String?,
    val linkedTaskId: String?,
)
