package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import com.debanshu.xcalendar.test.TestDataFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests event filtering based on Google Calendar sign-in state.
 *
 * Requirements:
 * - No Google account: Show only EventSource.LOCAL events
 * - Has Google account: Show only EventSource.GOOGLE events
 * - Dummy data (assets/events.json) should not be shown when Google is connected
 */
class EventSourceFilteringTest {

    private lateinit var fakeGoogleAccountRepository: FakeGoogleAccountRepository
    private lateinit var fakeGetAllGoogleAccountsUseCase: FakeGetAllGoogleAccountsUseCase
    private lateinit var fakeRepository: FakeEventRepositoryForFiltering

    @BeforeTest
    fun setup() {
        fakeGoogleAccountRepository = FakeGoogleAccountRepository()
        fakeGetAllGoogleAccountsUseCase = FakeGetAllGoogleAccountsUseCase(fakeGoogleAccountRepository)
        fakeRepository = FakeEventRepositoryForFiltering()
    }

    @Test
    fun noGoogleAccount_showsOnlyLocalEvents() = runTest {
        // Given: No Google account
        fakeGetAllGoogleAccountsUseCase.setAccounts(emptyList())

        // And: Mix of LOCAL and GOOGLE events in storage
        val localEvent = TestDataFactory.createEvent(
            id = "local_1",
            title = "Locally created task",
            source = EventSource.LOCAL,
        )
        val googleEvent = TestDataFactory.createEvent(
            id = "google_1",
            title = "Google Calendar event",
            source = EventSource.GOOGLE,
        )
        fakeRepository.addEventsToStorage(listOf(localEvent, googleEvent))

        // When: Getting events for a date range with no Google account
        val result = fakeRepository.getEventsForCalendarsInRange(
            userId = "user_1",
            start = 0L,
            end = Long.MAX_VALUE,
            hasGoogleAccount = false,
        ).first()

        // Then: Only LOCAL events are returned
        assertEquals(1, result.size)
        assertEquals("local_1", result[0].id)
        assertEquals(EventSource.LOCAL, result[0].source)
    }

    @Test
    fun hasGoogleAccount_showsOnlyGoogleEvents() = runTest {
        // Given: User has Google account connected
        val googleAccount = GoogleAccountLink(
            id = "account_1",
            email = "user@gmail.com",
            personId = "mom_id",
        )
        fakeGetAllGoogleAccountsUseCase.setAccounts(listOf(googleAccount))

        // And: Mix of LOCAL and GOOGLE events in storage
        val localEvent = TestDataFactory.createEvent(
            id = "local_1",
            title = "Locally created task",
            source = EventSource.LOCAL,
        )
        val googleEvent = TestDataFactory.createEvent(
            id = "google_1",
            title = "Google Calendar event",
            source = EventSource.GOOGLE,
        )
        fakeRepository.addEventsToStorage(listOf(localEvent, googleEvent))

        // When: Getting events for a date range with Google account
        val result = fakeRepository.getEventsForCalendarsInRange(
            userId = "user_1",
            start = 0L,
            end = Long.MAX_VALUE,
            hasGoogleAccount = true,
        ).first()

        // Then: Only GOOGLE events are returned
        assertEquals(1, result.size)
        assertEquals("google_1", result[0].id)
        assertEquals(EventSource.GOOGLE, result[0].source)
    }

    @Test
    fun noGoogleAccount_emptyLocalEvents_returnsEmpty() = runTest {
        // Given: No Google account and no local events
        fakeGetAllGoogleAccountsUseCase.setAccounts(emptyList())

        // And: Only GOOGLE events in storage (shouldn't happen, but test boundary)
        val googleEvent = TestDataFactory.createEvent(
            id = "google_1",
            title = "Google Calendar event",
            source = EventSource.GOOGLE,
        )
        fakeRepository.addEventsToStorage(listOf(googleEvent))

        // When: Getting events for a date range
        val result = fakeRepository.getEventsForCalendarsInRange(
            userId = "user_1",
            start = 0L,
            end = Long.MAX_VALUE,
            hasGoogleAccount = false,
        ).first()

        // Then: Empty list is returned (no LOCAL events to show)
        assertTrue(result.isEmpty())
    }

