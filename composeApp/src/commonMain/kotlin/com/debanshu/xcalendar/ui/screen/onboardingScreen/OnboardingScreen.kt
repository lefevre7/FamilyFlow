package com.debanshu.xcalendar.ui.screen.onboardingScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.usecase.google.FetchGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.GetGoogleAccountForPersonUseCase
import com.debanshu.xcalendar.domain.usecase.google.ImportGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.google.LinkGoogleAccountUseCase
import com.debanshu.xcalendar.domain.usecase.google.SyncGoogleCalendarsUseCase
import com.debanshu.xcalendar.domain.usecase.person.EnsureDefaultPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
import com.debanshu.xcalendar.platform.PlatformFeatures
import com.debanshu.xcalendar.platform.rememberGoogleAuthController
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val STEP_COUNT = 3

internal data class OnboardingNavigationState(
    val pageIndex: Int,
    val stepCount: Int = STEP_COUNT,
) {
    val isLastPage: Boolean
        get() = pageIndex >= stepCount - 1

    val primaryActionLabel: String
        get() = if (isLastPage) "Open Today" else "Next"

    fun nextPage(): Int = (pageIndex + 1).coerceAtMost(stepCount - 1)

    fun previousPage(): Int = (pageIndex - 1).coerceAtLeast(0)
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    val ensureDefaultPeopleUseCase = koinInject<EnsureDefaultPeopleUseCase>()
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val getGoogleAccountForPersonUseCase = koinInject<GetGoogleAccountForPersonUseCase>()
    val linkGoogleAccountUseCase = koinInject<LinkGoogleAccountUseCase>()
    val fetchGoogleCalendarsUseCase = koinInject<FetchGoogleCalendarsUseCase>()
    val importGoogleCalendarsUseCase = koinInject<ImportGoogleCalendarsUseCase>()
    val syncGoogleCalendarsUseCase = koinInject<SyncGoogleCalendarsUseCase>()
    val getCurrentUserUseCase = koinInject<GetCurrentUserUseCase>()
    val scope = rememberCoroutineScope()

    var pageIndex by rememberSaveable { mutableStateOf(0) }
    var profileStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var importStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var isImporting by rememberSaveable { mutableStateOf(false) }

    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())
    val mom = remember(people) { people.firstOrNull { it.role == PersonRole.MOM } }
    val momAccountFlow = remember(mom?.id) { mom?.id?.let { getGoogleAccountForPersonUseCase(it) } ?: flowOf(null) }
    val momAccount by momAccountFlow.collectAsState(initial = null)
    val navigationState = remember(pageIndex) { OnboardingNavigationState(pageIndex = pageIndex) }
    val isLastPage = navigationState.isLastPage

    val authController =
        rememberGoogleAuthController(
            onSuccess = { authResult ->
                val momId = mom?.id
                if (momId == null) {
                    importStatus = "Mom profile is missing. Add Mom in People, then try again."
                    return@rememberGoogleAuthController
                }
                scope.launch {
                    isImporting = true
                    importStatus = null
                    val result =
                        runCatching {
                            linkGoogleAccountUseCase(momId, authResult)
                            val calendars = fetchGoogleCalendarsUseCase(authResult.accountId)
                            val selectedCalendars = calendars.filter { it.primary }.ifEmpty { calendars.take(1) }
                            if (selectedCalendars.isEmpty()) {
                                throw IllegalStateException("No Google calendars were available to import.")
                            }
                            importGoogleCalendarsUseCase(
                                userId = getCurrentUserUseCase(),
                                accountId = authResult.accountId,
                                calendars = selectedCalendars,
                            )
                            syncGoogleCalendarsUseCase(manual = true)
                            selectedCalendars.size
                        }
                    isImporting = false
                    result
                        .onSuccess { importedCount ->
                            importStatus = "Connected Mom and imported $importedCount calendar(s)."
                        }.onFailure { throwable ->
                            importStatus = throwable.message ?: "Google import failed."
                        }
                }
            },
            onError = { message ->
                isImporting = false
                importStatus = message
            },
        )

    LaunchedEffect(Unit) {
        runCatching { ensureDefaultPeopleUseCase() }
            .onFailure { profileStatus = "Unable to confirm starter profiles right now." }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(XCalendarTheme.colorScheme.surfaceContainerLow)
                .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "ADHD MOM",
                style = XCalendarTheme.typography.headlineLarge,
                color = XCalendarTheme.colorScheme.onSurface,
            )
            Text(
                text = "Family Flow setup in 3 short steps.",
                style = XCalendarTheme.typography.bodyLarge,
                color = XCalendarTheme.colorScheme.onSurfaceVariant,
            )

            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Step ${pageIndex + 1} of $STEP_COUNT",
                        style = XCalendarTheme.typography.labelLarge,
                        color = XCalendarTheme.colorScheme.primary,
                    )
                    when (pageIndex) {
                        0 ->
                            StarterProfilesStep(
                                people = people,
                                statusMessage = profileStatus,
                            )

                        1 ->
                            OptionalGoogleImportStep(
                                momName = mom?.name,
                                momAccountEmail = momAccount?.email,
                                canConnectGoogle = PlatformFeatures.calendarOAuth.supported && authController.isAvailable,
                                isImporting = isImporting,
                                statusMessage = importStatus,
                                onConnectGoogle = { authController.launch() },
                            )

                        else -> DemoStep()
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(STEP_COUNT) { index ->
                    val selected = index == pageIndex
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        XCalendarTheme.colorScheme.primary
                                    } else {
                                        XCalendarTheme.colorScheme.outlineVariant
                                    },
                                ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (pageIndex > 0) {
                    TextButton(onClick = { pageIndex = navigationState.previousPage() }) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            pageIndex = navigationState.nextPage()
                        }
                    },
                ) {
                    Text(navigationState.primaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun StarterProfilesStep(
    people: List<Person>,
    statusMessage: String?,
) {
    val hasMom = people.any { it.role == PersonRole.MOM }
    val hasPartner = people.any { it.role == PersonRole.PARTNER }
    val childCount = people.count { it.role == PersonRole.CHILD }
    val hasKidA = childCount >= 1
    val hasKidB = childCount >= 2
    val hasKidC = childCount >= 3

    Text(
        text = "Starter family profiles",
        style = XCalendarTheme.typography.headlineSmall,
        color = XCalendarTheme.colorScheme.onSurface,
    )
    Text(
        text = "Mom lens + partner + kid presets are ready so you can start quickly.",
        style = XCalendarTheme.typography.bodyMedium,
        color = XCalendarTheme.colorScheme.onSurfaceVariant,
    )
    ProfileCheckRow(label = "Mom (admin lens)", isReady = hasMom)
    ProfileCheckRow(label = "Partner profile", isReady = hasPartner)
    ProfileCheckRow(label = "Kid preset A", isReady = hasKidA)
    ProfileCheckRow(label = "Kid preset B", isReady = hasKidB)
    ProfileCheckRow(label = "Kid preset C", isReady = hasKidC)
    Text(
        text = "You can rename profiles and colors later in People.",
        style = XCalendarTheme.typography.bodySmall,
        color = XCalendarTheme.colorScheme.onSurfaceVariant,
    )
    statusMessage?.let {
        InlineStatusCard(it)
    }
}

@Composable
private fun OptionalGoogleImportStep(
    momName: String?,
    momAccountEmail: String?,
    canConnectGoogle: Boolean,
    isImporting: Boolean,
    statusMessage: String?,
    onConnectGoogle: () -> Unit,
) {
    Text(
        text = "Optional Google import",
        style = XCalendarTheme.typography.headlineSmall,
        color = XCalendarTheme.colorScheme.onSurface,
    )
    Text(
        text = "Connect Mom now to import primary Google calendars. You can skip and do this later in People.",
        style = XCalendarTheme.typography.bodyMedium,
        color = XCalendarTheme.colorScheme.onSurfaceVariant,
    )

    if (momAccountEmail != null) {
        InlineStatusCard("Connected as $momAccountEmail")
    } else if (!PlatformFeatures.calendarOAuth.supported) {
        InlineStatusCard(PlatformFeatures.calendarOAuth.reason ?: "Google OAuth is not available on this platform.")
    } else if (!canConnectGoogle) {
        InlineStatusCard("Google OAuth is unavailable. Add a valid client ID in local.properties.")
    } else {
        Button(
            onClick = onConnectGoogle,
            enabled = !isImporting && momName != null,
        ) {
            Text(if (isImporting) "Connecting..." else "Connect Mom Google calendar")
        }
    }

    Text(
        text = "What happens: link account -> import calendars -> run initial sync.",
        style = XCalendarTheme.typography.bodySmall,
        color = XCalendarTheme.colorScheme.onSurfaceVariant,
    )
    statusMessage?.let {
        InlineStatusCard(it)
    }
}

@Composable
private fun DemoStep() {
    Text(
        text = "Quick demo before you start",
        style = XCalendarTheme.typography.headlineSmall,
        color = XCalendarTheme.colorScheme.onSurface,
    )
    Text(
        text = "Use these low-friction paths during busy days:",
        style = XCalendarTheme.typography.bodyMedium,
        color = XCalendarTheme.colorScheme.onSurfaceVariant,
    )
    DemoBullet(text = "Tap FAB for Quick Task (default). Long-press for Event or Voice.")
    DemoBullet(text = "Use Today Only to hide everything except what is near now.")
    DemoBullet(text = "Switch modes fast: Today | Week | Plan from bottom navigation.")
    DemoBullet(text = "Cards support one-tap Start, Done, Snooze, and Share actions.")
}

@Composable
private fun ProfileCheckRow(
    label: String,
    isReady: Boolean,
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
        Text(
            text = if (isReady) "Ready" else "Pending",
            style = XCalendarTheme.typography.labelMedium,
            color = if (isReady) XCalendarTheme.colorScheme.primary else XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DemoBullet(text: String) {
    Text(
        text = "- $text",
        style = XCalendarTheme.typography.bodyMedium,
        color = XCalendarTheme.colorScheme.onSurface,
    )
}

@Composable
private fun InlineStatusCard(message: String) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = XCalendarTheme.typography.bodySmall,
            color = XCalendarTheme.colorScheme.onSurfaceVariant,
        )
    }
}
