# Instrumented Test Infrastructure - Implementation Summary

## Completed Items (34-35)

### Item 34: Instrumented Test Infrastructure Setup ✅

**What was implemented:**
1. **Directory Structure**: Created `/composeApp/src/androidInstrumentedTest/kotlin/` with subdirectories:
   - `util/` - Test utilities and helper functions
   - `flows/` - End-to-end flow tests
   - `features/` - Feature-specific integration tests
   - `di/` - Dependency injection test modules

2. **Dependencies**: Added to `build.gradle.kts`:
   - `androidx.compose.ui:ui-test-junit4-android` - Compose UI testing
   - `androidx.test.espresso:espresso-core` - UI automation
   - `androidx.test.uiautomator:uiautomator` - System UI interaction
   - `com.squareup.leakcanary:leakcanary-android-instrumentation` - Memory leak detection
   - `io.insert-koin:koin-test` and `koin-test-junit4` - Koin DI testing
   - `kotlinx-coroutines-test` - Coroutine testing utilities

3. **Base Test Utilities** created in `util/`:
   - **TestRules.kt**: Custom Compose test rule with Koin setup, WorkManager initialization, and test dispatcher configuration
   - **TestActions.kt**: Reusable user actions (navigate, add task, login, toggle filters, etc.)
   - **TestAssertions.kt**: Domain-specific assertions (Today grouping, lens filters, event people, etc.)

**Build Configuration:**
- Added `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` to `defaultConfig`
- All dependencies properly configured with `androidTestImplementation`
- Build verified successfully with `./gradlew :composeApp:assembleDebug`

---

### Item 35: Test Data and Mock Strategy ✅

**What was implemented:**
1. **TestInstrumentedModule** (`di/TestModules.kt`):
   - Provides in-memory Room database (allowMainThreadQueries for testing)
   - Mock implementations for all platform-specific services
   - Centralized test module builder accessible to all tests

2. **Mock Implementations** for:
   - `FakeGoogleCalendarApi` - Mock Google Calendar API with configurable responses
   - `FakeCalendarSyncManager` - Mock sync manager for OAuth and sync flows
   - `FakeLlmManager` - Mock LLM with controllable responses
   - `FakeOcrLlmClient` - Mock OCR structuring client
   - `FakeOcrCaptureController` - Mock camera/gallery capture
   - `FakeGoogleTokenStore` & `FakeGoogleTokenManager` - Mock OAuth token management
   - `FakeReminderScheduler` - Mock notification scheduling
   - `FakeWidgetUpdater` - Mock widget refresh tracking
   - `FakeNotifier` - Mock toast and share actions
   - All preference repositories (EventPeople, Holiday, Lens, Reminder, VoiceDiagnostics)

3. **TestDataFactory** (`util/TestDataFactory.kt`):
   - Factory methods for creating test instances with sensible defaults
   - Person builders (createPerson, createMom)
   - Event builders (createEvent, createGoogleEvent, createTodayEvent, createEventForSection)
   - Task builders (createTask with correct Task model fields)
   - Routine builders (createRoutine with RoutineTimeOfDay)
   - InboxItem builders
   - ExternalCalendar and ExternalEvent builders for sync tests
   - All builders match the actual domain model signatures

**Key Design Decisions:**
- In-memory Room database instead of mocking DAOs (leverages real Room behavior)
- Mock implementations expose call history (`createEventCalls`, `syncCalls`, etc.) for verification
- Configurable responses (`calendarsToReturn`, `eventsToReturn`, etc.) for test scenarios
- Real Room database ensures DAO behavior is tested properly

---

## Next Steps: Items 36-39 (Test Implementation)

### Item 36: Today Survival Flow Tests

**File to create**: `flows/TodayFlowTest.kt`

**Test scenarios to implement**:
1. **Onboarding → Today screen → Morning/Afternoon/Evening grouping**
   - Start app, complete onboarding, verify Today screen shows time-of-day sections
   - Use `TestDataFactory.createEventForSection("morning")`, etc.
   - Assert with `composeTestRule.assertTodayGrouping(hasMorning = true, ...)`

2. **Done action → task completion persists**
   - Create a task, navigate to Today, tap "Done"
   - Verify task status changed to DONE in database
   - Check widget refresh was called (`testModule.mockWidgetUpdater.refreshCount`)

3. **Snooze action → task reschedule**
   - Create task, tap "Snooze"
   - Verify `scheduledStart` updated in database

4. **Share snapshot → sharesheet intent**
   - Tap "Share" on Today screen
   - Verify `testModule.mockNotifier.shares` contains snapshot

