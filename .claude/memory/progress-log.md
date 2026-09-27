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
- **Device check passed (2026-09-27):** the Last 7 days card shows 20 Sep to 27 Sep, only the selected apps count, the minutes are correct, and it persists across relaunch. The **realme keeps at least 7 days of usage events.** **Phase 4 ✅.**

## 2026-09-27: Phase 5, limit and day-status engine + Daily Limit step (session 2, continued)

- Domain:
  - `domain/calculator/LimitEvaluator.evaluate(usage, limit)` uses integer maths: GREEN if usage ≤ limit; YELLOW if usage×100 ≤ limit×125; else RED. `yellowMaxMinutes(limit)` = floor(limit×1.25).
  - `DailyLimitRules`: MIN 15, MAX 240, STEP 5, DEFAULT 60, isValid, snap.
- Sync now takes SettingsRepository and uses the current limit:
  - Today is saved with its live status.
  - Past days get their status when they're finalized; markFinalized takes a status.
  - Days finalized in Phase 4 with a null status get it once from their saved minutes (updateStatus), with no usage re-read.
  - The status of a finalized day is **never** re-evaluated when the limit changes later.
- `TodayUsageResult.Success(summary, limitMinutes, status)`.
- Data:
  - `DailyUsageDao.markFinalized(date, status)` and `updateStatus`.
  - `UserSettingsDao.setDailyLimit`. `RoomSettingsRepository.setDailyLimit` rejects values outside 15–240 (require).
  - No schema change: still DB v2.
- UI:
  - New `ui/dailylimit/{DailyLimitScreen, DailyLimitViewModel}`: a slider (15–240, step 5; `steps = 44`), presets 30/60/90/120, and a live legend (green up to limit +1 fuel / yellow up to 125% no fuel / red above −1 fuel). The button reads "Start my journey" in onboarding, "Save" from Home.
  - **Onboarding is now intro → usage access → app picker → daily limit → home.** Saving the limit (onboarding=true) sets onboardingComplete; the app picker no longer does.
  - Routes `app_picker?onboarding={onboarding}` and `daily_limit?onboarding={onboarding}` (BoolType, default true). Home opens them with onboarding=false and they pop back. DailyLimitViewModel reads the arg from the SavedStateHandle.
  - Home card: status label ("Green day so far"), total coloured by status, "of 1h 00m limit", progress bar. The Last 7 days card has a coloured dot per day. New "Change daily limit" button.
  - `ui/theme/StatusColors.kt`: StatusGreen, StatusYellow, StatusRed, `DayStatus.color()`.
- Tests: **67 JVM** (new: LimitEvaluator 6, DailyLimitRules 4, DailyLimitViewModel 4, Sync +4 status cases) and **13 on-device** (+updateStatus, dailyLimit persists, invalid limit rejected). All pass.
- Installed on the phone; the user's visual check is pending (the phone was locked).

## 2026-09-27: Phase 5 follow-up, store the judged limit per day (DB v3)

- The user reported Fri 25 Sep showing 🟢 with 44m after they changed the limit to 30. The DB confirmed it was **by design**: the day was finalized and coloured at the first Phase-5 sync with the default limit 60 (44 ≤ 60), and finalized colours are locked. 25 Sep was the only day where 30 and 60 give different colours. (23 Sep is absent because Android had no events that day, so it's skipped.)
- Options offered: A) keep locking + show the judged limit; B) recolour history with the current limit; C) A plus a one-time recolour. **The user chose A.**
- Implementation:
  - `DailyRecord.limitMinutes: Int?` and the `daily_usage.limitMinutes` column.
  - **DB v3** via `AutoMigration(2→3, spec = Migration2To3)`. onPostMigrate sets limitMinutes = 60 where status IS NOT NULL AND finalized = 1. That's safe because every pre-v3 status came from the first Phase-5 sync with the default 60; the live today row is rewritten by the next sync.
  - Sync stores the limit whenever it sets a status (today, finalize, markFinalized, updateStatus); the repository/DAO signatures take limitMinutes.
  - Home Last-7-days rows show "limit 1h 00m" under the date.
- Real v2→v3 migration verified on the phone DB: version 3, all finalized days have limitMinutes 60, today NULL until the next sync, and the user's limit of 30 is kept.
- Tests: 67 JVM + **14 on-device** (new MigrationTest migrate2To3) all pass.
- **Gotcha:** after a schema version bump, run `./gradlew assembleDebugAndroidTest` again *after* the main compile has exported the new `N.json`. Otherwise the test APK lacks the schema asset and the MigrationTest fails with FileNotFoundException. Check with `unzip -l app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | grep json`.
- **Device check passed (2026-09-27):** 'limit 1h 00m' shows on past days and 'limit 30m' on today. **Phase 5 ✅.**

## 2026-09-27: Phase 6, estimated scroll distance (session 2, continued)

- Constant: **`DistanceConfig.DEFAULT_CENTIMETRES_PER_MINUTE = 100` (1 m of feed per minute)**. The realme RMX3031 screen is 1080×2400 at 409 dpi, about 14.9 cm tall, so 1 m/min is about 6–7 screen-heights a minute. It's a tunable estimate, and the UI always says "estimated".
- Domain:
  - `DistanceConfig` (require > 0) and `DistanceCalculator.centimetres(minutes)` (Long; negative counts as 0).
  - `DistanceComparisons.compare(cm)` → `Comparison(landmark, times)`, using the tallest landmark passed, or the smallest as a fraction.
  - Landmarks: Qutub Minar 73 m, Great Pyramid 139 m, Eiffel Tower 330 m, Burj Khalifa 828 m, Everest 8,849 m.
- **Distance unit in the DB (`daily_usage.distance`) is centimetres.**
- Sync:
  - It saves distance for today (live) and for finalized days.
  - `HistoryRepository.updateStatus` was **renamed to `updateEvaluation(date, status, limit, distance)`**, and `markFinalized` also takes distance.
  - Finalized days missing a status or distance (distance 0 with minutes > 0) get them once from saved minutes. An existing status and judged limit are kept.
- `TodayUsageResult.Success` gained `distanceCentimetres`.
- UI:
  - Home has a new "Estimated scroll distance today" card: 📏 distance, "≈ 1.7× the height of the Eiffel Tower" (or "≈ 45% of the height of the Qutub Minar"), and a footnote built from DistanceConfig.
  - The Last 7 days card shows "≈ X estimated scroll distance" (sum of the rows).
  - `ui/common/DistanceFormat`: formatDistance ("137 m" / "1.3 km") and formatTimes.
- Tests: **81 JVM** (new: DistanceCalculator 5 incl. spec §64, DistanceComparisons 4, DistanceFormat 2, Sync +3) and **14 on-device**. All pass.
- Phone DB before the first sync of this build: distances 0 (the phone was locked). The user's limit is now 120 (they changed it), so today is YELLOW with 137 m.
- Pending: the user checks the distance card and the 7-day total, and past days' distances get backfilled on the first open.
