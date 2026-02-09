package com.debanshu.xcalendar.domain.model

data class CalendarSource(
    val calendarId: String,
    val provider: CalendarProvider,
    val providerCalendarId: String,
    val providerAccountId: String,
    val syncEnabled: Boolean = true,
    val lastSyncedAt: Long? = null,
)

enum class CalendarProvider {
    GOOGLE,
}
