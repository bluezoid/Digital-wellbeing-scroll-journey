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
- **Device check passed (2026-09-27, screenshots):** today 2h 17m → 137 m ≈ 1.9× Qutub Minar; 7-day total 488 m (matches the sum); today YELLOW against the 2h limit; past days keep 'limit 1h 00m'. Tracked apps on the phone: Chrome, Instagram, Brave, Facebook, YouTube (browsers were the top usage today). **Phase 6 ✅.** The user asked for advice on the 1 m/min constant before Phase 7.

## 2026-09-27: Phase 7, rocket engine (session 2, continued)

- The user confirmed the distance constant: **1 m/min is final for the MVP.**
- Domain:
  - `Planet` enum now has `fuelRequired` (Earth 0, Moon 3, Mars 7, Jupiter 14, Saturn 21, Uranus 30, Neptune 45, Pluto 60), `next`, and `forFuel(fuel)`.
  - `RocketState(currentFuel, lifetimeFuel, highestReachedPlanet)` with nextPlanet, fuelToNext, and legProgress (from the reached planet's threshold to the next one; 0 if fuel dropped below the milestone).
  - `RocketProgressCalculator`: fuelDelta G +1 / Y 0 / R −1. `apply()` gives fuel = max(0, fuel + delta); lifetimeFuel += max(delta, 0); highest = max(highest, forFuel(fuel)), so milestones are never lost.
  - `UserSettings.journeyStartDate` and `.rocket`; `DailyRecord.fuelApplied`.
  - `JourneyRepository` (daysAwaitingFuel, applyDay) and `ApplyJourneyFuelUseCase`: finalized days on/after journeyStartDate with fuelApplied = 0, oldest first.
  - `SettingsRepository.setOnboardingComplete(journeyStart)` and `ensureJourneyStarted` (both COALESCE, so an existing start is never moved).
- **Journey start rule:** the journey starts the day onboarding completes (the DailyLimit save in onboarding). For users onboarded before Phase 7, the sync sets it to *today* on the next run. **Backfilled/earlier days never give fuel. Today only gives fuel once it's finalized (the next day).**
- Data:
  - **DB v4** via `AutoMigration(3→4)` with no spec: `daily_usage.fuelApplied` (NOT NULL, default 0) and `user_settings.journeyStartDate` (TEXT, nullable).
  - `RoomJourneyRepository.applyDay` runs in **one transaction**: the conditional `UPDATE … SET fuelApplied = 1 WHERE date = ? AND fuelApplied = 0` (returns rows affected), then the rocket state is saved and the day's distance is added to journeyDistance. It's a no-op if already applied, which gives exactly-once even under concurrent syncs.
- Sync: after finalizing past days, it calls `ensureJourneyStarted(today)` and then `applyJourneyFuel()`.
- UI:
  - Home has a new "Rocket journey" card: "🌍 On Earth, ready for launch" or "🌙 Moon reached", a 🌍 🚀 🌙 row with a leg progress bar, "Fuel X / Y · next stop: Z", the journey start date, and the rules line.
  - History rows show a "+1 / 0 / −1 fuel" badge for fuel-applied days.
- Tests: **100 JVM** (new: RocketProgressCalculator 10 [spec §38, §40, §58–61], ApplyJourneyFuel 7, Sync +2, DailyLimitVM journey start) and **16 on-device** (+applyDay exactly once in a transaction, journey start COALESCE, MigrationTest 3→4). All pass.
- Phone DB after installing: version 4, settings (limit 120, onboarded, fuel 0, EARTH, journeyStartDate NULL until the first sync), and no fuel applied to backfilled days.
- Expected on the device: the journey starts 27 Sep. Today (137 min against a 120 limit) is YELLOW so far, so it would give 0 fuel when finalized on 28 Sep unless the status changes.
- **Device check passed (2026-09-27):** the rocket card shows Earth, Fuel 0/3 → Moon, journey started 27 Sep. The user accepted that the journey starts today (no backfilled fuel). **Phase 7 ✅.**

## 2026-09-27: Phase 8, real Dashboard (session 2, continued)

- **Replaced the temporary home** (`ui/home/*` deleted) with `ui/dashboard/{DashboardScreen, DashboardViewModel, DashboardUiState}`. Route `Routes.DASHBOARD = "dashboard"` and `StartDestination.DASHBOARD` (renamed from HOME).
- `DashboardViewModel` combines 3 sources into **one immutable `DashboardUiState`** (spec §34):
  1. a sync result MutableStateFlow (refresh() on every resume),
  2. `settingsRepository.observeSettings()`,
  3. `historyRepository.observeRecentDays(400)`.
  The mapping lives in the pure `DashboardStateBuilder.build(...)`. Fields: isLoading, accessDenied, errorMessage, today summary, limit, status, distance, rocket, journeyStartDate, currentStreak, week (7 × WeekDay), updatedAt.
- `domain/calculator/StreakCalculator` (pulled forward from Phase 9 because the dashboard shows the streak): `calculate(List<DayStatus?>)` gives Streak(current, best). Only GREEN extends a streak; yellow, red and missing (null) days break it; the current streak counts back from today, including a live green today (safe, since usage only grows). `contiguousStatuses(records, today)` fills gaps with null.
- `ui/journey/RocketJourneyView` (hero):
  - A header with "⛽ N fuel".
  - The headline ("🌍 On Earth, ready for launch" / "🌙 Moon reached").
  - The current leg: planet emoji, then a **Compose Canvas** dashed track with the travelled part solid, and the 🚀 rotated 45° positioned by an **animateFloatAsState** of legProgress; then the next planet.
  - "Fuel X / Y · next stop", a **route strip** of 8 planet emojis (reached = full alpha, current = dot), the journey start and rules.
- Dashboard layout: TopAppBar ("Scroll Journey", ⟳ refresh, ⋮ menu with Change apps / Change daily limit), then the access-denied card if needed, the rocket card, the Today card (status, total vs limit, progress, per-app), the streak and 7-day dot strip card ("🔥 N-day green streak" plurals / "No green streak right now…"), the distance card, and "Updated at".
- Shared UI helpers moved to `ui/common/{PlanetUi (emoji, nameRes), StatusUi (labelRes, StatusDot, StatusLabel, formatFuel), ComparisonUi (comparisonText, Landmark.nameRes)}`.
- Tests: **114 JVM** (new: StreakCalculator 9 [spec §36, §62, §63], DashboardViewModel 5 [spec §85, full fake stack incl. fuel → Mars]) and **16 on-device**. All pass.
  - Test gotcha: to keep a `WhileSubscribed` stateIn alive in runTest, use `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }`. `launchIn(backgroundScope)` never ran.
- Installed on the phone with no crash; the visual check is pending (the phone was locked).
- **Device check passed (2026-09-27):** all dashboard checks OK. **Phase 8 ✅.** The user added a requirement: the journey UI must be a **real 3D solar system** (Sun and all planets, 360° rotation), recorded as roadmap Phase 12b.

## 2026-09-27: Phase 9, History screen (session 2, continued)

- `DayAppMinutes` model and `HistoryRepository.getAppMinutes(date)` (reads daily_app_usage).
- `ui/history/MonthCalendar.build(month, statusByDate, today)`: a pure grid with **weeks starting Monday**; `CalendarCell(date?, status, hasRecord, isToday, isFuture)` with padding cells.
- `ui/history/HistoryViewModel`:
  - Combines observeRecentDays(400), the shown month, the selected date and the loaded detail.
  - `HistoryUiState` has weeks, current/best streak (StreakCalculator), the month's green/yellow/red counts, canGoPrevious (bounded by the earliest record's month) and canGoNext (bounded by the current month), and selectedDate plus `DayDetail(record, apps with names)`.
  - App names come from the saved selections, which include disabled apps. Today is selected on open.
