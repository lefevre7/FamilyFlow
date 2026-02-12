package com.debanshu.xcalendar.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskType
import com.debanshu.xcalendar.domain.model.VoiceCaptureSource
import com.debanshu.xcalendar.domain.usecase.inbox.ProcessVoiceNoteUseCase
import com.debanshu.xcalendar.domain.usecase.inbox.VoiceNoteProcessResult
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.task.CreateTaskUseCase
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.platform.VoiceCaptureController
import com.debanshu.xcalendar.platform.rememberVoiceCaptureController
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class QuickAddMode {
    TASK,
    EVENT,
    VOICE,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun QuickAddSheet(
    mode: QuickAddMode,
    onModeChange: (QuickAddMode) -> Unit,
    onRequestEvent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val createTaskUseCase = koinInject<CreateTaskUseCase>()
    val processVoiceNoteUseCase = koinInject<ProcessVoiceNoteUseCase>()
    val notifier = koinInject<PlatformNotifier>()

    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())

    val defaultPersonId = remember(people) {
        people.firstOrNull { it.role == PersonRole.MOM }?.id ?: people.firstOrNull()?.id
    }

    var voiceStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedVoiceText by rememberSaveable { mutableStateOf<String?>(null) }
    var isVoiceProcessing by rememberSaveable { mutableStateOf(false) }
    var selectedPersonId by rememberSaveable { mutableStateOf(defaultPersonId) }

    LaunchedEffect(defaultPersonId, selectedPersonId) {
        if (selectedPersonId == null && defaultPersonId != null) {
            selectedPersonId = defaultPersonId
        }
    }

    val voiceController = rememberVoiceCaptureController(
        onResult = { text ->
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) {
                capturedVoiceText = trimmed
                scope.launch {
                    isVoiceProcessing = true
                    voiceStatus = "Captured. Processing voice note..."
                    try {
                        when (
                            val result =
                                processVoiceNoteUseCase(
                                    rawText = trimmed,
                                    source = VoiceCaptureSource.QUICK_ADD_VOICE,
                                    personId = selectedPersonId,
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
                                notifier.showToast(baseMessage)
                                voiceStatus =
                                    if (result.usedHeuristicFallback) {
                                        "$baseMessage using heuristic fallback."
                                    } else {
                                        "$baseMessage."
                                    }
                            }

                            is VoiceNoteProcessResult.Failure -> {
                                voiceStatus = result.reason.userMessage
                            }
                        }
                    } finally {
                        isVoiceProcessing = false
                    }
                }
            } else {
                voiceStatus = "Didn't catch that. Try again."
            }
        },
        onError = { message ->
            isVoiceProcessing = false
            voiceStatus = message
        },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        QuickAddContent(
            mode = mode,
            onModeChange = onModeChange,
            onRequestEvent = onRequestEvent,
            onDismiss = onDismiss,
            people = people,
            defaultPersonId = defaultPersonId,
            createTaskUseCase = createTaskUseCase,
            voiceController = voiceController,
            voiceStatus = voiceStatus,
            capturedVoiceText = capturedVoiceText,
            isVoiceProcessing = isVoiceProcessing,
            scope = scope,
        )
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddContent(
    mode: QuickAddMode,
    onModeChange: (QuickAddMode) -> Unit,
    onRequestEvent: () -> Unit,
    onDismiss: () -> Unit,
    people: List<Person> = emptyList(),
    defaultPersonId: String? = null,
    createTaskUseCase: CreateTaskUseCase = koinInject(),
    voiceController: VoiceCaptureController? = null,
    voiceStatus: String? = null,
    capturedVoiceText: String? = null,
    isVoiceProcessing: Boolean = false,
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    var selectedPersonId by rememberSaveable { mutableStateOf(defaultPersonId) }
    var selectedPriority by rememberSaveable { mutableStateOf(TaskPriority.SHOULD) }
    var selectedEnergy by rememberSaveable { mutableStateOf(TaskEnergy.MEDIUM) }
    var taskTitle by rememberSaveable { mutableStateOf("") }
    var isTaskVoiceProcessing by rememberSaveable { mutableStateOf(false) }

    val notifier = koinInject<PlatformNotifier>()

    val taskVoiceController = rememberVoiceCaptureController(
        onResult = { text ->
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) {
                taskTitle = trimmed
                isTaskVoiceProcessing = false
                notifier.showToast("Voice captured")
            } else {
                isTaskVoiceProcessing = false
                notifier.showToast("Didn't catch that. Try again.")
            }
        },
        onError = { message ->
            isTaskVoiceProcessing = false
            notifier.showToast(message)
        }
    )

    LaunchedEffect(defaultPersonId, selectedPersonId) {
        if (selectedPersonId == null && defaultPersonId != null) {
            selectedPersonId = defaultPersonId
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Quick add",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == QuickAddMode.TASK,
                onClick = { onModeChange(QuickAddMode.TASK) },
                label = { Text("Task") },
            )
            FilterChip(
                selected = mode == QuickAddMode.EVENT,
                onClick = { onModeChange(QuickAddMode.EVENT) },
                label = { Text("Event") },
            )
            FilterChip(
                selected = mode == QuickAddMode.VOICE,
                onClick = { onModeChange(QuickAddMode.VOICE) },
                label = { Text("Voice") },
            )
        }

        when (mode) {
            QuickAddMode.TASK -> {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quick Task") },
                    placeholder = { Text("Type or say it — e.g., Pick up Sophie at 11:30") },
                    singleLine = false,
                    maxLines = 3,
                )

                if (PlatformFeatures.voiceCapture.supported && taskVoiceController.isAvailable) {
                    TextButton(
                        onClick = {
                            isTaskVoiceProcessing = true
                            taskVoiceController.start()
                        },
                        enabled = !isTaskVoiceProcessing,
                        modifier = Modifier.semantics {
                            contentDescription = if (isTaskVoiceProcessing) {
                                "Task voice capture in progress"
                            } else {
                                "Task voice capture. Double tap to say it."
                            }
                        },
                    ) {
                        Text(if (isTaskVoiceProcessing) "Listening..." else "Say it")
                    }
                }

                if (people.isNotEmpty()) {
                    QuickAddSectionTitle("Who")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        people.forEach { person ->
                            FilterChip(
                                selected = selectedPersonId == person.id,
                                onClick = { selectedPersonId = person.id },
                                label = { Text(person.name) },
                            )
                        }
                    }
                }

                QuickAddSectionTitle("Priority")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.label()) },
                        )
                    }
                }

                QuickAddSectionTitle("Energy")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskEnergy.values().forEach { energy ->
                        FilterChip(
                            selected = selectedEnergy == energy,
                            onClick = { selectedEnergy = energy },
                            label = { Text(energy.label()) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val trimmed = taskTitle.trim()
                            if (trimmed.isNotEmpty()) {
                                val now = Clock.System.now().toEpochMilliseconds()
                                val task =
                                    Task(
                                        id = Uuid.random().toString(),
                                        title = trimmed,
                                        priority = selectedPriority,
                                        energy = selectedEnergy,
                                        type = TaskType.FLEXIBLE,
                                        assignedToPersonId = selectedPersonId,
                                        affectedPersonIds = selectedPersonId?.let { listOf(it) } ?: emptyList(),
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                scope.launch {
                                    createTaskUseCase(task)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = taskTitle.trim().isNotEmpty(),
                    ) {
                        Text("Save")
                    }
                }
            }

            QuickAddMode.EVENT -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Create a timed event",
                            style = XCalendarTheme.typography.titleMedium,
                            color = XCalendarTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Use the event sheet to add details like time and location.",
                            style = XCalendarTheme.typography.bodyMedium,
                            color = XCalendarTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = onRequestEvent,
                        ) {
                            Text("Open event sheet")
                        }
                    }
                }
            }

            QuickAddMode.VOICE -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Voice brain dump",
                            style = XCalendarTheme.typography.titleMedium,
                            color = XCalendarTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Tap below to capture a quick thought.",
                            style = XCalendarTheme.typography.bodyMedium,
                            color = XCalendarTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!PlatformFeatures.voiceCapture.supported || voiceController == null || !voiceController.isAvailable) {
                            Text(
                                text = PlatformFeatures.voiceCapture.reason ?: "Voice capture not available on this device.",
                                style = XCalendarTheme.typography.bodySmall,
                                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            TextButton(
                                onClick = { voiceController.start() },
                                enabled = !isVoiceProcessing,
                            ) {
                                Text(if (isVoiceProcessing) "Processing..." else "Start voice capture")
                            }
                        }
                        capturedVoiceText?.let { captured ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Captured text",
                                        style = XCalendarTheme.typography.labelLarge,
                                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = captured,
                                        style = XCalendarTheme.typography.bodyMedium,
                                        color = XCalendarTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        if (isVoiceProcessing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "Structuring with local AI...",
                                    style = XCalendarTheme.typography.bodySmall,
                                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        voiceStatus?.let {
                            Text(
                                text = it,
                                style = XCalendarTheme.typography.bodySmall,
                                color = XCalendarTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuickAddSectionTitle(text: String) {
    Text(
        text = text,
        style = XCalendarTheme.typography.titleSmall,
        color = XCalendarTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun TaskPriority.label(): String =
    when (this) {
        TaskPriority.MUST -> "Must"
        TaskPriority.SHOULD -> "Should"
        TaskPriority.NICE -> "Nice"
    }

private fun TaskEnergy.label(): String =
    when (this) {
        TaskEnergy.LOW -> "Low"
        TaskEnergy.MEDIUM -> "Medium"
        TaskEnergy.HIGH -> "High"
    }
