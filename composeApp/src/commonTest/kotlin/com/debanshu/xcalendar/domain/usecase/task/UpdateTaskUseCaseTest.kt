package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateTaskUseCaseTest {

    private class FakeTaskRepository(
        task: Task,
    ) : ITaskRepository {
        private val state = MutableStateFlow(listOf(task))
        val upserts = mutableListOf<Task>()

        override fun getTasks(): Flow<List<Task>> = state

        override suspend fun getTaskById(taskId: String): Task? = state.value.firstOrNull { it.id == taskId }

        override suspend fun upsertTask(task: Task) {
            upserts += task
            state.value = state.value.filterNot { it.id == task.id } + task
        }

        override suspend fun upsertTasks(tasks: List<Task>) {
            tasks.forEach { upsertTask(it) }
        }

        override suspend fun deleteTask(task: Task) {
            state.value = state.value.filterNot { it.id == task.id }
        }
    }

    private class FakeReminderPreferencesRepository : IReminderPreferencesRepository {
        override val preferences: Flow<ReminderPreferences> = MutableStateFlow(ReminderPreferences())

        override suspend fun setRemindersEnabled(enabled: Boolean) = Unit
        override suspend fun setPrepMinutes(minutes: Int) = Unit
        override suspend fun setTravelBufferMinutes(minutes: Int) = Unit
        override suspend fun setAllDayTime(hour: Int, minute: Int) = Unit
        override suspend fun setSummaryEnabled(enabled: Boolean) = Unit
        override suspend fun setSummaryTimes(
            morningHour: Int,
            morningMinute: Int,
            middayHour: Int,
            middayMinute: Int,
        ) = Unit
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduledTaskIds = mutableListOf<String>()
        val cancelledTaskIds = mutableListOf<String>()

        override suspend fun scheduleEvent(
            event: com.debanshu.xcalendar.domain.model.Event,
            preferences: ReminderPreferences,
        ) = Unit

        override suspend fun cancelEvent(eventId: String) = Unit

        override suspend fun scheduleTask(task: Task, preferences: ReminderPreferences) {
            scheduledTaskIds += task.id
        }

        override suspend fun cancelTask(taskId: String) {
            cancelledTaskIds += taskId
        }

        override suspend fun scheduleSummaries(preferences: ReminderPreferences) = Unit
        override suspend fun cancelSummaries() = Unit
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCount = 0

        override suspend fun refreshTodayWidget() {
            refreshCount += 1
        }
    }

    @Test
    fun openTask_schedulesReminderAndRefreshesWidget() = runTest {
        val task = Task(id = "task-open", title = "Laundry", status = TaskStatus.OPEN)
        val taskRepository = FakeTaskRepository(task)
        val reminderScheduler = FakeReminderScheduler()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase =
            UpdateTaskUseCase(
                taskRepository = taskRepository,
                getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeReminderPreferencesRepository()),
                reminderScheduler = reminderScheduler,
                widgetUpdater = widgetUpdater,
            )

        useCase(task)

        assertTrue(taskRepository.upserts.isNotEmpty())
        assertEquals(listOf("task-open"), reminderScheduler.scheduledTaskIds)
        assertTrue(reminderScheduler.cancelledTaskIds.isEmpty())
        assertEquals(1, widgetUpdater.refreshCount)
    }

    @Test
    fun doneTask_cancelsReminderAndRefreshesWidget() = runTest {
        val task = Task(id = "task-done", title = "Dishes", status = TaskStatus.DONE)
        val taskRepository = FakeTaskRepository(task)
        val reminderScheduler = FakeReminderScheduler()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase =
            UpdateTaskUseCase(
                taskRepository = taskRepository,
                getReminderPreferencesUseCase = GetReminderPreferencesUseCase(FakeReminderPreferencesRepository()),
                reminderScheduler = reminderScheduler,
                widgetUpdater = widgetUpdater,
            )

        useCase(task)

        assertTrue(taskRepository.upserts.isNotEmpty())
        assertEquals(listOf("task-done"), reminderScheduler.cancelledTaskIds)
        assertTrue(reminderScheduler.scheduledTaskIds.isEmpty())
        assertEquals(1, widgetUpdater.refreshCount)
    }
}