- `ui/history/HistoryScreen`:
  - A TopAppBar with back.
  - A streak card (🔥 current / 🏆 best).
  - A calendar card with ‹ Month Year › navigation, M–S headers, and day circles filled with the status colour. The selected day is outlined, today is ringed in primary, days with no record get a grey ring, and future days are faded and not clickable.
  - A legend with counts.
  - A day detail card: date, status label, minutes · judged limit, distance, fuel badge, and per-app minutes. There's a "No usage data" card for empty days.
- Route `Routes.HISTORY = "history"`. The dashboard opens it via the ⋮ menu "History", a tap on the streak/week card, and "See full history ›". `DashboardCard` now takes an optional onClick (Card(onClick)).
- Tests: **123 JVM** (new: MonthCalendar 3, HistoryViewModel 6) and **16 on-device**. All pass.
- Installed with no crash; the phone check is pending.
- **Device check passed (2026-09-27):** History OK (streak 0/3, Sep grid colours, counts, day detail, month bounds). **Phase 9 ✅.**

## 2026-09-27: Phase 10, WorkManager background sync (session 2, continued)

- Added `androidx.work:work-runtime-ktx:2.9.1` (minCompileSdk 34; 2.10 needs 35) and `work-testing` (androidTest).
- `worker/DailyUsageWorker` (CoroutineWorker): if onboarding isn't complete → success. Otherwise it runs the **same** `syncUsageHistoryUseCase()` as app open. Outcome via `DailySyncPolicy.outcomeFor`: Success or AccessDenied → success; Failure → retry (exponential backoff, 30 min). Log tag `DailyUsageWorker`.
- `worker/DailySyncPolicy` (pure): PREFERRED_RUN_TIME 00:30; `initialDelay(now)` → the next 00:30.
- `worker/DailySyncScheduler.schedule(context)`: PeriodicWorkRequest 24 h, initial delay to 00:30, constraint battery-not-low, unique name `daily-usage-sync`, **ExistingPeriodicWorkPolicy.KEEP** (app starts don't reset timing). Called from `ScrollJourneyApp.onCreate`.
- WorkManager persists jobs across reboots by itself (no receiver of ours).
- Device verification (realme):
  - `adb shell dumpsys jobscheduler | grep -A14 "com.achyutpal.scrolljourney/androidx.work"` shows JOB #0, min latency about 1h44m (→ 00:30), required BATTERY_NOT_LOW + TIMING_DELAY.
  - `adb shell cmd jobscheduler run -f com.achyutpal.scrolljourney 0` wakes WorkManager, but it **won't run periodic work before its initial delay** (expected). Its logs are silent at default level.
  - The on-device test `worker/DailyUsageWorkerTest` (TestListenableWorkerBuilder, real container) ran the worker: Result.success, and today's row is saved. Log: `sync result=Success outcome=SUCCESS`.
- realme/ColorOS ("OplusHansManager") freezes background apps when the screen is off. Periodic work may be delayed; the sync on app open is the primary path, the worker is the backup. Consider guiding users to allow background activity (Settings).
- Tests: **127 JVM** (+DailySyncPolicy 4) and **17 on-device** (+DailyUsageWorkerTest). All pass.
- Still to observe in real life: after 00:30 tonight, `adb logcat -s DailyUsageWorker:D` (or the next day's history) should show the 27 Sep finalization and a fuel badge without opening the app first.
- **UI fixes from the user's 20-screenshot review (2026-09-27, 23:10):** all data was verified correct (colours, per-app sums, streak 0/3, distance). Fixed:
  1. **Back arrow** on the App picker and Daily limit when opened from the dashboard (`onBack` param, shared `ui/common/BackTopBar`; null during onboarding).
  2. The comparison "≈ 1× the height of…" now reads "≈ the height of…" (`distance_about_one`).
  3. The limit preset chips use `formatMinutesCompact` (30m / 1h / 1h 30m / 2h, maxLines 1); "2h 00m" had wrapped.
  128 JVM tests pass. Installed.
- **End of session 2 (2026-09-27 ~23:15).** The user is stopping for the day.

### Resume checklist for next session (2026-09-28)
1. Ask the user to open the app in the morning (the phone can be connected). Check: 27 Sep is finalized (not "live"), YELLOW against the 2h limit, with a "0 fuel" badge; the rocket is still at 0 fuel/Earth. Optionally `adb logcat -d -s DailyUsageWorker:D` for the 00:30 worker run (it may be delayed by ColorOS).
2. Ask the user to confirm the three UI fixes (back arrows, pyramid wording, chips).
3. Then **Phase 11 Settings**: change apps, change limit, **reset journey** (confirmation dialog; fuel = 0, highest = EARTH, journeyDistance = 0, journeyStartDate = today, history kept, clear fuelApplied? NO: keep fuel history, just restart the rocket from today), privacy text, and a battery/background hint for OEMs like realme. Move the ⋮ menu items into Settings.
4. After that: present the **3D solar system plan (12b)** to the user before coding, then Phase 12 validation.

## 2026-09-28: Session 3, Phase 11 Settings (built; device install pending)

- Morning check blocked: at 10:36 the phone shows as **`unauthorized`** in `adb devices` (the USB-debugging prompt needs accepting). Always run adb with `timeout` (e.g. `timeout 20 adb ...`); `adb exec-out` hangs with "waiting for device" when unauthorized.
- **Phase 11 built:**
  - `SettingsRepository.resetJourney(journeyStart)` via the DAO query `resetJourney`: currentFuel 0, lifetimeFuel 0, highestReachedPlanet EARTH, journeyDistance 0, journeyStartDate = today. **History untouched** (days, colours, minutes, fuelApplied/fuelDelta badges, per-app rows); limit and onboarding flag untouched.
  - `domain/repository/DeviceStatusRepository` + `data/system/AndroidDeviceStatusRepository`: isBackgroundRestricted (ActivityManager, API 28+), isIgnoringBatteryOptimizations (PowerManager), appVersionName.
  - `util.openAppDetailsSettings()` (ACTION_APPLICATION_DETAILS_SETTINGS with a fallback).
  - `ui/settings/{SettingsViewModel, SettingsScreen}`, with sections:
    - Tracking: tracked apps (count of enabled) → picker; daily limit → limit screen.
    - Phone permissions: Usage Access On/Off → usage access settings; Background activity Allowed/Restricted plus the battery-optimisation subtitle → app details; a realme/Xiaomi hint.
    - Rocket journey: planet · fuel · since date; **Reset journey** → AlertDialog (Cancel/Reset) → "Journey reset" confirmation.
    - Privacy text, and the version at the bottom.
    - Device status refreshes on every resume.
  - Route `settings`. The dashboard ⋮ menu is now **History, Settings** (Change apps/limit moved into Settings). DashboardScreen params are now onOpenHistory, onOpenSettings.
- Tests: **133 JVM** (+SettingsViewModel 5, incl. spec §69 reset and "history kept") and on-device +1 (`resetJourneyRestartsRocketButKeepsHistoryAndLimit`), 18 total, not yet run on the device.
- **Next once the phone is authorized:**
  1. The morning check from the resume checklist (27 Sep finalized YELLOW, 0 fuel badge, worker log).
  2. installDebug + run the on-device tests (manual am instrument).
  3. The user checks the Settings screen.

## 2026-09-28: Overnight check ✅ + Phase 11 installed (session 3)

- USB debugging re-authorized after about 5 min (the prompt had been suppressed; adb reconnect and kill-server restarts plus the user's phone-side steps fixed it).
- **Phase 10 verified in real life:** WorkManager's DB (`no_backup/androidx.work.workdb`, table workspec) shows DailyUsageWorker `period_count = 1` and `last_enqueue_time = 2026-09-28 00:30:16`, so **the periodic job ran by itself at 00:30**, about 9 h before the user opened the app (09:25, from `dumpsys usagestats`).
  - Tip: read `period_count` from that DB to prove periodic runs; the worker's logcat lines get rotated away.
- Data after the night:
  - 27 Sep: 144 min, YELLOW (limit 120; ≤150), finalized, **fuelApplied = 1, fuelDelta 0**.
  - Rocket: 0 fuel, EARTH, journeyDistance 14 400 cm (27 Sep's 144 m added), journey start 2026-09-27.
  - 28 Sep live: 12 min, GREEN so far.
- Phase 11 installed. **18/18 on-device tests pass**, including the reset test. App data intact.
- Pending: the user checks the Settings screen (reset dialog; background/usage status rows).
- **User check passed (2026-09-28):** Settings screen OK. **Phase 11 ✅.** Next: the 3D plan (12b) presented for approval; SceneView 2.3.0 (Filament 1.56, Kotlin stdlib 2.0.21, no minCompileSdk constraint) is compatible with the current toolchain.
- 2026-09-28: The user **approved all 3 recommendations** (3D approach, Mercury/Venus as scenery, Phase 12 validation in parallel from today). **Phase 12 validation starts 2026-09-28.**
- The user raised a pacing concern: Earth → Mars at 7 fuel is too quick, and the journey should go **beyond Pluto** (new planets/stars) and progress slowly. Claude proposed a new destination table (slower curve, sub-stops every week or two, deep-space stars up to about a year+), awaiting approval. The rocket is at 0 fuel/EARTH, so it's a safe moment to change.
- **2026-09-28: New route implemented.** `domain/model/Planet.kt` → `Destination.kt` (enum `Destination(fuelRequired, kind: Kind{PLANET, MOON, STOP, DWARF_PLANET, STAR, GALAXY})`, `next`, `inSolarSystem` (≤ OORT_CLOUD), `FINAL`, `forFuel`). The global rename Planet→Destination covers Kotlin files, and `ui/common/PlanetUi.kt` → `DestinationUi.kt` (emoji + nameRes for 18 stops). The DB column is still `highestReachedPlanet` (TEXT enum name): no migration.
  - The dashboard RouteStrip shows a 7-stop sliding window starting at the stop before the current one.
  - The completion string key is `rocket_journey_complete` ("centre of the galaxy").
  - RocketProgressCalculatorTest was rewritten for the route (13 tests). 136 JVM + 18 on-device tests pass; installed, no crash; the user's rocket state (EARTH, 0 fuel) loads fine.
- **Next:** 3D-1 spike (SceneView 2.3.0, a single textured rotating Earth with an orbit camera) on the phone.

## 2026-09-28: 3D-1 spike built (session 3)

- Added `io.github.sceneview:sceneview:2.3.0` (Filament 1.56, gltfio, filament-utils, kotlin-math, fragment 1.8.6, compose 1.7.8, fuel). It's compatible with compileSdk 34 / Kotlin 2.1.
- **Privacy:** SceneView/fuel brought in `android.permission.INTERNET`. It's **removed in our manifest** with `tools:node="remove"`; the merged manifest was checked. Remaining permissions come from WorkManager (network state, wake lock, boot, foreground service) plus usage stats.
- Texture `app/src/main/assets/textures/earth_2k.jpg` (Solar System Scope 2k_earth_daymap, CC BY 4.0, 452 KB). **Credit must be added in Settings (3D-4).**
- `ui/space/EarthSpikeScreen` (TEMPORARY):
  - rememberEngine/MaterialLoader.
  - An Earth `SphereNode(radius 1, stacks 48, slices 96)` with `materialLoader.createTextureInstance(ImageTexture from assets, roughness 0.9)`.
  - A camera at z 3.5 with `rememberCameraManipulator(orbitHome z 3.5, target origin)`.
  - onFrame spins the Earth 0.15°/frame, and a `FrameStats` FPS counter overlay.
  - Route `space_spike`, opened from the dashboard ⋮ → "3D test (Earth)".
- Size: the debug APK is 42.7 MB (all 4 ABIs). Per ABI the Filament libs are about 6–7.5 MB (arm64 7.0 MB); assets 4.7 MB (texture + SceneView's neutral IBL). Play AAB splits give about +7–12 MB per device.
- Tests: 136 JVM + 18 on-device pass. Installed; the phone was locked, so the **user must open ⋮ → 3D test and report the smoothness/FPS**.
- Default SceneView environment: black skybox + neutral IBL + a default directional light.
- **3D-1 user test (2026-09-28 12:10):** 57–59 FPS, but the Earth rendered as a flat **grey ball**.
  - **Root cause:** SceneView's `Sphere` has **one primitive per stack**. `SphereNode(materialInstances = listOf(x))` applies x to the first stack only (the top cap); every other stack gets Filament's default grey material.
  - **Fix:** use the `materialInstance = x` constructor (a single range covering the whole sphere). Diagnosed with a 3-sphere test (red lit, lit Earth, unlit Earth).
  - SceneView's `createImageInstance` (unlit) shows stripe artefacts on spheres, so **only use the lit `createTextureInstance`**.
  - After the fix: a correct textured Earth (continents correctly oriented, not mirrored) at 57 FPS (screenshot confirmed).
- **Debug shortcut:** debug builds accept `adb shell am start -n com.achyutpal.scrolljourney/.MainActivity --es debug_route <route>` (MainActivity checks FLAG_DEBUGGABLE; AppNavHost navigates via LaunchedEffect). E.g. `--es debug_route space_spike`. With the phone unlocked, Claude can open screens and `adb exec-out screencap -p` to inspect them.

## 2026-09-28: 3D-2 solar system built (session 3)

- The user's 3D-1 check passed (57–59 FPS, rotate/zoom OK); they expected more than one Earth, which is what 3D-2 delivers.
- **Custom Filament materials** (SceneView has no emissive/unlit option that works on spheres):
  - Sources in `materials/*.mat`; compiled with **matc 1.56.0** (must match the Filament runtime 1.56.0 from SceneView 2.3.0) to `app/src/main/assets/materials/*.filamat`. See `materials/README.md` for the commands. matc comes from the filament-v1.56.0-linux.tgz release.
  - `space_unlit_textured`: unlit, culling none, uv0, params baseTexture + intensity; used for the Sun (1.4) and the starfield sphere (0.8).
  - `space_unlit_color`: unlit, transparent, depthWrite false, params color (float4) + intensity; used for orbit paths (0.75, 0.8, 1, 0.22).
  - **Unlit materials are NOT affected by camera exposure**, so intensity ≈ 1 = texture as-is. The first try used 14 000/48 000 and blew the screen out white/pink.
- Textures in `assets/textures/` (≈2 MB total): sun, jupiter, saturn, stars (2K); mercury, venus (atmosphere), moon, mars, uranus, neptune, pluto (= makemake_fictional), ceres (= ceres_fictional) (1K); earth (2K); saturn_ring.png (RGBA strip). `CREDITS.txt`: Solar System Scope CC BY 4.0.
- `ui/space/SolarSystemLayout.kt` (pure): `SpacePoint`, `enum SpaceBody(texture, radius, orbitRadius, orbitPeriodSeconds, startAngleDegrees, spinPeriodSeconds)` with parent moons (MOON→EARTH, EUROPA→JUPITER, TITAN→SATURN).
  - Stylised layout: Sun r3.5; orbits Mercury 6.5, Venus 9, Earth 12, Mars 15.5, Ceres 18.5, Jupiter 24, Saturn 31, Uranus 37, Neptune 42, Pluto 46.
  - `positionAt(t)`, `spinAt(t)`; Saturn ring 1.8–3.3, tilt 27°; starfield radius 500.
- `ui/space/SpaceGeometry.kt`: orbitCircle (LINES, 160 segments), ring (annulus, u radial / v around).
- `ui/space/ClampedOrbitManipulator.kt`: delegates to SceneView's default orbit manipulator and blocks pinch-zoom past min 7 / max 140 from the origin.
- `ui/space/SpaceScreen.kt` (replaces EarthSpikeScreen):
  - Environment IBL intensity lowered to 6000 (dark night sides); `mainLightNode = null`.
  - A Sun **point light** at the origin (3.0e8 lm, falloff 200).
  - Lit planets via `createTextureInstance` (single materialInstance!); Saturn ring via `createTextureInstance(isOpaque = false)` + `setCullingMode(NONE)`.
  - Camera home (0, 26, 62) looking at the Sun; onFrame animates orbits and spins with a SceneClock; FPS overlay.
  - Route `space` (debug_route space), menu "3D solar system (preview)".
- Result on the realme (screenshot): Sun, orbit rings, Saturn + rings, Earth + Moon, Venus, Mars, Mercury, starfield at **58 FPS**. The INTERNET permission is still stripped.
- Tests: **142 JVM** (+SolarSystemLayoutTest 6) and 18 on-device pass. The debug APK is 44 MB (all ABIs).
- Pending: the user's check (rotate 360°, zoom limits, find all planets). Then **3D-3**: rocket + trail from RocketState, highlight reached/dim unreached, tap-a-planet card, deep-space star view past the Oort Cloud.

## 2026-09-28: 3D-2 camera: tap a planet to fly to it and follow it

- The user reported that pinch-zoom always zooms on the Sun (the fixed orbit target). Fix: a **focus camera**.
  - `ui/space/OrbitCameraState.kt` (pure, 7 tests): yaw/pitch (pitch clamped ±85°), distance, target, desiredDistance; min/max per focus (Sun overview: min 7, desired 67, pitch 23; a planet: min = radius×1.8, desired = radius×6, at least 1.6; max 140); pan offset; `advance(dt, focusCentre)` eases (5/s) toward the moving focus.
  - `ui/space/FocusOrbitManipulator.kt`: SceneView CameraManipulator → state (drag orbit, 2-finger pan, pinch zoom); `update()` is a no-op. The camera transform is set every frame from SpaceScreen.onFrame (SceneView only updates the manipulator after the first touch).
  - Tap → `scene.bodyFor(node)` (3D hit; the ring counts as Saturn) **or** `SpacePicking.pick` (screen-space: projects bodies with `cameraNode.worldToView`, touch radius max(36dp, drawn radius + 16dp)) → focusOn(body). Double-tap → Sun overview. The bottom card shows the body name + a hint.
  - The starfield and orbit nodes have `isHittable = false` (the camera is inside the starfield sphere).
  - ClampedOrbitManipulator removed.
- **Bug found:** SceneView's `lookAt(eye, target)` passes `target - eye` into kotlin-math `lookAt`, which subtracts eye again, so it's only correct when the target is the origin. Use kotlin-math `lookTowards(eye, target - eye, up)`. Recorded in research-notes.
- Verified on the phone via adb taps (live positions logged, then logs removed): tapping Earth flies the camera to Earth and follows it (58 FPS).
- Tests: **154 JVM** (+OrbitCameraState 7, SpacePicking 5) and 18 on-device pass.
- 2026-09-28: The user confirmed the tap-to-focus camera feels correct and asked about the Sun-centric orbit feel. Explained it's by design (focus-orbit); the user is fine keeping it. **3D-2 ✅.** Next: 3D-3.

## 2026-09-28: 3D-3 journey in 3D (built, device-verified by Claude)

- `ui/space/JourneyMap.kt` (pure, 10 tests):
  - Destination ↔ SpaceBody mapping. Deep-space markers (fixed positions, radius, colour): Kuiper Belt r54, Oort Cloud r95, Proxima 165, Alpha Centauri 180, Sirius 250, Vega 320, Galactic Centre 420 (inside the starfield r500).
  - `anchor(d, t)`, `radius(d)`.
  - `rocketPosition(rocket, t)`: lerp between the reached and next stop by legProgress; ends pushed off the surfaces by GAP 0.6, shrunk to 1/3 of the free space on short legs (planet→own moon). The rocket is never inside a body (tested 0..1000 fuel). At the final stop it parks above.
  - `rocketHeading`, `reachedRoute`, `upcomingRoute` (3 stops).
- `ui/space/JourneyStatus.kt` (pure, 4 tests): Reached / Next(fuelToGo, required) / Locked, with stop N of 18.
- `ui/space/RocketModel.kt`: primitives along −Z (cylinder body, stretched red sphere nose, 3 cube fins, unlit orange flame flickering, far-away glow visible only when camera distance > 10 and scaled with distance).
- `ui/space/SolarSystemScene.kt` (split out of SpaceScreen): all nodes + journey layer.
  - Rocket transform via `lookTowards(p, heading, up)`.
  - Green reached-glows, an orange pulsing next-glow, and deep-space marker spheres.
  - Kuiper and Oort rings.
  - Travelled + upcoming route lines (LINES, fixed vertex count, padded, `updateGeometry` every frame).
  - `focusFor(node)`.
- **Crash fixed:** Filament Precondition "AABB can't be empty". The route line geometry was created with all vertices at the origin; it's now seeded spanning ±STARFIELD_RADIUS (the creation-time AABB is also what culling uses).
- `ui/space/SpaceViewModel.kt`: rocket from settings; **debug builds: `--ei debug_fuel N`** previews any fuel level (MainActivity sets `SpaceViewModel.debugFuelOverride`; real data untouched).
- SpaceScreen:
  - `SpaceFocus` (Overview / Body / Marker / Rocket); screen-space picking includes markers and the rocket.
  - FocusCard: the title plus a journey line (✓ Reached / 🎯 Next · X more fuel / 🔒 needs F), "Scenery: not a stop" for the Sun/Mercury/Venus, and a rocket line "from → to · fuel a / b".
  - Buttons: "🚀 Find my rocket" and "Solar system". The rocket focus radius is 0.7. FPS moved to a small top-right label.
- Verified on the realme with the debug preview: 40 fuel (Europa → Saturn; green glows, route, Find my rocket flies to a clean rocket on the route) and 320 fuel (Proxima → Alpha Centauri), 56–59 FPS.
- Tests: **168 JVM** and 18 on-device pass.
- The user's real state is still 0 fuel at Earth (the rocket sits just outside Earth heading to the Moon).
- 2026-09-28: The user reviewed 3D-3 and disliked the oversized primitive rocket (as big as Earth, overlapping Earth/Moon), the zig-zag straight route lines and the flat disc glows. Claude analysed it and proposed the 'mission control' restyle; **the user approved all recommendations** (see decisions). Step 1 = beacon + CC0 rocket model at correct scale + rim glow; step 2 = flight arc + rings + labels.
- **Restyle step 1 done (2026-09-28):**
  - Rocket = Kenney Space Kit CC0 parts (`assets/models/rocket/`: baseA + fuelB + sidesB + topB, 4.1 model units, stacked at y 0/1/2/3), loaded as ModelNodes with `centerOrigin (0, −1, 0)`, scaled to **LENGTH 0.2** and rotated so the nose faces −Z.
  - **The GLBs needed cleaning:** the scene root node was also a child of `tmpParent` (gltfio returned null, so an NPE), plus a (2, 0, 1.5) offset and metallic = 1 materials (they render near-black here). A Python script rewrote the JSON chunk: single root, offset removed, metallic 0 / roughness 0.55, `KHR_materials_unlit` usage dropped. Noted in LICENSE.txt.
  - The far view shows a **beacon** (core glow, radius = camDist × 0.011, visible when > 4) plus a **comet tail** (9 fading glows behind, spacing camDist × 0.016). The model is visible up close; an engine glow at the back.
  - New material **`space_glow`** (unlit, additive, no depth write, requires tangents; params color/power/rim/intensity; rim 0 = core, 1 = edge).
  - The next stop gets a thin rim glow (power 6, ×1.18 radius). Reached glows removed. Star markers get a soft core halo (r × 3.5).
  - "Find my rocket" camera now **looks from the side away from the nearest body** (`OrbitCameraState.focus(viewFrom = …)`, `lookFrom()` sets yaw/pitch; +1 test), so the planet sits behind the rocket instead of blocking the view.
  - Verified on the phone: a detailed small rocket leaving Earth toward the Moon, 59 FPS; overview 57 FPS.
- **Restyle step 2 done (2026-09-28):**
  - The zig-zag route lines are removed. `JourneyMap.leg()` returns a **FlightArc** (quadratic Bézier from just off the last stop to just off the next, control = midpoint lifted by `ARC_LIFT 0.18` × length, gap rules kept). `rocketPosition` = `arc.pointAt(legProgress)` and `rocketHeading` = `arc.tangentAt()`. `reachedRoute/upcomingRoute` were removed. Tests updated (+4 arc tests, 171 total).
  - The scene draws the arc for the current leg only: **solid** orange from 0 → progress (40 segments) and **dashed** faint from progress → 1 (20 dashes). Nothing for past legs.
  - A faint green rim ring on reached stops (power 12, alpha 0.22, ×1.06). The next stop's rim ring is power 6, ×1.18.
  - **Floating Compose labels** (`LabelSpot`, `FloatingLabel`): "🚀 YOU", "<stop> · next · N fuel", "✓ <last reached>". Projected every frame via `cameraNode.worldToView`, positioned with `offset { } + graphicsLayer` (centred above). Priority order you → next → reached; **overlapping lower-priority labels are hidden** (nudging had put the Moon label next to Saturn).
  - Tuning: beacon 0.008 × distance; tail 16 segments, spacing 0.0055 × distance, alpha 0.35 (a smooth streak, not beads); engine glow r 0.028.
  - Verified on the phone with real data (Earth → Moon): overview shows only "YOU" + a fine tail; close-up shows a detailed rocket, thin rings, dashed arc; 59 FPS. The 40-fuel preview showed "Saturn · next · 10 fuel".
- **Restyle fixes after the user's review (2026-09-28):** the user reported (1) "Find my rocket" wrapping to 2 lines and left-aligned on planet cards, (2) the beacon glow and comet tail cutting into Earth (the launch planet), (3) the white beacon looking like an extra stop and the tail looking bolted onto Earth. Root causes: beacon r = 0.008 × camDist ≈ 0.52 in the overview (bigger than the Moon, 0.16, almost Earth's 0.6) while the rocket sits only 0.18 above Earth at fuel 0; the tail was a straight line of 16 dots pointing backwards (through Earth), about 5.8 units long.
  - `JourneyMap.clearance(rocket, t, point)`: distance to the nearer surface of the leg's two stops. `FlightArc.pointsBehind(from, spacing, count)`: points spaced by arc length behind the rocket, stopping at the arc start. +4 tests (**175 JVM**).
  - `RocketModel`: beacon 0.0025 × camDist, **capped by the clearance**, warm orange (1, 0.62, 0.28). The tail = 12 world-space dots **along the flown arc** (none at fuel 0), spacing 0.004 × camDist, alpha 0.3, each capped by its own clearance. The model is visible below cam 10; the beacon cross-fades in (smoothstep) from 8 to 12 via the material alpha, so there's no white ball on the model. `animate(seconds, cameraDistance, clearance, trail)`; add `rocket.nodes` (root + tail) to the scene.
  - Next-stop ring thinner: power 12, alpha 0.85, ×1.08, pulse ±3%.
  - Labels: a stop label that would cover the rocket on screen is hidden.
  - FocusCard: with 2 buttons, equal `weight(1f)`, one line, centred text, contentPadding 12 dp.
  - Installed on the realme; the phone was locked during the session, so Claude couldn't screenshot it. **The user's visual check is pending.**
- **2026-09-28: the user verified the review fixes on the phone:** all 3 reported points are fixed and nothing new was found. **3D-3 ✅ complete.** Next: 3D-4 (plan sent to the user for approval).
