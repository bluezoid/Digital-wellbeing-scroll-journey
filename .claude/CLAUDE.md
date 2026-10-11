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

- **Goal (2026-10-10): production-quality app, no Play Store for now.** Production plan P1–P4 fully approved and **done 2026-10-10**:
  - P1: Android 36 / AGP 8.13 / Kotlin 2.2 upgrade; **signed R8 release build 1.0.0** (key OUTSIDE the repo in `../scroll-journey-keys/`, must be backed up by the user); debug = separate app `.debug` "SJ Dev"; **backup export/import** (JSON via the system file picker) + "Restore from a backup" on the intro.
  - P2: drift-free nightly job (one-time chain at 00:30) + **15-min refresh**; stale future-day cleanup; 3D hero idle pause (0% CPU after 20 s); Auto Backup rules.
  - P3: **home-screen widgets** (Glance: Today 2×1, Journey 4×2) + **limit reminder notifications** (80% / reached; DB v6).
  - P4: lint clean (fixed an API 34-only crash), accessibility, rotation supported, plurals, credits.
- **The user's realme runs the 1.0.1 release build** (three test widgets left on its 2nd home page). 233 JVM + 56 on-device tests ✅.
- **1.0.1 released (2026-10-11)** after the user's Android 16 feedback: widget R8-merge fix, prettier responsive widgets + new Week 4×1 widget, deep-space/Galactic Centre fix (galaxy = glow-dot spiral), guided "restricted setting" help (detected by install source; verified with a real sideload on Android 16). Share package in `../scroll-journey-share/` (arm64 recommended + universal + HOW-TO-INSTALL.txt). Testers report problems via Settings → Share diagnostics. **Next update: versionCode 10002.**
- Phase 12 leftovers to observe in real use: a midnight-spanning session, a missed day, more DW comparisons.
- Debug shortcuts (debug build `com.achyutpal.scrolljourney.debug` only): `--es debug_route space|settings|usage_access…`, `--ei debug_fuel N`, `--es debug_reminder near|reached`, `--es debug_focus <DESTINATION>` (Explore focused on a stop).
- Sideload test (restricted settings) only works on the emulator: push the APK to /sdcard/Download and install it from the Files app (adb installs are never restricted).
- **Never run `connectedAndroidTest`**. Device tests: `adb install -r -t app-debug-androidTest.apk; adb shell am instrument -w com.achyutpal.scrolljourney.debug.test/androidx.test.runner.AndroidJUnitRunner` (UI tests need the phone unlocked). realme blocks `appops set`, `pm grant`, `settings put` over adb; use the system screens.
- Release: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-arm64-v8a-release.apk` (install with `adb install -r`; same key = in-place update, data kept).

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
    │   ├─ data/local/ScrollJourneyDatabase.kt (v6)  entity/{SelectedApp,DailyUsage,DailyAppUsage,UserSettings}Entity  dao/*
    │   ├─ data/history/RoomHistoryRepository.kt, data/settings/RoomSettingsRepository.kt, data/journey/RoomJourneyRepository.kt
    │   ├─ data/apps/{AndroidAppRepository, AppIconSource}.kt
    │   ├─ data/system/AndroidDeviceStatusRepository.kt, domain/repository/DeviceStatusRepository.kt
    │   ├─ data/backup/{BackupFile, BackupCodec, RoomBackupRepository, AndroidDocumentStore}, data/local/dao/BackupDao, domain/repository/{BackupRepository, DocumentStore}
    │   ├─ data/reminder/AndroidReminderGateway, domain/{calculator/ReminderPolicy, repository/ReminderGateway, usecase/CheckLimitRemindersUseCase}
    │   ├─ ui/widget/{WidgetSnapshot, WidgetContent, WidgetRing, JourneyWidgets}  (Glance widgets: Today 2×1, Journey 3×2/4×2, Week 4×1)
    │   ├─ data/diagnostics/{CrashLog, DiagnosticsReport, AndroidDiagnosticsRepository}, domain/repository/DiagnosticsRepository
    │   ├─ ui/onboarding/{IntroRoute, RestoreBackupViewModel}, ui/common/RestoreConfirmDialog, util/NotificationPermission
    │   ├─ util/UsageAccessSettings.kt (+ openAppDetailsSettings)
    │   ├─ navigation/AppNavHost.kt     routes: intro, usage_access, app_picker?onboarding=, daily_limit?onboarding=, dashboard, history, settings, space (Explore)
    │   └─ ui/{theme (+StatusColors), startup/StartupViewModel, onboarding/{IntroScreen, UsageAccessScreen}, permission/UsageAccessViewModel,
    │          apppicker/*, dailylimit/*, dashboard/{DashboardScreen, DashboardViewModel, DashboardUiState(+Builder)},
    │          history/{HistoryScreen, HistoryViewModel, MonthCalendar}, settings/{SettingsScreen, SettingsViewModel}, journey/RocketJourneyView, common/{DurationFormat, DistanceFormat, DestinationUi, BackTopBar, StatusUi, ComparisonUi}}
    │   ├─ ui/space/{SpaceScreen (Explore), JourneyHero (dashboard hero), HeroCamera, SceneLifecycle, SceneClock, SolarSystemScene,
    │   │           SolarSystemLayout, JourneyMap, JourneyStatus, RocketModel, SpaceGeometry, OrbitCameraState,
    │   │           FocusOrbitManipulator, SpacePicking, SpaceViewModel}.kt  (3D)
    │   └─ worker/{DailyUsageWorker (nightly chain), UsageRefreshWorker (15 min), DailySyncScheduler, DailySyncPolicy}
    ├─ schemas/                  Room schema JSON per DB version (committed)
    ├─ test/.../                 JVM unit tests + fakes/
    └─ androidTest/.../data/local/   on-device Room + migration tests
```

Build commands are in `memory/tech-stack.md`.
