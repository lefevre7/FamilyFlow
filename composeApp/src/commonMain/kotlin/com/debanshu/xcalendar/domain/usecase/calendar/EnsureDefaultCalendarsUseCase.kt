package com.debanshu.xcalendar.domain.usecase.calendar

import com.debanshu.xcalendar.common.convertStringToColor
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.repository.ICalendarRepository
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Ensures every user always has at least one writable local calendar.
 *
 * Google calendars are seeded by [ImportGoogleCalendarsUseCase] after OAuth.
 * When the user has no Google account connected this use case guarantees a "Personal"
 * local calendar (id = "local:personal") exists, so the Add Event dialog always has
 * at least one calendar to show and events can be saved immediately.
 *
 * Idempotent — safe to call on every app launch.
 */
@Factory
class EnsureDefaultCalendarsUseCase(
    private val calendarRepository: ICalendarRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) {
    companion object {
        const val LOCAL_PERSONAL_CALENDAR_ID = "local:personal"
        private const val LOCAL_PERSONAL_CALENDAR_NAME = "Personal"
    }

    suspend operator fun invoke() {
        val userId = getCurrentUserUseCase()
        val existing = calendarRepository.getCalendarsForUser(userId).first()
        // Only seed the default when no calendars are present (Google or otherwise).
        if (existing.isNotEmpty()) return

        val personal = Calendar(
            id = LOCAL_PERSONAL_CALENDAR_ID,
            name = LOCAL_PERSONAL_CALENDAR_NAME,
            color = convertStringToColor(LOCAL_PERSONAL_CALENDAR_ID + LOCAL_PERSONAL_CALENDAR_NAME),
            userId = userId,
            isVisible = true,
            isPrimary = true,
        )
        calendarRepository.upsertCalendar(listOf(personal))
    }
}