5. **Lens filter → Family/Mom/Person filtering**
   - Use `composeTestRule.selectLens("Mom")`
   - Verify only Mom-assigned items visible
   - Check `testModule.mockLensPrefsRepo.selection` value

6. **Today Only toggle → strict time window filtering**
   - Create events at different times
   - Toggle "Today Only"
   - Verify only items within +/-30 min of now are visible

7. **Sticky routines → always visible**
   - Create routine, verify it appears regardless of "Today Only" state

**Accessibility checks**:
- Verify content descriptions on all actions ("Done", "Snooze", "Share")
- Test text-size scaling (increase text size in Settings, verify layout adapts)

---

### Item 37: Quick Add + Google Sync Flow Tests

**Files to create**:
- `flows/QuickAddFlowTest.kt`
- `flows/GoogleSyncFlowTest.kt`

**Quick Add scenarios**:
1. **FAB tap → Task mode → form submission → item appears**
   - Use `composeTestRule.addQuickTask("Laundry")`
   - Verify task in `testModule.mockTaskRepository`

2. **FAB tap → Event mode → opens event sheet**
   - Tap FAB, select "Event", verify event creation UI appears

3. **FAB long-press → shortcuts (Task/Event/Voice)**
   - Long-press FAB, verify shortcuts appear
   - Use UI Automator for long-press gesture if needed

**Google Sync scenarios**:
1. **Settings → OAuth (mocked) → calendar selection → sync**
   - Configure `testModule.mockGoogleApi.calendarsToReturn`
   - Navigate to Settings, tap "Connect Google Calendar"
   - Mock OAuth success, verify calendar list appears

2. **Sync → verify EventSource.GOOGLE events appear**
   - Configure `testModule.mockSyncManager.eventsToReturn`
   - Trigger sync, verify Today shows Google events
   - Check event source filtering

3. **Conflict resolution → conflict sheet actions**
   - Create conflicting local/remote events
   - Trigger sync, verify conflict sheet appears
   - Test "Keep local", "Keep remote", "Merge" actions

**Memory leak detection**:
- Use `LeakDetectionRule` after sync completion

---

### Item 38: Plan Brain Dump + OCR Tests

**Files to create**:
- `flows/PlanFlowTest.kt`
- `features/OcrImportFeatureTest.kt`

**Plan Brain Dump scenarios**:
1. **Inline capture → LLM structuring (mocked) → inbox item created**
   - Navigate to Plan, enter text in brain-dump field
   - Configure `testModule.mockLlmManager.generateResponse`
   - Verify InboxItem with status NEW created

2. **Process action → status transition (NEW → PROCESSED)**
   - Create inbox item, tap "Process"
   - Verify status changed in database

**OCR scenarios**:
1. **Camera/gallery capture (mocked) → OCR processing → review list**
   - Mock `testModule.mockOcrController.captureResult`
   - Navigate to Plan → "Scan schedule"
   - Verify review list appears with parsed events

2. **Recurring-pattern prompt → person assignment → category mapping**
   - Mock OCR result with recurring event
   - Verify "Add as recurring?" prompt appears
   - Assign person, select category, accept
   - Verify EventSource.LOCAL event created with correct metadata

3. **Espresso for cross-Activity camera flows** (if needed):
   - Use `Espresso.onView()` for camera activity interactions
   - Or keep mocked with `FakeOcrCaptureController`

---

### Item 39: Week + People + Lens Filter Tests

**Files to create**:
- `flows/WeekFlowTest.kt`
- `features/PeopleFeatureTest.kt`

**Week scenarios**:
1. **Week screen → day expansion → filters**
   - Navigate to Week
   - Tap on a day column, verify expansion
   - Toggle "Only Mom" / "Only Must"
   - Verify filtering works

2. **Lens switching → verify per-day column updates**
   - Create events assigned to different people
   - Switch lens, verify Week columns update

**People scenarios**:
1. **People screen → edit person → verify lens filter updates**
   - Navigate to People, edit "Mom"
   - Change name or color
   - Verify Today/Week/Plan reflect changes