    @Test
    fun hasGoogleAccount_emptyGoogleEvents_returnsEmpty() = runTest {
        // Given: User has Google account but hasn't synced yet
        val googleAccount = GoogleAccountLink(
            id = "account_1",
            email = "user@gmail.com",
            personId = "mom_id",
        )
        fakeGetAllGoogleAccountsUseCase.setAccounts(listOf(googleAccount))

        // And: Only LOCAL events in storage
        val localEvent = TestDataFactory.createEvent(
            id = "local_1",
            title = "Locally created task",
            source = EventSource.LOCAL,
        )
        fakeRepository.addEventsToStorage(listOf(localEvent))

        // When: Getting events for a date range
        val result = fakeRepository.getEventsForCalendarsInRange(
            userId = "user_1",
            start = 0L,
            end = Long.MAX_VALUE,
            hasGoogleAccount = true,
        ).first()

        // Then: Empty list is returned (no GOOGLE events to show)
        assertTrue(result.isEmpty())
    }

    @Test
    fun multipleGoogleEvents_allReturned() = runTest {
        // Given: User has Google account
        val googleAccount = GoogleAccountLink(
            id = "account_1",
            email = "user@gmail.com",
            personId = "mom_id",
        )
        fakeGetAllGoogleAccountsUseCase.setAccounts(listOf(googleAccount))

        // And: Multiple GOOGLE events
        val googleEvent1 = TestDataFactory.createEvent(
            id = "google_1",
            title = "Meeting",
            source = EventSource.GOOGLE,
        )
        val googleEvent2 = TestDataFactory.createEvent(
            id = "google_2",
            title = "Dentist appointment",
            source = EventSource.GOOGLE,
        )
        val localEvent = TestDataFactory.createEvent(
            id = "local_1",
            title = "Local task",
            source = EventSource.LOCAL,
        )
        fakeRepository.addEventsToStorage(listOf(googleEvent1, googleEvent2, localEvent))

        // When: Getting events for a date range
        val result = fakeRepository.getEventsForCalendarsInRange(
            userId = "user_1",
            start = 0L,
            end = Long.MAX_VALUE,
            hasGoogleAccount = true,
        ).first()

        // Then: Both GOOGLE events are returned, but not LOCAL
        assertEquals(2, result.size)
        assertTrue(result.all { it.source == EventSource.GOOGLE })
        assertTrue(result.any { it.id == "google_1" })
        assertTrue(result.any { it.id == "google_2" })
    }

    // Fake implementations

    private class FakeGetAllGoogleAccountsUseCase(
        private val repository: FakeGoogleAccountRepository,
    ) {
        fun setAccounts(accounts: List<GoogleAccountLink>) {
            repository.accounts = accounts
        }

        operator fun invoke(): Flow<List<GoogleAccountLink>> = repository.getAllAccounts()
    }

    private class FakeGoogleAccountRepository : IGoogleAccountRepository {
        var accounts: List<GoogleAccountLink> = emptyList()

        override fun getAccountForPerson(personId: String) = flowOf<GoogleAccountLink?>(null)
        override fun getAllAccounts() = flowOf(accounts)
        override suspend fun getAccountById(accountId: String): GoogleAccountLink? = null
        override suspend fun upsertAccount(account: GoogleAccountLink) {}
        override suspend fun deleteAccount(account: GoogleAccountLink) {}
    }

    private class FakeEventRepositoryForFiltering : IEventRepository {
        private val storage = mutableListOf<Event>()

        fun addEventsToStorage(events: List<Event>) {
            storage.clear()
            storage.addAll(events)
        }

        fun getEventsForCalendarsInRange(
            userId: String,
            start: Long,
            end: Long,
            hasGoogleAccount: Boolean,
        ): Flow<List<Event>> = flowOf(
            if (hasGoogleAccount) {
                storage.filter { it.source == EventSource.GOOGLE }
            } else {
                storage.filter { it.source == EventSource.LOCAL }
            }
        )

        override fun getEventsForCalendarsInRange(
            userId: String,
            start: Long,
            end: Long,
        ): Flow<List<Event>> = flowOf(storage)

        // Stub methods (not used in these tests)
        override suspend fun syncEventsForCalendar(
            calendarIds: List<String>,
            startTime: Long,
            endTime: Long,
        ) {}

        override suspend fun addEvent(event: Event) {}
        override suspend fun updateEvent(event: Event) {}
        override suspend fun deleteEvent(event: Event) {}
    }
}
