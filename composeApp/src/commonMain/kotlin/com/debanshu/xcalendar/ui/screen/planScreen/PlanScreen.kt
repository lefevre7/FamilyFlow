package com.debanshu.xcalendar.ui.screen.planScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.Project
import com.debanshu.xcalendar.domain.model.ProjectStatus
import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.model.RoutineTimeOfDay
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.ScheduleSuggestion
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.TaskType
import com.debanshu.xcalendar.domain.model.effectivePersonId
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.StructureBrainDumpUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.CreateInboxItemUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.GetInboxItemsUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.UpdateInboxItemStatusUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.project.GetProjectsUseCase
import com.debanshu.xcalendar.domain.usecase.routine.GetRoutinesUseCase
import com.debanshu.xcalendar.domain.usecase.routine.UpsertRoutinesUseCase
import com.debanshu.xcalendar.domain.usecase.task.CreateTaskUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.task.UpdateTaskScheduleUseCase
import com.debanshu.xcalendar.domain.usecase.task.UpdateTaskUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.ui.components.FamilyLensMiniHeader
import com.debanshu.xcalendar.ui.screen.monthScreen.components.MonthView
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
fun PlanScreen(
    modifier: Modifier = Modifier,
    dateStateHolder: DateStateHolder,
    events: ImmutableList<Event>,
    isVisible: Boolean = true,
) {
    if (!isVisible) return

    val dateState by dateStateHolder.currentDateState.collectAsState()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val nowMillis = remember { Clock.System.now().toEpochMilliseconds() }
    val scope = rememberCoroutineScope()

    val getInboxItemsUseCase = koinInject<GetInboxItemsUseCase>()
    val createInboxItemUseCase = koinInject<CreateInboxItemUseCase>()
    val getProjectsUseCase = koinInject<GetProjectsUseCase>()
    val getTasksUseCase = koinInject<GetTasksUseCase>()
    val getRoutinesUseCase = koinInject<GetRoutinesUseCase>()
    val upsertRoutinesUseCase = koinInject<UpsertRoutinesUseCase>()
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val updateInboxItemStatusUseCase = koinInject<UpdateInboxItemStatusUseCase>()
    val updateTaskScheduleUseCase = koinInject<UpdateTaskScheduleUseCase>()
    val updateTaskUseCase = koinInject<UpdateTaskUseCase>()
    val getUserCalendarsUseCase = koinInject<GetUserCalendarsUseCase>()
    val getCurrentUserUseCase = koinInject<GetCurrentUserUseCase>()
    val createTaskUseCase = koinInject<CreateTaskUseCase>()
    val structureBrainDumpUseCase = koinInject<StructureBrainDumpUseCase>()
    val lensStateHolder = koinInject<LensStateHolder>()
    val notifier = koinInject<PlatformNotifier>()

    val userId = remember { getCurrentUserUseCase() }
    val calendars by remember { getUserCalendarsUseCase(userId) }.collectAsState(initial = emptyList())
    val visibleCalendars = remember(calendars) { calendars.filter { it.isVisible } }
    var selectedCalendarId by rememberSaveable { mutableStateOf<String?>(null) }
    var showOcrSheet by rememberSaveable { mutableStateOf(false) }
    var brainDumpInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(visibleCalendars) {
        if (selectedCalendarId == null && visibleCalendars.isNotEmpty()) {
            selectedCalendarId = visibleCalendars.first().id
        }
    }

    val inboxItems by remember { getInboxItemsUseCase() }.collectAsState(initial = emptyList())
    val projects by remember { getProjectsUseCase() }.collectAsState(initial = emptyList())
    val tasks by remember { getTasksUseCase() }.collectAsState(initial = emptyList())
    val routines by remember { getRoutinesUseCase() }.collectAsState(initial = emptyList())
    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())
    val lensSelection by lensStateHolder.selection.collectAsState()

    val momId = remember(people) { people.firstOrNull { it.role == PersonRole.MOM }?.id }
    val lensPersonId = remember(lensSelection, momId) { lensSelection.effectivePersonId(momId) }
    val filteredEvents = remember(events, lensPersonId) {
        if (lensPersonId == null) {
            events.toImmutableList()
        } else {
            events.filter { event ->
                event.affectedPersonIds.isEmpty() || event.affectedPersonIds.contains(lensPersonId)
            }.toImmutableList()
        }
    }
    val filteredTasks = remember(tasks, lensPersonId) {
        if (lensPersonId == null) {
            tasks
        } else {
            tasks.filter { task ->
                task.assignedToPersonId == lensPersonId || task.affectedPersonIds.contains(lensPersonId)
            }
        }
    }
    val filteredProjects = remember(projects, lensPersonId) {
        if (lensPersonId == null) {
            projects
        } else {
            projects.filter { project ->
                project.ownerPersonId == null || project.ownerPersonId == lensPersonId
            }
        }
    }

    val suggestions = remember(filteredEvents, filteredTasks, nowMillis, timeZone) {
        ScheduleEngine.aggregate(
            events = filteredEvents,
            tasks = filteredTasks,
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        ).suggestions
    }

    var dismissedSuggestionTaskIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var pendingSuggestionUndo by remember { mutableStateOf<PendingSuggestionUndo?>(null) }
    val suggestionsByTask = remember(suggestions, dismissedSuggestionTaskIds) {
        suggestions
            .groupBy { it.taskId }
            .filterKeys { taskId -> !dismissedSuggestionTaskIds.contains(taskId) }
    }
    val tasksById = remember(tasks) { tasks.associateBy { it.id } }
    val peopleById = remember(people) { people.associateBy { it.id } }

    LaunchedEffect(pendingSuggestionUndo) {
        val snapshot = pendingSuggestionUndo ?: return@LaunchedEffect
        delay(5_000L)
        if (pendingSuggestionUndo == snapshot) {
            pendingSuggestionUndo = null
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlanHeader(
            date = dateState.selectedDate,
            lensSelection = lensSelection,
            people = people,
            onLensChange = lensStateHolder::updateSelection,
        )

        BrainDumpSection(
            inboxItems = inboxItems,
            captureText = brainDumpInput,
            onCaptureTextChanged = { brainDumpInput = it },
            onCapture = {
                val raw = brainDumpInput.trim()
                if (raw.isBlank()) {
                    notifier.showToast("Type a thought first")
                } else {
                    scope.launch {
                        createInboxItemUseCase(
                            InboxItem(
                                id = Uuid.random().toString(),
                                rawText = raw,
                                source = InboxSource.TEXT,
                                status = InboxStatus.NEW,
                                createdAt = Clock.System.now().toEpochMilliseconds(),
                                personId = lensPersonId,
                            ),
                        )
                        brainDumpInput = ""
                        notifier.showToast("Added to Brain Dump")
                    }
                }
            },
            onProcessAll = {
                scope.launch {
                    val pending = inboxItems.filter { it.status == InboxStatus.NEW }
                    pending.forEach { item ->
                        val structured = structureBrainDumpUseCase(item.rawText)
                        val now = Clock.System.now().toEpochMilliseconds()
                        structured.tasks.forEach { draft ->
                            val task =
                                Task(
                                    id = Uuid.random().toString(),
                                    title = draft.title,
                                    notes = draft.notes,
                                    priority = draft.priority ?: TaskPriority.SHOULD,
                                    energy = draft.energy ?: TaskEnergy.MEDIUM,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            createTaskUseCase(task)
                        }
                        updateInboxItemStatusUseCase(item, InboxStatus.PROCESSED)
                    }
                }
            },
            onProcessItem = { item ->
                scope.launch {
                    if (item.status == InboxStatus.NEW) {
                        updateInboxItemStatusUseCase(item, InboxStatus.PROCESSED)
                    }
                }
            },
            onArchive = { item ->
                scope.launch {
                    if (item.status != InboxStatus.ARCHIVED) {
                        updateInboxItemStatusUseCase(item, InboxStatus.ARCHIVED)
                    }
                }
            },
        )

        ScanScheduleSection(
            onScan = { showOcrSheet = true },
        )

        SuggestionSection(
            suggestionsByTask = suggestionsByTask,
            tasksById = tasksById,
            peopleById = peopleById,
            timeZone = timeZone,
            pendingUndo = pendingSuggestionUndo,
            onAccept = { task, suggestion, _ ->
                scope.launch {
                    updateTaskScheduleUseCase(task, suggestion.startTime, suggestion.endTime)
                    pendingSuggestionUndo =
                        PendingSuggestionUndo(
                            previousTask = task,
                            acceptedSuggestion = suggestion,
                        )
                    notifier.showToast("Moved to ${formatSuggestionLabel(suggestion, timeZone)} - Undo")
                }
            },
            onNotNow = { task ->
                dismissedSuggestionTaskIds = (dismissedSuggestionTaskIds + task.id).distinct()
                notifier.showToast("Suggestion hidden for now")
            },
            onUndo = {
                val undo = pendingSuggestionUndo
                if (undo == null) {
                    notifier.showToast("Nothing to undo")
                } else {
                    scope.launch {
                        updateTaskUseCase(undo.previousTask)
                        pendingSuggestionUndo = null
                        notifier.showToast("Move undone")
                    }
                }
            },
        )

        MonthOverviewSection(
            month = dateState.selectedInViewMonth,
            events = filteredEvents,
            onDateClick = { date ->
                dateStateHolder.updateSelectedDateState(date)
            },
        )

        YearAlertsSection(
            anchorMonth = dateState.selectedInViewMonth,
            events = filteredEvents,
            tasks = filteredTasks,
            projects = filteredProjects,
            timeZone = timeZone,
            onOpenMonth = { yearMonth ->
                dateStateHolder.updateSelectedDateState(LocalDate(yearMonth.year, yearMonth.month, 1))
            },
        )

        SeasonalProjectsSection(projects = filteredProjects, timeZone = timeZone)
        PreschoolTemplateSection(
            people = people,
            routines = routines,
            tasks = tasks,
            onApplyTemplate = { template ->
                scope.launch {
                    val message =
                        applyPreschoolTemplate(
                            template = template,
                            people = people,
                            routines = routines,
                            tasks = tasks,
                            upsertRoutinesUseCase = upsertRoutinesUseCase,
                            createTaskUseCase = createTaskUseCase,
                        )
                    notifier.showToast(message)
                }
            },
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showOcrSheet) {
        OcrImportSheet(
            calendars = visibleCalendars,
            selectedCalendarId = selectedCalendarId,
            onCalendarSelected = { selectedCalendarId = it },
            onDismiss = { showOcrSheet = false },
        )
    }
}

@Composable
private fun PlanHeader(
    date: LocalDate,
    lensSelection: FamilyLensSelection,
    people: List<Person>,
    onLensChange: (FamilyLensSelection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Plan",
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "Brain dump, month view, and seasonal projects",
            style = XCalendarTheme.typography.bodyLarge,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDateFull(date),
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        FamilyLensMiniHeader(
            selection = lensSelection,
            people = people,
            onSelectionChange = onLensChange,
        )
    }
}

@Composable
internal fun BrainDumpSection(
    inboxItems: List<InboxItem>,
    captureText: String,
    onCaptureTextChanged: (String) -> Unit,
    onCapture: () -> Unit,
    onProcessAll: () -> Unit,
    onProcessItem: (InboxItem) -> Unit,
    onArchive: (InboxItem) -> Unit,
) {
    val newItems = remember(inboxItems) { inboxItems.filter { it.status == InboxStatus.NEW } }
    val processedItems = remember(inboxItems) { inboxItems.filter { it.status == InboxStatus.PROCESSED } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Brain Dump",
                style = XCalendarTheme.typography.titleLarge,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onProcessAll,
                enabled = newItems.isNotEmpty(),
                modifier = Modifier.semantics { contentDescription = "Process all new brain dump items" },
            ) {
                Text("Process suggestions")
            }
        }
        Text(
            text = "Capture first, organize later.",
            style = XCalendarTheme.typography.bodySmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = captureText,
            onValueChange = onCaptureTextChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Quick capture") },
            placeholder = { Text("Type or say it - e.g., Pick up preschool forms tomorrow") },
            maxLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = onCapture,
                enabled = captureText.trim().isNotEmpty(),
                modifier = Modifier.semantics { contentDescription = "Add capture to brain dump inbox" },
            ) {
                Text("Add to inbox")
            }
        }

        if (newItems.isEmpty() && processedItems.isEmpty()) {
            EmptyCard(message = "Inbox is clear. Capture thoughts as they pop up.")
        } else {
            if (newItems.isEmpty()) {
                EmptyCard(message = "No new captures. Processed items are listed below.")
            } else {
                newItems.forEach { item ->
                    InboxItemCard(
                        item = item,
                        onProcess = { onProcessItem(item) },
                        onArchive = { onArchive(item) },
                    )
                }
            }
            if (processedItems.isNotEmpty()) {
                Text(
                    text = "Processed",
                    style = XCalendarTheme.typography.titleMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                processedItems.forEach { item ->
                    InboxItemCard(
                        item = item,
                        onProcess = null,
                        onArchive = { onArchive(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxItemCard(
    item: InboxItem,
    onProcess: (() -> Unit)?,
    onArchive: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.rawText,
                style = XCalendarTheme.typography.bodyLarge,
                color = XCalendarTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(label = item.source.label())
                TagChip(label = item.status.label())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onProcess != null) {
                    TextButton(
                        onClick = onProcess,
                        modifier = Modifier.semantics { contentDescription = "Process ${item.rawText.take(20)}" },
                    ) { Text("Process") }
                }
                TextButton(onClick = { }) { Text("Edit") }
                TextButton(
                    onClick = onArchive,
                    modifier = Modifier.semantics { contentDescription = "Archive ${item.rawText.take(20)}" },
                ) { Text("Archive") }
            }
        }
    }
}

@Composable
private fun SuggestionSection(
    suggestionsByTask: Map<String, List<ScheduleSuggestion>>,
    tasksById: Map<String, Task>,
    peopleById: Map<String, com.debanshu.xcalendar.domain.model.Person>,
    timeZone: TimeZone,
    pendingUndo: PendingSuggestionUndo?,
    onAccept: (Task, ScheduleSuggestion, String) -> Unit,
    onNotNow: (Task) -> Unit,
    onUndo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Suggested slots",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        pendingUndo?.let { undo ->
            SuggestionUndoCard(
                pendingUndo = undo,
                timeZone = timeZone,
                onUndo = onUndo,
            )
        }

        if (suggestionsByTask.isEmpty()) {
            EmptyCard(message = "No suggestions yet. Add flexible tasks first.")
        } else {
            suggestionsByTask.forEach { (taskId, suggestions) ->
                val task = tasksById[taskId] ?: return@forEach
                SuggestionCard(
                    task = task,
                    suggestions = suggestions,
                    peopleById = peopleById,
                    timeZone = timeZone,
                    onAccept = onAccept,
                    onNotNow = { onNotNow(task) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    task: Task,
    suggestions: List<ScheduleSuggestion>,
    peopleById: Map<String, com.debanshu.xcalendar.domain.model.Person>,
    timeZone: TimeZone,
    onAccept: (Task, ScheduleSuggestion, String) -> Unit,
    onNotNow: () -> Unit,
) {
    val whoAffected =
        buildList {
            task.assignedToPersonId?.let { add(it) }
            addAll(task.affectedPersonIds)
        }.distinct().mapNotNull { peopleById[it]?.name }.joinToString(", ").ifBlank { null }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = task.title,
                style = XCalendarTheme.typography.titleMedium,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(label = task.priority.label())
                TagChip(label = task.energy.name.lowercase().replaceFirstChar { it.titlecase() })
            }
            whoAffected?.let { names ->
                Text(
                    text = "Who's affected: $names",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            suggestions.take(2).forEachIndexed { index, suggestion ->
                val slotLabel = slotLabel(index)
                Text(
                    text = "Slot $slotLabel: ${formatSuggestionLabel(suggestion, timeZone)}",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { onAccept(task, suggestion, slotLabel) },
                    modifier = Modifier.semantics { contentDescription = "Accept suggestion slot $slotLabel for ${task.title}" },
                ) {
                    Text("Accept slot $slotLabel")
                }
            }
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.semantics { contentDescription = "Dismiss suggestions for ${task.title} for now" },
            ) { Text("Not now") }
        }
    }
}

@Composable
private fun SuggestionUndoCard(
    pendingUndo: PendingSuggestionUndo,
    timeZone: TimeZone,
    onUndo: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Moved ${pendingUndo.previousTask.title} to ${formatSuggestionLabel(pendingUndo.acceptedSuggestion, timeZone)}",
                style = XCalendarTheme.typography.bodyMedium,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onUndo,
                modifier = Modifier.semantics { contentDescription = "Undo last suggestion move" },
            ) {
                Text("Undo")
            }
        }
    }
}

@Composable
private fun MonthOverviewSection(
    month: com.debanshu.xcalendar.common.model.YearMonth,
    events: ImmutableList<Event>,
    onDateClick: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Month overview",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            MonthView(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                month = month,
                events = events,
                holidays = persistentListOf(),
                isVisible = true,
                onDayClick = onDateClick,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun YearAlertsSection(
    anchorMonth: com.debanshu.xcalendar.common.model.YearMonth,
    events: ImmutableList<Event>,
    tasks: List<Task>,
    projects: List<Project>,
    timeZone: TimeZone,
    onOpenMonth: (com.debanshu.xcalendar.common.model.YearMonth) -> Unit,
) {
    val timelineMonths = remember(anchorMonth) { (0 until 12).map { anchorMonth.plusMonths(it) } }
    val highlights = remember(events, tasks, projects, timelineMonths, timeZone) {
        buildYearHighlights(
            events = events,
            tasks = tasks,
            projects = projects,
            months = timelineMonths,
            timeZone = timeZone,
        )
    }
    val summaryByMonth = remember(timelineMonths, highlights) {
        timelineMonths.map { month ->
            val items = highlights.filter { it.month == month }
            YearAlertSummary(
                month = month,
                alertCount = items.size,
                preview = items.firstOrNull()?.title,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Year alerts",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "Big items over the next 12 months.",
            style = XCalendarTheme.typography.bodySmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        val activeMonths = summaryByMonth.filter { it.alertCount > 0 }
        if (activeMonths.isEmpty()) {
            EmptyCard(message = "No major alerts in the next 12 months.")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activeMonths.forEach { summary ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        TextButton(onClick = { onOpenMonth(summary.month) }) {
                            Text("${formatMonthLabel(summary.month)} (${summary.alertCount})")
                        }
                    }
                }
            }
            highlights.take(5).forEach { item ->
                Text(
                    text = "${formatAlertDate(item.timeMillis, timeZone)} - ${item.title} (${item.kind})",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SeasonalProjectsSection(
    projects: List<Project>,
    timeZone: TimeZone,
) {
    val activeProjects = remember(projects) { projects.filter { it.status == ProjectStatus.ACTIVE } }
    val seasonalProjects = remember(activeProjects) {
        activeProjects
            .filter { !it.seasonLabel.isNullOrBlank() }
            .sortedWith(
                compareBy<Project>(
                    { seasonSortRank(it.seasonLabel) },
                    { it.startAt ?: Long.MAX_VALUE },
                    { it.title.lowercase() },
                ),
            )
    }
    val seasonalCounts = remember(seasonalProjects) {
        seasonalProjects
            .groupBy { normalizedSeasonLabel(it.seasonLabel.orEmpty()) }
            .mapValues { (_, grouped) -> grouped.size }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Seasonal projects",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (seasonalProjects.isEmpty()) {
            EmptyCard(message = "No seasonal projects yet.")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                seasonalCounts.forEach { (season, count) ->
                    TagChip(label = "$season - $count")
                }
            }
            seasonalProjects.forEach { project ->
                ProjectCard(project = project, timeZone = timeZone)
            }
        }
    }
}

@Composable
private fun PreschoolTemplateSection(
    people: List<Person>,
    routines: List<Routine>,
    tasks: List<Task>,
    onApplyTemplate: (PreschoolTemplate) -> Unit,
) {
    val preschoolChildId = remember(people) { people.firstOrNull { it.role == PersonRole.CHILD && it.ageYears == 4 }?.id }
    val existingRoutineIds = remember(routines) { routines.map { it.id }.toSet() }
    val existingTaskIds = remember(tasks) { tasks.map { it.id }.toSet() }
    val templates = remember(preschoolChildId) { preschoolTemplates(preschoolChildId) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Preschool templates",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "One tap to add morning, bedtime, and pickup checklist routines.",
            style = XCalendarTheme.typography.bodySmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        if (preschoolChildId == null) {
            EmptyCard(message = "Add a 4-year-old child profile to use preschool templates.")
        } else {
            templates.forEach { template ->
                val isApplied = template.isApplied(existingRoutineIds, existingTaskIds)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = template.title,
                            style = XCalendarTheme.typography.titleMedium,
                            color = XCalendarTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = template.description,
                            style = XCalendarTheme.typography.bodySmall,
                            color = XCalendarTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isApplied) "Added" else "Not added",
                                style = XCalendarTheme.typography.labelMedium,
                                color = if (isApplied) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = { onApplyTemplate(template) },
                                enabled = !isApplied,
                            ) {
                                Text(if (isApplied) "Added" else "Add template")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanScheduleSection(
    onScan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Scan schedule",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Import a school calendar photo and confirm the events.",
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onScan) {
                    Text("Scan schedule")
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    timeZone: TimeZone,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = project.title,
                style = XCalendarTheme.typography.titleMedium,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            project.seasonLabel?.let { TagChip(label = normalizedSeasonLabel(it)) }
            formatProjectWindow(project, timeZone)?.let { window ->
                Text(
                    text = "Window: $window",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            val notes = project.notes
            if (!notes.isNullOrBlank()) {
                Text(
                    text = notes,
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = XCalendarTheme.typography.labelMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun InboxSource.label(): String =
    when (this) {
        InboxSource.TEXT -> "Text"
        InboxSource.VOICE -> "Voice"
        InboxSource.OCR -> "OCR"
    }

private fun InboxStatus.label(): String =
    when (this) {
        InboxStatus.NEW -> "New"
        InboxStatus.PROCESSED -> "Processed"
        InboxStatus.ARCHIVED -> "Archived"
    }

private fun TaskPriority.label(): String =
    when (this) {
        TaskPriority.MUST -> "Must"
        TaskPriority.SHOULD -> "Should"
        TaskPriority.NICE -> "Nice"
    }

private fun formatSuggestionLabel(suggestion: ScheduleSuggestion, timeZone: TimeZone): String {
    val start = suggestion.startTime.toLocalDateTime(timeZone)
    val end = suggestion.endTime.toLocalDateTime(timeZone)
    val day = start.date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() }
    val date = "${start.date.month.name.take(3).lowercase().replaceFirstChar { it.titlecase() }} ${start.date.day}"
    val time = DateTimeFormatter.formatCompactTimeRange(start, end)
    return "$day $date • $time"
}

private fun formatDateFull(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$dayOfWeek, ${date.day} $month"
}

private fun slotLabel(index: Int): String =
    when (index) {
        0 -> "A"
        1 -> "B"
        else -> (index + 1).toString()
    }

private data class YearAlertSummary(
    val month: com.debanshu.xcalendar.common.model.YearMonth,
    val alertCount: Int,
    val preview: String?,
)

private data class YearHighlight(
    val month: com.debanshu.xcalendar.common.model.YearMonth,
    val timeMillis: Long,
    val title: String,
    val kind: String,
)

private enum class PreschoolTemplateType {
    MORNING_ROUTINE,
    BEDTIME_ROUTINE,
    PICKUP_CHECKLIST,
}

private data class PreschoolTemplate(
    val type: PreschoolTemplateType,
    val title: String,
    val description: String,
    val routineIds: List<String> = emptyList(),
    val taskIds: List<String> = emptyList(),
)

private fun preschoolTemplates(preschoolChildId: String?): List<PreschoolTemplate> {
    val morningRoutineId = scopedTemplateId("routine_preschool_morning", preschoolChildId)
    val bedtimeRoutineId = scopedTemplateId("routine_preschool_bedtime", preschoolChildId)
    val pickupTaskIds =
        listOf(
            scopedTemplateId("task_preschool_pickup_snack", preschoolChildId),
            scopedTemplateId("task_preschool_pickup_water", preschoolChildId),
            scopedTemplateId("task_preschool_pickup_clothes", preschoolChildId),
        )
    return listOf(
        PreschoolTemplate(
            type = PreschoolTemplateType.MORNING_ROUTINE,
            title = "Morning routine",
            description = "Daily start sequence for preschool mornings.",
            routineIds = listOf(morningRoutineId),
        ),
        PreschoolTemplate(
            type = PreschoolTemplateType.BEDTIME_ROUTINE,
            title = "Bedtime routine",
            description = "Simple evening wind-down routine for school nights.",
            routineIds = listOf(bedtimeRoutineId),
        ),
        PreschoolTemplate(
            type = PreschoolTemplateType.PICKUP_CHECKLIST,
            title = "Pickup checklist",
            description = "Creates reusable pickup prep tasks.",
            taskIds = pickupTaskIds,
        ),
    )
}

private fun PreschoolTemplate.isApplied(
    existingRoutineIds: Set<String>,
    existingTaskIds: Set<String>,
): Boolean {
    val routinesReady = routineIds.isEmpty() || routineIds.all(existingRoutineIds::contains)
    val tasksReady = taskIds.isEmpty() || taskIds.all(existingTaskIds::contains)
    return routinesReady && tasksReady
}

private suspend fun applyPreschoolTemplate(
    template: PreschoolTemplate,
    people: List<Person>,
    routines: List<Routine>,
    tasks: List<Task>,
    upsertRoutinesUseCase: UpsertRoutinesUseCase,
    createTaskUseCase: CreateTaskUseCase,
): String {
    val preschoolChildId = people.firstOrNull { it.role == PersonRole.CHILD && it.ageYears == 4 }?.id
        ?: return "Add a 4-year-old child profile first."
    val momId = people.firstOrNull { it.role == PersonRole.MOM }?.id
    val existingRoutineIds = routines.map { it.id }.toSet()
    val existingTaskIds = tasks.map { it.id }.toSet()
    if (template.isApplied(existingRoutineIds, existingTaskIds)) {
        return "${template.title} already added."
    }

    val now = Clock.System.now().toEpochMilliseconds()
    return when (template.type) {
        PreschoolTemplateType.MORNING_ROUTINE -> {
            val routineId = template.routineIds.firstOrNull() ?: return "Template is misconfigured."
            upsertRoutinesUseCase(
                Routine(
                    id = routineId,
                    title = "Preschool morning routine",
                    notes = "Get dressed -> Breakfast -> Pack backpack -> Shoes and water bottle",
                    timeOfDay = RoutineTimeOfDay.MORNING,
                    recurrenceRule = "FREQ=DAILY",
                    assignedToPersonId = preschoolChildId,
                    sortOrder = 10,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            "Added preschool morning routine."
        }

        PreschoolTemplateType.BEDTIME_ROUTINE -> {
            val routineId = template.routineIds.firstOrNull() ?: return "Template is misconfigured."
            upsertRoutinesUseCase(
                Routine(
                    id = routineId,
                    title = "Preschool bedtime routine",
                    notes = "Bath -> Pajamas -> Story -> Lights out",
                    timeOfDay = RoutineTimeOfDay.EVENING,
                    recurrenceRule = "FREQ=DAILY",
                    assignedToPersonId = preschoolChildId,
                    sortOrder = 20,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            "Added preschool bedtime routine."
        }

        PreschoolTemplateType.PICKUP_CHECKLIST -> {
            val specs =
                listOf(
                    template.taskIds.getOrNull(0) to "Preschool pickup: Pack snack",
                    template.taskIds.getOrNull(1) to "Preschool pickup: Fill water bottle",
                    template.taskIds.getOrNull(2) to "Preschool pickup: Bring change of clothes",
                ).mapNotNull { (id, title) -> id?.let { it to title } }
            val newTasks =
                specs
                    .filterNot { (id, _) -> existingTaskIds.contains(id) }
                    .mapIndexed { index, (id, title) ->
                        Task(
                            id = id,
                            title = title,
                            notes = "Template checklist item",
                            priority = TaskPriority.SHOULD,
                            energy = TaskEnergy.LOW,
                            type = TaskType.ROUTINE,
                            assignedToPersonId = momId,
                            affectedPersonIds = listOf(preschoolChildId),
                            createdAt = now + index,
                            updatedAt = now + index,
                        )
                    }
            if (newTasks.isEmpty()) {
                return "Pickup checklist already added."
            }
            newTasks.forEach { createTaskUseCase(it) }
            "Added ${newTasks.size} pickup checklist task(s)."
        }
    }
}

private fun scopedTemplateId(
    baseId: String,
    personId: String?,
): String = if (personId.isNullOrBlank()) baseId else "${baseId}_$personId"

private fun buildYearHighlights(
    events: List<Event>,
    tasks: List<Task>,
    projects: List<Project>,
    months: List<com.debanshu.xcalendar.common.model.YearMonth>,
    timeZone: TimeZone,
): List<YearHighlight> {
    val validMonths = months.toSet()
    val monthOrder = months.mapIndexed { index, month -> month to index }.toMap()
    val highlights = mutableListOf<YearHighlight>()

    events.forEach { event ->
        val month = com.debanshu.xcalendar.common.model.YearMonth.from(event.startTime.toLocalDateTime(timeZone).date)
        if (month in validMonths) {
            highlights += YearHighlight(month, event.startTime, event.title, "Event")
        }
    }

    tasks.filter { it.status == TaskStatus.OPEN }.forEach { task ->
        val time = task.dueAt ?: task.scheduledStart ?: return@forEach
        val month = com.debanshu.xcalendar.common.model.YearMonth.from(time.toLocalDateTime(timeZone).date)
        if (month in validMonths) {
            val kind = if (task.priority == TaskPriority.MUST) "Must task" else "Task"
            highlights += YearHighlight(month, time, task.title, kind)
        }
    }

    projects.filter { it.status == ProjectStatus.ACTIVE }.forEach { project ->
        project.startAt?.let { startAt ->
            val month = com.debanshu.xcalendar.common.model.YearMonth.from(startAt.toLocalDateTime(timeZone).date)
            if (month in validMonths) {
                highlights += YearHighlight(month, startAt, project.title, "Project start")
            }
        }
        project.endAt?.let { endAt ->
            val month = com.debanshu.xcalendar.common.model.YearMonth.from(endAt.toLocalDateTime(timeZone).date)
            if (month in validMonths) {
                highlights += YearHighlight(month, endAt, project.title, "Project milestone")
            }
        }
    }

    return highlights.sortedWith(
        compareBy<YearHighlight>(
            { monthOrder[it.month] ?: Int.MAX_VALUE },
            { it.timeMillis },
            { it.title.lowercase() },
        ),
    )
}

private fun formatMonthLabel(month: com.debanshu.xcalendar.common.model.YearMonth): String {
    val monthLabel = month.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$monthLabel ${month.year}"
}

private fun formatAlertDate(
    timeMillis: Long,
    timeZone: TimeZone,
): String {
    val date = timeMillis.toLocalDateTime(timeZone).date
    val month = date.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$month ${date.day}"
}

private fun seasonSortRank(label: String?): Int {
    val value = label.orEmpty().trim().lowercase()
    return when {
        value.contains("spring") -> 0
        value.contains("summer") -> 1
        value.contains("fall") || value.contains("autumn") -> 2
        value.contains("winter") -> 3
        else -> 4
    }
}

private fun normalizedSeasonLabel(label: String): String {
    val value = label.trim().lowercase()
    return when {
        value.contains("spring") -> "Spring"
        value.contains("summer") -> "Summer"
        value.contains("fall") || value.contains("autumn") -> "Fall"
        value.contains("winter") -> "Winter"
        else -> label.ifBlank { "Seasonal" }
    }
}

private fun formatProjectWindow(
    project: Project,
    timeZone: TimeZone,
): String? {
    val start = project.startAt?.let { formatAlertDate(it, timeZone) }
    val end = project.endAt?.let { formatAlertDate(it, timeZone) }
    return when {
        start != null && end != null -> "$start - $end"
        start != null -> "Starts $start"
        end != null -> "Due $end"
        else -> null
    }
}

private data class PendingSuggestionUndo(
    val previousTask: Task,
    val acceptedSuggestion: ScheduleSuggestion,
)
