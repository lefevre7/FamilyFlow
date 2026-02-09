package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class UpdateTaskUseCase(
    private val taskRepository: ITaskRepository,
    private val getReminderPreferencesUseCase: GetReminderPreferencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(task: Task): Task {
        val updated = task.copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        taskRepository.upsertTask(updated)
        val prefs = getReminderPreferencesUseCase().first()
        if (updated.status == TaskStatus.OPEN) {
            reminderScheduler.scheduleTask(updated, prefs)
        } else {
            reminderScheduler.cancelTask(updated.id)
        }
        widgetUpdater.refreshTodayWidget()
        return updated
    }
}
