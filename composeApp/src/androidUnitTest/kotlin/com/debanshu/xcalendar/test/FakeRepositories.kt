package com.debanshu.xcalendar.test

import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import com.debanshu.xcalendar.domain.notifications.ReminderScheduler
import com.debanshu.xcalendar.domain.repository.IInboxRepository
import com.debanshu.xcalendar.domain.repository.ILensPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IPersonRepository
import com.debanshu.xcalendar.domain.repository.IReminderPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IRoutineRepository
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import com.debanshu.xcalendar.domain.repository.IUiPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IVoiceDiagnosticsRepository
import com.debanshu.xcalendar.domain.widgets.WidgetUpdater
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.platform.VoiceCaptureController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakePersonRepository(people: List<Person> = emptyList()) : IPersonRepository {
    private val peopleFlow = MutableStateFlow(people)

    override fun getPeople(): Flow<List<Person>> = peopleFlow

    override suspend fun upsertPeople(people: List<Person>) {
        peopleFlow.value = people
    }

    override suspend fun upsertPerson(person: Person) {
        peopleFlow.value = peopleFlow.value.filterNot { it.id == person.id } + person
    }

    override suspend fun deletePerson(person: Person) {
        peopleFlow.value = peopleFlow.value.filterNot { it.id == person.id }
    }

    override suspend fun ensureDefaultPeople() = Unit
}

class FakeTaskRepository(tasks: List<Task> = emptyList()) : ITaskRepository {
    private val tasksFlow = MutableStateFlow(tasks)
    val upserted = mutableListOf<Task>()

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

class FakeRoutineRepository(routines: List<Routine> = emptyList()) : IRoutineRepository {
    private val routinesFlow = MutableStateFlow(routines)

    override fun getRoutines(): Flow<List<Routine>> = routinesFlow

    override suspend fun getRoutineById(routineId: String): Routine? =
        routinesFlow.value.firstOrNull { it.id == routineId }

    override suspend fun upsertRoutine(routine: Routine) {
        routinesFlow.value = routinesFlow.value.filterNot { it.id == routine.id } + routine
    }

    override suspend fun upsertRoutines(routines: List<Routine>) {
        routines.forEach { upsertRoutine(it) }
    }

    override suspend fun deleteRoutine(routine: Routine) {
        routinesFlow.value = routinesFlow.value.filterNot { it.id == routine.id }
    }
}

class FakeInboxRepository : IInboxRepository {
    val items = mutableListOf<InboxItem>()

    override fun getInboxItems(): Flow<List<InboxItem>> = flowOf(items)

    override suspend fun getInboxItemById(itemId: String): InboxItem? =
        items.firstOrNull { it.id == itemId }

    override suspend fun upsertInboxItem(item: InboxItem) {
        items.removeAll { it.id == item.id }
        items.add(item)
    }

    override suspend fun upsertInboxItems(items: List<InboxItem>) {
        items.forEach { upsertInboxItem(it) }
    }

    override suspend fun deleteInboxItem(item: InboxItem) {
        items.removeAll { it.id == item.id }
    }
}

class FakeReminderPreferencesRepository : IReminderPreferencesRepository {
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

class FakeLensPreferencesRepository : ILensPreferencesRepository {
    private val state = MutableStateFlow(FamilyLensSelection())

    override val selection: Flow<FamilyLensSelection> = state

    override suspend fun updateSelection(selection: FamilyLensSelection) {
        state.value = selection
    }
}

class FakeUiPreferencesRepository(initialDismissed: Boolean = false) : IUiPreferencesRepository {
    private val _navDragHintDismissed = MutableStateFlow(initialDismissed)

    override val navDragHintDismissed: Flow<Boolean> = _navDragHintDismissed

    override suspend fun setNavDragHintDismissed(dismissed: Boolean) {
        _navDragHintDismissed.value = dismissed
    }
}

class FakeVoiceDiagnosticsRepository : IVoiceDiagnosticsRepository {
    private val enabledState = MutableStateFlow(true)
    private val entriesState = MutableStateFlow<List<VoiceDiagnosticEntry>>(emptyList())

    override val diagnosticsEnabled: Flow<Boolean> = enabledState
    override val entries: Flow<List<VoiceDiagnosticEntry>> = entriesState

    override suspend fun isDiagnosticsEnabled(): Boolean = enabledState.value

    override suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        enabledState.value = enabled
    }

    override suspend fun append(entry: VoiceDiagnosticEntry) {
        entriesState.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }

    override suspend fun clear() {
        entriesState.value = emptyList()
    }

    fun entriesValue(): List<VoiceDiagnosticEntry> = entriesState.value

    private companion object {
        private const val MAX_ENTRIES = 200
    }
}

class FakeReminderScheduler : ReminderScheduler {
    val scheduledTasks = mutableListOf<Task>()
    val scheduledEvents = mutableListOf<Event>()

    override suspend fun scheduleEvent(event: Event, preferences: ReminderPreferences) {
        scheduledEvents.add(event)
    }

    override suspend fun cancelEvent(eventId: String) = Unit

    override suspend fun scheduleTask(task: Task, preferences: ReminderPreferences) {
        scheduledTasks.add(task)
    }

    override suspend fun cancelTask(taskId: String) = Unit

    override suspend fun scheduleSummaries(preferences: ReminderPreferences) = Unit

    override suspend fun cancelSummaries() = Unit
}

class FakeWidgetUpdater : WidgetUpdater {
    var refreshCount = 0

    override suspend fun refreshTodayWidget() {
        refreshCount += 1
    }
}

class FakeNotifier : PlatformNotifier {
    val messages = mutableListOf<String>()
    val sharedPayloads = mutableListOf<Pair<String, String>>()
    var lastToast: String? = null

    override fun showToast(message: String) {
        messages.add(message)
        lastToast = message
    }

    override fun shareText(subject: String, text: String) {
        sharedPayloads.add(subject to text)
    }
}

class FakeLocalLlmManager : LocalLlmManager {
    override val isAvailable: Boolean = false

    override fun getStatus(): com.debanshu.xcalendar.domain.llm.LlmModelStatus =
        com.debanshu.xcalendar.domain.llm.LlmModelStatus(
            available = false,
            source = com.debanshu.xcalendar.domain.llm.LlmModelSource.NONE,
            sizeBytes = 0L,
            requiredBytes = 0L,
        )

    override suspend fun ensureModelAvailable(): Boolean = false

    override suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean = false

    override suspend fun generate(
        prompt: String,
        sampling: com.debanshu.xcalendar.domain.llm.LlmSamplingConfig,
    ): String? = null

    override fun deleteModel(): Boolean = false

    override fun consumeWarningMessage(): String? = null
}
