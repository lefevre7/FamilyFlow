package com.debanshu.xcalendar.test

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IInboxRepository
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IPersonRepository
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IRoutineRepository
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.usecase.inbox.CreateInboxItemUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.routine.GetRoutinesUseCase
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.usecase.task.CreateTaskUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.task.UpdateTaskUseCase
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import com.debanshu.xcalendar.ui.state.TimerStateHolder
import org.koin.core.module.Module
import org.koin.dsl.module

data class TestDependencies(
    val module: Module,
    val taskRepository: FakeTaskRepository,
    val inboxRepository: FakeInboxRepository,
    val reminderScheduler: FakeReminderScheduler,
    val widgetUpdater: FakeWidgetUpdater,
    val notifier: FakeNotifier,
)

fun buildTestDependencies(
    people: List<Person> = emptyList(),
    tasks: List<Task> = emptyList(),
    routines: List<Routine> = emptyList(),
): TestDependencies {
    val taskRepository = FakeTaskRepository(tasks)
    val inboxRepository = FakeInboxRepository()
    val reminderScheduler = FakeReminderScheduler()
    val widgetUpdater = FakeWidgetUpdater()
    val notifier = FakeNotifier()

    val module =
        module {
            single<IPersonRepository> { FakePersonRepository(people) }
            single<ITaskRepository> { taskRepository }
            single<IRoutineRepository> { FakeRoutineRepository(routines) }
            single<IInboxRepository> { inboxRepository }
            single<IReminderPreferencesRepository> { FakeReminderPreferencesRepository() }
            single<ILensPreferencesRepository> { FakeLensPreferencesRepository() }
            single<ReminderScheduler> { reminderScheduler }
            single<WidgetUpdater> { widgetUpdater }
            single<PlatformNotifier> { notifier }

            single { GetPeopleUseCase(get()) }
            single { GetTasksUseCase(get()) }
            single { GetRoutinesUseCase(get()) }
            single { GetReminderPreferencesUseCase(get()) }
            single { CreateTaskUseCase(get(), get(), get(), get()) }
            single { UpdateTaskUseCase(get(), get(), get(), get()) }
            single { CreateInboxItemUseCase(get()) }
            single { TimerStateHolder() }
            single { LensStateHolder(get()) }
            single { SyncConflictStateHolder() }
        }

    return TestDependencies(
        module = module,
        taskRepository = taskRepository,
        inboxRepository = inboxRepository,
        reminderScheduler = reminderScheduler,
        widgetUpdater = widgetUpdater,
        notifier = notifier,
    )
}
