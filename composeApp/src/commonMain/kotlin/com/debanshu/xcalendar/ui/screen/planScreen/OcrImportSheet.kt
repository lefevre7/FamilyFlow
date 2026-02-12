package com.debanshu.xcalendar.ui.screen.planScreen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.Calendar
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.OcrCandidateEvent
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.util.ImportCategory
import com.debanshu.xcalendar.domain.util.ImportCategoryClassifier
import com.debanshu.xcalendar.domain.util.OcrStructuringEngine
import com.debanshu.xcalendar.domain.usecase.event.CreateEventUseCase
import com.debanshu.xcalendar.domain.usecase.ocr.StructureOcrUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.rememberOcrCaptureController
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.number
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OcrImportSheet(
    calendars: List<Calendar>,
    selectedCalendarId: String?,
    onCalendarSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val referenceDate = remember { Clock.System.now().toLocalDateTime(timeZone).date }
    val createEventUseCase = koinInject<CreateEventUseCase>()
    val structureOcrUseCase = koinInject<StructureOcrUseCase>()
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val scope = rememberCoroutineScope()
    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())
    val defaultPersonId = remember(people) { people.firstOrNull { it.role == PersonRole.MOM }?.id ?: people.firstOrNull()?.id }

    var rawText by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var isStructuring by rememberSaveable { mutableStateOf(false) }

    val editableCandidates = remember { mutableStateListOf<EditableCandidate>() }

    val controller = rememberOcrCaptureController(
        onResult = { text ->
            rawText = text
            isProcessing = false
        },
        onError = { message ->
            statusMessage = message
            isProcessing = false
        },
    )

    LaunchedEffect(rawText, defaultPersonId) {
        if (rawText.isBlank()) return@LaunchedEffect
        isStructuring = true
        statusMessage = null
        val structured = structureOcrUseCase(rawText, referenceDate, timeZone)
        val titleCounts = structured.candidates.groupingBy { normalizeCandidateTitle(it.title) }.eachCount()
        editableCandidates.clear()
        editableCandidates.addAll(
            structured.candidates.map { candidate ->
                candidate.toEditable(
                    defaultPersonId = defaultPersonId,
                    repeatedTitleCount = titleCounts[normalizeCandidateTitle(candidate.title)] ?: 0,
                )
            },
        )
        isStructuring = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Scan schedule",
                style = XCalendarTheme.typography.titleLarge,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Capture a school calendar and confirm the events we find.",
                style = XCalendarTheme.typography.bodyMedium,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )

            CalendarSelector(
                calendars = calendars,
                selectedCalendarId = selectedCalendarId,
                onSelect = onCalendarSelected,
            )

            if (!PlatformFeatures.ocr.supported || !controller.isAvailable) {
                Text(
                    text = PlatformFeatures.ocr.reason ?: "OCR is not available on this device.",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            isProcessing = true
                            controller.captureFromCamera()
                        },
                    ) {
                        Text("Scan with camera")
                    }
                    TextButton(
                        onClick = {
                            isProcessing = true
                            controller.pickFromGallery()
                        },
                    ) {
                        Text("Choose from gallery")
                    }
                }
            }

            if (isProcessing) {
                StatusCard("Processing image...")
            }

            if (isStructuring) {
                StatusCard("Structuring OCR with local AI JSON extraction...")
            }

            statusMessage?.let { StatusCard(it) }

            if (rawText.isNotBlank()) {
                RawTextCard(rawText)
            }

            if (editableCandidates.isNotEmpty()) {
                val pendingCount = editableCandidates.count { it.decision == CandidateDecision.PENDING }
                val acceptedCount = editableCandidates.count { it.decision == CandidateDecision.ACCEPTED }
                Text(
                    text = "Review events",
                    style = XCalendarTheme.typography.titleMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$acceptedCount accepted • $pendingCount pending",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                editableCandidates.forEach { candidate ->
                    OcrCandidateCard(
                        candidate = candidate,
                        people = people,
                        onUpdate = { updated ->
                            val index = editableCandidates.indexOfFirst { it.id == updated.id }
                            if (index >= 0) {
                                editableCandidates[index] = updated
                            }
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val calendar = calendars.firstOrNull { it.id == selectedCalendarId }
                            if (calendar == null) {
                                statusMessage = "Choose a calendar first."
                                return@TextButton
                            }
                            if (pendingCount > 0) {
                                statusMessage = "Review each candidate: Accept or Discard."
                                return@TextButton
                            }
                            val accepted = editableCandidates.filter { it.decision == CandidateDecision.ACCEPTED }
                            if (accepted.isEmpty()) {
                                statusMessage = "Accept at least one event to import."
                                return@TextButton
                            }
                            scope.launch {
                                val errors = saveCandidates(
                                    calendar = calendar,
                                    candidates = accepted,
                                    referenceDate = referenceDate,
                                    timeZone = timeZone,
                                    createEventUseCase = createEventUseCase,
                                )
                                statusMessage = errors
                                if (errors == null) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = acceptedCount > 0 && pendingCount == 0,
                    ) {
                        Text("Add events")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalendarSelector(
    calendars: List<Calendar>,
    selectedCalendarId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Calendar",
            style = XCalendarTheme.typography.titleSmall,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (calendars.isEmpty()) {
            StatusCard("No calendars available yet.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                calendars.forEach { calendar ->
                    FilterChip(
                        selected = calendar.id == selectedCalendarId,
                        onClick = { onSelect(calendar.id) },
                        label = { Text(calendar.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RawTextCard(text: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "OCR text",
                style = XCalendarTheme.typography.titleSmall,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Scroll to review full captured OCR text",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OcrCandidateCard(
    candidate: EditableCandidate,
    people: List<Person>,
    onUpdate: (EditableCandidate) -> Unit,
) {
    val editingEnabled = candidate.decision != CandidateDecision.DISCARDED && candidate.isEditing
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(label = candidate.decision.label())
                TagChip(label = "Category: ${candidate.category.label}")
            }
            OutlinedTextField(
                value = candidate.title,
                onValueChange = {
                    onUpdate(
                        candidate.copy(
                            title = it,
                            decision = CandidateDecision.PENDING,
                            isEditing = true,
                        ),
                    )
                },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = editingEnabled,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = candidate.dateText,
                    onValueChange = {
                        onUpdate(
                            candidate.copy(
                                dateText = it,
                                decision = CandidateDecision.PENDING,
                                isEditing = true,
                            ),
                        )
                    },
                    label = { Text("Date (MM/DD)") },
                    modifier = Modifier.weight(1f),
                    enabled = editingEnabled,
                )
                OutlinedTextField(
                    value = candidate.startTimeText,
                    onValueChange = {
                        onUpdate(
                            candidate.copy(
                                startTimeText = it,
                                decision = CandidateDecision.PENDING,
                                isEditing = true,
                            ),
                        )
                    },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f),
                    enabled = editingEnabled,
                )
                OutlinedTextField(
                    value = candidate.endTimeText,
                    onValueChange = {
                        onUpdate(
                            candidate.copy(
                                endTimeText = it,
                                decision = CandidateDecision.PENDING,
                                isEditing = true,
                            ),
                        )
                    },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f),
                    enabled = editingEnabled,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = candidate.allDay,
                    onClick = {
                        onUpdate(
                            candidate.copy(
                                allDay = !candidate.allDay,
                                decision = CandidateDecision.PENDING,
                                isEditing = true,
                            ),
                        )
                    },
                    label = { Text("All day") },
                    enabled = editingEnabled,
                )
                Text(
                    text = "Source: ${candidate.sourceText.take(60)}",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "Category",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportCategory.entries.forEach { category ->
                    FilterChip(
                        selected = candidate.category == category,
                        onClick = {
                            onUpdate(
                                candidate.copy(
                                    category = category,
                                    decision = CandidateDecision.PENDING,
                                    isEditing = true,
                                ),
                            )
                        },
                        label = { Text(category.label) },
                        enabled = editingEnabled,
                    )
                }
            }
            if (people.isNotEmpty()) {
                Text(
                    text = "Who's affected",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = candidate.selectedPersonId == null,
                        onClick = {
                            onUpdate(
                                candidate.copy(
                                    selectedPersonId = null,
                                    decision = CandidateDecision.PENDING,
                                    isEditing = true,
                                ),
                            )
                        },
                        label = { Text("Unassigned") },
                        enabled = editingEnabled,
                    )
                    people.forEach { person ->
                        FilterChip(
                            selected = candidate.selectedPersonId == person.id,
                            onClick = {
                                onUpdate(
                                    candidate.copy(
                                        selectedPersonId = person.id,
                                        decision = CandidateDecision.PENDING,
                                        isEditing = true,
                                    ),
                                )
                            },
                            label = { Text(person.name) },
                            enabled = editingEnabled,
                        )
                    }
                }
            }
            candidate.suggestedRecurringRule?.let {
                Text(
                    text = "Add as recurring?",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = candidate.addAsRecurring,
                        onClick = {
                            onUpdate(
                                candidate.copy(
                                    addAsRecurring = true,
                                    decision = CandidateDecision.PENDING,
                                    isEditing = true,
                                ),
                            )
                        },
                        label = { Text("Yes") },
                        enabled = editingEnabled,
                    )
                    FilterChip(
                        selected = !candidate.addAsRecurring,
                        onClick = {
                            onUpdate(
                                candidate.copy(
                                    addAsRecurring = false,
                                    decision = CandidateDecision.PENDING,
                                    isEditing = true,
                                ),
                            )
                        },
                        label = { Text("No") },
                        enabled = editingEnabled,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = {
                        onUpdate(candidate.copy(decision = CandidateDecision.ACCEPTED, isEditing = false))
                    },
                ) {
                    Text("Accept")
                }
                TextButton(
                    onClick = {
                        onUpdate(candidate.copy(decision = CandidateDecision.PENDING, isEditing = true))
                    },
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = {
                        onUpdate(candidate.copy(decision = CandidateDecision.DISCARDED, isEditing = false))
                    },
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = XCalendarTheme.typography.bodySmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private suspend fun saveCandidates(
    calendar: Calendar,
    candidates: List<EditableCandidate>,
    referenceDate: LocalDate,
    timeZone: TimeZone,
    createEventUseCase: CreateEventUseCase,
): String? {
    if (candidates.isEmpty()) return "No events selected."
    var hadError = false
    candidates.forEach { candidate ->
        val event = candidate.toEvent(calendar, referenceDate, timeZone)
        if (event == null) {
            hadError = true
        } else {
            createEventUseCase(event)
        }
    }
    return if (hadError) {
        "Some events need a valid date/time before saving."
    } else {
        null
    }
}

private enum class CandidateDecision {
    PENDING,
    ACCEPTED,
    DISCARDED,
}

private data class EditableCandidate(
    val id: String,
    val title: String,
    val dateText: String,
    val startTimeText: String,
    val endTimeText: String,
    val allDay: Boolean,
    val category: ImportCategory,
    val selectedPersonId: String?,
    val suggestedRecurringRule: String?,
    val addAsRecurring: Boolean,
    val decision: CandidateDecision,
    val isEditing: Boolean,
    val sourceText: String,
)

private fun OcrCandidateEvent.toEditable(
    defaultPersonId: String?,
    repeatedTitleCount: Int,
): EditableCandidate {
    val detectedCategory = ImportCategoryClassifier.classify(title, sourceText)
    val recurringRule =
        OcrStructuringEngine.inferRecurringRule(sourceText, startDate)
            ?: OcrStructuringEngine.inferRecurringRule(title, startDate)
            ?: if (repeatedTitleCount > 1) "FREQ=WEEKLY" else null
    return EditableCandidate(
        id = id,
        title = title,
        dateText = startDate?.let { "${it.month.number}/${it.day}" } ?: "",
        startTimeText = startTime?.let { it.toString() } ?: "",
        endTimeText = endTime?.let { it.toString() } ?: "",
        allDay = allDay,
        category = if (detectedCategory == ImportCategory.OTHER) ImportCategory.SCHOOL else detectedCategory,
        selectedPersonId = defaultPersonId,
        suggestedRecurringRule = recurringRule,
        addAsRecurring = recurringRule != null,
        decision = CandidateDecision.PENDING,
        isEditing = true,
        sourceText = sourceText,
    )
}

@OptIn(ExperimentalUuidApi::class)
private fun EditableCandidate.toEvent(
    calendar: Calendar,
    referenceDate: LocalDate,
    timeZone: TimeZone,
): Event? {
    val titleValue = title.trim()
    if (titleValue.isBlank()) return null
    val date = OcrStructuringEngine.parseDate(dateText, referenceDate) ?: return null
    val startTime = if (allDay) LocalTime(0, 0) else OcrStructuringEngine.parseTime(startTimeText)
    val endTime = if (allDay) LocalTime(0, 0) else OcrStructuringEngine.parseTime(endTimeText)

    if (!allDay && startTime == null) return null
    val computedStart = date.atTime(startTime ?: LocalTime(0, 0))
    val computedEnd =
        if (allDay) {
            date.plus(DatePeriod(days = 1)).atTime(LocalTime(0, 0))
        } else {
            val resolvedEnd = endTime ?: startTime?.let { plusMinutes(it, 60) } ?: LocalTime(0, 0)
            date.atTime(resolvedEnd)
        }
    val startMillis = computedStart.toInstant(timeZone).toEpochMilliseconds()
    val endMillis = computedEnd.toInstant(timeZone).toEpochMilliseconds()

    return Event(
        id = Uuid.random().toString(),
        calendarId = calendar.id,
        calendarName = calendar.name,
        title = titleValue,
        description = ImportCategoryClassifier.applyCategory("OCR import: ${sourceText.take(120)}", category),
        location = null,
        startTime = startMillis,
        endTime = endMillis,
        isAllDay = allDay,
        isRecurring = addAsRecurring && !suggestedRecurringRule.isNullOrBlank(),
        recurringRule = if (addAsRecurring) suggestedRecurringRule else null,
        reminderMinutes = emptyList(),
        color = calendar.color,
        affectedPersonIds = selectedPersonId?.let { listOf(it) } ?: emptyList(),
    )
}

private fun plusMinutes(time: LocalTime, minutes: Int): LocalTime {
    val total = (time.hour * 60 + time.minute + minutes) % (24 * 60)
    val hour = total / 60
    val minute = total % 60
    return LocalTime(hour, minute)
}

@Composable
private fun TagChip(label: String) {
    FilterChip(
        selected = false,
        onClick = {},
        enabled = false,
        label = { Text(label) },
    )
}

private fun CandidateDecision.label(): String =
    when (this) {
        CandidateDecision.PENDING -> "Pending"
        CandidateDecision.ACCEPTED -> "Accepted"
        CandidateDecision.DISCARDED -> "Discarded"
    }

private fun normalizeCandidateTitle(title: String): String =
    title
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
