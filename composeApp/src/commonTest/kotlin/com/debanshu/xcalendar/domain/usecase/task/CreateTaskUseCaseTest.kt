package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTaskUseCaseTest {

    private class FakeTaskRepository : ITaskRepository {
        val upserted = mutableListOf<Task>()
        private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

        override fun getTasks(): Flow<List<Task>> = tasksFlow

        override suspend fun getTaskById(taskId: String): Task? =
            tasksFlow.value.firstOrNull { it.id == taskId }

        override suspend fun upsertTask(task: Task) {
            upserted.add(task)
            tasksFlow.value = tasksFlow.value.filterNot { it.id == task.id } + task
        }

        override suspend fun upsertTasks(tasks: List<Task>) {
            tasks.forEach { upsertTask(it) }
        }

        override suspend fun deleteTask(task: Task) {
            tasksFlow.value = tasksFlow.value.filterNot { it.id == task.id }
        }
    }

    private class FakeReminderPreferencesRepository : IReminderPreferencesRepository {
        override val preferences: Flow<ReminderPreferences> = flowOf(ReminderPreferences())

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
        val scheduledTasks = mutableListOf<Task>()

        override suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences) = Unit

        override suspend fun cancelEvent(eventId: String) = Unit

        override suspend fun scheduleTask(task: Task, preferences: ReminderPreferences) {
            scheduledTasks.add(task)
        }

        override suspend fun cancelTask(taskId: String) = Unit

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
    fun invoke_savesTaskSchedulesRemindersAndRefreshesWidget() = runTest {
        val repository = FakeTaskRepository()
        val prefsRepository = FakeReminderPreferencesRepository()
        val scheduler = FakeReminderScheduler()
        val widgetUpdater = FakeWidgetUpdater()
        val getPrefs = GetReminderPreferencesUseCase(prefsRepository)
        val useCase = CreateTaskUseCase(repository, getPrefs, scheduler, widgetUpdater)

        val task = Task(id = "task-1", title = "Laundry")
        useCase(task)

        assertTrue(repository.upserted.isNotEmpty())
        assertEquals("Laundry", repository.upserted.first().title)
        assertEquals(1, scheduler.scheduledTasks.size)
        assertEquals("task-1", scheduler.scheduledTasks.first().id)
        assertEquals(1, widgetUpdater.refreshCount)
    }
}
