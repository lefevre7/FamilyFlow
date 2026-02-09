package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.CalendarProvider
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.auth.GoogleTokenStore
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.sync.SyncConflict
import com.debanshu.xcalendar.domain.util.ImportCategoryClassifier
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetAllCalendarSourcesUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Factory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Factory
class SyncGoogleCalendarsUseCase(
    private val syncManager: CalendarSyncManager,
    private val getAllCalendarSourcesUseCase: GetAllCalendarSourcesUseCase,
    private val getGoogleAccountByIdUseCase: GetGoogleAccountByIdUseCase,
    private val getUserCalendarsUseCase: GetUserCalendarsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val eventRepository: IEventRepository,
    private val conflictStateHolder: SyncConflictStateHolder,
    private val tokenStore: GoogleTokenStore,
) {
    suspend operator fun invoke(manual: Boolean = false): List<SyncConflict> {
        val userId = getCurrentUserUseCase()
        val calendars = getUserCalendarsUseCase(userId).first()
        val sources = getAllCalendarSourcesUseCase()
            .filter { it.provider == CalendarProvider.GOOGLE && it.syncEnabled }
        if (sources.isEmpty()) return emptyList()

        val now = Clock.System.now().toEpochMilliseconds()
        val range = buildSyncRange()
        val events = eventRepository.getEventsForCalendarsInRange(userId, range.first, range.second)
            .first()
        val conflicts = mutableListOf<SyncConflict>()

        sources.forEach { source ->
            if (tokenStore.getTokens(source.providerAccountId) == null) return@forEach
            val calendar = calendars.firstOrNull { it.id == source.calendarId } ?: return@forEach
            val linkedAccount = getGoogleAccountByIdUseCase(source.providerAccountId)
            val defaultPersonId = linkedAccount?.personId
            val remoteEvents =
                syncManager.listEvents(
                    accountId = source.providerAccountId,
                    calendarId = source.providerCalendarId,
                    timeMin = range.first,
                    timeMax = range.second,
                )
            val remoteIds = remoteEvents.map { it.id }.toSet()
            val localForCalendar = events.filter { it.calendarId == source.calendarId }
            val localByExternalId = localForCalendar.filter { !it.externalId.isNullOrBlank() }
                .associateBy { it.externalId!! }
            val localPending = localForCalendar
                .filter { it.externalId.isNullOrBlank() }

            localPending.forEach { pending ->
                val remote =
                    syncManager.createEvent(
                        accountId = source.providerAccountId,
                        calendarId = source.providerCalendarId,
                        event = pending.toExternalEventPlaceholder(),
                    )
                if (remote != null) {
                    val updated = pending.copy(
                        externalId = remote.id,
                        externalUpdatedAt = remote.updatedAt,
                        lastSyncedAt = now,
                    )
                    eventRepository.updateEvent(updated)
                }
            }

            remoteEvents.forEach { remote ->
                val local = localByExternalId[remote.id]
                if (remote.cancelled) {
                    if (local != null) {
                        eventRepository.deleteEvent(local)
                    }
                    return@forEach
                }
                if (local == null) {
                    val created = remote.toLocalEvent(calendar, now, defaultPersonId)
                    eventRepository.addEvent(created)
                    return@forEach
                }
                val lastSyncedAt = local.lastSyncedAt ?: 0L
                val localChanged = lastSyncedAt <= 0L
                val remoteBaseline = local.externalUpdatedAt ?: 0L
                val remoteChanged = remote.updatedAt > remoteBaseline
                if (localChanged && remoteChanged) {
                    conflicts.add(
                        SyncConflict(
                            calendarId = calendar.id,
                            localEvent = local,
                            remoteEvent = remote,
                        )
                    )
                } else if (remoteChanged) {
                    val updated = remote.mergeInto(local, calendar, now)
                    eventRepository.updateEvent(updated)
                } else if (localChanged) {
                    syncManager.updateEvent(
                        accountId = source.providerAccountId,
                        calendarId = source.providerCalendarId,
                        eventId = remote.id,
                        event = local.toExternalEvent(remote),
                    )?.let { response ->
                        val updated = local.copy(
                            externalUpdatedAt = response.updatedAt,
                            lastSyncedAt = now,
                        )
                        eventRepository.updateEvent(updated)
                    }
                } else if (local.lastSyncedAt != now) {
                    val updated = local.copy(lastSyncedAt = now)
                    eventRepository.updateEvent(updated)
                }
            }

            localByExternalId.values
                .filter { it.externalId != null && it.externalId !in remoteIds }
                .forEach { eventRepository.deleteEvent(it) }
        }

        if (manual) {
            conflictStateHolder.setConflicts(conflicts)
        }
        return conflicts
    }

    private fun buildSyncRange(): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone).date
        val start = now.minus(DatePeriod(days = 30))
        val end = now.plus(DatePeriod(months = 6))
        val startMillis = start.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endMillis = end.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return startMillis to endMillis
    }

    private fun ExternalEvent.toLocalEvent(
        calendar: Calendar,
        syncedAt: Long,
        personId: String?,
    ): Event {
        val category = ImportCategoryClassifier.classify(summary, description)
        return Event(
            id = Uuid.random().toString(),
            calendarId = calendar.id,
            calendarName = calendar.name,
            title = summary,
            description = ImportCategoryClassifier.applyCategory(description, category),
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            isRecurring = false,
            recurringRule = null,
            reminderMinutes = emptyList(),
            color = calendar.color,
            source = EventSource.GOOGLE,
            externalId = id,
            externalUpdatedAt = updatedAt,
            lastSyncedAt = syncedAt,
            affectedPersonIds = personId?.let { listOf(it) } ?: emptyList(),
        )
    }

    private fun ExternalEvent.mergeInto(
        local: Event,
        calendar: Calendar,
        syncedAt: Long,
    ): Event {
        val category = ImportCategoryClassifier.classify(summary, description)
        return local.copy(
            calendarName = calendar.name,
            title = summary,
            description = ImportCategoryClassifier.applyCategory(description, category),
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            color = calendar.color,
            source = EventSource.GOOGLE,
            externalUpdatedAt = updatedAt,
            lastSyncedAt = syncedAt,
        )
    }

    private fun Event.toExternalEvent(remote: ExternalEvent): ExternalEvent {
        return ExternalEvent(
            id = remote.id,
            summary = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            updatedAt = remote.updatedAt,
            cancelled = false,
        )
    }

    private fun Event.toExternalEventPlaceholder(): ExternalEvent =
        ExternalEvent(
            id = "",
            summary = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            updatedAt = 0L,
        )
}
