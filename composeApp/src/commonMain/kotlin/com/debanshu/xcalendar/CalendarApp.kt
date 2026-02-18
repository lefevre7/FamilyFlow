package com.debanshu.xcalendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.debanshu.xcalendar.ui.CalendarViewModel
import com.debanshu.xcalendar.ui.components.CalendarBottomNavigationBar
import com.debanshu.xcalendar.ui.components.DockPositionFractions
import com.debanshu.xcalendar.ui.components.ErrorSnackbar
import com.debanshu.xcalendar.ui.components.GlobalSelectionVisual
import com.debanshu.xcalendar.ui.components.dialog.QuickAddMode
import com.debanshu.xcalendar.ui.components.dialog.QuickAddSheet
import com.debanshu.xcalendar.ui.components.dialog.AddEventDialog
import com.debanshu.xcalendar.ui.components.dialog.EventDetailsDialog
import com.debanshu.xcalendar.ui.components.dialog.HolidayDetailsDialog
import com.debanshu.xcalendar.domain.repository.IDateSelectionPreferencesRepository
import com.debanshu.xcalendar.domain.repository.IUiPreferencesRepository
import com.debanshu.xcalendar.domain.usecase.person.GetPeopleUseCase
import com.debanshu.xcalendar.ui.navigation.NavigableScreen
import com.debanshu.xcalendar.ui.navigation.NavigationHost
import com.debanshu.xcalendar.ui.navigation.replaceLast
import com.debanshu.xcalendar.ui.screen.onboardingScreen.OnboardingScreen
import com.debanshu.xcalendar.ui.state.DateStateHolder
import com.debanshu.xcalendar.ui.state.LensStateHolder
import com.debanshu.xcalendar.ui.theme.XCalendarTheme
import com.debanshu.xcalendar.ui.viewmodel.EventViewModel
import com.debanshu.xcalendar.platform.getSystemAccessibility
import com.debanshu.xcalendar.domain.model.User
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.domain.model.ReminderPreferences
import com.debanshu.xcalendar.domain.usecase.settings.GetReminderPreferencesUseCase
import com.debanshu.xcalendar.domain.usecase.user.GetCurrentUserUseCase
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
    val lensStateHolder = koinInject<LensStateHolder>()
    val getPeopleUseCase = koinInject<GetPeopleUseCase>()
    val dateSelectionPreferencesRepository = koinInject<IDateSelectionPreferencesRepository>()
    val uiPreferencesRepository = koinInject<IUiPreferencesRepository>()
    val getReminderPreferencesUseCase = koinInject<GetReminderPreferencesUseCase>()
    val getCurrentUserUseCase = koinInject<GetCurrentUserUseCase>()
    val reminderPreferences by remember { getReminderPreferencesUseCase() }.collectAsState(initial = ReminderPreferences())
    val fallbackEventUser =
        remember {
            User(
                id = getCurrentUserUseCase(),
                name = "Mom",
                email = "",
                photoUrl = "",
            )
        }
    CalendarApp(
        calendarViewModel = calendarViewModel,
        eventViewModel = eventViewModel,
        dateStateHolder = dateStateHolder,
        lensStateHolder = lensStateHolder,
        getPeopleUseCase = getPeopleUseCase,
        dateSelectionPreferencesRepository = dateSelectionPreferencesRepository,
        uiPreferencesRepository = uiPreferencesRepository,
        quickAddRequests = quickAddRequests,
        onQuickAddHandled = onQuickAddHandled,
        showOnboardingInitially = showOnboardingInitially,
        onOnboardingCompleted = onOnboardingCompleted,
        reminderPreferences = reminderPreferences,
        fallbackEventUser = fallbackEventUser,
    )
}

