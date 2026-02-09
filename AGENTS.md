# AGENTS TODO — ADHD MOM (Family Flow)

Guardrails
- Android-first; keep KMP compiling with stubs for iOS/desktop.
- Mom admin only (no partner edit / no backend).
- LLM: LiteRT-LM (Gemma3) with asset pack + `media.githubusercontent.com` download fallback.
- OCR: Tesseract4Android, `eng.traineddata` bundled in APK.
- Display name: ADHD MOM; keep applicationId/package unchanged.
- After every item: run current unit tests (`./gradlew :composeApp:test` and `./gradlew :composeApp:testDebugUnitTest` once Android unit tests exist) and fix failures before moving on.
- For each item: search the codebase for related modules; if unclear, ask follow-up questions until high confidence.

- [x] 1. Baseline mapping and guardrail setup. Map current navigation, data flow, and DI wiring; document where new screens, repositories, and storage should attach; add temporary stubs for iOS/desktop targets for any new Android-only features. After completion, run unit tests and fix failures. adhd_mom_baseline.md is in the /docs/ folder.
- [x] 2. Data model expansion. Add Person/Task/Routine/Project/Inbox models, Room entities, DAOs, and migrations; add repository interfaces and implementations; update mappers and use cases; seed default profiles (Mom, partner, 3 kids) in local data. After completion, run unit tests and fix failures.
- [x] 3. Schedule aggregation + filtering. Implement `ScheduleItem` composition (events + tasks), person filters, priority/energy ordering, and “Today Only” time windowing; add basic conflict detection and suggestion slots. After completion, run unit tests and fix failures.
- [x] 4. Navigation overhaul. Replace bottom nav with Today | Week | Plan | People | Settings; wire new navigation keys and routes; keep legacy calendar screens hidden. After completion, run unit tests and fix failures.
- [x] 5. Today (Survival Mode). Implement Morning/Afternoon/Evening grouped cards with “+N more,” Today-only toggle, quick actions (Start/Done/Snooze/Share), and sticky routines. After completion, run unit tests and fix failures.
- [x] 6. Week (Reality Check). Implement per-day columns, person color bars, “Only Mom required” and “Only Must” filters, and day bottom-sheet expansion. After completion, run unit tests and fix failures.
- [x] 7. Plan (Brain Dump + Month + Seasonal Projects). Add inbox list, processing actions, seasonal project cards, and month overview; integrate suggestion acceptance flow. After completion, run unit tests and fix failures.
- [x] 8. People screen + profile management. Provide profile list, avatar/color editing, and role labels; enforce Mom-admin-only editing rules; keep others view-only. After completion, run unit tests and fix failures.
- [x] 9. Quick-add + voice capture. Implement FAB with Task (default), Event, and Voice; add quick task modal with tags/energy/priority; add SpeechRecognizer flow for brain-dump capture. After completion, run unit tests and fix failures.
- [x] 10. OCR import pipeline. Add camera/gallery capture, Tesseract OCR, review list of candidate events, and per-item edit/accept; route OCR text through LLM structuring (strict JSON schema + validation). After completion, run unit tests and fix failures.
- [x] 11. LiteRT-LM integration. Port the StockSignal LiteRT-LM runtime and model management (asset pack + download fallback); add model download UI/flow; integrate GPU/CPU fallback and incompatibility handling; expose LLM service to OCR + brain dump processing. After completion, run unit tests and fix failures.
- [x] 12. Google Calendar import + sync. Add OAuth flow (client IDs from `local.properties`), selected calendar import, two-way sync, and background refresh (manual + 6h); add conflict resolution sheet with suggested actions. After completion, run unit tests and fix failures.
- [x] 13. Reminders, timers, and widgets. Implement prep/start notifications, optional travel-time buffer (location permission), visual timers, and widgets (Today snapshot + Quick-add). After completion, run unit tests and fix failures.
- [x] 14. Theme and branding. Retune Material3 Expressive to calm palette and reduced motion defaults; update typography sizes; rename display name to ADHD MOM; replace launcher icon. After completion, run unit tests and fix failures.
- [x] 15. Add/expand tests (penultimate step). Add unit tests for schedule aggregation, suggestion engine, OCR parsing, LLM JSON validation, and quick-add; add Robolectric tests for Today grouping and quick-add flow; run all tests and fix failures.
- [x] 16. Cleanup and stabilization. Remove dead code, update docs/README, resolve warnings where feasible, ensure KMP stubs are safe, and re-run all tests. After completion, run unit tests and fix failures.
