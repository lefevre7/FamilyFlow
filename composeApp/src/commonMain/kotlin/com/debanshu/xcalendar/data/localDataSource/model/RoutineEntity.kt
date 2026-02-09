package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.debanshu.xcalendar.domain.model.RoutineTimeOfDay

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignedToPersonId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("id", unique = true),
        Index("assignedToPersonId"),
        Index("isActive"),
    ],
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val timeOfDay: RoutineTimeOfDay,
    val recurrenceRule: String?,
    val assignedToPersonId: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
