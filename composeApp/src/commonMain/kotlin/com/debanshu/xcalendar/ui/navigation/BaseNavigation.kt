package com.debanshu.xcalendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.debanshu.xcalendar.domain.model.Event
import com.debanshu.xcalendar.domain.model.Holiday
import com.debanshu.xcalendar.ui.screen.peopleScreen.PeopleScreen
import com.debanshu.xcalendar.ui.screen.planScreen.PlanScreen
import com.debanshu.xcalendar.ui.screen.settingsScreen.SettingsScreen
import com.debanshu.xcalendar.ui.screen.todayScreen.TodayScreen
import com.debanshu.xcalendar.ui.screen.weekRealityScreen.WeekRealityScreen
import com.debanshu.xcalendar.ui.state.DateStateHolder
import kotlinx.collections.immutable.ImmutableList

@Composable
fun NavigationHost(
    modifier: Modifier,
    backStack: NavBackStack<NavKey>,
    dateStateHolder: DateStateHolder,
    events: ImmutableList<Event>,
    holidays: ImmutableList<Holiday>,
    onEventClick: (Event) -> Unit = {},
    onHolidayClick: (Holiday) -> Unit = {},
) {
    // Track current screen for shared element visibility
    val currentScreen = backStack.lastOrNull()

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry(NavigableScreen.Today) {
                    TodayScreen(
                        dateStateHolder = dateStateHolder,
                        events = events,
                        holidays = holidays,
                        isVisible = currentScreen == NavigableScreen.Today,
                        onEventClick = onEventClick,
                        onNavigateToSettings = {
                            backStack.replaceLast(NavigableScreen.Settings)
                        },
                    )
                }
                entry(NavigableScreen.Week) {
                    WeekRealityScreen(
                        dateStateHolder = dateStateHolder,
                        events = events,
                        holidays = holidays,
                        isVisible = currentScreen == NavigableScreen.Week,
                        onEventClick = onEventClick,
                        onHolidayClick = onHolidayClick,
                        onNavigateToSettings = {
                            backStack.replaceLast(NavigableScreen.Settings)
                        },
                    )
                }
                entry(NavigableScreen.Plan) {
                    PlanScreen(
                        dateStateHolder = dateStateHolder,
                        events = events,
                        holidays = holidays,
                        isVisible = currentScreen == NavigableScreen.Plan,
                        onEventClick = onEventClick,
                    )
                }
                entry(NavigableScreen.People) {
                    PeopleScreen(
                        isVisible = currentScreen == NavigableScreen.People,
                    )
                }
                entry(NavigableScreen.Settings) {
                    SettingsScreen(
                        isVisible = currentScreen == NavigableScreen.Settings,
                    )
                }
            },
    )
}
