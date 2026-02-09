package com.debanshu.xcalendar.domain.usecase.event

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.domain.util.DomainError
import com.debanshu.xcalendar.domain.util.DomainResult
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class DeleteEventUseCase(
    private val eventRepository: IEventRepository,
    private val getCalendarSourceUseCase: GetCalendarSourceUseCase,
    private val syncManager: CalendarSyncManager,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(event: Event): DomainResult<Unit> =
        try {
            val source = getCalendarSourceUseCase(event.calendarId).first()
            if (source != null && !event.externalId.isNullOrBlank()) {
                syncManager.deleteEvent(
                    accountId = source.providerAccountId,
                    calendarId = source.providerCalendarId,
                    eventId = event.externalId,
                )
            }
            eventRepository.deleteEvent(event)
            reminderScheduler.cancelEvent(event.id)
            widgetUpdater.refreshTodayWidget()
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown(e.message ?: "Failed to delete event"))
        }
}