2. **Event assignment (who's affected) → verify labels**
   - Create event, assign to multiple people
   - Verify "Who's affected" labels on Today/Week/Plan cards
   - Use `composeTestRule.assertEventPeople("Event Title", listOf("Mom", "Child1"))`

**Lens persistence tests**:
1. **Select lens → restart app → verify lens persists**
   - Select "Mom" lens
   - Use `ActivityScenario.recreate()` to simulate restart
   - Verify lens selection persists via `testModule.mockLensPrefsRepo`

**Navigation verification**:
- Use NavBackStack assertions to verify current screen
- Check bottom nav selection state

---

## Running Instrumented Tests

### Local Execution (Developer)
```bash
# Run all instrumented tests
./gradlew :composeApp:connectedDebugAndroidTest

# Run specific test class
./gradlew :composeApp:connectedDebugAndroidTest --tests "*.TodayFlowTest"

# Run with emulator
# 1. Start emulator via Android Studio or command line
# 2. Run tests (will auto-install APK and test APK)
```

### Test Output
- Results: `composeApp/build/reports/androidTests/connected/`
- XML results: `composeApp/build/outputs/androidTest-results/`

### Troubleshooting

**Common Issues**:
1. **Flaky tests**: Use `composeTestRule.waitForIdle()` and `waitUntilExists()` helpers
2. **Permission issues**: Add `GrantPermissionRule` for microphone (voice capture tests)
3. **Koin conflicts**: Ensure `stopKoin()` in teardown
4. **Compose synchronization**: Use `composeTestRule.waitUntil { condition }` for async operations

**Idling Resources**:
- Compose tests auto-wait for Compose recomposition
- For coroutines, use `kotlinx-coroutines-test` TestDispatcher

**Emulator Setup**:
- Min API 30 (per minSdk)
- Recommended: API 33+ for best test stability
- Enable "Disable animations" in Developer Options

---

## Test Coverage Goals

### Priority 1 (Critical Paths)
- Today screen grouping and actions (Item 36)
- Quick Add task flow (Item 37)
- Google sync basic flow (Item 37)

### Priority 2 (Core Features)
- Lens filtering across screens (Items 36, 39)
- Plan brain dump (Item 38)
- Week filtering (Item 39)

### Priority 3 (Enhancements)
- OCR import full flow (Item 38)
- Accessibility validation (Item 36)
- Memory leak detection (Item 37)

---

## Architecture Notes

### Test Module Injection
```kotlin
@get:Rule
val composeTestRule = createInstrumentedComposeRule<MainActivity>()

@get:Rule
val testDataRule = TestDataRule()

@Before
fun setUp() {
    val testModule = testDataRule.testModule.buildModule(context)
    startKoin { modules(testModule) }
}
```

### Mock Configuration Pattern
```kotlin
@Test
fun googleSyncShowsEvents() {
    // Configure mocks
    testModule.mockGoogleApi.calendarsToReturn = listOf(
        TestDataFactory.createExternalCalendar(name = "Work")
    )
    testModule.mockSyncManager.eventsToReturn = listOf(
        TestDataFactory.createExternalEvent(summary = "Meeting")
    )
    
    // Execute flow
    composeTestRule.loginWithGoogle()
    composeTestRule.navigateToScreen("Today")
    
    // Verify
    composeTestRule.onNodeWithText("Meeting").assertIsDisplayed()
}
```

### Database Verification
```kotlin
// Query in-memory database to verify state
val db = get<AppDatabase>()
val tasks = db.taskDao().getAllTasks().first()
assertEquals(1, tasks.size)
assertEquals(TaskStatus.DONE, tasks[0].status)
```

---

## Remaining Work (Items 36-39)

**Estimated Effort**: ~4-6 hours per item
- Each item requires 5-10 test scenarios
- Total: ~20-40 test methods across 6-8 test files

**Implementation Order**:
1. Item 36 (Today) - Foundation for other screens
2. Item 37 (Quick Add + Sync) - Core user flows
3. Item 39 (Week + People) - Multi-screen interactions
4. Item 38 (Plan + OCR) - Most complex (camera/LLM mocks)

**Success Criteria**:
- All tests pass on API 30+ emulator
- No memory leaks detected
- Accessibility checks pass
- Tests run in < 5 minutes total
- Coverage > 70% for core user flows

---

## Files Created (Items 34-35)

### Utilities
- `/composeApp/src/androidInstrumentedTest/kotlin/com/debanshu/xcalendar/util/TestRules.kt`
- `/composeApp/src/androidInstrumentedTest/kotlin/com/debanshu/xcalendar/util/TestActions.kt`
- `/composeApp/src/androidInstrumentedTest/kotlin/com/debanshu/xcalendar/util/TestAssertions.kt`
- `/composeApp/src/androidInstrumentedTest/kotlin/com/debanshu/xcalendar/util/TestDataFactory.kt`

### DI
- `/composeApp/src/androidInstrumentedTest/kotlin/com/debanshu/xcalendar/di/TestModules.kt`

### Configuration
- Updated: `/composeApp/build.gradle.kts` (test dependencies)
- Updated: `/gradle/libs.versions.toml` (test library versions)

**All files compile successfully. Ready for test implementation in items 36-39.**
