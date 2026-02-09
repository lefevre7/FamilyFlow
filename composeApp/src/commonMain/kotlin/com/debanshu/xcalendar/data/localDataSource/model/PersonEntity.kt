package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.debanshu.xcalendar.domain.model.PersonRole

@Entity(
    tableName = "people",
    indices = [
        Index("id", unique = true),
        Index("role"),
        Index("isDefault"),
        Index("isArchived"),
    ],
)
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: PersonRole,
    val ageYears: Int?,
    val color: Int,
    val avatarUrl: String,
    val isAdmin: Boolean,
    val isDefault: Boolean,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
