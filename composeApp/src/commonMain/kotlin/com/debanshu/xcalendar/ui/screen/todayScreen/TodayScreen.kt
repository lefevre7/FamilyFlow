package com.debanshu.xcalendar.ui.screen.todayScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.common.toLocalDateTime
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.FamilyLensSelection
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.Routine
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.ScheduleItem
import com.debanshu.xcalendar.domain.model.ScheduleSource
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.domain.usecase.event.UpdateEventUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.ProcessVoiceNoteUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.VoiceNoteProcessResult
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.routine.GetRoutinesUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.usecase.task.UpdateTaskUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.platform.rememberVoiceCaptureController
import com.debanshu.xcalendar.ui.components.FamilyLensMiniHeader
import com.debanshu.xcalendar.ui.components.core.ScheduleHolidayTag
import com.debanshu.xcalendar.ui.state.ActiveTimer
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import com.debanshu.xcalendar.ui.state.TimerStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform
import kotlin.time.Clock
import com.debanshu.xcalendar.domain.model.effectivePersonId

private const val NOW_WINDOW_MINUTES = 30
private const val MAX_VISIBLE_PER_SECTION = 3
private const val SNOOZE_MINUTES = 30

private enum class DaySection {
    MORNING,
    AFTERNOON,
    EVENING,
}

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    dateStateHolder: DateStateHolder,
    events: ImmutableList<Event>,
    holidays: ImmutableList<Holiday>,
    isVisible: Boolean = true,
    onEventClick: (Event) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    if (!isVisible) return

    val dateState by dateStateHolder.currentDateState.collectAsState()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val nowHour = nowMillis.toLocalDateTime(timeZone).hour
    val scope = rememberCoroutineScope()

    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val getTasksUseCase = koinInject<GetTasksUseCase>()
    val getRoutinesUseCase = koinInject<GetRoutinesUseCase>()
    val processVoiceNoteUseCase = koinInject<ProcessVoiceNoteUseCase>()
    val timerStateHolder = koinInject<TimerStateHolder>()
    val lensStateHolder = koinInject<LensStateHolder>()
    val conflictStateHolder = koinInject<SyncConflictStateHolder>()
    val notifier = koinInject<PlatformNotifier>()
    val haptic = LocalHapticFeedback.current

    val peopleFlow = remember { getPeopleUseCase() }
    val tasksFlow = remember { getTasksUseCase() }
    val routinesFlow = remember { getRoutinesUseCase() }

    val people by peopleFlow.collectAsState(initial = emptyList())
    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val routines by routinesFlow.collectAsState(initial = emptyList())
    val conflicts by conflictStateHolder.conflicts.collectAsState()
    val timerState by timerStateHolder.timer.collectAsState()
    val lensSelection by lensStateHolder.selection.collectAsState()
    var remainingMillis by remember(timerState?.endsAt) {
        mutableStateOf(timerState?.endsAt?.minus(System.currentTimeMillis()) ?: 0L)
    }

    LaunchedEffect(timerState?.endsAt) {
        val active = timerState ?: return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val remaining = active.endsAt - now
            if (remaining <= 0L) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                notifier.showToast("Timer done")
                timerStateHolder.stopTimer()
                break
            }
            remainingMillis = remaining
            delay(1_000L)
        }
    }

    val momId = remember(people) { people.firstOrNull { it.role == PersonRole.MOM }?.id }
    var todayOnly by rememberSaveable { mutableStateOf(false) }
    var dismissedEventIds by rememberSaveable(dateState.selectedDate) { mutableStateOf(emptyList<String>()) }
    var isVoiceProcessing by rememberSaveable { mutableStateOf(false) }

    val voiceController = rememberVoiceCaptureController(
        onResult = { text ->
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) {
                scope.launch {
                    isVoiceProcessing = true
                    notifier.showToast("Captured. Processing voice note...")
                    try {
                        when (
                            val result =
                                processVoiceNoteUseCase(
                                    rawText = trimmed,
                                    source = VoiceCaptureSource.TODAY_QUICK_CAPTURE,
                                    personId = momId,
                                )
                        ) {
                            is VoiceNoteProcessResult.Success -> {
                                val count = result.taskCount
                                val baseMessage =
                                    if (count == 1) {
                                        "Added 1 task from voice note"
                                    } else {
                                        "Added $count tasks from voice note"
                                    }
                                notifier.showToast(
                                    if (result.usedHeuristicFallback) {
                                        "$baseMessage using heuristic fallback"
                                    } else {
                                        baseMessage
                                    },
                                )
                            }

                            is VoiceNoteProcessResult.Failure -> {
                                notifier.showToast(result.reason.userMessage)
                            }
                        }
                    } finally {
                        isVoiceProcessing = false
                    }
                }
            } else {
                notifier.showToast("Didn't catch that. Try again.")
            }
        },
        onError = { message ->
            isVoiceProcessing = false
            notifier.showToast(message)
        }
    )

    val isToday = dateState.selectedDate == dateState.currentDate
    LaunchedEffect(isToday) {
        if (!isToday) {
            todayOnly = false
        }
    }

    val (dayStart, dayEnd) = remember(dateState.selectedDate, timeZone) {
        val start = dateState.selectedDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = start + 24 * 60 * 60 * 1000L
        start to end
    }

    val eventsForDay = remember(events, dayStart, dayEnd) {
        events.filter { overlaps(it.startTime, it.endTime, dayStart, dayEnd) }
    }

    val tasksForDay = remember(tasks, dayStart, dayEnd) {
        tasks
            .filter { it.status == TaskStatus.OPEN }
            .filter { task ->
                val scheduled = task.scheduledStart
                val scheduledEnd = task.scheduledEnd
                if (scheduled != null && scheduledEnd != null) {
                    overlaps(scheduled, scheduledEnd, dayStart, dayEnd)
                } else {
                    true
                }
            }
    }

    val holidaysForDay = remember(holidays, dateState.selectedDate, timeZone) {
        holidays.filter { holiday ->
            holiday.date.toLocalDateTime(timeZone).date == dateState.selectedDate
        }
    }

    val filter = remember(lensSelection, momId) {
        ScheduleFilter(
            personId = lensSelection.effectivePersonId(momId),
            onlyMust = false,
            includeUnassignedEvents = true,
            nowWindowMinutes = null,
        )
    }

    val aggregation = remember(eventsForDay, tasksForDay, filter, nowMillis) {
        ScheduleEngine.aggregate(
            events = eventsForDay,
            tasks = tasksForDay,
            filter = filter,
            nowMillis = nowMillis,
            timeZone = timeZone,
        )
    }

    val scheduleItems = remember(aggregation.items, todayOnly, isToday, nowMillis, dismissedEventIds) {
        val activeItems =
            aggregation.items.filterNot { item ->
                item.source == ScheduleSource.EVENT && dismissedEventIds.contains(item.id)
            }
        if (todayOnly && isToday) {
            activeItems.filter { shouldShowInTodayOnly(item = it, nowMillis = nowMillis) }
        } else {
            activeItems
        }
    }

    val groupedItems = remember(scheduleItems, nowHour, timeZone) {
        scheduleItems.groupBy { sectionForItem(it, nowHour, timeZone) }
    }

    val activeRoutines = remember(routines) {
        routines.filter { it.isActive }.sortedBy { it.sortOrder }
    }

    val peopleById = remember(people) { people.associateBy { it.id } }
    val snapshotText = remember(dateState.selectedDate, scheduleItems, peopleById, timeZone) {
        buildTodaySnapshotText(
            date = dateState.selectedDate,
            items = scheduleItems,
            peopleById = peopleById,
            timeZone = timeZone,
        )
    }

    val onDoneItem: (ScheduleItem) -> Unit = remember(scope, notifier) {
        { item ->
            when (item.source) {
                ScheduleSource.TASK -> {
                    val task = item.originalTask
                    if (task != null) {
                        val updateTaskUseCase =
                            runCatching { KoinPlatform.getKoin().get<UpdateTaskUseCase>() }.getOrNull()
                        if (updateTaskUseCase == null) {
                            notifier.showToast("Done action unavailable")
                        } else {
                            scope.launch {
                                val updatedAt = Clock.System.now().toEpochMilliseconds()
                                updateTaskUseCase(task.copy(status = TaskStatus.DONE, updatedAt = updatedAt))
                                notifier.showToast("Marked done")
                            }
                        }
                    }
                }
                ScheduleSource.EVENT -> {
                    dismissedEventIds = (dismissedEventIds + item.id).distinct()
                    notifier.showToast("Hidden from today")
                }
            }
        }
    }

    val onSnoozeItem: (ScheduleItem) -> Unit =
        remember(scope, notifier) {
            { item ->
                val snoozeMillis = SNOOZE_MINUTES * 60_000L
                when (item.source) {
                    ScheduleSource.TASK -> {
                        val task = item.originalTask
                        if (task != null) {
                            val updateTaskUseCase =
                                runCatching { KoinPlatform.getKoin().get<UpdateTaskUseCase>() }.getOrNull()
                            if (updateTaskUseCase == null) {
                                notifier.showToast("Snooze action unavailable")
                            } else {
                                scope.launch {
                                    val actionNow = Clock.System.now().toEpochMilliseconds()
                                    val scheduledStart = task.scheduledStart
                                    val scheduledEnd = task.scheduledEnd
                                    val hasSchedule = scheduledStart != null && scheduledEnd != null
                                    val nextStart =
                                        if (hasSchedule) {
                                            scheduledStart + snoozeMillis
                                        } else {
                                            actionNow + snoozeMillis
                                        }
                                    val nextEnd =
                                        if (hasSchedule) {
                                            scheduledEnd + snoozeMillis
                                        } else {
                                            nextStart + task.durationMinutes * 60_000L
                                        }
                                    updateTaskUseCase(
                                        task.copy(
                                            status = TaskStatus.OPEN,
                                            scheduledStart = nextStart,
                                            scheduledEnd = nextEnd,
                                            updatedAt = actionNow,
                                        ),
                                    )
                                    notifier.showToast("Snoozed $SNOOZE_MINUTES minutes")
                                }
                            }
                        }
                    }
                    ScheduleSource.EVENT -> {
                        val event = item.originalEvent
                        if (event != null) {
                            if (event.isAllDay) {
                                notifier.showToast("All-day events cannot be snoozed")
                            } else {
                                val updateEventUseCase =
                                    runCatching { KoinPlatform.getKoin().get<UpdateEventUseCase>() }.getOrNull()
                                if (updateEventUseCase == null) {
                                    notifier.showToast("Snooze action unavailable")
                                } else {
                                    scope.launch {
                                        val result =
                                            updateEventUseCase(
                                                event.copy(
                                                    startTime = event.startTime + snoozeMillis,
                                                    endTime = event.endTime + snoozeMillis,
                                                ),
                                            )
                                        when (result) {
                                            is com.debanshu.xcalendar.domain.util.DomainResult.Success ->
                                                notifier.showToast("Snoozed $SNOOZE_MINUTES minutes")
                                            is com.debanshu.xcalendar.domain.util.DomainResult.Error ->
                                                notifier.showToast(result.error.message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    val onShareItem: (ScheduleItem) -> Unit = remember(notifier, peopleById, timeZone) {
        { item ->
            notifier.shareText(
                subject = "ADHD MOM - Today item",
                text = buildItemShareText(item = item, peopleById = peopleById, timeZone = timeZone),
            )
        }
    }

    var morningExpanded by rememberSaveable(dateState.selectedDate) { mutableStateOf(false) }
    var afternoonExpanded by rememberSaveable(dateState.selectedDate) { mutableStateOf(false) }
    var eveningExpanded by rememberSaveable(dateState.selectedDate) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (timerState != null) {
            ActiveTimerCard(
                timer = timerState,
                remainingMillis = remainingMillis,
                onStop = { timerStateHolder.stopTimer() },
            )
        }
        TodayHeader(
            date = dateState.selectedDate,
            lensSelection = lensSelection,
            people = people,
            onLensChange = lensStateHolder::updateSelection,
            todayOnly = todayOnly,
            onTodayOnlyChange = { todayOnly = it },
            todayOnlyEnabled = isToday,
            onShareSnapshot = {
                notifier.shareText(
                    subject = "ADHD MOM - Today's snapshot",
                    text = snapshotText,
                )
            },
            onQuickCapture = if (PlatformFeatures.voiceCapture.supported && voiceController.isAvailable) {
                { voiceController.start() }
            } else null,
            isVoiceProcessing = isVoiceProcessing,
        )

        if (holidaysForDay.isNotEmpty()) {
            HolidaySection(holidays = holidaysForDay)
        }

        if (conflicts.isNotEmpty()) {
            ConflictReviewBanner(
                conflictCount = conflicts.size,
                onOpenSettings = onNavigateToSettings,
            )
        }

        if (activeRoutines.isNotEmpty()) {
            RoutineSection(routines = activeRoutines)
        }

        if (todayOnly) {
            NowSection(
                items = scheduleItems,
                peopleById = peopleById,
                onDone = onDoneItem,
                onSnooze = onSnoozeItem,
                onShare = onShareItem,
                onEventClick = onEventClick,
            )
        } else {
            val morningItems = groupedItems[DaySection.MORNING].orEmpty()
            val afternoonItems = groupedItems[DaySection.AFTERNOON].orEmpty()
            val eveningItems = groupedItems[DaySection.EVENING].orEmpty()

            val hasItems = morningItems.isNotEmpty() || afternoonItems.isNotEmpty() || eveningItems.isNotEmpty()
            if (!hasItems) {
                EmptySection(message = "You are clear for today.")
            } else {
                if (morningItems.isNotEmpty()) {
                    DaySectionGroup(
                        title = "Morning",
                        items = morningItems,
                        expanded = morningExpanded,
                        onToggleExpand = { morningExpanded = !morningExpanded },
                        peopleById = peopleById,
                        onDone = onDoneItem,
                        onSnooze = onSnoozeItem,
                        onShare = onShareItem,
                        onEventClick = onEventClick,
                    )
                }
                if (afternoonItems.isNotEmpty()) {
                    DaySectionGroup(
                        title = "Afternoon",
                        items = afternoonItems,
                        expanded = afternoonExpanded,
                        onToggleExpand = { afternoonExpanded = !afternoonExpanded },
                        peopleById = peopleById,
                        onDone = onDoneItem,
                        onSnooze = onSnoozeItem,
                        onShare = onShareItem,
                        onEventClick = onEventClick,
                    )
                }
                if (eveningItems.isNotEmpty()) {
                    DaySectionGroup(
                        title = "Evening",
                        items = eveningItems,
                        expanded = eveningExpanded,
                        onToggleExpand = { eveningExpanded = !eveningExpanded },
                        peopleById = peopleById,
                        onDone = onDoneItem,
                        onSnooze = onSnoozeItem,
                        onShare = onShareItem,
                        onEventClick = onEventClick,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun HolidaySection(holidays: List<Holiday>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Holidays",
            style = XCalendarTheme.typography.titleMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        holidays.forEach { holiday ->
            ScheduleHolidayTag(name = holiday.name)
        }
    }
}

@Composable
private fun TodayHeader(
    date: LocalDate,
    lensSelection: FamilyLensSelection,
    people: List<Person>,
    onLensChange: (FamilyLensSelection) -> Unit,
    todayOnly: Boolean,
    onTodayOnlyChange: (Boolean) -> Unit,
    todayOnlyEnabled: Boolean,
    onShareSnapshot: () -> Unit,
    onQuickCapture: (() -> Unit)? = null,
    isVoiceProcessing: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Today",
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = formatFullDate(date),
            style = XCalendarTheme.typography.bodyLarge,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        FamilyLensMiniHeader(
            selection = lensSelection,
            people = people,
            onSelectionChange = onLensChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { onTodayOnlyChange(!todayOnly) },
                enabled = todayOnlyEnabled,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (todayOnly) {
                                "Today only filter on. Double tap to show the full day."
                            } else {
                                "Today only filter off. Double tap to show only items near now."
                            }
                    },
            ) {
                Text(if (todayOnly) "Today Only: On" else "Today Only: Off")
            }
            if (onQuickCapture != null) {
                TextButton(
                    onClick = onQuickCapture,
                    enabled = !isVoiceProcessing,
                    modifier = Modifier.semantics {
                        contentDescription = if (isVoiceProcessing) {
                            "Quick voice capture in progress"
                        } else {
                            "Quick voice capture. Double tap to say it."
                        }
                    },
                ) {
                    Text(if (isVoiceProcessing) "Processing..." else "Say it")
                }
            }
            TextButton(
                onClick = onShareSnapshot,
                modifier = Modifier.semantics { contentDescription = "Share today snapshot" },
            ) { Text("Share snapshot") }
            Text(
                text = "Hide everything except now",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun ConflictReviewBanner(
    conflictCount: Int,
    onOpenSettings: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Conflicts need review",
                style = XCalendarTheme.typography.titleMedium,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "$conflictCount sync conflict(s) are waiting in Settings.",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.semantics { contentDescription = "Open Settings to review sync conflicts" },
            ) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun RoutineSection(routines: List<Routine>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Routines",
            style = XCalendarTheme.typography.titleMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        routines.forEach { routine ->
            RoutineCard(routine = routine)
        }
    }
}

@Composable
private fun RoutineCard(routine: Routine) {
    val timerStateHolder = koinInject<TimerStateHolder>()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.title,
                    style = XCalendarTheme.typography.titleMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = routine.timeOfDay.name.lowercase().replaceFirstChar { it.titlecase() },
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = {
                    timerStateHolder.startTimer(
                        itemId = routine.id,
                        title = routine.title,
                        durationMillis = 30 * 60_000L,
                    )
                }
            ) { Text("Start") }
        }
    }
}

@Composable
private fun NowSection(
    items: List<ScheduleItem>,
    peopleById: Map<String, Person>,
    onDone: (ScheduleItem) -> Unit,
    onSnooze: (ScheduleItem) -> Unit,
    onShare: (ScheduleItem) -> Unit,
    onEventClick: (Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Now",
            style = XCalendarTheme.typography.titleMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (items.isEmpty()) {
            EmptySection(message = "No items in the current window.")
        } else {
            items.forEach { item ->
                ScheduleItemCard(
                    item = item,
                    peopleById = peopleById,
                    onDone = onDone,
                    onSnooze = onSnooze,
                    onShare = onShare,
                    onEventClick = onEventClick,
                )
            }
        }
    }
}

@Composable
private fun DaySectionGroup(
    title: String,
    items: List<ScheduleItem>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    peopleById: Map<String, Person>,
    onDone: (ScheduleItem) -> Unit,
    onSnooze: (ScheduleItem) -> Unit,
    onShare: (ScheduleItem) -> Unit,
    onEventClick: (Event) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = XCalendarTheme.typography.titleMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        val visibleItems = if (expanded) items else items.take(MAX_VISIBLE_PER_SECTION)
        visibleItems.forEach { item ->
            ScheduleItemCard(
                item = item,
                peopleById = peopleById,
                onDone = onDone,
                onSnooze = onSnooze,
                onShare = onShare,
                onEventClick = onEventClick,
            )
        }
        val hiddenCount = items.size - visibleItems.size
        if (hiddenCount > 0 && !expanded) {
            TextButton(onClick = onToggleExpand) {
                Text("+ $hiddenCount more")
            }
        } else if (expanded && items.size > MAX_VISIBLE_PER_SECTION) {
            TextButton(onClick = onToggleExpand) {
                Text("Show less")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleItemCard(
    item: ScheduleItem,
    peopleById: Map<String, Person>,
    onDone: (ScheduleItem) -> Unit,
    onSnooze: (ScheduleItem) -> Unit,
    onShare: (ScheduleItem) -> Unit,
    onEventClick: (Event) -> Unit,
) {
    val timerStateHolder = koinInject<TimerStateHolder>()
    val people = item.personIds.mapNotNull { peopleById[it] }
    val whoAffectedLabel = people.joinToString(", ") { it.name }.ifBlank { null }
    val accentColor = resolveAccentColor(item, people, XCalendarTheme.colorScheme.primary)
    val clickableModifier =
        item.originalEvent?.let { event ->
            Modifier.clickable { onEventClick(event) }
        } ?: Modifier
    Card(
        modifier = Modifier.fillMaxWidth().then(clickableModifier),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(6.dp).fillMaxHeight().background(accentColor),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarRow(people)
                    Text(
                        text = formatTimeLabel(item),
                        style = XCalendarTheme.typography.bodySmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item.title,
                    style = XCalendarTheme.typography.titleMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                whoAffectedLabel?.let { names ->
                    Text(
                        text = "Who's affected: $names",
                        style = XCalendarTheme.typography.bodySmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityChip(priority = item.priority)
                    EnergyChip(energy = item.energy)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val duration =
                                when (item.source) {
                                    ScheduleSource.EVENT -> {
                                        val end = item.endTime ?: now
                                        (end - now).coerceAtLeast(60_000L)
                                    }
                                    ScheduleSource.TASK -> {
                                        val minutes = item.originalTask?.durationMinutes ?: 30
                                        minutes * 60_000L
                                    }
                                }
                            timerStateHolder.startTimer(item.id, item.title, duration)
                        },
                        modifier = Modifier.semantics { contentDescription = "Start timer for ${item.title}" },
                    ) { Text("Start") }
                    TextButton(
                        onClick = { onDone(item) },
                        modifier = Modifier.semantics { contentDescription = "Mark ${item.title} done" },
                    ) { Text("Done") }
                    TextButton(
                        onClick = { onSnooze(item) },
                        modifier = Modifier.semantics { contentDescription = "Snooze ${item.title}" },
                    ) { Text("Snooze") }
                    TextButton(
                        onClick = { onShare(item) },
                        modifier = Modifier.semantics { contentDescription = "Share ${item.title}" },
                    ) { Text("Share") }
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerCard(
    timer: ActiveTimer?,
    remainingMillis: Long,
    onStop: () -> Unit,
) {
    val totalMillis = ((timer?.endsAt ?: 0L) - (timer?.startedAt ?: 0L)).coerceAtLeast(1L)
    val progress = 1f - (remainingMillis.toFloat() / totalMillis.toFloat())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active timer",
                    style = XCalendarTheme.typography.labelMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = timer?.title.orEmpty(),
                    style = XCalendarTheme.typography.titleMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRemaining(remainingMillis),
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress.coerceIn(0f, 1f) })
                Text(
                    text = (progress * 100).toInt().toString() + "%",
                    style = XCalendarTheme.typography.labelSmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onStop) { Text("Stop") }
        }
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d left", minutes, seconds)
}

@Composable
private fun AvatarRow(people: List<Person>) {
    if (people.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        people.take(3).forEach { person ->
            Box(
                modifier = Modifier.size(28.dp).background(Color(person.color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = person.name.take(1).uppercase(),
                    style = XCalendarTheme.typography.labelMedium,
                    color = XCalendarTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(priority: TaskPriority?) {
    val label =
        when (priority) {
            TaskPriority.MUST -> "Must"
            TaskPriority.SHOULD -> "Should"
            TaskPriority.NICE -> "Nice"
            null -> "Event"
        }
    TagChip(label = label)
}

@Composable
private fun EnergyChip(energy: TaskEnergy?) {
    val label =
        when (energy) {
            TaskEnergy.LOW -> "Low energy"
            TaskEnergy.MEDIUM -> "Medium energy"
            TaskEnergy.HIGH -> "High energy"
            null -> null
        }
    if (label != null) {
        TagChip(label = label)
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

@Composable
private fun EmptySection(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolveAccentColor(item: ScheduleItem, people: List<Person>, fallbackColor: Color): Color {
    val personColor = people.firstOrNull()?.color
    val eventColor = item.originalEvent?.color
    return when {
        personColor != null -> Color(personColor)
        eventColor != null -> Color(eventColor)
        else -> fallbackColor
    }
}

private fun formatTimeLabel(item: ScheduleItem): String {
    if (item.isAllDay) return "All day"
    val start = item.startTime
    val end = item.endTime
    if (start == null || end == null) return "Flexible"
    val startLocal = start.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = end.toLocalDateTime(TimeZone.currentSystemDefault())
    return DateTimeFormatter.formatCompactTimeRange(startLocal, endLocal)
}

private fun formatFullDate(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$dayOfWeek, ${date.day} $month"
}

private fun shouldShowInTodayOnly(item: ScheduleItem, nowMillis: Long): Boolean {
    val start = item.startTime ?: return false
    val end = item.endTime
    val windowMillis = NOW_WINDOW_MINUTES * 60_000L
    val activeNow = end != null && nowMillis in start..end
    val startsInWindow = start in (nowMillis - windowMillis)..(nowMillis + windowMillis)
    return activeNow || startsInWindow
}

private fun buildTodaySnapshotText(
    date: LocalDate,
    items: List<ScheduleItem>,
    peopleById: Map<String, Person>,
    timeZone: TimeZone,
): String {
    val dateLabel = formatFullDate(date)
    if (items.isEmpty()) return "Today snapshot ($dateLabel)\nNo scheduled items."

    val lines =
        items.take(12).mapIndexed { index, item ->
            val who =
                item.personIds
                    .mapNotNull { peopleById[it]?.name }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { "Family" }
            "${index + 1}. ${formatTimeLabelForShare(item, timeZone)} ${item.title} ($who)"
        }

    return buildString {
        appendLine("Today snapshot ($dateLabel)")
        lines.forEach { appendLine(it) }
    }.trim()
}

private fun buildItemShareText(
    item: ScheduleItem,
    peopleById: Map<String, Person>,
    timeZone: TimeZone,
): String {
    val who =
        item.personIds
            .mapNotNull { peopleById[it]?.name }
            .distinct()
            .joinToString(", ")
            .ifBlank { "Family" }
    return "${formatTimeLabelForShare(item, timeZone)} ${item.title}\nWho's affected: $who"
}

private fun formatTimeLabelForShare(item: ScheduleItem, timeZone: TimeZone): String {
    if (item.isAllDay) return "[All day]"
    val start = item.startTime
    val end = item.endTime
    if (start == null || end == null) return "[Flexible]"
    val startLocal = start.toLocalDateTime(timeZone)
    val endLocal = end.toLocalDateTime(timeZone)
    return "[${DateTimeFormatter.formatCompactTimeRange(startLocal, endLocal)}]"
}

private fun overlaps(start: Long, end: Long, windowStart: Long, windowEnd: Long): Boolean {
    return start < windowEnd && end > windowStart
}

private fun sectionForItem(item: ScheduleItem, nowHour: Int, timeZone: TimeZone): DaySection {
    val hour = item.startTime?.toLocalDateTime(timeZone)?.hour ?: nowHour
    return when {
        hour < 12 -> DaySection.MORNING
        hour < 17 -> DaySection.AFTERNOON
        else -> DaySection.EVENING
    }
}
