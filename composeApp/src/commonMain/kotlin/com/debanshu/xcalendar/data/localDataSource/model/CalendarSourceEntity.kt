package com.debanshu.xcalendar.data.localDataSource.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_sources",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("calendarId", unique = true),
        Index("providerAccountId")
    ]
)
data class CalendarSourceEntity(
    @PrimaryKey val calendarId: String,
    val provider: String,
    val providerCalendarId: String,
    val providerAccountId: String,
    val syncEnabled: Boolean,
    val lastSyncedAt: Long?,
)
