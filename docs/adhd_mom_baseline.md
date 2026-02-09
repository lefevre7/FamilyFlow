# ADHD MOM baseline map

This file records the current XCalendar architecture and where ADHD MOM features attach.

## Current navigation
- Entry composable: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/CalendarApp.kt`.
- Navigation host: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/navigation/BaseNavigation.kt`.
- Screens: `Month`, `Week`, `Day`, `ThreeDay`, `Schedule` in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/`.
- Bottom nav: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/components/CalendarBottomNavigationBar.kt`.
- Add/edit event sheets: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/components/dialog/`.

## Current data flow
- Domain models: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/model/`.
- Room entities + DAOs: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/localDataSource/`.
- Remote calendar data: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/remoteDataSource/RemoteCalendarApiService.kt`.
- Stores + caching: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/store/`.
- Repositories: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/repository/`.
- Use cases: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/usecase/`.
- ViewModels: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/viewmodel/` and `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/CalendarViewModel.kt`.

## Dependency injection
- Koin modules and `expect fun getDatabase()`: `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/di/Koin.kt`.
- Platform database actuals in `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/di/`,
  `composeApp/src/iosMain/kotlin/com/debanshu/xcalendar/di/`, and
  `composeApp/src/desktopMain/kotlin/com/debanshu/xcalendar/di/`.

## ADHD MOM integration points
- New navigation keys in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/navigation/NavigableScreen.kt`.
- New screens in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/`:
  Today, Week (new layout), Plan, People, Settings.
- Shared UI components in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/components/`.
- New domain models and repositories in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/` and Room entities in `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/localDataSource/model/`.
- Android-only services should be accessed via common interfaces and injected, not called directly from UI.

## Platform stubs
- Android-only features are gated by `PlatformFeatures` in
  `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/platform/PlatformFeatureAvailability.kt`.
- iOS and Desktop actuals return `supported=false` so KMP stays compiling while Android
  implementations evolve.
