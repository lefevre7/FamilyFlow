package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "google_accounts",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("id", unique = true),
        Index("personId")
    ]
)
data class GoogleAccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?,
    val personId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
