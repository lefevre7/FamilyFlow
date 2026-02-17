# Family Flow

A family-first mental offloading scheduling app built for a mom with ADHD. The app reduces cognitive load by organizing people-first schedules into clear execution views, with capture-first flows and conservative automation.

**Core Modes**
- Today Survival
- Week Reality Check
- Plan (Brain Dump + Month + Projects)
- People
- Settings

**Key Capabilities**
- Quick-add for task, event, and voice capture
- Brain-dump inbox with LLM structuring
- OCR import for school calendars using Tesseract4Android + LLM review
- Google Calendar import and two-way sync for selected calendars
- Suggestion engine for flexible tasks with conservative accept/undo
- Gentle reminders, visual timers, and widgets
- Local-first storage with optional sync

**Current Scope And Non-goals**
- Google Calendar is the only external calendar provider in this phase (no iCal link import yet).
- Event people ownership is persisted via a sidecar store (no Room schema migration for event-people mapping).
- Mom is the only admin/editor in this phase; partner and kids are view-oriented roles.
- Suggestions are conservative and require explicit accept; there is no silent auto-reschedule.

**Tech Stack**
- Kotlin Multiplatform + Compose Multiplatform
- Room, WorkManager, Koin
- LiteRT-LM (Gemma 3 1B int4)
- Tesseract4Android

**Setup**
1. Use JDK 21.
2. Add `ClientId` in `local.properties` for Google Calendar OAuth.
3. The Gemma model is bundled in `composeApp/src/androidMain/assets/llm/`. The app can also download the model on demand with a fallback URL to `media.githubusercontent.com`.
4. Tesseract data is bundled in `composeApp/src/androidMain/assets/tessdata/eng.traineddata`.
5. Holidays are fetched from Enrico Holidays API (kayaposoft.com) which is free and requires no API key. Default region is USA/Utah and can be configured in Settings.

**Google OAuth Client Setup (Debug + Release)**
1. Confirm package/redirect used by the app:
   - Package: `com.debanshu.xcalendar`
   - Redirect URI: `com.debanshu.xcalendar:/oauth2redirect`
2. Get debug SHA fingerprints:
   - `./gradlew :composeApp:signingReport`
   - Copy SHA-1/SHA-256 for the `debug` variant.
3. Create (or update) Android OAuth client in Google Cloud Console for debug:
   - Type: `Android`
   - Package name: `com.debanshu.xcalendar`
   - SHA-1: debug SHA-1 from signing report
4. For release sign-in, create a release keystore and read release SHA fingerprints:
   - `keytool -list -v -keystore ~/keystores/adhd-mom-release.jks -alias adhd-mom`
   - Create a second Android OAuth client in Google Cloud with the same package and release SHA-1.
5. Put the active Android client ID in `local.properties`:
   - `ClientId=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com`
6. Rebuild after changing `ClientId`:
   - `./gradlew :composeApp:assembleDebug`

Note: this project currently uses a single `ClientId` value from `local.properties`. Use the debug client ID for debug builds and switch to the release client ID when validating signed release OAuth.

**Install And Run (Android)**
1. Start an emulator from Android Studio Device Manager, or connect a physical Android device with USB debugging enabled.
2. Build the debug APK:
   - `./gradlew :composeApp:assembleDebug`
