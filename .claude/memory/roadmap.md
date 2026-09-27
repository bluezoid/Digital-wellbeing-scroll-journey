# Roadmap and phases

These follow the user's spec build order (`spec.md`). Mark phases ✅ done, 🔄 in progress, or ⏳ not started. The current phase is also shown in `.claude/CLAUDE.md`.
Rule: build one small piece → run on the Android 13 phone → test → show the user → review → move on. Never build the whole app at once.

| Phase | Goal | Done when | Status |
|---|---|---|---|
| Planning | Direction, stack, MVP list, spec | Spec approved | ✅ 2026-09-27 |
| 0 | Project setup: Gradle, package, theme, navigation, unit tests | Builds, tests pass | ✅ 2026-09-27 |
| 1 | Onboarding (Intro 1, Intro 2) + Usage Access explanation → Settings → auto-detect on return; revoked warning | Round trip verified **on the physical phone** | ✅ 2026-09-27 (realme, Android 13) |
| 2 | App picker (PackageManager, `<queries>`, default social apps pre-selected) + Room brought in early with SelectedAppEntity only | Correct list on Android 13, selection survives restart | ✅ 2026-09-27 (33 launcher apps listed on the realme) |
| 3 | UsageStats integration (`UsageDataSource`, `queryEvents`, per-app minutes today) | Matches Digital Wellbeing within about ±1 min | 🔄 code done, DW comparison pending |
| 4 | Room + history (add the other 3 entities via DB v2 migration, upsert, backfill, onboardingComplete flag) | Survives restart and reboot | ⏳ |
| 5 | Limit / day-status engine (integer maths) | Unit tests pass | ⏳ |
| 6 | Distance engine (DistanceConfig) | Unit tests pass | ⏳ |
| 7 | Rocket engine (fuel, thresholds, milestone protection) | Unit tests pass | ⏳ |
| 8 | Dashboard (single DashboardUiState) | Renders real data | ⏳ |
| 9 | History calendar + streaks | Month grid and streak counts correct | ⏳ |
| 10 | WorkManager DailyUsageWorker + reconciliation on app open | Missed days filled, no duplicates | ⏳ |
| 11 | Settings (apps, limit, reset journey with confirmation, privacy) | Reset keeps history | ⏳ |
| 12 | 7–14 day real-device validation (reboot, missed day, 23:59→00:01, uninstall/reinstall) | Acceptance checklist in spec §87 fully ticked | ⏳ |
| Later | Notifications, widget, XP/badges, share cards, subscription, optional accessibility precise mode | none | ⏳ |
