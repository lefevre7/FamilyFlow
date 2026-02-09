package com.debanshu.xcalendar.domain.usecase.event

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.util.DomainError
import com.debanshu.xcalendar.domain.util.DomainResult
import com.debanshu.xcalendar.domain.util.EventValidationException
import com.debanshu.xcalendar.domain.util.EventValidator
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class UpdateEventUseCase(
    private val eventRepository: IEventRepository,
    private val getCalendarSourceUseCase: GetCalendarSourceUseCase,
    private val syncManager: CalendarSyncManager,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(event: Event): DomainResult<Unit> {
        return try {
            if (event.id.isBlank()) {
                return DomainResult.Error(
                    DomainError.ValidationError("Event ID cannot be empty for update operation"),
                )
            }
            EventValidator.validate(
                title = event.title,
                startTime = event.startTime,
                endTime = event.endTime,
                calendarId = event.calendarId,
                isAllDay = event.isAllDay,
            )

            val source = getCalendarSourceUseCase(event.calendarId).first()
            val now = Clock.System.now().toEpochMilliseconds()
            val savedEvent =
                if (source != null) {
                    val external = event.toExternalEvent()
                    val remote =
                        if (!event.externalId.isNullOrBlank()) {
                            syncManager.updateEvent(
                                accountId = source.providerAccountId,
                                calendarId = source.providerCalendarId,
                                eventId = event.externalId,
                                event = external,
                            )
                        } else {
                            syncManager.createEvent(
                                accountId = source.providerAccountId,
                                calendarId = source.providerCalendarId,
                                event = external,
                            )
                        }
                    if (remote != null) {
                        event.copy(
                            source = EventSource.GOOGLE,
                            externalId = remote.id,
                            externalUpdatedAt = remote.updatedAt,
                            lastSyncedAt = now,
                        )
                    } else {
                        event.copy(
                            source = EventSource.GOOGLE,
                            lastSyncedAt = 0L,
                        )
                    }
                } else {
                    event.copy(source = EventSource.LOCAL)
                }

            eventRepository.updateEvent(savedEvent)
            val prefs = getReminderPreferencesUseCase().first()
            reminderScheduler.scheduleEvent(savedEvent, prefs)
            widgetUpdater.refreshTodayWidget()
            DomainResult.Success(Unit)
        } catch (e: EventValidationException) {
            DomainResult.Error(DomainError.ValidationError(e.message ?: "Validation failed"))
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown(e.message ?: "Failed to update event"))
        }
    }

    private fun Event.toExternalEvent(): ExternalEvent =
        ExternalEvent(
            id = externalId ?: "",
            summary = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            updatedAt = externalUpdatedAt ?: 0L,
        )
}