3. Install the APK:
   - `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
4. Launch the app:
   - `adb shell am start -n com.debanshu.xcalendar/.MainActivity`
5. On first launch, complete or skip onboarding, then land on Today view.

**Build And Test**
1. Build: `./gradlew :composeApp:assembleDebug`
2. Unit tests: `./gradlew :composeApp:test`
3. Android unit tests: `./gradlew :composeApp:testDebugUnitTest`

**Instrumented Tests**

Instrumented tests run on an Android emulator or physical device and provide comprehensive end-to-end validation of UI flows and integrations.

*Run Command*
```bash
./gradlew :composeApp:connectedDebugAndroidTest
```

*Prerequisites*
- Start an emulator or connect a physical device with USB debugging enabled
- Device API level should be 26 (Android 8.0) or higher
- Ensure sufficient storage on device for test APK installation

*Test Structure*
- **flows/** - End-to-end user journey tests (Today Survival, Week Reality Check, Plan Brain Dump, Quick Add, Google Sync)
- **features/** - Feature-specific interaction tests (People management, OCR import, Accessibility validation)
- **performance/** - Performance benchmarks and smoke tests (load times, memory usage, ANR detection)
- **util/** - Reusable test utilities (TestRules, TestActions, TestAssertions, TestDataFactory)
- **di/** - Test dependency injection (TestInstrumentedModule with fake repositories and mocked services)

*Coverage Summary (78 test scenarios)*
- TodayFlowTest: 8 scenarios (grouping, actions, lens filters, Today Only, sticky routines, accessibility)
- QuickAddFlowTest: 6 scenarios (FAB modes, Task/Event/Voice, priority/energy, verification)
- GoogleSyncFlowTest: 7 scenarios (OAuth, calendar sync, conflict resolution, memory leak detection)
- PlanFlowTest: 6 scenarios (inline capture, inbox processing, status transitions, validation)
- OcrImportFeatureTest: 10 scenarios (scan schedule, recurring patterns, person assignment, category mapping)
- WeekFlowTest: 8 scenarios (filters, lens switching, day expansion, persistence, accessibility)
- PeopleFeatureTest: 11 scenarios (edit person, lens updates, role labels, cross-screen sync)
- AccessibilityFeatureTest: 11 scenarios (content descriptions, interactive elements, screen reader compatibility)
- PerformanceTest: 14 scenarios (load times, smoke tests, ANR detection, memory leak detection, baseline logging)

*Performance Baselines*
- Screen load time target: < 500ms
- Screen load time max: < 2000ms
- Baseline measurements are logged during test execution for all main screens (Today/Week/Plan/People/Settings)

*Adding New Tests*
1. Extend **TestRules.kt** for custom Compose test rules with Koin test module setup
2. Add reusable actions to **TestActions.kt** (e.g., `navigateToScreen`, `addTask`, `login`)
3. Add domain-specific assertions to **TestAssertions.kt** (e.g., verify Today grouping, lens filters)
4. Use **TestDataFactory** for test data builders (events, tasks, people with consistent IDs)
5. Follow existing patterns: Flow-based repository APIs with `.first()` for synchronous assertions
6. Use `composeTestRule.waitForIdle()` after navigation and interactions to ensure UI stability

*Troubleshooting Common Issues*

**Emulator Setup**
- Ensure emulator is fully booted before running tests (check `adb devices` shows device as online)
- Use a stable emulator image (Pixel 5, API 34, x86_64 recommended for fast execution)
- Increase emulator RAM to 4GB+ if experiencing crashes during test execution

**Koin Test Module Conflicts**
- If seeing "KoinAppAlreadyStartedException", ensure `@get:Rule` is used for `createAndroidComposeRule`
- Test modules override production modules; check `TestInstrumentedModule` for correct fake registrations
- Use `get<IRepository>()` injection (interface type) instead of concrete `FakeRepository` for flexibility

**Compose Test Synchronization**
- Always call `composeTestRule.waitForIdle()` after navigation or state-changing actions
- Use `runBlocking { delay(100) }` for brief waits when `waitForIdle()` is insufficient (e.g., animation completion)
- For async repository operations, use `.first()` to convert Flow to synchronous value: `repository.getItems().first()`
- Manual polling pattern for state verification:
  ```kotlin
  runBlocking {
      withTimeout(5000) {
          while (condition not met) {
              val currentState = repository.getState().first()
              // check condition
              delay(100)
          }
      }
  }
  ```

**Permission Setup**
- Voice capture tests use `@get:Rule val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)`
- Ensure permissions are granted before executing permission-dependent flows
- If tests fail with permission errors, check emulator permission settings (Settings → Apps → Family Flow → Permissions)

**Flakiness Reduction**
- Use `hasContentDescription()` matchers instead of exact text matching when content is dynamic
- Prefer `substring = true` in `onNodeWithText()` for partial text matching to handle dynamic content
- Increase timeout values if tests fail intermittently on slower devices: `withTimeout(10000)` instead of `withTimeout(5000)`
- Use `performScrollTo()` before assertions if elements may be off-screen

*CI/CD Notes*
- Instrumented tests are currently **developer-run locally** (manual execution before commits)
- Not integrated into CI pipeline due to emulator setup complexity and execution time
- Run full test suite before merging: `./gradlew :composeApp:test :composeApp:testDebugUnitTest :composeApp:connectedDebugAndroidTest`
- Keep Robolectric tests in `androidUnitTest/` for fast feedback; instrumented tests complement with integration coverage

**Clear Cache**
`adb shell am start -n com.debanshu.xcalendar/.MainActivity`

**Signed Release (Android)**
1. Create a release keystore (one-time):
   - `keytool -genkeypair -v -keystore ~/keystores/adhd-mom-release.jks -alias adhd-mom -keyalg RSA -keysize 4096 -validity 10000`
2. Build release artifacts:
   - APK: `./gradlew :composeApp:assembleRelease`
   - AAB: `./gradlew :composeApp:bundleRelease`
3. Sign release APK (`ANDROID_HOME` must be set):
   - `BUILD_TOOLS=$ANDROID_HOME/build-tools/35.0.0`
   - `$BUILD_TOOLS/apksigner sign --ks ~/keystores/adhd-mom-release.jks --ks-key-alias adhd-mom --out composeApp/build/outputs/apk/release/composeApp-release-signed.apk composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk`
4. Verify signed APK:
   - `$BUILD_TOOLS/apksigner verify --verbose --print-certs composeApp/build/outputs/apk/release/composeApp-release-signed.apk`
5. Sign AAB (for Play upload):
   - `jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore ~/keystores/adhd-mom-release.jks composeApp/build/outputs/bundle/release/composeApp-release.aab adhd-mom`
6. Verify signed AAB:
   - `jarsigner -verify -verbose composeApp/build/outputs/bundle/release/composeApp-release.aab`
7. Optional local install of signed APK:
   - `adb install -r composeApp/build/outputs/apk/release/composeApp-release-signed.apk`

**Project Structure (Relevant Paths)**
- `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/`
- `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/`
- `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/`
- `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/platform/`
- `composeApp/src/androidMain/assets/`
