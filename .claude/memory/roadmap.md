# Roadmap and phases

These follow the user's spec build order (`spec.md`). Mark phases ✅ done, 🔄 in progress, or ⏳ not started. The current phase is also shown in `.claude/CLAUDE.md`.
Rule: build one small piece → run on the Android 13 phone → test → show the user → review → move on. Never build the whole app at once.

| Phase | Goal | Done when | Status |
|---|---|---|---|
| Planning | Direction, stack, MVP list, spec | Spec approved | ✅ 2026-09-27 |
| 0 | Project setup: Gradle, package, theme, navigation, unit tests | Builds, tests pass | ✅ 2026-09-27 |
| 1 | Onboarding (Intro 1, Intro 2) + Usage Access explanation → Settings → auto-detect on return; revoked warning | Round trip verified **on the physical phone** | ✅ 2026-09-27 (realme, Android 13) |
| 2 | App picker (PackageManager, `<queries>`, default social apps pre-selected) + Room brought in early with SelectedAppEntity only | Correct list on Android 13, selection survives restart | ✅ 2026-09-27 (33 launcher apps listed on the realme) |
| 3 | UsageStats integration (`UsageDataSource`, `queryEvents`, per-app minutes today) | Matches Digital Wellbeing within about ±1 min | ✅ 2026-09-27 (exact match with Digital Wellbeing for all selected apps) |
| 4 | Room + history (add the other 3 entities via DB v2 migration, upsert, backfill, onboardingComplete flag) | Survives restart and reboot | ✅ 2026-09-27 (7 days backfilled, 20–27 Sep, correct per selected apps) |
| 5 | Limit / day-status engine (integer maths) + Daily Limit onboarding step + judged limit per day (DB v3) | Unit tests pass, colours correct on the phone | ✅ 2026-09-27 |
| 6 | Distance engine (DistanceConfig) | Unit tests pass, distance card shows on the phone | ✅ 2026-09-27 |
| 7 | Rocket engine (fuel, thresholds, milestone protection, exactly-once, journey start) | Unit tests pass, rocket card on the phone | ✅ 2026-09-27 |
| 8 | Dashboard (single DashboardUiState) + StreakCalculator (pulled from 9) | Renders real data | ✅ 2026-09-27 |
| 9 | History screen: month calendar grid + current/best streak (calculator already done in 8) + day detail | Month grid and streak counts correct | ✅ 2026-09-27 |
| 10 | WorkManager DailyUsageWorker + reconciliation on app open | Missed days filled, no duplicates | ✅ 2026-09-28 (ran by itself at 00:30, finalized 27 Sep YELLOW, 0 fuel applied once) |
| 11 | Settings (apps, limit, reset journey with confirmation, privacy, background status) | Reset keeps history | 🔄 installed, 18/18 device tests pass; user check pending |
| 12 | 7–14 day real-device validation (reboot, missed day, 23:59→00:01, uninstall/reinstall) | Acceptance checklist in spec §87 fully ticked | ⏳ |
| 12b | **3D solar system journey (USER REQUIREMENT, 2026-09-27)**: replace the flat rocket card with a real-time 3D scene. Sun + Earth, Moon, Mars, Jupiter, Saturn (rings), Uranus, Neptune, Pluto on orbits; the rocket placed on the route by fuel; the user can **orbit the camera 360°**, pinch-zoom and pan; reached planets highlighted. Planned approach: **SceneView for Android (Google Filament)** with glTF/GLB planet models or textured spheres (NASA/Solar System Scope public textures), an orbit camera manipulator, and the scene in a Compose `AndroidView`/SceneView composable. Keep a 2D fallback for low-end devices. Needs its own plan (models, licensing, size, performance) before coding | Scene runs smoothly on the realme, rotates 360°, rocket at the correct stop | ⏳ planned (after core phases 9–11, before or alongside 12 validation; confirm timing with the user) |
| Later | Notifications, widget, XP/badges, share cards, subscription, optional accessibility precise mode | none | ⏳ |
