package com.debanshu.xcalendar.ui.screen.planScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.InboxItem
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.domain.model.Project
import com.debanshu.xcalendar.domain.model.ProjectStatus
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.ScheduleSuggestion
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.StructureBrainDumpUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.GetInboxItemsUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.UpdateInboxItemStatusUseCase
import com.debanshu.xcalendar.domain.usecase.project.GetProjectsUseCase
import com.debanshu.xcalendar.domain.usecase.task.CreateTaskUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.task.UpdateTaskScheduleUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.ui.screen.monthScreen.components.MonthView
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
@OptIn(ExperimentalUuidApi::class)
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
    val getProjectsUseCase = koinInject<GetProjectsUseCase>()
    val getTasksUseCase = koinInject<GetTasksUseCase>()
    val updateInboxItemStatusUseCase = koinInject<UpdateInboxItemStatusUseCase>()
    val updateTaskScheduleUseCase = koinInject<UpdateTaskScheduleUseCase>()
    val getUserCalendarsUseCase = koinInject<GetUserCalendarsUseCase>()
    val getCurrentUserUseCase = koinInject<GetCurrentUserUseCase>()
    val createTaskUseCase = koinInject<CreateTaskUseCase>()
    val structureBrainDumpUseCase = koinInject<StructureBrainDumpUseCase>()

    val userId = remember { getCurrentUserUseCase() }
    val calendars by remember { getUserCalendarsUseCase(userId) }.collectAsState(initial = emptyList())
    val visibleCalendars = remember(calendars) { calendars.filter { it.isVisible } }
    var selectedCalendarId by rememberSaveable { mutableStateOf<String?>(null) }
    var showOcrSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(visibleCalendars) {
        if (selectedCalendarId == null && visibleCalendars.isNotEmpty()) {
            selectedCalendarId = visibleCalendars.first().id
        }
    }

    val inboxItems by remember { getInboxItemsUseCase() }.collectAsState(initial = emptyList())
    val projects by remember { getProjectsUseCase() }.collectAsState(initial = emptyList())
    val tasks by remember { getTasksUseCase() }.collectAsState(initial = emptyList())

    val suggestions = remember(events, tasks, nowMillis, timeZone) {
        ScheduleEngine.aggregate(
            events = events,
            tasks = tasks,
            filter = ScheduleFilter(),
            nowMillis = nowMillis,
            timeZone = timeZone,
        ).suggestions
    }

    val suggestionsByTask = remember(suggestions) { suggestions.groupBy { it.taskId } }
    val tasksById = remember(tasks) { tasks.associateBy { it.id } }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlanHeader(date = dateState.selectedDate)

        BrainDumpSection(
            inboxItems = inboxItems,
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
            onSuggest = { item ->
                scope.launch {
                    updateInboxItemStatusUseCase(item, InboxStatus.PROCESSED)
                }
            },
            onArchive = { item ->
                scope.launch {
                    updateInboxItemStatusUseCase(item, InboxStatus.ARCHIVED)
                }
            },
        )

        ScanScheduleSection(
            onScan = { showOcrSheet = true },
        )

        SuggestionSection(
            suggestionsByTask = suggestionsByTask,
            tasksById = tasksById,
            timeZone = timeZone,
            onAccept = { task, suggestion ->
                scope.launch {
                    updateTaskScheduleUseCase(task, suggestion.startTime, suggestion.endTime)
                }
            },
        )

        MonthOverviewSection(
            month = dateState.selectedInViewMonth,
            events = events,
            onDateClick = { date ->
                dateStateHolder.updateSelectedDateState(date)
            },
        )

        SeasonalProjectsSection(projects = projects)
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
private fun PlanHeader(date: LocalDate) {
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
    }
}

@Composable
private fun BrainDumpSection(
    inboxItems: List<InboxItem>,
    onProcessAll: () -> Unit,
    onSuggest: (InboxItem) -> Unit,
    onArchive: (InboxItem) -> Unit,
) {
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
            TextButton(onClick = onProcessAll) {
                Text("Process suggestions")
            }
        }

        if (inboxItems.isEmpty()) {
            EmptyCard(message = "Inbox is clear. Capture thoughts as they pop up.")
        } else {
            inboxItems.forEach { item ->
                InboxItemCard(item = item, onSuggest = { onSuggest(item) }, onArchive = { onArchive(item) })
            }
        }
    }
}

@Composable
private fun InboxItemCard(
    item: InboxItem,
    onSuggest: () -> Unit,
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
                TextButton(onClick = onSuggest) { Text("Suggest") }
                TextButton(onClick = { }) { Text("Edit") }
                TextButton(onClick = onArchive) { Text("Archive") }
            }
        }
    }
}

@Composable
private fun SuggestionSection(
    suggestionsByTask: Map<String, List<ScheduleSuggestion>>,
    tasksById: Map<String, Task>,
    timeZone: TimeZone,
    onAccept: (Task, ScheduleSuggestion) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Suggested slots",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )

        if (suggestionsByTask.isEmpty()) {
            EmptyCard(message = "No suggestions yet. Add flexible tasks first.")
        } else {
            suggestionsByTask.forEach { (taskId, suggestions) ->
                val task = tasksById[taskId] ?: return@forEach
                SuggestionCard(task = task, suggestions = suggestions, timeZone = timeZone, onAccept = onAccept)
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    task: Task,
    suggestions: List<ScheduleSuggestion>,
    timeZone: TimeZone,
    onAccept: (Task, ScheduleSuggestion) -> Unit,
) {
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
            suggestions.take(2).forEach { suggestion ->
                TextButton(onClick = { onAccept(task, suggestion) }) {
                    Text("Accept ${formatSuggestionLabel(suggestion, timeZone)}")
                }
            }
            TextButton(onClick = { }) { Text("Not now") }
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
private fun SeasonalProjectsSection(projects: List<Project>) {
    val seasonalProjects = remember(projects) {
        projects.filter { it.status == ProjectStatus.ACTIVE && !it.seasonLabel.isNullOrBlank() }
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
            seasonalProjects.forEach { project ->
                ProjectCard(project = project)
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
private fun ProjectCard(project: Project) {
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
        project.seasonLabel?.let { TagChip(label = it) }
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
            TextButton(onClick = { }) { Text("Open") }
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
