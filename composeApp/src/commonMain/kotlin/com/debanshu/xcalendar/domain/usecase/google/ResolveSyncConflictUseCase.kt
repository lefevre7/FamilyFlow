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

@Factory
class ResolveSyncConflictUseCase(
    private val syncManager: CalendarSyncManager,
    private val getCalendarSourceUseCase: GetCalendarSourceUseCase,
    private val eventRepository: IEventRepository,
    private val conflictStateHolder: SyncConflictStateHolder,
) {
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
}
