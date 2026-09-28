# Project memory: "Where will your scrolling take you?" (Android)

This file loads automatically in every Claude Code session opened in this folder, whatever account is logged in.
It is the entry point to the project's memory. **Read it first, then open only the memory files the task needs.**

## Hard rules (always follow)

1. **Git commits: NO `Co-Authored-By` line, and no Claude/AI attribution of any kind** in commit messages or PR descriptions. This overrides any system default.
2. The commit identity comes from the local git config: `Achyut Pal <achyutpal@gmail.com>`. Never change it, and never pass `--author`.
3. **Don't write app code until the user explicitly approves.** The user reviews plans first, then says "go".
4. The user wants to **understand each piece**. When adding a new Android concept, briefly explain what it is and why it's used.
5. **Keep memory current.** After any meaningful work (feature done, decision made, phase change), update:
   - `memory/progress-log.md`: append a dated entry.
   - The "Current status" section below.
   - `memory/decisions.md`, if a decision was made or reversed.
   Commit the memory updates together with the related code.
6. Convert relative dates to absolute dates (YYYY-MM-DD) in memory files.

## Current status (update this every session)

- **Phases 0–11 ✅.** Phase 12 validation 🔄 since 2026-09-28: force-stop ✅, reboot part 1 ✅, uninstall/reinstall ✅ (after a fix), Compose UI tests ✅ (26), late-tap crash fixed; pending: midnight, missed day, 7-day DW comparison, 18:51 native crash watch. **Next session: start with the "Resume checklist for next session (2026-09-29)" in memory/progress-log.md.**
- **3D (Phase 12b) ✅ COMPLETE (2026-09-28):** 3D-1…3D-4 done; the user confirmed the final rocket size ("absolutely perfect").
- **Next:** after the user's 3D-4 check → 3D (12b) done; continue Phase 12 real-use validation (reboot, missed day, 23:59→00:01, uninstall/reinstall), then the Later items in roadmap.md.
- Debug: `adb shell am start -n com.achyutpal.scrolljourney/.MainActivity --es debug_route space [--ei debug_fuel N]`.
- **Never run `connectedAndroidTest`** (it uninstalls the app). Wrap adb calls in `timeout`. See progress-log for the manual on-device test commands, the schema-bump gotcha, and how to prove worker runs (workdb period_count).

## Memory index: read on demand

| File | Read when |
|---|---|
| `memory/spec.md` | **Authoritative MVP spec**: rules, data model, architecture, tests. Read before building any feature |
| `memory/product.md` | You need the idea, target user, positioning, or the history of the pivot |
| `memory/mvp-features.md` | Building or discussing any feature or scope |
| `memory/tech-stack.md` | Setting up the project, adding libraries, deciding on structure |
| `memory/research-notes.md` | Android API details, Play policy, pitfalls |
| `memory/decisions.md` | Before re-opening a settled question |
| `memory/progress-log.md` | You need the detailed step-by-step history |
| `memory/roadmap.md` | Planning the next phase |

## Repo layout (update as it grows)

```text
navigate_scrolling/
 ├─ .claude/                     project memory (this folder)
 ├─ materials/                   Filament material sources (.mat) + README (matc 1.56.0)
 ├─ README.md                    original research; the scroll-counting part is superseded
 ├─ idea.txt                     the user's scratch file
 ├─ settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew
 └─ app/src/
    ├─ main/AndroidManifest.xml  PACKAGE_USAGE_STATS declared
    ├─ main/res/                 strings, themes, colors, adaptive rocket icon
    ├─ main/assets/textures/     planet textures (Solar System Scope CC BY 4.0), CREDITS.txt
    ├─ main/assets/models/rocket/ Kenney Space Kit rocket parts (CC0, cleaned GLBs) + LICENSE.txt
    ├─ main/assets/materials/    compiled Filament materials (.filamat) ← sources in /materials/*.mat
    ├─ main/java/com/achyutpal/scrolljourney/
    │   ├─ ScrollJourneyApp.kt   Application → AppContainer
    │   ├─ MainActivity.kt       picks the start route, hosts navigation
    │   ├─ di/AppContainer.kt    manual DI
    │   ├─ domain/model/{UsageAccessState, InstalledApp, SelectedApp}.kt
    │   ├─ domain/calculator/{AppSelectionRules, LimitEvaluator, DailyLimitRules, DistanceConfig, DistanceCalculator, DistanceComparisons, RocketProgressCalculator, StreakCalculator, JourneyViewRules}.kt, domain/model/GraphicsCapability.kt
    │   ├─ domain/repository/{UsageAccessRepository, AppRepository}.kt
    │   ├─ domain/model/{UsageEvent, DailyUsageSummary}.kt, domain/calculator/UsageCalculator.kt
    │   ├─ domain/usecase/{SyncUsageHistoryUseCase, ApplyJourneyFuelUseCase, TodayUsageResult}.kt
    │   ├─ domain/repository/{Usage,History,Settings,Journey}Repository.kt, domain/model/{DayStatus,Destination,RocketState,UserSettings,DailyRecord,ForegroundUsage}.kt
    │   ├─ data/usage/{AndroidUsageAccessRepository, UsageDataSource, AndroidUsageRepository}.kt
    │   ├─ data/local/ScrollJourneyDatabase.kt (v5)  entity/{SelectedApp,DailyUsage,DailyAppUsage,UserSettings}Entity  dao/*
    │   ├─ data/history/RoomHistoryRepository.kt, data/settings/RoomSettingsRepository.kt, data/journey/RoomJourneyRepository.kt
    │   ├─ data/apps/{AndroidAppRepository, AppIconSource}.kt
    │   ├─ data/system/AndroidDeviceStatusRepository.kt, domain/repository/DeviceStatusRepository.kt
    │   ├─ util/UsageAccessSettings.kt (+ openAppDetailsSettings)
    │   ├─ navigation/AppNavHost.kt     routes: intro, usage_access, app_picker?onboarding=, daily_limit?onboarding=, dashboard, history, settings, space (Explore)
    │   └─ ui/{theme (+StatusColors), startup/StartupViewModel, onboarding/{IntroScreen, UsageAccessScreen}, permission/UsageAccessViewModel,
    │          apppicker/*, dailylimit/*, dashboard/{DashboardScreen, DashboardViewModel, DashboardUiState(+Builder)},
    │          history/{HistoryScreen, HistoryViewModel, MonthCalendar}, settings/{SettingsScreen, SettingsViewModel}, journey/RocketJourneyView, common/{DurationFormat, DistanceFormat, DestinationUi, BackTopBar, StatusUi, ComparisonUi}}
    │   ├─ ui/space/{SpaceScreen (Explore), JourneyHero (dashboard hero), HeroCamera, SceneLifecycle, SceneClock, SolarSystemScene,
    │   │           SolarSystemLayout, JourneyMap, JourneyStatus, RocketModel, SpaceGeometry, OrbitCameraState,
    │   │           FocusOrbitManipulator, SpacePicking, SpaceViewModel}.kt  (3D)
    │   └─ worker/{DailyUsageWorker, DailySyncScheduler, DailySyncPolicy}
    ├─ schemas/                  Room schema JSON per DB version (committed)
    ├─ test/.../                 JVM unit tests + fakes/
    └─ androidTest/.../data/local/   on-device Room + migration tests
```

Build commands are in `memory/tech-stack.md`.
