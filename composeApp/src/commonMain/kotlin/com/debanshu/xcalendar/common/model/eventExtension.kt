package com.debanshu.xcalendar.common.model

import com.debanshu.xcalendar.common.convertStringToColor
import com.debanshu.xcalendar.data.localDataSource.model.EventEntity
import com.debanshu.xcalendar.data.localDataSource.model.EventWithReminders
import com.debanshu.xcalendar.data.remoteDataSource.model.calendar.EventResponseItem
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource


fun EventResponseItem.asEvent(): Event {
    return Event(
        id = id,
        title = title,
        description = description,
        location = location,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringRule = recurringRule,
        reminderMinutes = reminderMinutes,
        calendarId = calendarId,
        calendarName = calendarName ?: "",
        color = convertStringToColor(calendarId + calendarName),
        source = EventSource.LOCAL,
    )
}

fun EventWithReminders.asEvent(): Event {
    return Event(
        id = event.id,
        calendarId = event.calendarId,
        title = event.title,
        description = event.description,
        location = event.location,
        startTime = event.startTime,
        endTime = event.endTime,
        isAllDay = event.isAllDay,
        isRecurring = event.isRecurring,
        recurringRule = event.recurringRule,
        reminderMinutes = reminders.map { it.minutes },
        calendarName = event.calendarName,
        color = convertStringToColor(event.calendarId + event.calendarName),
        source = EventSource.valueOf(event.source),
        externalId = event.externalId,
        externalUpdatedAt = event.externalUpdatedAt,
        lastSyncedAt = event.lastSyncedAt,
    )
}

fun EventEntity.asEvent(): Event {
    return Event(
        id = id,
        calendarId = calendarId,
        title = title,
        description = description,
        location = location,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringRule = recurringRule,
        reminderMinutes = emptyList(),
        calendarName = calendarName,
        color = convertStringToColor(calendarId + calendarName),
        source = EventSource.valueOf(source),
        externalId = externalId,
        externalUpdatedAt = externalUpdatedAt,
        lastSyncedAt = lastSyncedAt,
    )
}

fun Event.asEntity(): EventEntity =
    EventEntity(
        id = id,
        calendarId = calendarId,
        title = title,
        description = description,
        location = location,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        calendarName = calendarName,
        recurringRule = recurringRule,
        source = source.name,
        externalId = externalId,
        externalUpdatedAt = externalUpdatedAt,
        lastSyncedAt = lastSyncedAt,
    )
