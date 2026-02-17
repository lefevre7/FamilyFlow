package com.debanshu.xcalendar.ui.screen.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.llm.LlmModelSource
import com.debanshu.xcalendar.domain.llm.LlmModelStatus
import com.debanshu.xcalendar.domain.llm.LocalLlmManager
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.model.VoiceDiagnosticEntry
import com.debanshu.xcalendar.domain.repository.IVoiceDiagnosticsRepository
import com.debanshu.xcalendar.domain.sync.SyncConflict
import com.debanshu.xcalendar.domain.sync.SyncResolutionAction
import com.debanshu.xcalendar.domain.usecase.calendarSource.GetAllCalendarSourcesUseCase
import com.debanshu.xcalendar.domain.usecase.google.GetAllGoogleAccountsUseCase
import com.debanshu.xcalendar.domain.usecase.google.ResolveSyncConflictUseCase
import com.debanshu.xcalendar.domain.usecase.google.SyncGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.usecase.settings.RescheduleRemindersUseCase
import com.debanshu.xcalendar.domain.usecase.settings.UpdateReminderPreferencesUseCase
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.PlatformNotifier
import com.debanshu.xcalendar.platform.rememberNotificationPermissionController
import com.debanshu.xcalendar.platform.rememberWidgetPinController
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.state.SyncConflictStateHolder
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Instant

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    if (!isVisible) return

    val llmManager = koinInject<LocalLlmManager>()
    val voiceDiagnosticsRepository = koinInject<IVoiceDiagnosticsRepository>()
    val getAllGoogleAccountsUseCase = koinInject<GetAllGoogleAccountsUseCase>()
    val getAllCalendarSourcesUseCase = koinInject<GetAllCalendarSourcesUseCase>()
    val syncGoogleCalendarsUseCase = koinInject<SyncGoogleCalendarsUseCase>()
    val resolveSyncConflictUseCase = koinInject<ResolveSyncConflictUseCase>()
    val conflictStateHolder = koinInject<SyncConflictStateHolder>()
    val getReminderPreferencesUseCase = koinInject<GetReminderPreferencesUseCase>()
    val updateReminderPreferencesUseCase = koinInject<UpdateReminderPreferencesUseCase>()
    val rescheduleRemindersUseCase = koinInject<RescheduleRemindersUseCase>()
    val notifier = koinInject<PlatformNotifier>()
    val notificationPermission = rememberNotificationPermissionController()
    val widgetPinController = rememberWidgetPinController()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var modelStatus by remember { mutableStateOf(llmManager.getStatus()) }
    var downloadProgress by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var widgetMessage by remember { mutableStateOf<String?>(null) }
    var diagnosticsMessage by remember { mutableStateOf<String?>(null) }
    var calendarSourceCount by remember { mutableStateOf(0) }
    var pendingReminderEnable by rememberSaveable { mutableStateOf(false) }
    var reminderPermissionPrompted by rememberSaveable { mutableStateOf(false) }
    val reminderPreferences by remember { getReminderPreferencesUseCase() }
        .collectAsState(initial = ReminderPreferences())

    val googleAccounts by remember { getAllGoogleAccountsUseCase() }.collectAsState(initial = emptyList())
    val conflicts by conflictStateHolder.conflicts.collectAsState()
    val diagnosticsEnabled by remember { voiceDiagnosticsRepository.diagnosticsEnabled }.collectAsState(initial = true)
    val diagnosticEntries by remember { voiceDiagnosticsRepository.entries }.collectAsState(initial = emptyList())
    val latestDiagnosticPayload = remember(diagnosticEntries) { buildLatestVoiceDiagnosticPayload(diagnosticEntries) }

    LaunchedEffect(Unit) {
        modelStatus = llmManager.getStatus()
        llmManager.consumeWarningMessage()?.let { message = it }
        calendarSourceCount = getAllCalendarSourcesUseCase().size
    }

    LaunchedEffect(notificationPermission.isGranted, pendingReminderEnable) {
        if (!pendingReminderEnable) return@LaunchedEffect
        val canEnable =
            !notificationPermission.isRequired || notificationPermission.isGranted
        if (canEnable) {
            updateReminderPreferencesUseCase.setRemindersEnabled(true)
            rescheduleRemindersUseCase()
            pendingReminderEnable = false
        }
    }

    LaunchedEffect(
        reminderPreferences.remindersEnabled,
        notificationPermission.isRequired,
        notificationPermission.isGranted,
    ) {
        if (!notificationPermission.isRequired) return@LaunchedEffect
        if (notificationPermission.isGranted) {
            reminderPermissionPrompted = false
            return@LaunchedEffect
        }
        if (reminderPreferences.remindersEnabled && !reminderPermissionPrompted) {
            reminderPermissionPrompted = true
            notificationPermission.request()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Settings",
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "Notifications, accessibility, sync, and offline AI.",
            style = XCalendarTheme.typography.bodyLarge,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )

        ReminderSection(
            preferences = reminderPreferences,
            notificationPermissionGranted = notificationPermission.isGranted,
            onToggleReminders = { enabled ->
                scope.launch {
                    if (enabled && notificationPermission.isRequired && !notificationPermission.isGranted) {
                        pendingReminderEnable = true
                        notificationPermission.request()
                        return@launch
                    }
                    pendingReminderEnable = false
                    updateReminderPreferencesUseCase.setRemindersEnabled(enabled)
                    rescheduleRemindersUseCase()
                }
            },
            onPrepMinutesChange = { minutes ->
                scope.launch {
                    updateReminderPreferencesUseCase.setPrepMinutes(minutes)
                    rescheduleRemindersUseCase()
                }
            },
            onTravelBufferChange = { minutes ->
                scope.launch {
                    updateReminderPreferencesUseCase.setTravelBufferMinutes(minutes)
                    rescheduleRemindersUseCase()
                }
            },
            onAllDayTimeChange = { hour, minute ->
                scope.launch {
                    updateReminderPreferencesUseCase.setAllDayTime(hour, minute)
                    rescheduleRemindersUseCase()
                }
            },
            onSummaryToggle = { enabled ->
                scope.launch {
                    updateReminderPreferencesUseCase.setSummaryEnabled(enabled)
                    rescheduleRemindersUseCase()
                }
            },
        )

        AccessibilitySection(
            preferences = reminderPreferences,
            onReducedMotionToggle = { enabled ->
                scope.launch {
                    updateReminderPreferencesUseCase.setReducedMotionEnabled(enabled)
                }
            },
            onHighContrastToggle = { enabled ->
                scope.launch {
                    updateReminderPreferencesUseCase.setHighContrastEnabled(enabled)
                }
            },
            onTextScaleSelect = { scale ->
                scope.launch {
                    updateReminderPreferencesUseCase.setTextScale(scale)
                }
            },
        )

        WidgetSection(
            canRequestPin = widgetPinController.isSupported,
            message = widgetMessage,
            onRequestPin = {
                val requested = widgetPinController.requestTodayWidgetPin()
                widgetMessage =
                    if (requested) {
                        "Widget pin request sent. Choose where to place it on Home screen."
                    } else {
                        "Pin request unavailable on this launcher. Long press Home > Widgets > Family Flow."
                    }
            },
        )

        OfflineAiSection(
            modelStatus = modelStatus,
            downloadProgress = downloadProgress,
            message = message,
            onPrepare = {
                scope.launch {
                    val ready = llmManager.ensureModelAvailable()
                    if (!ready) {
                        message = "Unable to prepare the bundled model."
                    }
                    modelStatus = llmManager.getStatus()
                }
            },
            onDownload = {
                scope.launch {
                    downloadProgress = 0
                    val success =
                        llmManager.downloadModel { progress ->
                            downloadProgress = progress
                        }
                    downloadProgress = null
                    message = if (success) {
                        "Model downloaded successfully."
                    } else {
                        "Download failed. Check your connection and try again."
                    }
                    modelStatus = llmManager.getStatus()
                }
            },
            onDelete = {
                scope.launch {
                    val deleted = llmManager.deleteModel()
                    message = if (deleted) {
                        "Local model deleted."
                    } else {
                        "Unable to delete local model."
                    }
                    modelStatus = llmManager.getStatus()
                }
            },
        )

        VoiceDiagnosticsSection(
            enabled = diagnosticsEnabled,
            entries = diagnosticEntries,
            message = diagnosticsMessage,
            hasLatestAttempt = !latestDiagnosticPayload.isNullOrBlank(),
            onToggleEnabled = { enabled ->
                scope.launch {
                    voiceDiagnosticsRepository.setDiagnosticsEnabled(enabled)
                    diagnosticsMessage =
                        if (enabled) {
                            "Voice diagnostics enabled."
                        } else {
                            "Voice diagnostics disabled."
                        }
                }
            },
            onClear = {
                scope.launch {
                    voiceDiagnosticsRepository.clear()
                    diagnosticsMessage = "Voice diagnostics cleared."
                }
            },
            onCopyLatest = {
                val payload = latestDiagnosticPayload ?: return@VoiceDiagnosticsSection
                clipboardManager.setText(AnnotatedString(payload))
                diagnosticsMessage = "Copied latest attempt diagnostics."
            },
            onShareLatest = {
                val payload = latestDiagnosticPayload ?: return@VoiceDiagnosticsSection
                notifier.shareText(
                    subject = "Family Flow - Voice diagnostics",
                    text = payload,
                )
            },
        )

        GoogleSyncSection(
            accountCount = googleAccounts.size,
            calendarCount = calendarSourceCount,
            conflicts = conflicts,
            onSyncNow = {
                scope.launch {
                    syncGoogleCalendarsUseCase(manual = true)
                }
            },
            onResolve = { conflict, action ->
                scope.launch {
                    resolveSyncConflictUseCase(conflict, action)
                }
            },
        )
    }
}

