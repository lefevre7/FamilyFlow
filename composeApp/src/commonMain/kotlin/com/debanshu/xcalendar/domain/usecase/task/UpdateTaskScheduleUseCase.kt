package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class UpdateTaskScheduleUseCase(
    private val taskRepository: ITaskRepository,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(task: Task, startTime: Long, endTime: Long): Task {
        val updated = task.copy(scheduledStart = startTime, scheduledEnd = endTime)
        taskRepository.upsertTask(updated)
        val prefs = getReminderPreferencesUseCase().first()
        reminderScheduler.scheduleTask(updated, prefs)
        widgetUpdater.refreshTodayWidget()
        return updated
    }
}