@Composable
private fun CalendarApp(
    calendarViewModel: CalendarViewModel,
    eventViewModel: EventViewModel,
    dateStateHolder: DateStateHolder,
    lensStateHolder: LensStateHolder,
    getPeopleUseCase: GetPeopleUseCase,
    dateSelectionPreferencesRepository: IDateSelectionPreferencesRepository,
    uiPreferencesRepository: IUiPreferencesRepository,
    quickAddRequests: Flow<QuickAddMode?>? = null,
    onQuickAddHandled: (() -> Unit)? = null,
    showOnboardingInitially: Boolean = false,
    onOnboardingCompleted: (() -> Unit)? = null,
    reminderPreferences: ReminderPreferences = ReminderPreferences(),
    fallbackEventUser: User,
) {
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val eventUiState by eventViewModel.uiState.collectAsState()
    val dataState by dateStateHolder.currentDateState.collectAsState()
    val lensSelection by lensStateHolder.selection.collectAsState()
    val people by remember { getPeopleUseCase() }.collectAsState(initial = emptyList())
    val backStack = rememberNavBackStack(config, NavigableScreen.Today)
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var quickAddMode by remember { mutableStateOf(QuickAddMode.TASK) }
    var showEventSheet by remember { mutableStateOf(false) }
    var showOnboarding by rememberSaveable { mutableStateOf(showOnboardingInitially) }
    var dockPositionX by rememberSaveable { mutableStateOf(0.5f) }
    var dockPositionY by rememberSaveable { mutableStateOf(1f) }
    // Default false while DataStore loads (avoids flash for users who already dismissed).
    val navDragHintDismissed by uiPreferencesRepository.navDragHintDismissed
        .collectAsState(initial = true)
    val showDragHint = !navDragHintDismissed
    val scope = rememberCoroutineScope()
    val quickAddRequest = quickAddRequests?.collectAsState(initial = null)?.value

    LaunchedEffect(quickAddRequest) {
        if (quickAddRequest != null) {
            quickAddMode = quickAddRequest
            showQuickAddSheet = true
            onQuickAddHandled?.invoke()
        }
    }

    // Persist selected date changes, while app launch defaults to today via DateStateHolder initialization.
    LaunchedEffect(dataState.selectedDate) {
        dateSelectionPreferencesRepository.updateSelectedDateIso(dataState.selectedDate.toString())
    }

    // Use EventViewModel as single source of truth for selected event
    // The details sheet visibility is derived from whether an event is selected
    val selectedEvent = eventUiState.selectedEvent

    // Selected holiday drives HolidayDetailsDialog visibility
    var selectedHoliday by remember { mutableStateOf<Holiday?>(null) }

    val visibleCalendars by remember(calendarUiState.calendars) {
        derivedStateOf { calendarUiState.calendars.filter { it.isVisible } }
    }
    val events = remember(calendarUiState.events) { calendarUiState.events }
    val holidays = remember(calendarUiState.holidays) { calendarUiState.holidays }

    // Combine error messages from both ViewModels
    val displayError = calendarUiState.displayError ?: eventUiState.errorMessage

    // Auto-detect system high-contrast preference
    val systemAccessibility = remember { getSystemAccessibility() }
    val systemHighContrast = systemAccessibility.isHighContrastEnabled()
    val effectiveHighContrast = reminderPreferences.highContrastEnabled || systemHighContrast

    XCalendarTheme(
        highContrastEnabled = effectiveHighContrast,
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
                val selectedScreen = backStack.lastOrNull() as? NavigableScreen ?: NavigableScreen.Today
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
                        holidays = holidays,
                        onEventClick = { event -> eventViewModel.selectEvent(event) },
                        onHolidayClick = { holiday -> selectedHoliday = holiday },
                    )
                    if (selectedScreen.shouldShowGlobalSelectionVisual()) {
                        GlobalSelectionVisual(
                            date = dataState.selectedDate,
                            lensSelection = lensSelection,
                            people = people,
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(
                                        top = paddingValues.calculateTopPadding() + 8.dp,
                                        end = 16.dp,
                                    ).zIndex(1f),
                        )
                    }
                    CalendarBottomNavigationBar(
                        modifier = Modifier.fillMaxSize(),
                        selectedView = selectedScreen,
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
                        dockPosition =
                            DockPositionFractions(
                                x = dockPositionX,
                                y = dockPositionY,
                            ),
                        onDockPositionChange = { position ->
                            dockPositionX = position.x
                            dockPositionY = position.y
                            // User dragged the dock → they learned the feature; dismiss hint forever.
                            if (!navDragHintDismissed) {
                                scope.launch {
                                    uiPreferencesRepository.setNavDragHintDismissed(true)
                                }
                            }
                        },
                        showDragHint = showDragHint && !showOnboarding,
                        onDismissDragHint = {
                            scope.launch {
                                uiPreferencesRepository.setNavDragHintDismissed(true)
                            }
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
                    AddEventDialog(
                        user = calendarUiState.accounts.firstOrNull() ?: fallbackEventUser,
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

                selectedHoliday?.let { holiday ->
                    HolidayDetailsDialog(
                        holiday = holiday,
                        onDismiss = { selectedHoliday = null },
                    )
                }
            }
        }
    }
}

private fun NavigableScreen.shouldShowGlobalSelectionVisual(): Boolean =
    when (this) {
        NavigableScreen.Today,
        NavigableScreen.Week,
        NavigableScreen.Plan,
            -> true
        NavigableScreen.People,
        NavigableScreen.Settings,
            -> false
    }