@Composable
private fun WidgetSection(
    canRequestPin: Boolean,
    message: String?,
    onRequestPin: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Home Screen Widget",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (!PlatformFeatures.widgets.supported) {
            StatusCard(PlatformFeatures.widgets.reason ?: "Widgets are not available on this platform.")
            return
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Pin the compact Today widget with Quick Task and Voice shortcuts.",
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = onRequestPin,
                    enabled = canRequestPin,
                ) {
                    Text("Add widget to Home screen")
                }
                if (!canRequestPin) {
                    Text(
                        text = "If this button is disabled, long press Home > Widgets > Family Flow.",
                        style = XCalendarTheme.typography.bodySmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        message?.let { StatusCard(it) }
    }
}

@Composable
private fun OfflineAiSection(
    modelStatus: LlmModelStatus,
    downloadProgress: Int?,
    message: String?,
    onPrepare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Offline AI (Gemma 3 1B)",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (!PlatformFeatures.localLlm.supported) {
            StatusCard(PlatformFeatures.localLlm.reason ?: "Local AI not available.")
            return
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = statusLine(modelStatus),
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                Text(
                    text = sizeLine(modelStatus),
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                modelStatus.incompatibilityMessage?.let {
                    StatusCard(it)
                }
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Downloading… $downloadProgress%",
                        style = XCalendarTheme.typography.labelMedium,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (modelStatus.source == LlmModelSource.ASSET) {
                            TextButton(onClick = onPrepare) { Text("Prepare bundled model") }
                        }
                        TextButton(onClick = onDownload) { Text("Download latest") }
                        if (modelStatus.source == LlmModelSource.LOCAL) {
                            TextButton(onClick = onDelete) { Text("Delete local model") }
                        }
                    }
                }
            }
        }

        message?.let { StatusCard(it) }
    }
}

