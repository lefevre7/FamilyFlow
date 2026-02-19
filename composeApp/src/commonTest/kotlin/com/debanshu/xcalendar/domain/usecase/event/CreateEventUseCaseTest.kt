package com.debanshu.xcalendar.domain.usecase.event

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.repository.IGoogleAccountRepository
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.util.DomainError
import com.debanshu.xcalendar.domain.util.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateEventUseCaseTest {

    // Fake repository for testing
    private class FakeEventRepository : IEventRepository {
        val addedEvents = mutableListOf<Event>()
        val updatedEvents = mutableListOf<Event>()
        val deletedEvents = mutableListOf<Event>()
        
        override suspend fun syncEventsForCalendar(
            calendarIds: List<String>,
            startTime: Long,
            endTime: Long
        ) {
            // Not needed for these tests
        }

        override fun getEventsForCalendarsInRange(
            userId: String,
            start: Long,
            end: Long
        ): Flow<List<Event>> = flowOf(addedEvents)

        override suspend fun addEvent(event: Event) {
            addedEvents.add(event)
        }

        override suspend fun updateEvent(event: Event) {
            updatedEvents.add(event)
            val index = addedEvents.indexOfFirst { it.id == event.id }
            if (index >= 0) {
                addedEvents[index] = event
            }
        }

        override suspend fun deleteEvent(event: Event) {
            deletedEvents.add(event)
            addedEvents.removeAll { it.id == event.id }
        }
    }

    private open class FakeCalendarSourceRepository : ICalendarSourceRepository {
        override fun getSourceForCalendar(calendarId: String) = flowOf<CalendarSource?>(null)

        override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> = emptyList()

        override suspend fun getAllSources(): List<CalendarSource> = emptyList()

        override suspend fun upsertSources(sources: List<CalendarSource>) = Unit

        override suspend fun deleteSourcesForAccount(accountId: String) = Unit

        override suspend fun deleteSourceForCalendar(calendarId: String) = Unit
    }

    private class FakeCalendarSyncManager(
        var shouldThrow: Boolean = false,
        var returnedEvent: com.debanshu.xcalendar.domain.model.ExternalEvent? = null,
    ) : CalendarSyncManager {
        override suspend fun listCalendars(accountId: String) = emptyList<com.debanshu.xcalendar.domain.model.ExternalCalendar>()

        override suspend fun listEvents(
            accountId: String,
            calendarId: String,
            timeMin: Long,
            timeMax: Long,
        ) = emptyList<com.debanshu.xcalendar.domain.model.ExternalEvent>()

        override suspend fun createEvent(
            accountId: String,
            calendarId: String,
            event: com.debanshu.xcalendar.domain.model.ExternalEvent,
        ): com.debanshu.xcalendar.domain.model.ExternalEvent? {
            if (shouldThrow) throw RuntimeException("Simulated network error")
            return returnedEvent
        }

        override suspend fun updateEvent(
            accountId: String,
            calendarId: String,
            eventId: String,
            event: com.debanshu.xcalendar.domain.model.ExternalEvent,
        ) = null

        override suspend fun deleteEvent(
            accountId: String,
            calendarId: String,
            eventId: String,
        ) = false
    }

    private class FakeReminderPreferencesRepository : IReminderPreferencesRepository {
        override val preferences = flowOf(ReminderPreferences())

        override suspend fun setRemindersEnabled(enabled: Boolean) = Unit
        override suspend fun setPrepMinutes(minutes: Int) = Unit
        override suspend fun setTravelBufferMinutes(minutes: Int) = Unit
        override suspend fun setAllDayTime(hour: Int, minute: Int) = Unit
        override suspend fun setSummaryEnabled(enabled: Boolean) = Unit
        override suspend fun setSummaryTimes(
            morningHour: Int,
            morningMinute: Int,
            middayHour: Int,
            middayMinute: Int,
        ) = Unit
    }

    private class FakeReminderScheduler : ReminderScheduler {
        override suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences) = Unit
        override suspend fun cancelEvent(eventId: String) = Unit
        override suspend fun scheduleTask(task: com.debanshu.xcalendar.domain.model.Task, preferences: ReminderPreferences) = Unit
        override suspend fun cancelTask(taskId: String) = Unit
        override suspend fun scheduleSummaries(preferences: ReminderPreferences) = Unit
        override suspend fun cancelSummaries() = Unit
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        override suspend fun refreshTodayWidget() = Unit
    }

    /** Returns no Google accounts by default — simulates offline / no-Google state. */
    private class FakeGoogleAccountRepository(
        private val accounts: List<GoogleAccountLink> = emptyList(),
    ) : IGoogleAccountRepository {
        override fun getAccountForPerson(personId: String): Flow<GoogleAccountLink?> = flowOf(null)
        override fun getAllAccounts(): Flow<List<GoogleAccountLink>> = flowOf(accounts)
        override suspend fun getAccountById(accountId: String): GoogleAccountLink? = null
        override suspend fun upsertAccount(account: GoogleAccountLink) = Unit
        override suspend fun deleteAccount(account: GoogleAccountLink) = Unit
    }

    private fun makeCreateUseCase(
        repo: FakeEventRepository = FakeEventRepository(),
        sourceRepo: FakeCalendarSourceRepository = FakeCalendarSourceRepository(),
        sync: FakeCalendarSyncManager = FakeCalendarSyncManager(),
        googleAccounts: List<GoogleAccountLink> = emptyList(),
    ): CreateEventUseCase = CreateEventUseCase(
        eventRepository = repo,
        getCalendarSourceUseCase = GetCalendarSourceUseCase(sourceRepo),
        syncManager = sync,
        getReminderPreferencesUseCase = GetReminderPreferencesUseCase(reminderPreferencesRepository),
        reminderScheduler = reminderScheduler,
        widgetUpdater = widgetUpdater,
        getAllGoogleAccountsUseCase = GetAllGoogleAccountsUseCase(FakeGoogleAccountRepository(googleAccounts)),
    )

    private val calendarSourceRepository = FakeCalendarSourceRepository()
    private val getCalendarSourceUseCase = GetCalendarSourceUseCase(calendarSourceRepository)
    private val calendarSyncManager = FakeCalendarSyncManager()
    private val reminderPreferencesRepository = FakeReminderPreferencesRepository()
    private val getReminderPreferencesUseCase = GetReminderPreferencesUseCase(reminderPreferencesRepository)
    private val reminderScheduler = FakeReminderScheduler()
    private val widgetUpdater = FakeWidgetUpdater()
    // Default: no Google accounts (LOCAL source expected)
    private val noGoogleAccounts: List<GoogleAccountLink> = emptyList()
    private val oneGoogleAccount = listOf(
        GoogleAccountLink(id = "gaccount-1", email = "mom@gmail.com", personId = "person-mom")
    )

    private fun createTestEvent(
        id: String = "test-id-123",
        title: String = "Test Event",
        calendarId: String = "calendar-1",
        startTime: Long = 1704067200000L, // 2024-01-01 00:00:00 UTC
        endTime: Long = 1704070800000L,   // 2024-01-01 01:00:00 UTC
        isAllDay: Boolean = false
    ) = Event(
        id = id,
        calendarId = calendarId,
        calendarName = "Test Calendar",
        title = title,
        description = "Test Description",
        location = null,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = false,
        recurringRule = null,
        reminderMinutes = listOf(15),
        color = 0xFF2196F3.toInt()
    )

    // ==================== CreateEventUseCase Tests ====================

    @Test
    fun `CreateEventUseCase adds valid event to repository`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val testEvent = createTestEvent()

        // When
        val result = useCase(testEvent)

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.addedEvents.size)
        assertEquals(testEvent.id, fakeRepository.addedEvents.first().id)
        assertEquals(testEvent.title, fakeRepository.addedEvents.first().title)
    }

    @Test
    fun `CreateEventUseCase saves event as LOCAL when no Google account connected`() = runTest {
        // Given — 1C: no Google account → LOCAL source
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository, googleAccounts = noGoogleAccounts)

        // When
        val result = useCase(createTestEvent())

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(EventSource.LOCAL, fakeRepository.addedEvents.first().source)
    }

    @Test
    fun `CreateEventUseCase saves event as GOOGLE when Google account is connected`() = runTest {
        // Given — 1C: Google account present → GOOGLE source even without CalendarSource
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository, googleAccounts = oneGoogleAccount)

        // When
        val result = useCase(createTestEvent())

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(EventSource.GOOGLE, fakeRepository.addedEvents.first().source)
    }

    @Test
    fun `CreateEventUseCase saves locally first before attempting Google sync (2A)`() = runTest {
        // Given — sync throws; event must still be in repo
        val fakeRepository = FakeEventRepository()
        val throwingSync = FakeCalendarSyncManager(shouldThrow = true)
        val calSourceRepo = object : FakeCalendarSourceRepository() {
            override fun getSourceForCalendar(calendarId: String) = flowOf(
                CalendarSource(
                    calendarId = calendarId,
                    provider = com.debanshu.xcalendar.domain.model.CalendarProvider.GOOGLE,
                    providerCalendarId = "google-cal-id",
                    providerAccountId = "gaccount-1",
                    syncEnabled = true,
                    lastSyncedAt = 0L,
                )
            )
        }
        val useCase = makeCreateUseCase(
            repo = fakeRepository,
            sourceRepo = calSourceRepo,
            sync = throwingSync,
            googleAccounts = oneGoogleAccount,
        )

        // When
        val result = useCase(createTestEvent())

        // Then: success because local save already happened before sync attempted
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.addedEvents.size)
        // Event persisted locally with lastSyncedAt = 0L (pending retry)
        assertEquals(0L, fakeRepository.addedEvents.first().lastSyncedAt)
    }

    @Test
    fun `CreateEventUseCase updates event with externalId after successful Google sync`() = runTest {
        // Given — CalendarSource exists AND sync succeeds
        val fakeRepository = FakeEventRepository()
        val syncedExternal = com.debanshu.xcalendar.domain.model.ExternalEvent(
            id = "remote-evt-abc",
            summary = "Test Event",
            description = null,
            location = null,
            startTime = 1704067200000L,
            endTime = 1704070800000L,
            isAllDay = false,
            updatedAt = 1704067200000L,
        )
        val successfulSync = FakeCalendarSyncManager(returnedEvent = syncedExternal)
        val calSourceRepo = object : FakeCalendarSourceRepository() {
            override fun getSourceForCalendar(calendarId: String) = flowOf(
                CalendarSource(
                    calendarId = calendarId,
                    provider = com.debanshu.xcalendar.domain.model.CalendarProvider.GOOGLE,
                    providerCalendarId = "google-cal-id",
                    providerAccountId = "gaccount-1",
                    syncEnabled = true,
                    lastSyncedAt = 0L,
                )
            )
        }
        val useCase = makeCreateUseCase(
            repo = fakeRepository,
            sourceRepo = calSourceRepo,
            sync = successfulSync,
            googleAccounts = oneGoogleAccount,
        )

        // When
        val result = useCase(createTestEvent())

        // Then: addEvent + updateEvent both called; updatedEvent has externalId
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.addedEvents.size)
        assertEquals(1, fakeRepository.updatedEvents.size)
        assertEquals("remote-evt-abc", fakeRepository.updatedEvents.first().externalId)
    }

    @Test
    fun `CreateEventUseCase returns error for blank title`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val invalidEvent = createTestEvent(title = "   ")

        // When
        val result = useCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
        assertTrue(fakeRepository.addedEvents.isEmpty())
    }

    @Test
    fun `CreateEventUseCase returns error for empty title`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val invalidEvent = createTestEvent(title = "")

        // When
        val result = useCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
    }

    @Test
    fun `CreateEventUseCase returns error when end time before start time`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val invalidEvent = createTestEvent(
            startTime = 1704070800000L,  // Later time
            endTime = 1704067200000L,    // Earlier time
        )

        // When
        val result = useCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
    }

    @Test
    fun `CreateEventUseCase returns error for blank calendar ID`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val invalidEvent = createTestEvent(calendarId = "")

        // When
        val result = useCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
    }

    @Test
    fun `CreateEventUseCase allows all-day event with same start and end`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val useCase = makeCreateUseCase(repo = fakeRepository)
        val allDayEvent = createTestEvent(
            startTime = 1704067200000L,
            endTime = 1704067200000L,
            isAllDay = true,
        )

        // When
        val result = useCase(allDayEvent)

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.addedEvents.size)
    }

    // ==================== UpdateEventUseCase Tests ====================

    @Test
    fun `UpdateEventUseCase updates valid event in repository`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val createUseCase = makeCreateUseCase(repo = fakeRepository)
        val updateUseCase =
            UpdateEventUseCase(
                fakeRepository,
                getCalendarSourceUseCase,
                calendarSyncManager,
                getReminderPreferencesUseCase,
                reminderScheduler,
                widgetUpdater,
            )

        val originalEvent = createTestEvent(id = "event-1", title = "Original Title")
        createUseCase(originalEvent)

        val updatedEvent = fakeRepository.addedEvents.first().copy(title = "Updated Title")

        // When
        val result = updateUseCase(updatedEvent)

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.updatedEvents.size)
        assertEquals("Updated Title", fakeRepository.updatedEvents.first().title)
    }

    @Test
    fun `UpdateEventUseCase returns error for blank event ID`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val updateUseCase =
            UpdateEventUseCase(
                fakeRepository,
                getCalendarSourceUseCase,
                calendarSyncManager,
                getReminderPreferencesUseCase,
                reminderScheduler,
                widgetUpdater,
            )
        val invalidEvent = createTestEvent(id = "")

        // When
        val result = updateUseCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
    }

    @Test
    fun `UpdateEventUseCase returns error for invalid event data`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val updateUseCase =
            UpdateEventUseCase(
                fakeRepository,
                getCalendarSourceUseCase,
                calendarSyncManager,
                getReminderPreferencesUseCase,
                reminderScheduler,
                widgetUpdater,
            )
        val invalidEvent = createTestEvent(title = "")

        // When
        val result = updateUseCase(invalidEvent)

        // Then
        assertIs<DomainResult.Error>(result)
        assertIs<DomainError.ValidationError>(result.error)
    }

    // ==================== DeleteEventUseCase Tests ====================

    @Test
    fun `DeleteEventUseCase removes event from repository`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val createUseCase = makeCreateUseCase(repo = fakeRepository)
        val deleteUseCase =
            DeleteEventUseCase(
                fakeRepository,
                getCalendarSourceUseCase,
                calendarSyncManager,
                reminderScheduler,
                widgetUpdater,
            )

        val testEvent = createTestEvent()
        createUseCase(testEvent)
        assertEquals(1, fakeRepository.addedEvents.size)

        // When
        val result = deleteUseCase(fakeRepository.addedEvents.first())

        // Then
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(1, fakeRepository.deletedEvents.size)
        assertTrue(fakeRepository.addedEvents.isEmpty())
    }

    // ==================== GetEventsForDateRangeUseCase Tests ====================

    @Test
    fun `GetEventsForDateRangeUseCase returns events from repository`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val createUseCase = makeCreateUseCase(repo = fakeRepository)
        val getEventsUseCase = GetEventsForDateRangeUseCase(fakeRepository)

        val testEvent = createTestEvent()
        createUseCase(testEvent)

        // When
        val eventsFlow = getEventsUseCase("user_id", 0L, Long.MAX_VALUE)

        // Then
        eventsFlow.collect { events ->
            assertEquals(1, events.size)
            assertEquals(testEvent.id, events.first().id)        }
    }
}