package com.debanshu.xcalendar.domain.usecase.event

import com.debanshu.xcalendar.common.AppLogger
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.ExternalEvent
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IEventRepository
import com.debanshu.xcalendar.domain.sync.CalendarSyncManager
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetCalendarSourceUseCase
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.util.DomainError
import com.debanshu.xcalendar.domain.util.DomainResult
import com.debanshu.xcalendar.domain.util.EventValidationException
import com.debanshu.xcalendar.domain.util.EventValidator
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class CreateEventUseCase(
    private val eventRepository: IEventRepository,
    private val getCalendarSourceUseCase: GetCalendarSourceUseCase,
    private val syncManager: CalendarSyncManager,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val getAllGoogleAccountsUseCase: GetAllGoogleAccountsUseCase,
) {
    suspend operator fun invoke(event: Event): DomainResult<Unit> =
        try {
            // Validate event data before saving
            EventValidator.validate(
                title = event.title,
                startTime = event.startTime,
                endTime = event.endTime,
                calendarId = event.calendarId,
                isAllDay = event.isAllDay,
            )

            val nowMillis = System.currentTimeMillis()
            val calendarSource = getCalendarSourceUseCase(event.calendarId).first()
            val googleAccounts = getAllGoogleAccountsUseCase().first()
            val hasGoogleAccount = googleAccounts.isNotEmpty()

            // 1C: When any Google account is connected, force source = GOOGLE so the event is
            // immediately visible in the filtered view (EventRepository shows only GOOGLE when
            // Google is connected). When no Google account exists use LOCAL.
            val effectiveSource = if (hasGoogleAccount) EventSource.GOOGLE else EventSource.LOCAL

            // 2A: Persist locally FIRST so the event is always saved regardless of network.
            // Start with lastSyncedAt = 0L (pending sync).
            val localEvent = event.copy(source = effectiveSource, lastSyncedAt = 0L)
            eventRepository.addEvent(localEvent)

            // Schedule reminders and refresh widget immediately after local save.
            val prefs = getReminderPreferencesUseCase().first()
            reminderScheduler.scheduleEvent(localEvent, prefs)
            widgetUpdater.refreshTodayWidget()

            // Attempt to sync to Google if this calendar has a CalendarSource mapping.
            // On failure, the event stays local and the background sync job will retry.
            if (calendarSource != null) {
                try {
                    val external = localEvent.toExternalEvent()
                    val remote = syncManager.createEvent(
                        accountId = calendarSource.providerAccountId,
                        calendarId = calendarSource.providerCalendarId,
                        event = external,
                    )
                    if (remote != null) {
                        val synced = localEvent.copy(
                            externalId = remote.id,
                            externalUpdatedAt = remote.updatedAt,
                            lastSyncedAt = nowMillis,
                        )
                        eventRepository.updateEvent(synced)
                    }
                } catch (e: Exception) {
                    AppLogger.w(e) { "CreateEventUseCase: Google sync failed for ${event.id}; " +
                            "event saved locally with lastSyncedAt=0 — background sync will retry" }
                    // Non-fatal: local copy is already persisted.
                }
            }

            DomainResult.Success(Unit)
        } catch (e: EventValidationException) {
            DomainResult.Error(DomainError.ValidationError(e.message ?: "Validation failed"))
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown(e.message ?: "Failed to create event"))
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
            updatedAt = 0L,
        )
}

