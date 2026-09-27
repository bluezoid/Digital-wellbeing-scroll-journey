# Progress log

Append-only, newest at the bottom. One entry per work session: what was done, which files changed, what's next.

---

## 2026-09-26: Research (session 1)

- The user shared the original idea: count scrolls in Instagram/YouTube/Facebook, then scrolls → distance → a rocket travelling from the home location to the planets.
- Wrote `README.md` with the research plan: the AccessibilityService approach, learning order, Play policy risk, and Assignment #1 (a Logcat scroll spike).
- Answered "is it possible?": yes, but with Play policy, accuracy, onboarding and OEM risks.

## 2026-09-27: Pivot and planning (session 1, continued)

- The user proposed switching to **screen time** per selected app. Agreed: it's exact, Play-safe, and needs no always-on service.
- Advised keeping the "scroll distance" hook as an estimate derived from minutes, and choosing the **wellbeing direction** (reward less usage). Both accepted as the plan.
- Dropped home location.
- Recommended the stack: native Kotlin + Compose, MVVM, Room, DataStore, WorkManager, manual DI, minSdk 26.
- Proposed MVP features F1–F10 with rocket and streak defaults. **Awaiting approval.**
- Created the project memory system: `.claude/CLAUDE.md` plus `.claude/memory/` (product, mvp-features, tech-stack, research-notes, decisions, roadmap, progress-log).
- Git: the repo was already initialised on `main` with local identity Achyut Pal <achyutpal@gmail.com>. Made the first commit (README, idea.txt, .claude/). No co-author lines.
- **Next:** the user approves or edits the MVP list, then Phase 1 (create the Android Studio project, ask for the package name, build the permission screen and today's minutes).

## 2026-09-27: Spec adopted, Phase 0 + Phase 1 built (session 2)

- The user provided the full technical spec (HLD/LLD/tests) as the reference. It serves as approval of F1–F10 and the rocket rules. Saved a condensed copy as `memory/spec.md` (authoritative).
- **Phase 0: project foundation**
  - Gradle Kotlin DSL + version catalog, wrapper 8.10.2, AGP 8.7.2, Kotlin 2.1.0, compileSdk/targetSdk 34, minSdk 26. Package `com.achyutpal.scrolljourney`, app name "Scroll Journey."
  - Deep-space Compose theme (dark and light), adaptive rocket launcher icon (vector), `.gitignore`.
  - Manual DI: `ScrollJourneyApp` (Application) → `di/AppContainer`.
- **Phase 1: onboarding + Usage Access**
  - Manifest declares `PACKAGE_USAGE_STATS` (tools:ignore ProtectedPermissions).
  - `domain/model/UsageAccessState` (GRANTED/REVOKED/UNKNOWN) and `domain/repository/UsageAccessRepository` (interface).
  - `data/usage/AndroidUsageAccessRepository`: AppOpsManager `OPSTR_GET_USAGE_STATS` (unsafeCheckOpNoThrow on API 29+; MODE_DEFAULT falls back to checkCallingOrSelfPermission).
  - `util/UsageAccessSettings.openUsageAccessSettings()`: tries the app-specific usage-access page, then the generic list, then Settings.
  - `ui/permission/UsageAccessViewModel` (StateFlow, `refresh()`, factory via AppContainer).
  - Screens:
    - `ui/onboarding/IntroScreen`: 2-page HorizontalPager with dots.
    - `ui/onboarding/UsageAccessScreen`: steps card and "Open settings"; re-checks on every resume with `LifecycleResumeEffect` and auto-navigates when granted.
    - `ui/home/HomePlaceholderScreen`: TEMPORARY. Shows the "granted" state or the revoked warning with an Open settings button.
  - `navigation/AppNavHost`: string routes intro → usage_access → home (onboarding popped from the back stack).
  - `MainActivity`: the start destination depends on the permission (no persisted flag until Phase 4).
- Tests: `UsageAccessViewModelTest` has 4 passing JVM unit tests (fake repository).
- `assembleDebug` OK. Lint shows only "newer version available," plus targetSdk-for-Play notices.
- **Not yet done:** install and test on the physical Android 13 phone (none was connected; `adb devices` was empty).
- **Next:** the user connects the phone with USB debugging and runs `./gradlew installDebug`, then checks: fresh install goes to intro → usage access → Settings → toggle → back → auto "granted" screen; revoke in Settings → warning on return; relaunch keeps the right start screen. After that passes, Phase 2 (app picker).

## 2026-09-27: Phase 1 verified on the physical phone (session 2, continued)

- Test device: **realme, Android 13**, adb serial AEOFCYC66LSCZDLZ.
- All Phase 1 checks passed: fresh install goes to intro; Open settings reaches the toggle; granting auto-advances to "Usage Access is on"; revoking and returning shows the "Usage Access is disabled" warning; relaunch lands on the correct screen.
- In the first manual run the user reported the revoked warning **not** appearing. It couldn't be reproduced: the watcher log showed revokes detected within about 1 s (`appOpsMode=1 granted=false`). The most likely cause is that the toggle didn't actually change the first time. Worth rechecking during Phase 12.
- Added a debug log in `AndroidUsageAccessRepository` (tag `UsageAccess`, shows the raw AppOps mode). Read it with `adb logcat -s UsageAccess:D`.
- realme facts:
  - Revoking sets the mode to `ignore` (MODE_IGNORED=1). The process isn't killed.
  - **`adb shell appops set` is blocked** (SecurityException MANAGE_APP_OPS_MODES), so the permission must be toggled by hand in Settings for testing. (Enabling "Disable permission monitoring" in Developer options may allow it.)
- **Next:** Phase 2, the app picker. Waiting on the user's approval of the storage approach for the selection.

## 2026-09-27: Phase 2, app picker built (session 2, continued)

- The user chose **Option A**: bring Room in early with only `SelectedAppEntity`. Phase 4 adds the other 3 tables via version bump + migration.
- Build: added KSP 2.1.0-1.0.28, Room 2.7.2 (runtime, ktx, compiler), and kotlinx-coroutines-test 1.8.1. The Room schema is exported to `app/schemas/` (commit it; needed for migration tests).
- Manifest: a `<queries>` block for MAIN/LAUNCHER intents (package visibility on Android 11+). No QUERY_ALL_PACKAGES.
- Domain:
  - `InstalledApp` and `SelectedApp` models.
  - `domain/calculator/AppSelectionRules`: DEFAULT_PACKAGES, initialSelection, sortForDisplay, buildSelectionRecords.
  - Rules: untick means disabled, not deleted. A saved app that is no longer installed gets disabled, so a reinstall isn't auto re-enabled. addedAt is preserved.
  - `AppRepository` interface.
- Data:
  - `data/local/{ScrollJourneyDatabase v1, entity/SelectedAppEntity (+ mappers), dao/SelectedAppDao (getAll, observeEnabled Flow, @Upsert)}`.
  - `data/apps/AndroidAppRepository`: `queryIntentActivities` for launcher apps, excluding our own package.
  - `data/apps/AppIconSource` + `AndroidAppIconSource`: cached 96 px ImageBitmap.
- UI:
  - `ui/apppicker/{AppPickerViewModel, AppPickerScreen}`: search box, checkbox list with icons, "Continue (N selected)" disabled when N = 0, error text if the list is empty.
  - `ui/startup/StartupViewModel`: picks the start screen. Apps saved → HOME; access granted but no apps → APP_PICKER; otherwise ONBOARDING. "Has an enabled tracked app" stands in for onboardingComplete until Phase 4.
  - MainActivity shows a blank background until the start screen is known.
  - Home placeholder now lists the tracked apps and has a "Change apps" button (the picker opened from Home pops back; in onboarding it continues to Home).
- Tests: 19 JVM unit tests pass (AppSelectionRules 10, AppPickerViewModel 5, UsageAccessViewModel 4), using `fakes/FakeAppRepository`.
- The phone has Instagram, Facebook and YouTube installed (no Reddit, X or Snapchat).
- Installed on the phone, but the **device check is pending**: the phone was locked, so the screenshot was black.
- **Device check passed (2026-09-27):** 33 launcher apps listed, Instagram/Facebook/YouTube pre-selected, search works, Continue is disabled with 0 selected, the selection survives relaunch, and Change apps with Back behaves correctly. **Phase 2 ✅.**

## 2026-09-27: Phase 3, usage tracking built (session 2, continued)

- Domain:
  - `UsageEvent` + `UsageEventType` (RESUMED, PAUSED, SCREEN_OFF, SHUTDOWN), our own types, not Android's.
  - `AppUsage` (foregroundMillis → minutes, rounded down) and `DailyUsageSummary` (totalMinutes = sum of per-app minutes, so the breakdown adds up; hasAnyUsage).
  - `domain/calculator/UsageCalculator.foregroundMillis(events, windowStart, windowEnd, tracked)`. Rules:
    - An app is in the foreground while ANY of its activities is resumed (tracked per className).
    - A first PAUSED in the window with no RESUMED before it means the app was open at window start, so it counts from windowStart (midnight crossing).
    - Still open at windowEnd: counted to the end.
    - SCREEN_OFF or SHUTDOWN closes all sessions.
    - Stray pauses are ignored.
  - `UsageRepository` interface.
  - `domain/usecase/GetTodayUsageUseCase` gives `TodayUsageResult` Success, AccessDenied or Failure. The window is local midnight to now. Only enabled apps count, sorted most-used first. It checks access first, since queryEvents returns nothing silently without access; SecurityException becomes AccessDenied, and CancellationException is re-thrown.
- Data:
  - `data/usage/UsageDataSource` + `AndroidUsageDataSource` (the only UsageStatsManager caller). It maps MOVE_TO_FOREGROUND/BACKGROUND (same values as ACTIVITY_RESUMED/PAUSED), SCREEN_NON_INTERACTIVE and DEVICE_SHUTDOWN.
  - `AndroidUsageRepository` runs the calculator and logs under tag `UsageTracking`.
- UI: the temporary home now shows "Today": the total, per-app minutes, "Updated at", Refresh and Change apps, plus an AccessDenied card. It refreshes on every resume. `ui/common/formatMinutes` gives "1h 05m".
- Tests: 41 JVM tests pass. New: UsageCalculator 12, GetTodayUsage 9, DurationFormat 1. Fakes are shared in `test/.../fakes/`.
- Installed on the phone; the **device comparison with Digital Wellbeing is pending** (the phone was locked).
- Debug: `adb logcat -s UsageTracking:D` shows the event count and per-package milliseconds on each refresh.
- **Device check passed (2026-09-27):** Scroll Journey's minutes matched Digital Wellbeing **exactly** for all selected apps (Instagram, YouTube, Facebook) on the realme. The queryEvents approach and rounding down are confirmed. **Phase 3 ✅.**

## 2026-09-27: Phase 4, Room history built (session 2, continued)

- **DB v2** via `AutoMigration(1→2, spec = Migration1To2)`. New tables:
  - `daily_usage`: date "YYYY-MM-DD" (PK), totalMinutes, status DayStatus? (null until Phase 5), fuelDelta, distance, **finalized**.
  - `daily_app_usage`: **composite PK (date, packageName)**, minutes.
  - `user_settings`: id = 1, dailyLimitMinutes 60, onboardingComplete, currentFuel, lifetimeFuel, highestReachedPlanet (Planet enum stored by name), journeyDistance. The journey columns were added now so Phase 7 needs no migration.
  - Migration1To2.onPostMigrate inserts the settings row with onboardingComplete = EXISTS(enabled selected_apps), so existing users aren't sent back to the intro.
- Domain:
  - `DayStatus`, `Planet`, `UserSettings`, `DailyRecord`, and `ForegroundUsage(millisByPackage, eventCount)`. eventCount 0 means no data, which is different from 0 minutes.
  - `UsageRepository.getForegroundUsage` replaces getForegroundMillis.
  - `HistoryRepository` (getDay, saveDay [transaction: upsert day + replace per-app rows], markFinalized, getRange, observeRecentDays) and `SettingsRepository` (getSettings, observeSettings, setOnboardingComplete; the row is created lazily).
  - **`SyncUsageHistoryUseCase` replaces GetTodayUsageUseCase** (deleted). For each of the last 7 days: finalized → skip; has events → save finalized; no events but saved earlier → markFinalized; no events and nothing saved → skip. Then today (midnight → now) is saved unfinalized. It returns `TodayUsageResult` (moved to its own file). Past days use the current app selection.
- Data: `data/history/RoomHistoryRepository`, `data/settings/RoomSettingsRepository`, DAOs `DailyUsageDao`, `DailyAppUsageDao`, `UserSettingsDao`.
- UI:
  - StartupViewModel uses `settings.onboardingComplete` (the temporary "has apps" rule is gone).
  - AppPickerViewModel.onSave calls setOnboardingComplete.
  - Home syncs on resume and shows a "Last 7 days" card (Flow from Room; unfinalized days are labelled "today, live").
- Tests:
  - 49 JVM unit tests pass (new: Sync 14, Startup 3).
  - **10 on-device instrumented tests pass** (HistoryAndSettingsDatabaseTest 8, MigrationTest 2). Added AndroidX test runner, ext-junit and room-testing; androidTest assets include `app/schemas`.
- **The real v1→v2 migration was verified on the phone's actual DB** (pulled via run-as): user_version 2, 5 enabled apps kept, settings row (1, 60, onboardingComplete = 1, 0, 0, EARTH, 0).
- **How to run on-device tests WITHOUT wiping app data:** `./gradlew assembleDebugAndroidTest`, then `adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, then `adb shell am instrument -w com.achyutpal.scrolljourney.test/androidx.test.runner.AndroidJUnitRunner`, then `adb uninstall com.achyutpal.scrolljourney.test`. **Never use connectedAndroidTest**: it uninstalls the app, which loses the data and Usage Access.
- Inspect the phone DB: `adb exec-out run-as com.achyutpal.scrolljourney cat databases/scroll_journey.db > x.db` (also -wal and -shm), then open it with sqlite3 or python.
- Pending: the user checks the "Last 7 days" card on the phone (the phone was locked).
