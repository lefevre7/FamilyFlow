package com.debanshu.xcalendar.domain.usecase.google

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.sync.SyncConflict
import com.debanshu.xcalendar.domain.sync.SyncResolutionAction
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Factory
class ResolveSyncConflictUseCase(
    private val syncManager: CalendarSyncManager,
    private val getCalendarSourceUseCase: GetCalendarSourceUseCase,
    private val eventRepository: IEventRepository,
    private val conflictStateHolder: SyncConflictStateHolder,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(conflict: SyncConflict, action: SyncResolutionAction) {
        val source = getCalendarSourceUseCase(conflict.calendarId).first() ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        when (action) {
            SyncResolutionAction.KEEP_LOCAL -> {
                syncManager.updateEvent(
                    accountId = source.providerAccountId,
                    calendarId = source.providerCalendarId,
                    eventId = conflict.remoteEvent.id,
                    event = conflict.localEvent.toExternalEvent(conflict.remoteEvent),
                )?.let { remote ->
                    val updated = conflict.localEvent.copy(
                        externalUpdatedAt = remote.updatedAt,
                        lastSyncedAt = now,
                    )
                    eventRepository.updateEvent(updated)
                }
            }
            SyncResolutionAction.KEEP_REMOTE -> {
                val updated = conflict.remoteEvent.mergeInto(conflict.localEvent, now)
                eventRepository.updateEvent(updated)
            }
            SyncResolutionAction.DUPLICATE -> {
                val duplicate =
                    conflict.remoteEvent.toLocalDuplicate(
                        calendarId = conflict.calendarId,
                        calendarName = conflict.localEvent.calendarName,
                        color = conflict.localEvent.color,
                        syncedAt = now,
                    )
                eventRepository.addEvent(duplicate)
            }
        }
        conflictStateHolder.resolveConflict(conflict)
    }

    private fun Event.toExternalEvent(remote: ExternalEvent): ExternalEvent =
        ExternalEvent(
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

    private fun ExternalEvent.mergeInto(local: Event, syncedAt: Long): Event =
        local.copy(
            title = summary,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            source = EventSource.GOOGLE,
            externalUpdatedAt = updatedAt,
            lastSyncedAt = syncedAt,
        )

    @OptIn(ExperimentalUuidApi::class)
    private fun ExternalEvent.toLocalDuplicate(
        calendarId: String,
        calendarName: String,
        color: Int,
        syncedAt: Long,
    ): Event =
        Event(
            id = Uuid.random().toString(),
            calendarId = calendarId,
            calendarName = calendarName,
            title = summary,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            isRecurring = false,
            recurringRule = null,
            reminderMinutes = emptyList(),
            color = color,
            source = EventSource.GOOGLE,
            externalId = id,
            externalUpdatedAt = updatedAt,
            lastSyncedAt = syncedAt,
        )
}
