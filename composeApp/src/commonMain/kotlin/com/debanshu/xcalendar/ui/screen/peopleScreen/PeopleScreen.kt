package com.debanshu.xcalendar.ui.screen.peopleScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.ExternalCalendar
import com.debanshu.xcalendar.domain.model.GoogleAccountLink
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.usecase.calendar.GetUserCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.FetchGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.GetGoogleAccountForPersonUseCase
import com.debanshu.xcalendar.domain.usecase.google.ImportGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.LinkGoogleAccountUseCase
import com.debanshu.xcalendar.domain.usecase.google.SyncGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.UnlinkGoogleAccountUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.person.UpdatePersonUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.rememberGoogleAuthController
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PeopleScreen(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    if (!isVisible) return

    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val updatePersonUseCase = koinInject<UpdatePersonUseCase>()
    val getGoogleAccountForPersonUseCase = koinInject<GetGoogleAccountForPersonUseCase>()
    val linkGoogleAccountUseCase = koinInject<LinkGoogleAccountUseCase>()
    val unlinkGoogleAccountUseCase = koinInject<UnlinkGoogleAccountUseCase>()
    val fetchGoogleCalendarsUseCase = koinInject<FetchGoogleCalendarsUseCase>()
    val importGoogleCalendarsUseCase = koinInject<ImportGoogleCalendarsUseCase>()
    val syncGoogleCalendarsUseCase = koinInject<SyncGoogleCalendarsUseCase>()
    val getCurrentUserUseCase = koinInject<GetCurrentUserUseCase>()
    val scope = rememberCoroutineScope()

    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())
    val sortedPeople = remember(people) { people.sortedWith(compareBy({ it.sortOrder }, { it.name })) }

    var editTarget by remember { mutableStateOf<Person?>(null) }
    var authTarget by remember { mutableStateOf<Person?>(null) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var pendingCalendars by remember { mutableStateOf<List<ExternalCalendar>>(emptyList()) }
    var pendingAccountId by remember { mutableStateOf<String?>(null) }
    var selectedCalendarIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCalendarSheet by remember { mutableStateOf(false) }

    val authController = rememberGoogleAuthController(
        onSuccess = { result ->
            val person = authTarget
            if (person == null) {
                authMessage = "No profile selected for linking."
                return@rememberGoogleAuthController
            }
            scope.launch {
                linkGoogleAccountUseCase(person.id, result)
                authTarget = null
                val calendars = fetchGoogleCalendarsUseCase(result.accountId)
                pendingCalendars = calendars
                pendingAccountId = result.accountId
                selectedCalendarIds = calendars.filter { it.primary }.map { it.id }.toSet()
                showCalendarSheet = true
            }
        },
        onError = { message ->
            authMessage = message
        },
    )

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "People",
            style = XCalendarTheme.typography.headlineMedium,
            color = XCalendarTheme.colorScheme.onSurface,
        )
        Text(
            text = "Only Mom can connect and manage Google calendars. All profiles are editable.",
            style = XCalendarTheme.typography.bodyMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )

        if (sortedPeople.isEmpty()) {
            EmptyState()
        } else {
            sortedPeople.forEach { person ->
                PersonCard(
                    person = person,
                    getGoogleAccountForPersonUseCase = getGoogleAccountForPersonUseCase,
                    onConnectGoogle = {
                        authTarget = person
                        authController.launch()
                    },
                    onDisconnectGoogle = { account ->
                        scope.launch {
                            unlinkGoogleAccountUseCase(account)
                        }
                    },
                    onManageCalendars = { account ->
                        scope.launch {
                            val calendars = fetchGoogleCalendarsUseCase(account.id)
                            pendingCalendars = calendars
                            pendingAccountId = account.id
                            selectedCalendarIds = calendars.filter { it.primary }.map { it.id }.toSet()
                            showCalendarSheet = true
                        }
                    },
                    onEdit = {
                        editTarget = person
                    },
                )
            }
        }

        authMessage?.let { message ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = XCalendarTheme.typography.bodySmall,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    editTarget?.let { person ->
        EditPersonDialog(
            person = person,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                scope.launch {
                    updatePersonUseCase(updated)
                    editTarget = null
                }
            },
        )
    }

    if (showCalendarSheet) {
        CalendarSelectionSheet(
            calendars = pendingCalendars,
            selectedCalendarIds = selectedCalendarIds,
            onToggle = { id ->
                selectedCalendarIds =
                    if (selectedCalendarIds.contains(id)) {
                        selectedCalendarIds - id
                    } else {
                        selectedCalendarIds + id
                    }
            },
            onDismiss = { showCalendarSheet = false },
            onConfirm = {
                val accountId = pendingAccountId
                if (accountId != null) {
                    val userId = getCurrentUserUseCase()
                    val selected = pendingCalendars.filter { selectedCalendarIds.contains(it.id) }
                    scope.launch {
                        importGoogleCalendarsUseCase(userId, accountId, selected)
                        syncGoogleCalendarsUseCase(manual = true)
                        showCalendarSheet = false
                    }
                } else {
                    showCalendarSheet = false
                }
            },
        )
    }
}

