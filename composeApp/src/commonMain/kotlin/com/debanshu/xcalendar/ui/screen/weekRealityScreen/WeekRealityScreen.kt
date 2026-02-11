package com.debanshu.xcalendar.ui.screen.weekRealityScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.debanshu.xcalendar.domain.model.ScheduleFilter
import com.debanshu.xcalendar.domain.model.ScheduleItem
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.effectivePersonId
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.task.GetTasksUseCase
import com.debanshu.xcalendar.domain.util.ScheduleEngine
import com.debanshu.xcalendar.ui.components.FamilyLensMiniHeader
import com.debanshu.xcalendar.ui.components.SwipeablePager
import com.debanshu.xcalendar.ui.components.core.ScheduleHolidayTag
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.utils.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.koin.compose.koinInject
import kotlin.time.Clock

private const val MAX_ITEMS_PER_DAY = 5
private const val DAYS_IN_WEEK = 7

private data class DaySchedule(
    val date: LocalDate,
    val items: List<ScheduleItem>,
)

@Composable
fun WeekRealityScreen(
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

    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val getTasksUseCase = koinInject<GetTasksUseCase>()
    val lensStateHolder = koinInject<LensStateHolder>()
    val conflictStateHolder = koinInject<SyncConflictStateHolder>()

    val peopleFlow = remember { getPeopleUseCase() }
    val tasksFlow = remember { getTasksUseCase() }

    val people by peopleFlow.collectAsState(initial = emptyList())
    val tasks by tasksFlow.collectAsState(initial = emptyList())
    val lensSelection by lensStateHolder.selection.collectAsState()
    val conflicts by conflictStateHolder.conflicts.collectAsState()

    val momId = remember(people) { people.firstOrNull { it.role == PersonRole.MOM }?.id }
    var onlyMomRequired by rememberSaveable { mutableStateOf(false) }
    var onlyMust by rememberSaveable { mutableStateOf(false) }

    val currentWeekStart = remember(dateState.selectedDate) { startOfWeek(dateState.selectedDate) }

    SwipeablePager(
        modifier = modifier.fillMaxSize(),
        currentReference = currentWeekStart,
        calculateOffset = { current, base ->
            val daysDiff = (current.toEpochDays() - base.toEpochDays()).toInt()
            daysDiff / DAYS_IN_WEEK
        },
        pageToReference = { baseDate, initialPage, page ->
            val offset = (page - initialPage) * DAYS_IN_WEEK
            baseDate.plus(DatePeriod(days = offset))
        },
        onReferenceChange = { newStart ->
            dateStateHolder.updateSelectedDateState(newStart)
        },
    ) { weekStart ->
        WeekPage(
            weekStart = weekStart,
            currentDate = dateState.currentDate,
            events = events,
            holidays = holidays,
            tasks = tasks,
            people = people,
            momId = momId,
            lensSelection = lensSelection,
            onlyMomRequired = onlyMomRequired,
            onlyMust = onlyMust,
            onLensChange = lensStateHolder::updateSelection,
            onToggleOnlyMomRequired = { onlyMomRequired = !onlyMomRequired },
            onToggleOnlyMust = { onlyMust = !onlyMust },
            timeZone = timeZone,
            nowMillis = nowMillis,
            onSelectDay = { selectedDay ->
                dateStateHolder.updateSelectedDateState(selectedDay)
            },
            onEventClick = onEventClick,
            pendingConflictCount = conflicts.size,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekPage(
    weekStart: LocalDate,
    currentDate: LocalDate,
    events: ImmutableList<Event>,
    holidays: ImmutableList<Holiday>,
    tasks: List<com.debanshu.xcalendar.domain.model.Task>,
    people: List<Person>,
    momId: String?,
    lensSelection: FamilyLensSelection,
    onlyMomRequired: Boolean,
    onlyMust: Boolean,
    onLensChange: (FamilyLensSelection) -> Unit,
    onToggleOnlyMomRequired: () -> Unit,
    onToggleOnlyMust: () -> Unit,
    timeZone: TimeZone,
    nowMillis: Long,
    onSelectDay: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    pendingConflictCount: Int,
    onNavigateToSettings: () -> Unit,
) {
    val weekDays = remember(weekStart) {
        (0 until DAYS_IN_WEEK).map { offset ->
            weekStart.plus(DatePeriod(days = offset))
        }
    }

    val filter = remember(lensSelection, onlyMomRequired, onlyMust, momId) {
        val selectedPersonId =
            if (onlyMomRequired && momId != null) {
                momId
            } else {
                lensSelection.effectivePersonId(momId)
            }
        ScheduleFilter(
            personId = selectedPersonId,
            onlyMust = onlyMust,
            includeUnassignedEvents = true,
            nowWindowMinutes = null,
        )
    }

    val daySchedules = remember(weekDays, events, tasks, filter, nowMillis, timeZone) {
        weekDays.map { date ->
            val (dayStart, dayEnd) = dayWindow(date, timeZone)
            val dayEvents = events.filter { overlaps(it.startTime, it.endTime, dayStart, dayEnd) }
            val dayTasks = tasks.filter { task -> shouldIncludeTaskForDay(task, dayStart, dayEnd) }
            val aggregation =
                ScheduleEngine.aggregate(
                    events = dayEvents,
                    tasks = dayTasks,
                    filter = filter,
                    nowMillis = nowMillis,
                    timeZone = timeZone,
                )
            DaySchedule(date = date, items = aggregation.items)
        }
    }
    val holidaysByDay = remember(weekDays, holidays, timeZone) {
        weekDays.associateWith { date ->
            holidays.filter { holiday ->
                holiday.date.toLocalDateTime(timeZone).date == date
            }
        }
    }

    val peopleById = remember(people) { people.associateBy { it.id } }

    var selectedDay by rememberSaveable(weekStart) { mutableStateOf<LocalDate?>(null) }
    val selectedSchedule = selectedDay?.let { day -> daySchedules.firstOrNull { it.date == day } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WeekHeader(
            weekStart = weekStart,
            weekEnd = weekStart.plus(DatePeriod(days = DAYS_IN_WEEK - 1)),
            lensSelection = lensSelection,
            people = people,
            onlyMomRequired = onlyMomRequired,
            onlyMust = onlyMust,
            onLensChange = onLensChange,
            onToggleOnlyMomRequired = onToggleOnlyMomRequired,
            onToggleOnlyMust = onToggleOnlyMust,
        )

        if (pendingConflictCount > 0) {
            ConflictReviewBanner(
                conflictCount = pendingConflictCount,
                onOpenSettings = onNavigateToSettings,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            weekDays.forEachIndexed { index, date ->
                val schedule = daySchedules[index]
                DayColumn(
                    date = date,
                    isToday = date == currentDate,
                    items = schedule.items,
                    holidays = holidaysByDay[date].orEmpty(),
                    peopleById = peopleById,
                    onEventClick = onEventClick,
                    onMoreClick = {
                        selectedDay = date
                        onSelectDay(date)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }

    if (selectedSchedule != null && selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDay = null },
        ) {
            DayDetailSheet(
                date = selectedSchedule.date,
                items = selectedSchedule.items,
                holidays = holidaysByDay[selectedSchedule.date].orEmpty(),
                peopleById = peopleById,
                onEventClick = { event ->
                    selectedDay = null
                    onEventClick(event)
                },
            )
        }
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    lensSelection: FamilyLensSelection,
    people: List<Person>,
    onlyMomRequired: Boolean,
    onlyMust: Boolean,
    onLensChange: (FamilyLensSelection) -> Unit,
    onToggleOnlyMomRequired: () -> Unit,
    onToggleOnlyMust: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Week",
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "${formatDateShort(weekStart)} – ${formatDateShort(weekEnd)}",
            style = XCalendarTheme.typography.bodyLarge,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
        FamilyLensMiniHeader(
            selection = lensSelection,
            people = people,
            onSelectionChange = onLensChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = onlyMomRequired,
                onClick = onToggleOnlyMomRequired,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (onlyMomRequired) {
                                "Only Mom required filter on"
                            } else {
                                "Only Mom required filter off"
                            }
                    },
                label = { Text("Only Mom required") },
            )
            FilterChip(
                selected = onlyMust,
                onClick = onToggleOnlyMust,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (onlyMust) {
                                "Only Must filter on"
                            } else {
                                "Only Must filter off"
                            }
                    },
                label = { Text("Only Must") },
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
private fun DayColumn(
    date: LocalDate,
    isToday: Boolean,
    items: List<ScheduleItem>,
    holidays: List<Holiday>,
    peopleById: Map<String, Person>,
    onEventClick: (Event) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMoreClick() },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() },
                style = XCalendarTheme.typography.labelMedium,
                color = if (isToday) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = date.day.toString(),
                style = XCalendarTheme.typography.titleMedium,
                color = if (isToday) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurface,
            )
        }

        val visibleItems = items.take(MAX_ITEMS_PER_DAY)
        if (visibleItems.isEmpty() && holidays.isEmpty()) {
            EmptyDayCard()
        } else {
            holidays.take(2).forEach { holiday ->
                ScheduleHolidayTag(name = holiday.name)
            }
            if (holidays.size > 2) {
                Text(
                    text = "+ ${holidays.size - 2} holiday(s)",
                    style = XCalendarTheme.typography.labelSmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            visibleItems.forEach { item ->
                WeekItemCard(
                    item = item,
                    peopleById = peopleById,
                    onEventClick = onEventClick,
                    onNonEventClick = onMoreClick,
                )
            }
        }
        val remaining = items.size - visibleItems.size
        if (remaining > 0) {
            TextButton(
                onClick = onMoreClick,
                modifier = Modifier.semantics { contentDescription = "Open ${formatDateShort(date)} details" },
            ) {
                Text("+ $remaining")
            }
        }
    }
}

@Composable
private fun WeekItemCard(
    item: ScheduleItem,
    peopleById: Map<String, Person>,
    onEventClick: (Event) -> Unit,
    onNonEventClick: (() -> Unit)? = null,
) {
    val people = item.personIds.mapNotNull { peopleById[it] }
    val whoAffectedLabel = people.joinToString(", ") { it.name }.ifBlank { null }
    val accentColor = resolveAccentColor(item, people, XCalendarTheme.colorScheme.primary)
    val event = item.originalEvent
    val clickableModifier =
        if (event != null) {
            Modifier.clickable { onEventClick(event) }
        } else if (onNonEventClick != null) {
            Modifier.clickable { onNonEventClick() }
        } else {
            Modifier
        }

    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(4.dp).height(48.dp).background(accentColor),
            )
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatTimeLabel(item),
                    style = XCalendarTheme.typography.labelSmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.title,
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                whoAffectedLabel?.let { names ->
                    Text(
                        text = "Who's affected: $names",
                        style = XCalendarTheme.typography.labelSmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.priority != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = item.priority.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = XCalendarTheme.typography.labelSmall,
                            color = XCalendarTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    Card(shape = RoundedCornerShape(12.dp)) {
        Text(
            text = "Clear",
            modifier = Modifier.padding(8.dp),
            style = XCalendarTheme.typography.labelSmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayDetailSheet(
    date: LocalDate,
    items: List<ScheduleItem>,
    holidays: List<Holiday>,
    peopleById: Map<String, Person>,
    onEventClick: (Event) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = formatDateFull(date),
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (holidays.isNotEmpty()) {
            holidays.forEach { holiday ->
                ScheduleHolidayTag(name = holiday.name)
            }
        }
        if (items.isEmpty() && holidays.isEmpty()) {
            Text(
                text = "No items for this day.",
                style = XCalendarTheme.typography.bodyMedium,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            items.forEach { item ->
                WeekItemCard(
                    item = item,
                    peopleById = peopleById,
                    onEventClick = onEventClick,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun startOfWeek(date: LocalDate): LocalDate {
    val shift = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
    return date.plus(DatePeriod(days = -shift))
}

private fun dayWindow(date: LocalDate, timeZone: TimeZone): Pair<Long, Long> {
    val start = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
    val end = start + 24 * 60 * 60 * 1000L
    return start to end
}

private fun overlaps(start: Long, end: Long, windowStart: Long, windowEnd: Long): Boolean {
    return start < windowEnd && end > windowStart
}

private fun shouldIncludeTaskForDay(
    task: com.debanshu.xcalendar.domain.model.Task,
    dayStart: Long,
    dayEnd: Long,
): Boolean {
    if (task.status != TaskStatus.OPEN) return false
    val scheduledStart = task.scheduledStart
    val scheduledEnd = task.scheduledEnd
    if (scheduledStart != null && scheduledEnd != null) {
        return overlaps(scheduledStart, scheduledEnd, dayStart, dayEnd)
    }
    val dueAt = task.dueAt
    return dueAt != null && dueAt in dayStart until dayEnd
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

private fun formatDateShort(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.titlecase() }
    return "$month ${date.day}"
}

private fun formatDateFull(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
    val month = date.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$dayOfWeek, ${date.day} $month"
}
