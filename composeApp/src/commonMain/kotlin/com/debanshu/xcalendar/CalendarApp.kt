package com.debanshu.xcalendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.debanshu.xcalendar.ui.CalendarViewModel
import com.debanshu.xcalendar.ui.components.CalendarBottomNavigationBar
import com.debanshu.xcalendar.ui.components.ErrorSnackbar
import com.debanshu.xcalendar.ui.components.dialog.QuickAddMode
import com.debanshu.xcalendar.ui.components.dialog.QuickAddSheet
import com.debanshu.xcalendar.ui.components.dialog.AddEventDialog
import com.debanshu.xcalendar.ui.components.dialog.EventDetailsDialog
import com.debanshu.xcalendar.ui.navigation.NavigableScreen
import com.debanshu.xcalendar.ui.navigation.NavigationHost
import com.debanshu.xcalendar.ui.navigation.replaceLast
import com.debanshu.xcalendar.ui.screen.onboardingScreen.OnboardingScreen
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.viewmodel.EventViewModel
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val config =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(NavigableScreen.Today::class, NavigableScreen.Today.serializer())
                    subclass(NavigableScreen.Week::class, NavigableScreen.Week.serializer())
                    subclass(NavigableScreen.Plan::class, NavigableScreen.Plan.serializer())
                    subclass(NavigableScreen.People::class, NavigableScreen.People.serializer())
                    subclass(NavigableScreen.Settings::class, NavigableScreen.Settings.serializer())
                }
            }
    }

@Composable
fun CalendarApp(
    quickAddRequests: Flow<QuickAddMode?>? = null,
    onQuickAddHandled: (() -> Unit)? = null,
    showOnboardingInitially: Boolean = false,
    onOnboardingCompleted: (() -> Unit)? = null,
) {
    val calendarViewModel = koinViewModel<CalendarViewModel>()
    val eventViewModel = koinViewModel<EventViewModel>()
    val dateStateHolder = koinInject<DateStateHolder>()
    val getReminderPreferencesUseCase = koinInject<GetReminderPreferencesUseCase>()
    val reminderPreferences by remember { getReminderPreferencesUseCase() }.collectAsState(initial = ReminderPreferences())
    CalendarApp(
        calendarViewModel = calendarViewModel,
        eventViewModel = eventViewModel,
        dateStateHolder = dateStateHolder,
        quickAddRequests = quickAddRequests,
        onQuickAddHandled = onQuickAddHandled,
        showOnboardingInitially = showOnboardingInitially,
        onOnboardingCompleted = onOnboardingCompleted,
        reminderPreferences = reminderPreferences,
    )
}

@Composable
private fun CalendarApp(
    calendarViewModel: CalendarViewModel,
    eventViewModel: EventViewModel,
    dateStateHolder: DateStateHolder,
    quickAddRequests: Flow<QuickAddMode?>? = null,
    onQuickAddHandled: (() -> Unit)? = null,
    showOnboardingInitially: Boolean = false,
    onOnboardingCompleted: (() -> Unit)? = null,
    reminderPreferences: ReminderPreferences = ReminderPreferences(),
) {
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val eventUiState by eventViewModel.uiState.collectAsState()
    val dataState by dateStateHolder.currentDateState.collectAsState()
    val backStack = rememberNavBackStack(config, NavigableScreen.Today)
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var quickAddMode by remember { mutableStateOf(QuickAddMode.TASK) }
    var showEventSheet by remember { mutableStateOf(false) }
    var showOnboarding by rememberSaveable { mutableStateOf(showOnboardingInitially) }
    val quickAddRequest = quickAddRequests?.collectAsState(initial = null)?.value

    LaunchedEffect(quickAddRequest) {
        if (quickAddRequest != null) {
            quickAddMode = quickAddRequest
            showQuickAddSheet = true
            onQuickAddHandled?.invoke()
        }
    }

    // Use EventViewModel as single source of truth for selected event
    // The details sheet visibility is derived from whether an event is selected
    val selectedEvent = eventUiState.selectedEvent

    val visibleCalendars by remember(calendarUiState.calendars) {
        derivedStateOf { calendarUiState.calendars.filter { it.isVisible } }
    }
    val events = remember(calendarUiState.events) { calendarUiState.events }

    // Combine error messages from both ViewModels
    val displayError = calendarUiState.displayError ?: eventUiState.errorMessage

    XCalendarTheme(
        highContrastEnabled = reminderPreferences.highContrastEnabled,
        reducedMotionEnabled = reminderPreferences.reducedMotionEnabled,
        textScale = reminderPreferences.textScale,
    ) {
        if (showOnboarding) {
            OnboardingScreen(
                onComplete = {
                    showOnboarding = false
                    onOnboardingCompleted?.invoke()
                },
                onSkip = {
                    showOnboarding = false
                    onOnboardingCompleted?.invoke()
                },
            )
        } else {
            Scaffold(
                containerColor = XCalendarTheme.colorScheme.surfaceContainerLow,
                snackbarHost = {
                    ErrorSnackbar(
                        message = displayError,
                        onDismiss = {
                            calendarViewModel.clearError()
                            eventViewModel.clearError()
                        },
                    )
                },
            ) { paddingValues ->
                Box {
                    NavigationHost(
                        modifier =
                            Modifier.padding(
                                top = paddingValues.calculateTopPadding(),
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                            ),
                        backStack = backStack,
                        dateStateHolder = dateStateHolder,
                        events = events,
                    )
                    CalendarBottomNavigationBar(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = paddingValues.calculateBottomPadding()),
                        selectedView = backStack.lastOrNull() as? NavigableScreen ?: NavigableScreen.Today,
                        onViewSelect = { view ->
                            backStack.replaceLast(view)
                        },
                        onAddClick = {
                            quickAddMode = QuickAddMode.TASK
                            showQuickAddSheet = true
                        },
                        onAddTaskShortcut = {
                            quickAddMode = QuickAddMode.TASK
                            showQuickAddSheet = true
                        },
                        onAddEventShortcut = {
                            showEventSheet = true
                        },
                        onAddVoiceShortcut = {
                            quickAddMode = QuickAddMode.VOICE
                            showQuickAddSheet = true
                        },
                    )
                }
                if (showQuickAddSheet) {
                    QuickAddSheet(
                        mode = quickAddMode,
                        onModeChange = { quickAddMode = it },
                        onRequestEvent = {
                            showQuickAddSheet = false
                            showEventSheet = true
                        },
                        onDismiss = { showQuickAddSheet = false },
                    )
                }
                if (showEventSheet) {
                    calendarUiState.accounts.firstOrNull()?.let {
                        AddEventDialog(
                            user = it,
                            calendars = visibleCalendars.toImmutableList(),
                            selectedDate = dataState.selectedDate,
                            onSave = { event ->
                                eventViewModel.addEvent(event)
                                showEventSheet = false
                            },
                            onDismiss = {
                                showEventSheet = false
                            },
                        )
                    }
                }

                selectedEvent?.let { selected ->
                    EventDetailsDialog(
                        event = selected,
                        onEdit = { editedEvent ->
                            eventViewModel.editEvent(editedEvent)
                        },
                        onDismiss = {
                            eventViewModel.clearSelectedEvent()
                        },
                    )
                }
            }
        }
    }
}