@Composable
private fun PersonCard(
    person: Person,
    getGoogleAccountForPersonUseCase: GetGoogleAccountForPersonUseCase,
    onConnectGoogle: () -> Unit,
    onDisconnectGoogle: (GoogleAccountLink) -> Unit,
    onManageCalendars: (GoogleAccountLink) -> Unit,
    onEdit: () -> Unit,
) {
    val account by remember(person.id) { getGoogleAccountForPersonUseCase(person.id) }
        .collectAsState(initial = null)
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Color(person.color), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = person.name.take(1).uppercase(),
                            style = XCalendarTheme.typography.titleMedium,
                            color = XCalendarTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = person.name,
                            style = XCalendarTheme.typography.titleMedium,
                            color = XCalendarTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoleChip(label = person.role.label())
                            if (person.isAdmin) {
                                RoleChip(label = "Admin")
                            } else {
                                RoleChip(label = "Calendar view only")
                            }
                        }
                    }
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
            }

            if (person.role == PersonRole.MOM || person.role == PersonRole.PARTNER) {
                if (!PlatformFeatures.calendarOAuth.supported) {
                    RoleChip(label = PlatformFeatures.calendarOAuth.reason ?: "Calendar sync unavailable")
                } else if (account == null) {
                    TextButton(onClick = onConnectGoogle, enabled = person.isAdmin) {
                        Text("Connect Google calendar")
                    }
                } else {
                    Text(
                        text = "Google: ${account?.email}",
                        style = XCalendarTheme.typography.bodyMedium,
                        color = XCalendarTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { account?.let(onManageCalendars) }, enabled = person.isAdmin) {
                            Text("Manage calendars")
                        }
                        TextButton(onClick = { account?.let(onDisconnectGoogle) }, enabled = person.isAdmin) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSelectionSheet(
    calendars: List<ExternalCalendar>,
    selectedCalendarIds: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select calendars") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (calendars.isEmpty()) {
                    Text("No calendars available.")
                } else {
                    calendars.forEach { calendar ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(calendar.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onToggle(calendar.id) }) {
                                Text(if (selectedCalendarIds.contains(calendar.id)) "Selected" else "Select")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RoleChip(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = XCalendarTheme.typography.labelMedium,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No profiles yet",
                style = XCalendarTheme.typography.titleMedium,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Add family members to see them here.",
                style = XCalendarTheme.typography.bodyMedium,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditPersonDialog(
    person: Person,
    onDismiss: () -> Unit,
    onSave: (Person) -> Unit,
) {
    var name by rememberSaveable(person.id) { mutableStateOf(person.name) }
    var selectedColor by rememberSaveable(person.id) { mutableStateOf(person.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit ${person.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Avatar color",
                    style = XCalendarTheme.typography.bodyMedium,
                    color = XCalendarTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    colorChoices().forEach { colorValue ->
                        val isSelected = colorValue == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedColor = colorValue },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorValue), CircleShape),
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.White, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim().ifEmpty { person.name }
                    onSave(person.copy(name = trimmed, color = selectedColor))
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun PersonRole.label(): String =
    when (this) {
        PersonRole.MOM -> "Mom"
        PersonRole.PARTNER -> "Partner"
        PersonRole.CHILD -> "Child"
        PersonRole.CAREGIVER -> "Caregiver"
        PersonRole.OTHER -> "Other"
    }

private fun colorChoices(): List<Int> =
    listOf(
        0xFFFFC8A2.toInt(),
        0xFFFFE0A3.toInt(),
        0xFFB5EAD7.toInt(),
        0xFFC7CEEA.toInt(),
        0xFFE2C9F9.toInt(),
        0xFFAED9E0.toInt(),
        0xFFFFB7B2.toInt(),
    )