@Composable
internal fun VoiceDiagnosticsSection(
    enabled: Boolean,
    entries: List<VoiceDiagnosticEntry>,
    message: String?,
    hasLatestAttempt: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onClear: () -> Unit,
    onCopyLatest: () -> Unit,
    onShareLatest: () -> Unit,
) {
    val sortedEntries = remember(entries) { entries.sortedByDescending { it.timestampMillis } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Voice + Local AI diagnostics",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToggleRow(
                    label = "Enable detailed diagnostics",
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCopyLatest, enabled = hasLatestAttempt) {
                        Text("Copy latest attempt")
                    }
                    TextButton(onClick = onShareLatest, enabled = hasLatestAttempt) {
                        Text("Share latest attempt")
                    }
                    TextButton(onClick = onClear, enabled = sortedEntries.isNotEmpty()) {
                        Text("Clear")
                    }
                }
                if (sortedEntries.isEmpty()) {
                    Text(
                        text = "No diagnostics logged yet.",
                        style = XCalendarTheme.typography.bodySmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    sortedEntries.forEach { entry ->
                        VoiceDiagnosticsEntryCard(entry)
                    }
                }
            }
        }
        message?.let { StatusCard(it) }
    }
}

@Composable
internal fun VoiceDiagnosticsEntryCard(entry: VoiceDiagnosticEntry) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${entry.source.name} • ${entry.step.name} • ${entry.level.name}",
                style = XCalendarTheme.typography.labelLarge,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "${formatDiagnosticTimestamp(entry.timestampMillis)} • Session ${entry.sessionId}",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.message,
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            entry.attemptIndex?.let {
                Text(
                    text = "Attempt: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.taskCount?.let {
                Text(
                    text = "Tasks: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.transcript?.let {
                Text(
                    text = "Transcript: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
            }
            entry.llmPrompt?.let {
                Text(
                    text = "Prompt: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
            }
            entry.llmRawResponse?.let {
                Text(
                    text = "Raw output: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
            }
            entry.llmExtractedJson?.let {
                Text(
                    text = "Extracted JSON: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
            }
            entry.errorMessage?.let {
                Text(
                    text = "Error: $it",
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ReminderSection(
    preferences: ReminderPreferences,
    notificationPermissionGranted: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    onPrepMinutesChange: (Int) -> Unit,
    onTravelBufferChange: (Int) -> Unit,
    onAllDayTimeChange: (Int, Int) -> Unit,
    onSummaryToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Reminders & Timers",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToggleRow(
                    label = "Enable reminders",
                    checked = preferences.remindersEnabled,
                    onCheckedChange = onToggleReminders,
                )
                if (preferences.remindersEnabled && !notificationPermissionGranted) {
                    Text(
                        text = "Notifications are off. Turn them on to receive reminders.",
                        style = XCalendarTheme.typography.bodySmall,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SelectionRow(
                    label = "Prep time",
                    options = listOf(10, 20, 30),
                    selected = preferences.prepMinutes,
                    onSelect = onPrepMinutesChange,
                    formatter = { "$it min" },
                )
                SelectionRow(
                    label = "Travel buffer",
                    options = listOf(0, 10, 20),
                    selected = preferences.travelBufferMinutes,
                    onSelect = onTravelBufferChange,
                    formatter = { if (it == 0) "None" else "+$it min" },
                )
                SelectionRow(
                    label = "All‑day reminder",
                    options = listOf(8, 9, 10),
                    selected = preferences.allDayHour,
                    onSelect = { hour -> onAllDayTimeChange(hour, 0) },
                    formatter = { "${it}:00 AM" },
                )
                ToggleRow(
                    label = "Morning + mid‑day summary",
                    checked = preferences.summaryEnabled,
                    onCheckedChange = onSummaryToggle,
                )
            }
        }
    }
}

@Composable
private fun AccessibilitySection(
    preferences: ReminderPreferences,
    onReducedMotionToggle: (Boolean) -> Unit,
    onHighContrastToggle: (Boolean) -> Unit,
    onTextScaleSelect: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Accessibility",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToggleRow(
                    label = "Reduced motion (recommended)",
                    checked = preferences.reducedMotionEnabled,
                    onCheckedChange = onReducedMotionToggle,
                )
                ToggleRow(
                    label = "High contrast",
                    checked = preferences.highContrastEnabled,
                    onCheckedChange = onHighContrastToggle,
                )
                Text(
                    text = "Text size",
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(0.95f to "S", 1.0f to "M", 1.1f to "L", 1.2f to "XL")
                    options.forEach { (scale, label) ->
                        TextButton(onClick = { onTextScaleSelect(scale) }) {
                            Text(
                                text = label,
                                color =
                                    if (kotlin.math.abs(preferences.textScale - scale) < 0.01f) {
                                        XCalendarTheme.colorScheme.primary
                                    } else {
                                        XCalendarTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SelectionRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    formatter: (Int) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                TextButton(onClick = { onSelect(option) }) {
                    Text(
                        text = formatter(option),
                        color = if (isSelected) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurfaceVariant,
                    )
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusLine(status: LlmModelStatus): String {
    val label =
        when (status.source) {
            LlmModelSource.LOCAL -> "Local model ready"
            LlmModelSource.ASSET -> "Bundled model available"
            LlmModelSource.NONE -> "Model not available"
        }
    return if (!status.available) {
        "$label (not usable)"
    } else {
        label
    }
}

private fun sizeLine(status: LlmModelStatus): String {
    return if (status.sizeBytes > 0L) {
        "Size: ${formatBytes(status.sizeBytes)}"
    } else {
        "Expected size: ${formatBytes(status.requiredBytes)}"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}

internal fun buildLatestVoiceDiagnosticPayload(entries: List<VoiceDiagnosticEntry>): String? {
    if (entries.isEmpty()) return null
    val latestSessionId = entries.maxByOrNull { it.timestampMillis }?.sessionId ?: return null
    val sessionEntries =
        entries.filter { it.sessionId == latestSessionId }.sortedBy { it.timestampMillis }
    if (sessionEntries.isEmpty()) return null

    return buildString {
        appendLine("Family Flow voice diagnostics")
        appendLine("Session: $latestSessionId")
        sessionEntries.forEach { entry ->
            appendLine("")
            appendLine("[${formatDiagnosticTimestamp(entry.timestampMillis)}] ${entry.source.name} ${entry.step.name} ${entry.level.name}")
            appendLine("Message: ${entry.message}")
            entry.attemptIndex?.let { appendLine("Attempt: $it") }
            entry.taskCount?.let { appendLine("Tasks: $it") }
            entry.transcript?.let { appendLine("Transcript: $it") }
            entry.llmPrompt?.let { appendLine("Prompt: $it") }
            entry.llmRawResponse?.let { appendLine("Raw output: $it") }
            entry.llmExtractedJson?.let { appendLine("Extracted JSON: $it") }
            entry.errorMessage?.let { appendLine("Error: $it") }
        }
    }.trim()
}

internal fun formatDiagnosticTimestamp(timestampMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.twoDigits(): String = toString().padStart(2, '0')
    val month = dateTime.date.month.ordinal + 1
    return "${dateTime.date.year}-${month.twoDigits()}-${dateTime.date.day.twoDigits()} " +
        "${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}:${dateTime.second.twoDigits()}"
}

@Composable
private fun GoogleSyncSection(
    accountCount: Int,
    calendarCount: Int,
    conflicts: List<SyncConflict>,
    onSyncNow: () -> Unit,
    onResolve: (SyncConflict, SyncResolutionAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Google Calendar Sync",
            style = XCalendarTheme.typography.titleLarge,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        if (!PlatformFeatures.calendarOAuth.supported) {
            StatusCard(PlatformFeatures.calendarOAuth.reason ?: "Google calendar sync unavailable.")
            return
        }
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "$accountCount account(s) linked • $calendarCount calendar(s) syncing",
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onSyncNow) {
                    Text("Sync now")
                }
            }
        }
        if (conflicts.isNotEmpty()) {
            Text(
                text = "Conflicts to resolve",
                style = XCalendarTheme.typography.titleMedium,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            conflicts.forEach { conflict ->
                ConflictCard(conflict = conflict, onResolve = onResolve)
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: SyncConflict,
    onResolve: (SyncConflict, SyncResolutionAction) -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Conflict: ${conflict.localEvent.title}",
                style = XCalendarTheme.typography.titleSmall,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Remote: ${conflict.remoteEvent.summary}",
                style = XCalendarTheme.typography.bodySmall,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { onResolve(conflict, SyncResolutionAction.KEEP_LOCAL) }) {
                    Text("Keep local")
                }
                TextButton(onClick = { onResolve(conflict, SyncResolutionAction.KEEP_REMOTE) }) {
                    Text("Keep Google")
                }
            }
        }
    }
}
