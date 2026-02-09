package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.CalendarProvider
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.sync.SyncConflict
import com.debanshu.xcalendar.domain.sync.SyncResolutionAction
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.test.FakeEventRepository
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolveSyncConflictUseCaseTest {

    private class FakeCalendarSourceRepository(
        private val sourceByCalendar: Map<String, CalendarSource>,
    ) : ICalendarSourceRepository {
        override fun getSourceForCalendar(calendarId: String): Flow<CalendarSource?> =
            flowOf(sourceByCalendar[calendarId])

        override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> =
            sourceByCalendar.values.filter { it.providerAccountId == accountId }

        override suspend fun getAllSources(): List<CalendarSource> =
            sourceByCalendar.values.toList()

        override suspend fun upsertSources(sources: List<CalendarSource>) = Unit

        override suspend fun deleteSourcesForAccount(accountId: String) = Unit

        override suspend fun deleteSourceForCalendar(calendarId: String) = Unit
    }

    private class FakeCalendarSyncManager : CalendarSyncManager {
        override suspend fun listCalendars(accountId: String): List<ExternalCalendar> = emptyList()

        override suspend fun listEvents(
            accountId: String,
            calendarId: String,
            timeMin: Long,
            timeMax: Long,
        ): List<ExternalEvent> = emptyList()

        override suspend fun createEvent(
            accountId: String,
            calendarId: String,
            event: ExternalEvent,
        ): ExternalEvent? = event

        override suspend fun updateEvent(
            accountId: String,
            calendarId: String,
            eventId: String,
            event: ExternalEvent,
        ): ExternalEvent? = event

        override suspend fun deleteEvent(
            accountId: String,
            calendarId: String,
            eventId: String,
        ): Boolean = true
    }

    @Test
    fun duplicateResolution_keepsAffectedPeopleMapping() = runTest {
        val localEvent =
            Event(
                id = "local-1",
                calendarId = "cal-1",
                calendarName = "Family",
                title = "Pickup",
                startTime = 1_000L,
                endTime = 2_000L,
                color = 0xFF0000,
                affectedPersonIds = listOf("person_mom", "person_kid_4"),
            )
        val remoteEvent =
            ExternalEvent(
                id = "remote-1",
                summary = "Pickup from Google",
                startTime = 1_500L,
                endTime = 2_500L,
                isAllDay = false,
                updatedAt = 10_000L,
            )
        val conflict =
            SyncConflict(
                calendarId = "cal-1",
                localEvent = localEvent,
                remoteEvent = remoteEvent,
            )
        val source =
            CalendarSource(
                calendarId = "cal-1",
                provider = CalendarProvider.GOOGLE,
                providerCalendarId = "google-cal-1",
                providerAccountId = "acct-1",
            )
        val sourceRepository = FakeCalendarSourceRepository(mapOf("cal-1" to source))
        val eventRepository = FakeEventRepository()
        val conflictStateHolder = SyncConflictStateHolder()
        conflictStateHolder.setConflicts(listOf(conflict))

        val useCase =
            ResolveSyncConflictUseCase(
                syncManager = FakeCalendarSyncManager(),
                getCalendarSourceUseCase = GetCalendarSourceUseCase(sourceRepository),
                eventRepository = eventRepository,
                conflictStateHolder = conflictStateHolder,
            )

        useCase(conflict, SyncResolutionAction.DUPLICATE)

        val duplicate = eventRepository.addedEvents.single()
        assertEquals(localEvent.affectedPersonIds, duplicate.affectedPersonIds)
        assertTrue(conflictStateHolder.conflicts.value.isEmpty())
    }
}
