package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.data.localDataSource.model.CalendarSourceEntity
import com.debanshu.xcalendar.domain.model.CalendarProvider
import com.debanshu.xcalendar.domain.model.CalendarSource

fun CalendarSourceEntity.asSource(): CalendarSource =
    CalendarSource(
        calendarId = calendarId,
        provider = CalendarProvider.valueOf(provider),
        providerCalendarId = providerCalendarId,
        providerAccountId = providerAccountId,
        syncEnabled = syncEnabled,
        lastSyncedAt = lastSyncedAt,
    )

fun CalendarSource.asEntity(): CalendarSourceEntity =
    CalendarSourceEntity(
        calendarId = calendarId,
        provider = provider.name,
        providerCalendarId = providerCalendarId,
        providerAccountId = providerAccountId,
        syncEnabled = syncEnabled,
        lastSyncedAt = lastSyncedAt,
    )
