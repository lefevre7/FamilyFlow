# ADHD MOM Spec Conformance Matrix (Items 1-16)

Audit date: 2026-02-09

## Scope decisions locked for this phase
- Mom admin only (no partner edit permissions model).
- Google Calendar only (no iCal import in this phase).
- Event people ownership must be implemented without Room migration.

## Method
- Reviewed current implementation for each completed AGENTS item.
- Mapped gaps against the original Family Flow product spec.
- Identified file-level ownership and linked each gap to backlog items `18`-`33`.

## Matrix
| Item | Conformance | Evidence (primary file ownership) | Open gaps vs spec | Mapped backlog |
|---|---|---|---|---|
| 1. Baseline mapping and guardrails | Partial | `docs/adhd_mom_baseline.md`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/navigation/BaseNavigation.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/platform/PlatformFeatureAvailability.kt` | Baseline doc still references legacy screen structure and does not include all post-overhaul architecture details. | 33 |
| 2. Data model expansion | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/model/Person.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/model/Task.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/data/localDataSource/AppDatabase.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/repository/PersonRepository.kt` | Person/task/routine/project/inbox are present, but events do not support "who's affected" ownership. Shared task acceptance flow not modeled. | 18, 21 |
| 3. Schedule aggregation + filtering | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/util/ScheduleEngine.kt` | Conflict detection is basic; suggestion scoring does not yet cover due-date, routine anchors, or configurable load caps. Sorting and now-window behavior are simpler than spec intent. | 20, 22 |
| 4. Navigation overhaul | Mostly complete | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/navigation/NavigableScreen.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/components/CalendarBottomNavigationBar.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/CalendarApp.kt` | Route set is correct, but shared persistent mini-header/lens architecture across Today/Week/Plan is not yet unified. | 19, 21 |
| 5. Today (Survival Mode) | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/todayScreen/TodayScreen.kt` | Grouping and routines exist, but `Done/Snooze/Share` are no-op; strict now-window semantics and who-affected labels are incomplete; lens persistence across restarts missing. | 18, 19, 20 |
| 6. Week (Reality Check) | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/weekRealityScreen/WeekRealityScreen.kt` | Day columns and filters exist, but week does not fully participate in shared persistent lens/mini-header model and lacks event who-affected consistency. | 18, 19, 21 |
| 7. Plan (Brain Dump + Month + Seasonal Projects) | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/planScreen/PlanScreen.kt` | Inbox + month + projects exist, but inline brain-dump capture is missing and lens parity/people labeling is incomplete. Suggestion UX is present but not full v2 spec behavior. | 19, 21, 22, 23 |
| 8. People screen + profile management | Mostly complete | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/peopleScreen/PeopleScreen.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/usecase/person/UpdatePersonUseCase.kt` | Mom-admin-only editing is enforced. Remaining gaps are downstream event/task who-affected usage, not People screen CRUD itself. | 18, 21 |
| 9. Quick-add + voice capture | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/components/dialog/QuickAddDialog.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/platform/VoiceCaptureController.android.kt` | Core modes exist, but FAB long-press reveal affordance is not implemented as explicit shortcut reveal UX; accessibility touch/label polish needed. | 24, 27 |
| 10. OCR import pipeline | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/screen/planScreen/OcrImportSheet.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/platform/OcrCaptureController.android.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/util/OcrStructuringEngine.kt` | Candidate review exists, but recurring-pattern prompt, person assignment in review, and default school-category mapping are missing. | 25, 26 |
| 11. LiteRT-LM integration | Mostly complete | `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/domain/llm/PlatformLocalLlmManager.android.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/domain/llm/LiteRtLlmRuntime.android.kt` | Runtime + asset/download fallback exist. Remaining hardening is warning reduction and ongoing robustness polish. | 31, 33 |
| 12. Google Calendar import + sync | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/domain/usecase/google/SyncGoogleCalendarsUseCase.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/data/google/GoogleCalendarApi.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/sync/GoogleCalendarSyncWorker.kt` | OAuth/import/sync/conflicts exist, but conflict entry points are mostly in Settings and not surfaced contextually from Today/Week. | 30 |
| 13. Reminders, timers, widgets | Partial | `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/domain/notifications/AndroidReminderScheduler.android.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/state/TimerStateHolder.kt`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar/widget/TodayWidget.kt` | Core reminders/timer/widget exist. Remaining UX behavior gaps include richer Today action integration and accessibility polish; travel buffer is static (non-route-aware by design in this phase). | 20, 27 |
| 14. Theme and branding | Partial | `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/theme/Theme.kt`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar/ui/theme/Typography.kt`, `composeApp/src/androidMain/res/values/strings.xml` | Branding/name is correct; reduced motion default/high-contrast/text-size controls are not fully implemented in settings behavior layer. | 27 |
| 15. Add/expand tests (old penultimate) | Partial | `composeApp/src/commonTest/kotlin/com/debanshu/xcalendar/domain/util/ScheduleEngineTest.kt`, `composeApp/src/commonTest/kotlin/com/debanshu/xcalendar/domain/util/OcrStructuringEngineTest.kt`, `composeApp/src/commonTest/kotlin/com/debanshu/xcalendar/domain/util/BrainDumpStructuringEngineTest.kt`, `composeApp/src/androidUnitTest/kotlin/com/debanshu/xcalendar/ui/TodayScreenGroupingTest.kt`, `composeApp/src/androidUnitTest/kotlin/com/debanshu/xcalendar/ui/QuickAddFlowTest.kt` | Existing tests validate baseline behavior but do not cover new backlog areas (event-people sidecar, lens persistence, today action handlers, OCR recurring/category/person mapping, onboarding v2, accessibility controls). | 32 |
| 16. Cleanup and stabilization | Partial | `README.md`, `composeApp/src/commonMain/kotlin/com/debanshu/xcalendar`, `composeApp/src/androidMain/kotlin/com/debanshu/xcalendar` | Functional baseline is stable, but high-signal warnings remain and docs need refresh once backlog items are complete. | 31, 33 |

## Cross-cutting gap summary
- **Data semantics gap:** event-level people ownership and labels are missing (`18`, `21`).
- **Execution UX gap:** Today actions are present but not wired (`20`).
- **Planning/suggestion gap:** v2 suggestion heuristics and load caps are not implemented (`22`).
- **Capture/OCR gap:** recurring/person/category handling in OCR review is incomplete (`25`, `26`).
- **Accessibility gap:** reduced motion default and visibility controls are not complete (`27`).
- **Validation gap:** expanded tests for all above changes are pending (`32`).

## Next implementation order
1. `18` -> `19` -> `20` (unlock core semantics + daily execution quality).
2. `21` -> `22` -> `23` -> `24` (lens parity, suggestion quality, capture UX).
3. `25` -> `26` -> `27` -> `28` -> `29` -> `30` (spec completion).
4. `31` -> `32` -> `33` (hardening, verification, final stabilization).

## Status update (post-implementation)
- Update date: 2026-02-09.
- Backlog items `18` through `32` are implemented and verified by unit/Android unit tests.
- Scope constraints remain intentional for this phase:
  - Google Calendar only (no iCal provider integration).
  - Event people mapping uses sidecar persistence (no Room migration).
  - Mom-admin-only editing model.
- Remaining work is item `33`: final cleanup, assemble verification, and smoke validation pass.
