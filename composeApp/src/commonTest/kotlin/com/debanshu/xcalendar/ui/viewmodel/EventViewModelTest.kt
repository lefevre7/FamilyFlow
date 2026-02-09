package com.debanshu.xcalendar.ui.viewmodel

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.CalendarSource
import com.debanshu.xcalendar.domain.repository.ICalendarSourceRepository
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.usecase.event.CreateEventUseCase
import com.debanshu.xcalendar.domain.usecase.event.DeleteEventUseCase
import com.debanshu.xcalendar.domain.usecase.event.UpdateEventUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake repository for testing
    private class FakeEventRepository(
        private val shouldFail: Boolean = false
    ) : IEventRepository {
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
            if (shouldFail) throw RuntimeException("Test error")
            addedEvents.add(event)
        }

        override suspend fun updateEvent(event: Event) {
            if (shouldFail) throw RuntimeException("Test error")
            updatedEvents.add(event)
        }

        override suspend fun deleteEvent(event: Event) {
            if (shouldFail) throw RuntimeException("Test error")
            deletedEvents.add(event)
        }
    }

    private class FakeCalendarSourceRepository : ICalendarSourceRepository {
        override fun getSourceForCalendar(calendarId: String) = flowOf<CalendarSource?>(null)

        override suspend fun getSourcesForAccount(accountId: String): List<CalendarSource> = emptyList()

        override suspend fun getAllSources(): List<CalendarSource> = emptyList()

        override suspend fun upsertSources(sources: List<CalendarSource>) = Unit

        override suspend fun deleteSourcesForAccount(accountId: String) = Unit

        override suspend fun deleteSourceForCalendar(calendarId: String) = Unit
    }

    private class FakeCalendarSyncManager : CalendarSyncManager {
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
        ) = null

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

    private val calendarSourceRepository = FakeCalendarSourceRepository()
    private val getCalendarSourceUseCase = GetCalendarSourceUseCase(calendarSourceRepository)
    private val calendarSyncManager = FakeCalendarSyncManager()
    private val reminderPreferencesRepository = FakeReminderPreferencesRepository()
    private val getReminderPreferencesUseCase = GetReminderPreferencesUseCase(reminderPreferencesRepository)
    private val reminderScheduler = FakeReminderScheduler()
    private val widgetUpdater = FakeWidgetUpdater()

    private fun createTestEvent(
        id: String = "test-id-123",
        title: String = "Test Event"
    ) = Event(
        id = id,
        calendarId = "calendar-1",
        calendarName = "Test Calendar",
        title = title,
        description = null,
        location = null,
        startTime = 1704067200000L,
        endTime = 1704070800000L,
        isAllDay = false,
        isRecurring = false,
        recurringRule = null,
        reminderMinutes = emptyList(),
        color = 0xFF2196F3.toInt()
    )

    private fun createViewModel(fakeRepository: FakeEventRepository = FakeEventRepository()): EventViewModel {
        return EventViewModel(
            createEventUseCase =
                CreateEventUseCase(
                    fakeRepository,
                    getCalendarSourceUseCase,
                    calendarSyncManager,
                    getReminderPreferencesUseCase,
                    reminderScheduler,
                    widgetUpdater,
                ),
            updateEventUseCase =
                UpdateEventUseCase(
                    fakeRepository,
                    getCalendarSourceUseCase,
                    calendarSyncManager,
                    getReminderPreferencesUseCase,
                    reminderScheduler,
                    widgetUpdater,
                ),
            deleteEventUseCase =
                DeleteEventUseCase(
                    fakeRepository,
                    getCalendarSourceUseCase,
                    calendarSyncManager,
                    reminderScheduler,
                    widgetUpdater,
                )
        )
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectEvent updates selectedEvent in state`() = runTest {
        // Given
        val viewModel = createViewModel()
        val testEvent = createTestEvent()

        // When
        viewModel.selectEvent(testEvent)
        advanceUntilIdle()

        // Then
        assertEquals(testEvent, viewModel.uiState.value.selectedEvent)
    }

    @Test
    fun `clearSelectedEvent clears selectedEvent`() = runTest {
        // Given
        val viewModel = createViewModel()
        val testEvent = createTestEvent()
        viewModel.selectEvent(testEvent)
        advanceUntilIdle()

        // When
        viewModel.clearSelectedEvent()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.selectedEvent)
    }

    @Test
    fun `addEvent success clears error message`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val viewModel = createViewModel(fakeRepository)
        val testEvent = createTestEvent()

        // When
        viewModel.addEvent(testEvent)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, fakeRepository.addedEvents.size)
    }

    @Test
    fun `addEvent with invalid title sets error message`() = runTest {
        // Given
        val viewModel = createViewModel()
        val invalidEvent = createTestEvent(title = "") // Empty title should fail validation

        // When
        viewModel.addEvent(invalidEvent)
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `editEvent success clears selectedEvent and error`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val viewModel = createViewModel(fakeRepository)
        val testEvent = createTestEvent()
        viewModel.selectEvent(testEvent)
        advanceUntilIdle()

        // When
        viewModel.editEvent(testEvent)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.selectedEvent)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, fakeRepository.updatedEvents.size)
    }

    @Test
    fun `deleteEvent success clears selectedEvent and error`() = runTest {
        // Given
        val fakeRepository = FakeEventRepository()
        val viewModel = createViewModel(fakeRepository)
        val testEvent = createTestEvent()
        viewModel.selectEvent(testEvent)
        advanceUntilIdle()

        // When
        viewModel.deleteEvent(testEvent)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.selectedEvent)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, fakeRepository.deletedEvents.size)
    }

    @Test
    fun `clearError clears error message`() = runTest {
        // Given
        val viewModel = createViewModel()
        val invalidEvent = createTestEvent(title = "") // Will cause validation error
        viewModel.addEvent(invalidEvent)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        // When
        viewModel.clearError()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
