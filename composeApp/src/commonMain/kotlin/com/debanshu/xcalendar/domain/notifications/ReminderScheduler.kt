package com.debanshu.xcalendar.domain.notifications

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Task

interface ReminderScheduler {
    suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences)

    suspend fun cancelEvent(eventId: String)

    suspend fun scheduleTask(task: Task, preferences: ReminderPreferences)

    suspend fun cancelTask(taskId: String)

    suspend fun scheduleSummaries(preferences: ReminderPreferences)

    suspend fun cancelSummaries()
}
