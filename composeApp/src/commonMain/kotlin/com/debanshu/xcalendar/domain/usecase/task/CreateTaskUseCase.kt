package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class CreateTaskUseCase(
    private val taskRepository: ITaskRepository,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(task: Task): Task {
        taskRepository.upsertTask(task)
        val prefs = getReminderPreferencesUseCase().first()
        reminderScheduler.scheduleTask(task, prefs)
        widgetUpdater.refreshTodayWidget()
        return task
    }
}
