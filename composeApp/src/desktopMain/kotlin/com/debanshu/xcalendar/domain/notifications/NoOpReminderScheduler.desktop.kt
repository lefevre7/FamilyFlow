package com.debanshu.xcalendar.domain.notifications

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Task
import org.koin.core.annotation.Single

@Single(binds = [ReminderScheduler::class])
class NoOpReminderScheduler : ReminderScheduler {
    override suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences) = Unit

    override suspend fun cancelEvent(eventId: String) = Unit

    override suspend fun scheduleTask(task: Task, preferences: ReminderPreferences) = Unit

    override suspend fun cancelTask(taskId: String) = Unit

    override suspend fun scheduleSummaries(preferences: ReminderPreferences) = Unit

    override suspend fun cancelSummaries() = Unit
}
