package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.auth.GoogleAuthTokens
import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.CalendarProvider
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.ICalendarRepository
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetAllCalendarSourcesUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.test.FakeEventRepository
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class SyncGoogleCalendarsUseCaseTest {

    private class FakeCalendarSourceRepository(
        private val sources: List<CalendarSource>,
    ) : ICalendarSourceRepository {
        override fun getSourceForCalendar(calendarId: String): Flow<CalendarSource?> =
            flowOf(sources.firstOrNull { it.calendarId == calendarId })

        override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> =
            sources.filter { it.providerAccountId == accountId }

        override suspend fun getAllSources(): List<CalendarSource> = sources

        override suspend fun upsertSources(sources: List<CalendarSource>) = Unit

        override suspend fun deleteSourcesForAccount(accountId: String) = Unit

        override suspend fun deleteSourceForCalendar(calendarId: String) = Unit
    }

    private class FakeCalendarRepository(
        private val calendars: List<Calendar>,
    ) : ICalendarRepository {
        override suspend fun refreshCalendarsForUser(userId: String) = Unit

        override fun getCalendarsForUser(userId: String): Flow<List<Calendar>> =
            flowOf(calendars.filter { it.userId == userId })

        override suspend fun upsertCalendar(calendars: List<Calendar>) = Unit

        override suspend fun deleteCalendar(calendar: Calendar) = Unit
    }

    private class FakeGoogleAccountRepository(
        private val account: GoogleAccountLink,
    ) : IGoogleAccountRepository {
        override fun getAccountForPerson(personId: String): Flow<GoogleAccountLink?> =
            flowOf(if (account.personId == personId) account else null)

        override fun getAllAccounts(): Flow<List<GoogleAccountLink>> = flowOf(listOf(account))

        override suspend fun getAccountById(accountId: String): GoogleAccountLink? =
            if (account.id == accountId) account else null

        override suspend fun upsertAccount(account: GoogleAccountLink) = Unit

        override suspend fun deleteAccount(account: GoogleAccountLink) = Unit
    }

    private class FakeCalendarSyncManager(
        private val remoteEvents: List<ExternalEvent>,
    ) : CalendarSyncManager {
        override suspend fun listCalendars(accountId: String): List<ExternalCalendar> = emptyList()

        override suspend fun listEvents(
            accountId: String,
            calendarId: String,
            timeMin: Long,
            timeMax: Long,
        ): List<ExternalEvent> = remoteEvents

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

    private class FakeGoogleTokenStore : GoogleTokenStore {
        private val tokensByAccount = mutableMapOf<String, GoogleAuthTokens>()

        override fun saveTokens(accountId: String, tokens: GoogleAuthTokens) {
            tokensByAccount[accountId] = tokens
        }

        override fun getTokens(accountId: String): GoogleAuthTokens? = tokensByAccount[accountId]

        override fun clearTokens(accountId: String) {
            tokensByAccount.remove(accountId)
        }
    }

    @Test
    fun remoteImport_assignsAffectedPersonAndCategoryPrefix() = runTest {
        val userId = GetCurrentUserUseCase().invoke()
        val source =
            CalendarSource(
                calendarId = "local-cal-1",
                provider = CalendarProvider.GOOGLE,
                providerCalendarId = "google-cal-1",
                providerAccountId = "account-1",
            )
        val calendar =
            Calendar(
                id = "local-cal-1",
                name = "Family",
                color = 0,
                userId = userId,
            )
        val account =
            GoogleAccountLink(
                id = "account-1",
                email = "mom@example.com",
                personId = "person_mom",
            )
        val remoteEvent =
            ExternalEvent(
                id = "remote-1",
                summary = "Preschool pickup",
                description = "Bring snacks",
                startTime = Clock.System.now().toEpochMilliseconds() + 60_000L,
                endTime = Clock.System.now().toEpochMilliseconds() + 3_600_000L,
                isAllDay = false,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        val tokenStore =
            FakeGoogleTokenStore().apply {
                saveTokens(
                    accountId = "account-1",
                    tokens = GoogleAuthTokens(accessToken = "token"),
                )
            }
        val eventRepository = FakeEventRepository()
        val conflictStateHolder = SyncConflictStateHolder()

        val useCase =
            SyncGoogleCalendarsUseCase(
                syncManager = FakeCalendarSyncManager(listOf(remoteEvent)),
                getAllCalendarSourcesUseCase = GetAllCalendarSourcesUseCase(FakeCalendarSourceRepository(listOf(source))),
                getGoogleAccountByIdUseCase = GetGoogleAccountByIdUseCase(FakeGoogleAccountRepository(account)),
                getUserCalendarsUseCase = GetUserCalendarsUseCase(FakeCalendarRepository(listOf(calendar))),
                getCurrentUserUseCase = GetCurrentUserUseCase(),
                eventRepository = eventRepository,
                conflictStateHolder = conflictStateHolder,
                tokenStore = tokenStore,
            )

        val conflicts = useCase(manual = true)

        assertTrue(conflicts.isEmpty())
        assertEquals(1, eventRepository.addedEvents.size)
        val created = eventRepository.addedEvents.single()
        assertEquals(listOf("person_mom"), created.affectedPersonIds)
        assertEquals("Category: school\nBring snacks", created.description)
        assertTrue(conflictStateHolder.conflicts.value.isEmpty())
    }
}
