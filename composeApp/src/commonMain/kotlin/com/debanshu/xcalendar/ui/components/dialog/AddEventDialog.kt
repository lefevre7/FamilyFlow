package com.debanshu.xcalendar.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.common.convertStringToColor
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.EventSource
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.User
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import xcalendar.composeapp.generated.resources.Res
import xcalendar.composeapp.generated.resources.ic_description
import xcalendar.composeapp.generated.resources.ic_location
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private enum class EventEditorMode {
    CREATE,
    EDIT,
}

/**
 * Bottom sheet dialog for creating a new event.
 */
@Composable
fun AddEventDialog(
    user: User,
    calendars: ImmutableList<Calendar>,
    selectedDate: LocalDate,
    onSave: (Event) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    EventEditorSheet(
        mode = EventEditorMode.CREATE,
        user = user,
        calendars = calendars,
        selectedDate = selectedDate,
        initialEvent = null,
        onSave = onSave,
        onDelete = null,
        onDismiss = onDismiss,
    )
}

/**
 * Bottom sheet dialog for editing an existing event.
 */
@Composable
fun EditEventDialog(
    user: User,
    calendars: ImmutableList<Calendar>,
    event: Event,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedDate =
        remember(event.startTime) {
            Instant.fromEpochMilliseconds(event.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }
    EventEditorSheet(
        mode = EventEditorMode.EDIT,
        user = user,
        calendars = calendars,
        selectedDate = selectedDate,
        initialEvent = event,
        onSave = onSave,
        onDelete = onDelete,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun EventEditorSheet(
    mode: EventEditorMode,
    user: User,
    calendars: ImmutableList<Calendar>,
    selectedDate: LocalDate,
    initialEvent: Event?,
    onSave: (Event) -> Unit,
    onDelete: ((Event) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())

    val initialStartDateTime =
        remember(initialEvent, selectedDate) {
            initialEvent?.startTime
                ?.let { millis -> Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone) }
                ?: LocalDateTime(
                    selectedDate.year,
                    selectedDate.month,
                    selectedDate.dayOfMonth,
                    12,
                    0,
                )
        }
    val initialEndDateTime =
        remember(initialEvent, selectedDate) {
            initialEvent?.endTime
                ?.let { millis -> Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone) }
                ?: LocalDateTime(
                    selectedDate.year,
                    selectedDate.month,
                    selectedDate.dayOfMonth,
                    12,
                    30,
                )
        }

    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var description by remember(initialEvent) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var location by remember(initialEvent) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var selectedCalendarId by
        remember(initialEvent, calendars) {
            mutableStateOf(initialEvent?.calendarId ?: calendars.firstOrNull()?.id.orEmpty())
        }
    var selectedPersonIds by
        remember(initialEvent) {
            mutableStateOf(initialEvent?.affectedPersonIds?.toSet() ?: emptySet())
        }
    var isAllDay by remember(initialEvent) { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var startDateTime by remember(initialEvent) { mutableStateOf(initialStartDateTime) }
    var endDateTime by remember(initialEvent) { mutableStateOf(initialEndDateTime) }
    var reminderMinutes by remember(initialEvent) { mutableStateOf(initialEvent?.reminderMinutes?.firstOrNull() ?: 20) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(calendars, selectedCalendarId) {
        if (calendars.isEmpty()) return@LaunchedEffect
        val currentExists = calendars.any { it.id == selectedCalendarId }
        if (!currentExists) {
            selectedCalendarId = calendars.first().id
        }
    }

    LaunchedEffect(people, mode) {
        if (mode == EventEditorMode.EDIT) return@LaunchedEffect
        if (selectedPersonIds.isNotEmpty() || people.isEmpty()) return@LaunchedEffect
        val defaultPersonId =
            people.firstOrNull { it.role == PersonRole.MOM }?.id ?: people.firstOrNull()?.id
        if (defaultPersonId != null) {
            selectedPersonIds = setOf(defaultPersonId)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
        ) {
            EditorHeader(
                mode = mode,
                onCancel = onDismiss,
                onDelete = if (mode == EventEditorMode.EDIT && onDelete != null) {
                    { showDeleteConfirm = true }
                } else {
                    null
                },
                onSave = {
                    if (title.isBlank()) return@EditorHeader
                    val selectedCalendar = calendars.find { it.id == selectedCalendarId }
                    val normalizedStart =
                        if (isAllDay) {
                            LocalDateTime(
                                startDateTime.year,
                                startDateTime.month,
                                startDateTime.dayOfMonth,
                                0,
                                0,
                            )
                        } else {
                            startDateTime
                        }
                    val normalizedEnd =
                        if (isAllDay) {
                            LocalDateTime(
                                endDateTime.year,
                                endDateTime.month,
                                endDateTime.dayOfMonth,
                                23,
                                59,
                            )
                        } else {
                            ensureEndAfterStart(startDateTime = normalizedStart, endDateTime = endDateTime, timeZone = timeZone)
                        }

                    onSave(
                        createOrUpdateEvent(
                            existing = initialEvent,
                            title = title,
                            description = description,
                            location = location,
                            selectedCalendar = selectedCalendar,
                            selectedCalendarId = selectedCalendarId,
                            startDateTime = normalizedStart,
                            endDateTime = normalizedEnd,
                            isAllDay = isAllDay,
                            reminderMinutes = reminderMinutes,
                            affectedPersonIds = selectedPersonIds.toList(),
                            timeZone = timeZone,
                        ),
                    )
                },
            )

            TitleTextField(
                value = title,
                onValueChange = { title = it },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            DateTimeSection(
                isAllDay = isAllDay,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                onAllDayChange = { checked ->
                    isAllDay = checked
                    if (checked) {
                        startDateTime = LocalDateTime(startDateTime.year, startDateTime.month, startDateTime.dayOfMonth, 0, 0)
                        endDateTime = LocalDateTime(endDateTime.year, endDateTime.month, endDateTime.dayOfMonth, 23, 59)
                    }
                },
                onStartDateClick = { showStartDatePicker = true },
                onEndDateClick = { showEndDatePicker = true },
                onStartTimeClick = { if (!isAllDay) showStartTimePicker = true },
                onEndTimeClick = { if (!isAllDay) showEndTimePicker = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            CalendarSelectionSection(
                user = user,
                calendars = calendars,
                selectedCalendarId = selectedCalendarId,
                onCalendarSelected = { selectedCalendarId = it },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            WhoAffectedSection(
                people = people,
                selectedPersonIds = selectedPersonIds,
                onTogglePerson = { personId ->
                    selectedPersonIds =
                        if (selectedPersonIds.contains(personId)) {
                            selectedPersonIds - personId
                        } else {
                            selectedPersonIds + personId
                        }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            EventOptionRow(
                icon = Res.drawable.ic_location,
                text = "Location",
                onClick = {},
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("Enter location") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("event_location_input"),
                singleLine = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            NotificationRow(
                reminderMinutes = reminderMinutes,
                onReminderChange = { reminderMinutes = it },
                onClick = { showReminderPicker = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

            EventOptionRow(
                icon = Res.drawable.ic_description,
                text = "Description",
                onClick = {},
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add details") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("event_description_input"),
                minLines = 3,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showReminderPicker) {
        ModalBottomSheet(
            onDismissRequest = { showReminderPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Reminder",
                    style = XCalendarTheme.typography.titleMedium,
                )
                listOf(0, 10, 20, 30, 60).forEach { minutes ->
                    TextButton(
                        onClick = {
                            reminderMinutes = minutes
                            showReminderPicker = false
                        },
                    ) {
                        Text(
                            text = if (minutes == 0) "No reminder" else "$minutes minutes before",
                            color = if (minutes == reminderMinutes) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateTime.date.toUtcDatePickerMillis())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.toUtcLocalDate() ?: startDateTime.date
                        startDateTime = LocalDateTime(picked.year, picked.month, picked.dayOfMonth, startDateTime.hour, startDateTime.minute)
                        if (!isAllDay && endDateTime.toEpochMillis(timeZone) <= startDateTime.toEpochMillis(timeZone)) {
                            endDateTime = plusMinutes(startDateTime, 30, timeZone)
                        }
                        showStartDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateTime.date.toUtcDatePickerMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.toUtcLocalDate() ?: endDateTime.date
                        endDateTime = LocalDateTime(picked.year, picked.month, picked.dayOfMonth, endDateTime.hour, endDateTime.minute)
                        if (!isAllDay && endDateTime.toEpochMillis(timeZone) <= startDateTime.toEpochMillis(timeZone)) {
                            endDateTime = plusMinutes(startDateTime, 30, timeZone)
                        }
                        showEndDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker && !isAllDay) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = startDateTime.hour,
                initialMinute = startDateTime.minute,
            )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select start time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDateTime =
                            LocalDateTime(
                                startDateTime.year,
                                startDateTime.month,
                                startDateTime.dayOfMonth,
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                        if (endDateTime.toEpochMillis(timeZone) <= startDateTime.toEpochMillis(timeZone)) {
                            endDateTime = plusMinutes(startDateTime, 30, timeZone)
                        }
                        showStartTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker && !isAllDay) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = endDateTime.hour,
                initialMinute = endDateTime.minute,
            )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select end time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val proposedEnd =
                            LocalDateTime(
                                endDateTime.year,
                                endDateTime.month,
                                endDateTime.dayOfMonth,
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                        endDateTime = ensureEndAfterStart(startDateTime = startDateTime, endDateTime = proposedEnd, timeZone = timeZone)
                        showEndTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDeleteConfirm && initialEvent != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete event?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(initialEvent)
                        showDeleteConfirm = false
                    },
                ) {
                    Text("Delete", color = XCalendarTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhoAffectedSection(
    people: List<Person>,
    selectedPersonIds: Set<String>,
    onTogglePerson: (String) -> Unit,
) {
    if (people.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Who's affected",
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            people.forEach { person ->
                FilterChip(
                    selected = selectedPersonIds.contains(person.id),
                    onClick = { onTogglePerson(person.id) },
                    label = { Text(person.name) },
                )
            }
        }
    }
}

@Composable
private fun EditorHeader(
    mode: EventEditorMode,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mode == EventEditorMode.EDIT && onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = XCalendarTheme.colorScheme.error)
                }
            }
            TextButton(onClick = onSave) { Text("Save") }
        }
    }
}

@Composable
private fun TitleTextField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = XCalendarTheme.typography.headlineSmall,
        placeholder = {
            Text(
                text = "Add title",
                color = XCalendarTheme.colorScheme.onSurface,
                style = XCalendarTheme.typography.headlineSmall,
            )
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("event_title_input"),
        singleLine = true,
    )
}

@Composable
private fun DateTimeSection(
    isAllDay: Boolean,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onAllDayChange: (Boolean) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("All day", style = XCalendarTheme.typography.bodyMedium)
            Switch(
                checked = isAllDay,
                onCheckedChange = onAllDayChange,
            )
        }

        DateTimeRow(
            label = "Starts",
            dateText = formatDate(startDateTime.date),
            timeText = if (isAllDay) "All day" else formatTime(startDateTime),
            onDateClick = onStartDateClick,
            onTimeClick = onStartTimeClick,
            showTime = !isAllDay,
        )

        DateTimeRow(
            label = "Ends",
            dateText = formatDate(endDateTime.date),
            timeText = if (isAllDay) "All day" else formatTime(endDateTime),
            onDateClick = onEndDateClick,
            onTimeClick = onEndTimeClick,
            showTime = !isAllDay,
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    dateText: String,
    timeText: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    showTime: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = XCalendarTheme.typography.labelMedium, color = XCalendarTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDateClick) { Text(dateText) }
            if (showTime) {
                TextButton(onClick = onTimeClick) { Text(timeText) }
            } else {
                Text(text = timeText, style = XCalendarTheme.typography.bodySmall, color = XCalendarTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun createOrUpdateEvent(
    existing: Event?,
    title: String,
    description: String,
    location: String,
    selectedCalendar: Calendar?,
    selectedCalendarId: String,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    isAllDay: Boolean,
    reminderMinutes: Int,
    affectedPersonIds: List<String>,
    timeZone: TimeZone,
): Event {
    val movedCalendar = existing != null && existing.calendarId != selectedCalendarId

    val baseEvent =
        existing ?: Event(
            id = Uuid.random().toString(),
            calendarId = selectedCalendarId,
            calendarName = selectedCalendar?.name ?: "",
            title = title,
            startTime = startDateTime.toInstant(timeZone).toEpochMilliseconds(),
            endTime = endDateTime.toInstant(timeZone).toEpochMilliseconds(),
            isAllDay = isAllDay,
            reminderMinutes = emptyList(),
            color = selectedCalendar?.color ?: convertStringToColor("defaultColor", 255),
            source = EventSource.LOCAL,
            affectedPersonIds = affectedPersonIds,
        )

    return baseEvent.copy(
        calendarId = selectedCalendarId,
        calendarName = selectedCalendar?.name ?: baseEvent.calendarName,
        title = title,
        description = description.takeIf { it.isNotBlank() },
        location = location.takeIf { it.isNotBlank() },
        startTime = startDateTime.toInstant(timeZone).toEpochMilliseconds(),
        endTime = endDateTime.toInstant(timeZone).toEpochMilliseconds(),
        isAllDay = isAllDay,
        reminderMinutes = if (reminderMinutes > 0) listOf(reminderMinutes) else emptyList(),
        color = selectedCalendar?.color ?: baseEvent.color,
        source = if (movedCalendar) EventSource.LOCAL else baseEvent.source,
        externalId = if (movedCalendar) null else baseEvent.externalId,
        externalUpdatedAt = if (movedCalendar) null else baseEvent.externalUpdatedAt,
        lastSyncedAt = if (movedCalendar) null else baseEvent.lastSyncedAt,
        affectedPersonIds = affectedPersonIds.distinct(),
    )
}

private fun LocalDate.toUtcDatePickerMillis(): Long =
    this.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private fun LocalDateTime.toEpochMillis(timeZone: TimeZone): Long =
    this.toInstant(timeZone).toEpochMilliseconds()

private fun ensureEndAfterStart(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    timeZone: TimeZone,
): LocalDateTime {
    val startMillis = startDateTime.toEpochMillis(timeZone)
    val endMillis = endDateTime.toEpochMillis(timeZone)
    if (endMillis > startMillis) return endDateTime
    return plusMinutes(startDateTime, 30, timeZone)
}

private fun plusMinutes(
    value: LocalDateTime,
    minutes: Int,
    timeZone: TimeZone,
): LocalDateTime {
    val millis = value.toInstant(timeZone).toEpochMilliseconds() + (minutes * 60_000L)
    return Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
}

private fun formatDate(date: LocalDate): String {
    val month =
        date.month.name
            .lowercase()
            .replaceFirstChar { it.titlecase() }
    return "$month ${date.dayOfMonth}, ${date.year}"
}

private fun formatTime(dateTime: LocalDateTime): String {
    val hour12 =
        when {
            dateTime.hour == 0 -> 12
            dateTime.hour > 12 -> dateTime.hour - 12
            else -> dateTime.hour
        }
    val minute = dateTime.minute.toString().padStart(2, '0')
    val amPm = if (dateTime.hour >= 12) "PM" else "AM"
    return "$hour12:$minute $amPm"
}
