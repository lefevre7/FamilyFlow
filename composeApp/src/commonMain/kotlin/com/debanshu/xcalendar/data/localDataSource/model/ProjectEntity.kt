package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.debanshu.xcalendar.domain.model.ProjectStatus

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerPersonId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("id", unique = true),
        Index("ownerPersonId"),
        Index("status"),
    ],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val status: ProjectStatus,
    val seasonLabel: String?,
    val startAt: Long?,
    val endAt: Long?,
    val ownerPersonId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
