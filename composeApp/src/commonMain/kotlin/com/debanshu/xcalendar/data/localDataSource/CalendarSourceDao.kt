package com.debanshu.xcalendar.data.localDataSource

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.debanshu.xcalendar.data.localDataSource.model.CalendarSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarSourceDao {
    @Query("SELECT * FROM calendar_sources WHERE calendarId = :calendarId LIMIT 1")
    fun getSourceForCalendar(calendarId: String): Flow<CalendarSourceEntity?>

    @Query("SELECT * FROM calendar_sources WHERE providerAccountId = :accountId")
    suspend fun getSourcesForAccount(accountId: String): List<CalendarSourceEntity>

    @Query("SELECT * FROM calendar_sources")
    suspend fun getAllSources(): List<CalendarSourceEntity>

    @Upsert
    suspend fun upsertSources(sources: List<CalendarSourceEntity>)

    @Query("DELETE FROM calendar_sources WHERE providerAccountId = :accountId")
    suspend fun deleteSourcesForAccount(accountId: String)

    @Query("DELETE FROM calendar_sources WHERE calendarId = :calendarId")
    suspend fun deleteSourceForCalendar(calendarId: String)
}
