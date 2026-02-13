package com.debanshu.xcalendar.data.store

import com.debanshu.xcalendar.common.AppLogger
import com.debanshu.xcalendar.common.model.asEntity
import com.debanshu.xcalendar.common.model.asEvent
import com.debanshu.xcalendar.data.localDataSource.EventDao
import com.debanshu.xcalendar.data.localDataSource.model.EventReminderEntity
import com.debanshu.xcalendar.data.remoteDataSource.RemoteCalendarApiService
import com.debanshu.xcalendar.data.remoteDataSource.Result
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.mobilenativefoundation.store.store5.Bookkeeper
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

/**
 * Creates a MutableStore for events that handles:
 * - Fetching from the remote API (disabled when Google Calendar is connected)
 * - Caching to Room database
 * - Write operations (create, update, delete) with network sync
 */
object EventStoreFactory {

    fun create(
        apiService: RemoteCalendarApiService,
        eventDao: EventDao,
        bookkeeper: Bookkeeper<EventKey>,
    ): MutableStore<EventKey, List<Event>> {
        return MutableStoreBuilder.from(
            fetcher = createFetcher(apiService),
            sourceOfTruth = createSourceOfTruth(eventDao),
            converter = createEventListConverter()
        )
            .validator(EventValidator.create())
            .build(
                updater = createUpdater(apiService),
                bookkeeper = bookkeeper
            )
    }

    private fun createEventListConverter(): Converter<List<Event>, List<Event>, List<Event>> =
        Converter.Builder<List<Event>, List<Event>, List<Event>>()
            .fromNetworkToLocal { it }
            .fromOutputToLocal { it }
            .build()

    /**
     * Fetcher for demo/test data only.
     * 
     * RemoteCalendarApiService provides dummy events from GitHub (assets/events.json)
     * for testing and development. In production, this returns empty list because:
     * 1. Real data comes from GoogleCalendarApi (EventSource.GOOGLE)
     * 2. User-created data comes from local Room DB (EventSource.LOCAL)
     * 3. EventRepository is the single decision point for filtering by EventSource
     * 
     * When adding new calendar sources (e.g., iCal), only EventRepository needs changes.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createFetcher(
        apiService: RemoteCalendarApiService,
    ): Fetcher<EventKey, List<Event>> = Fetcher.of { key ->
        // RemoteCalendarApiService is for testing/demo only.
        // EventRepository handles all production filtering logic.
        AppLogger.d { "Fetcher: Returning empty list (demo API disabled in production)" }
        emptyList()
    }

    private fun createSourceOfTruth(
        eventDao: EventDao
    ): SourceOfTruth<EventKey, List<Event>, List<Event>> = SourceOfTruth.of(
        reader = { key ->
            eventDao.getEventsWithRemindersBetweenDates(key.userId, key.startTime, key.endTime)
                .map { eventsWithReminders ->
                    eventsWithReminders.map { it.asEvent() }
                }
        },
        writer = { _, events ->
            events.forEach { event ->
                val eventEntity = event.asEntity()
                val reminderEntities = event.reminderMinutes.map { minutes ->
                    EventReminderEntity(event.id, minutes)
                }
                eventDao.insertEventWithReminders(eventEntity, reminderEntities)
            }
        },
        delete = { key ->
            AppLogger.d { "Delete called for key: $key (no-op for range queries)" }
        },
        deleteAll = {
            AppLogger.d { "DeleteAll called (no-op - preserving local data)" }
        }
    )

    /**
     * Creates an Updater for syncing event lists to the remote server.
     * 
     * Current implementation: Local-only mode.
     * The app currently uses static JSON files as a demo API, so writes are
     * persisted locally via SourceOfTruth and marked as successful.
     * 
     * Future implementation: When a real backend is available, this should:
     * 1. Call apiService.syncEvents(events) or similar
     * 2. Handle conflict resolution (server vs local changes)
     * 3. Return appropriate UpdaterResult based on API response
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createUpdater(
        apiService: RemoteCalendarApiService
    ): Updater<EventKey, List<Event>, Unit> = Updater.by(
        post = { key, events ->
            AppLogger.d { "Updater: ${events.size} events saved locally (offline-first mode)" }
            // Data is already persisted via SourceOfTruth.writer
            // In offline-first architecture, local write is the source of truth
            // Network sync would happen here when backend supports it
            UpdaterResult.Success.Typed(Unit)
        }
    )
}

/**
 * Factory for creating a single-event MutableStore for individual CRUD operations
 */
object SingleEventStoreFactory {

    fun create(
        eventDao: EventDao,
        bookkeeper: Bookkeeper<SingleEventKey>
    ): MutableStore<SingleEventKey, Event> {
        return MutableStoreBuilder.from(
            fetcher = createFetcher(eventDao),
            sourceOfTruth = createSourceOfTruth(eventDao),
            converter = createSingleEventConverter()
        )
            .build(
                updater = createUpdater(),
                bookkeeper = bookkeeper
            )
    }

    private fun createSingleEventConverter(): Converter<Event, Event, Event> =
        Converter.Builder<Event, Event, Event>()
            .fromNetworkToLocal { it }
            .fromOutputToLocal { it }
            .build()

    private fun createFetcher(
        eventDao: EventDao
    ): Fetcher<SingleEventKey, Event> = Fetcher.of { key ->
        AppLogger.d { "Fetching single event: ${key.eventId}" }
        val entity = eventDao.getEventById(key.eventId)
            ?: throw StoreException("Event not found: ${key.eventId}")
        entity.asEvent()
    }

    private fun createSourceOfTruth(
        eventDao: EventDao
    ): SourceOfTruth<SingleEventKey, Event, Event> = SourceOfTruth.of(
        reader = { key ->
            flow {
                val entity = eventDao.getEventById(key.eventId)
                if (entity != null) {
                    emit(entity.asEvent())
                }
            }
        },
        writer = { _, event ->
            val eventEntity = event.asEntity()
            val reminderEntities = event.reminderMinutes.map { minutes ->
                EventReminderEntity(event.id, minutes)
            }
            eventDao.insertEventWithReminders(eventEntity, reminderEntities)
        },
        delete = { key ->
            eventDao.getEventById(key.eventId)?.let { entity ->
                eventDao.deleteEvent(entity)
            }
        },
        deleteAll = {
            AppLogger.d { "DeleteAll called on SingleEventStore (no-op)" }
        }
    )

    /**
     * Creates an Updater for syncing individual events to the remote server.
     * 
     * Current implementation: Local-only mode.
     * The app currently uses static JSON files as a demo API, so writes are
     * persisted locally via SourceOfTruth and marked as successful.
     * 
     * Future implementation: When a real backend is available, this should:
     * 1. POST/PUT to apiService.createEvent(event) or apiService.updateEvent(event)
     * 2. Handle server-side validation errors
     * 3. Update local entity with server-assigned IDs if needed
     * 4. Return UpdaterResult.Error on failure for Bookkeeper to track
     */
    private fun createUpdater(): Updater<SingleEventKey, Event, Unit> = Updater.by(
        post = { key, event ->
            AppLogger.d { "Updater: Event ${key.eventId} saved locally (offline-first mode)" }
            // Data is already persisted via SourceOfTruth.writer
            // Network sync would happen here when backend supports it
            UpdaterResult.Success.Typed(Unit)
        }
    )
}
