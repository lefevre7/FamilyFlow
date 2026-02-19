package com.debanshu.xcalendar.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.domain.model.HolidayAnnotation
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.ui.theme.XCalendarColors
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import xcalendar.composeapp.generated.resources.Res
import xcalendar.composeapp.generated.resources.ic_close
import xcalendar.composeapp.generated.resources.ic_description
import xcalendar.composeapp.generated.resources.ic_location
import xcalendar.composeapp.generated.resources.ic_notifications

/**
 * Bottom sheet editor for user-supplied annotations on a [Holiday].
 *
 * - Title and date are **read-only** (displayed but not editable).
 * - Editable fields: description, location, reminder, who's affected.
 * - A "Reset" button clears all annotations when a prior annotation exists.
 *
 * @param holiday The holiday whose annotation is being edited.
 * @param existingAnnotation Current persisted annotation, or null if none.
 * @param onSave Called with the updated [HolidayAnnotation] when the user taps Save.
 * @param onReset Called when the user confirms resetting all annotations.
 * @param onDismiss Called when the sheet is dismissed without saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayAnnotationEditorSheet(
    holiday: Holiday,
    existingAnnotation: HolidayAnnotation?,
    onSave: (HolidayAnnotation) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        HolidayAnnotationEditorContent(
            holiday = holiday,
            existingAnnotation = existingAnnotation,
            people = people,
            onSave = onSave,
            onReset = onReset,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Inner content of the holiday annotation editor. Separated from [HolidayAnnotationEditorSheet]
 * so it can be composed directly in unit tests without a [ModalBottomSheet] wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HolidayAnnotationEditorContent(
    holiday: Holiday,
    existingAnnotation: HolidayAnnotation?,
    people: List<Person> = emptyList(),
    onSave: (HolidayAnnotation) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var description by remember(existingAnnotation) {
        mutableStateOf(existingAnnotation?.description.orEmpty())
    }
    var location by remember(existingAnnotation) {
        mutableStateOf(existingAnnotation?.location.orEmpty())
    }
    var reminderMinutes by remember(existingAnnotation) {
        mutableIntStateOf(existingAnnotation?.reminderMinutes ?: 0)
    }
    var selectedPersonIds by remember(existingAnnotation) {
        mutableStateOf(existingAnnotation?.affectedPersonIds?.toSet() ?: emptySet())
    }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val formattedDate = remember(holiday.date) {
        val localDate = Instant.fromEpochMilliseconds(holiday.date)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val dayOfWeek = localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
        val month = localDate.month.name.lowercase().replaceFirstChar { it.titlecase() }
        "$dayOfWeek, ${localDate.day} $month ${localDate.year}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
            // ── Header row ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("holiday_editor_cancel"),
                ) { Text("Cancel") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existingAnnotation != null && !existingAnnotation.isEmpty()) {
                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.testTag("holiday_editor_reset"),
                        ) {
                            Text("Reset", color = XCalendarTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                HolidayAnnotation(
                                    holidayId = holiday.id,
                                    description = description.takeIf { it.isNotBlank() },
                                    location = location.takeIf { it.isNotBlank() },
                                    reminderMinutes = reminderMinutes.takeIf { it > 0 },
                                    affectedPersonIds = selectedPersonIds.toList(),
                                    updatedAt = System.nanoTime(),
                                ),
                            )
                        },
                        modifier = Modifier.testTag("holiday_editor_save"),
                    ) { Text("Save") }
                }
            }

            // ── Locked: holiday name ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(XCalendarColors.holiday, CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = holiday.name,
                    style = XCalendarTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = XCalendarTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }

            // ── Locked: holiday date ──────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = formattedDate,
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)

            // ── Editable: Who's Affected ──────────────────────────────────────
            AnnotationWhoAffectedSection(
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)

            // ── Editable: Location ────────────────────────────────────────────
            EventOptionRow(
                icon = Res.drawable.ic_location,
                text = "Location",
                onClick = {},
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("Enter location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("holiday_annotation_location_input"),
                singleLine = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)

            // ── Editable: Reminder ────────────────────────────────────────────
            EventOptionRow(
                icon = Res.drawable.ic_notifications,
                text = if (reminderMinutes == 0) "No reminder" else "$reminderMinutes minutes before",
                onClick = { showReminderPicker = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)

            // ── Editable: Description/Notes ───────────────────────────────────
            EventOptionRow(
                icon = Res.drawable.ic_description,
                text = "Notes",
                onClick = {},
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("holiday_annotation_description_input"),
                minLines = 3,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

    // ── Reminder picker sheet ─────────────────────────────────────────────────
    if (showReminderPicker) {
        ModalBottomSheet(
            onDismissRequest = { showReminderPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Reminder", style = XCalendarTheme.typography.titleMedium)
                listOf(0, 10, 20, 30, 60).forEach { minutes ->
                    TextButton(
                        onClick = {
                            reminderMinutes = minutes
                            showReminderPicker = false
                        },
                    ) {
                        Text(
                            text = if (minutes == 0) "No reminder" else "$minutes minutes before",
                            color = if (minutes == reminderMinutes) {
                                XCalendarTheme.colorScheme.primary
                            } else {
                                XCalendarTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }

    // ── Reset confirmation dialog ─────────────────────────────────────────────
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Remove customizations?") },
            text = { Text("All notes, location, reminder, and people assignment will be cleared for this holiday.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirm = false
                    },
                ) {
                    Text("Remove", color = XCalendarTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnotationWhoAffectedSection(
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
