# MVP technical spec (condensed, AUTHORITATIVE)

The user provided this on 2026-09-27 as the reference for the whole MVP ("Scroll Journey: Kotlin Architecture, HLD, LLD, Workflows & Test Plan").
This file is a faithful condensed copy. Where it conflicts with older memory files, **this file wins**, except for the deviations listed at the bottom.

## Basics

- App name: **Scroll Journey**. Package: **`com.achyutpal.scrolljourney`**.
- Kotlin, Jetpack Compose, MVVM + clean-architecture principles, StateFlow, coroutines, Room, WorkManager, Navigation Compose.
- No network, backend, auth, or location.
- **Primary test device: the user's physical Android 13 phone**, over USB debugging (`adb devices`). The emulator is only for later UI tests.
- Counts **minutes in selected apps**, never scrolls. UI copy must say **"Estimated scroll distance"**, never "You scrolled X m."

## Features F1–F10 (APPROVED)

1. F1 Onboarding and permission
2. F2 App picker
3. F3 Daily limit (combined, 15–240, default 60)
4. F4 Usage tracking
5. F5 History (Room)
6. F6 Dashboard
7. F7 Rocket journey
8. F8 Streak calendar
9. F9 Distance estimate
10. F10 Settings (apps, limit, reset journey, privacy)

**Out of scope:** scroll counting, AccessibilityService, location, maps, login, backend, cloud, subscriptions, notifications, widgets, XP, badges, charts, per-app limits, blocking, social features, AI, sync.

## Architecture and dependency rule

```text
UI → ViewModel → UseCase → Repository → DataSource (Android APIs / Room)
```

- Never UI → Room, UsageStatsManager or PackageManager. Never ViewModel → DAO directly.
- The domain layer is pure Kotlin: no Context, Compose, Room, UsageStatsManager or PackageManager.
- MainActivity: entry point, nav host, permission refresh only. No calculations, no DB access.
- Use cases only where they isolate a real rule. Don't create files just to have more files.

Package layout:

```text
com.achyutpal.scrolljourney
├── data/{local/{AppDatabase, dao, entity}, usage/AndroidUsageDataSource, repository}
├── domain/{model, calculator, usecase}
├── ui/{onboarding, apppicker, dashboard, history, journey, settings}
├── worker/DailyUsageWorker
├── navigation/
├── util/
└── MainActivity
```

Repositories:
- `UsageRepository`: `getTodayUsage()`, `getUsageForDate(date)`, `getUsageForDateRange(start, end)`
- `AppRepository`: PackageManager plus selected apps
- `HistoryRepository`: Room daily usage
- `SettingsRepository`

Use cases (suggested): GetTodayUsage, GetDailyHistory, SaveDailyUsage, GetInstalledApps, UpdateSelectedApps, GetSettings, UpdateDailyLimit, EvaluateDayStatus, CalculateDistance, CalculateRocketProgress, CalculateStreak, ResetJourney.

## Data model (Room `ScrollJourneyDatabase`, version 1, real migrations; never destructive on upgrade)

- `SelectedAppEntity`: packageName (PK), appName, enabled, addedAt
- `DailyUsageEntity`: date "YYYY-MM-DD" (PK), totalMinutes, status: DayStatus, fuelDelta, distance: Long
- `DailyAppUsageEntity`: id (PK), date, packageName, minutes. **UNIQUE(date, packageName)**, upsert
- `UserSettingsEntity`: id (PK), dailyLimitMinutes = 60, onboardingComplete = false, plus journey state (see Rocket)
- DAOs: SelectedAppDao, DailyUsageDao, DailyAppUsageDao, UserSettingsDao
- `enum Planet { EARTH, MOON, MARS, JUPITER, SATURN, URANUS, NEPTUNE, PLUTO }`
- `enum DayStatus { GREEN, YELLOW, RED }`
- `enum UsageAccessState { GRANTED, REVOKED, UNKNOWN }`

## Rules (locked)

- **Day status**, integer maths only:
  - GREEN if `usage <= limit`
  - YELLOW if `usage*100 <= limit*125`
  - otherwise RED
  - With limit 60: 60 is green, 61 and 75 are yellow, 76 is red.
- **Fuel:** GREEN +1, YELLOW 0, RED −1. `fuel = max(0, fuel + delta)`.
- **Planet thresholds:** Moon 3, Mars 7, Jupiter 14, Saturn 21, Uranus 30, Neptune 45, Pluto 60.
- **Rocket state:** `currentFuel`, `lifetimeFuel`, `highestReachedPlanet`. highestReachedPlanet is permanent: a red day lowers fuel but never moves the rocket back past a reached planet.
  - Example: at Mars with fuel 7, a red day gives fuel 6, still Mars.
- `RocketProgressCalculator(currentFuel, highestReachedPlanet, dayStatus) → RocketState`
- **Copy:** say "60 fuel points to reach Pluto," never "Pluto in 6 months."
- **Streak:** only GREEN counts. Yellow or red resets it. Current streak counts back from today; best streak is the longest run. `StreakCalculator(List<DayStatus>) → (current, best)` is pure Kotlin.
- **Distance:** `minutes × DistanceConfig.scrollDistancePerMinute`. Use Long (no overflow). The constant lives in one config class. Comparisons (Eiffel Tower, Burj Khalifa, Everest, …) stay few and simple.
- **Limit validation:** accepts 15–240; rejects 14, 241, 0 and negatives.
- **Reset journey:** a confirmation dialog, then fuel = 0, highest = EARTH, journeyDistance = 0. **History is kept.**
- **Uninstalled tracked app:** keep its history, remove it from the current selection. **Reinstalled:** don't re-enable it automatically.
- **No data vs API failure:** distinguish them. Show "No usage recorded yet" rather than a silent 0.

## Usage, history and background

- Everything goes through a `UsageDataSource` abstraction. Today = local midnight until now.
- Android keeps events only a few days, so **Room is the historical source of truth**.
- On first grant, backfill whatever days Android still has. Don't promise a fixed number of days.
- **Reconcile on app open** (finalise missing previous days, apply fuel, update today). WorkManager `DailyUsageWorker` is a reliability backup: idempotent, fills missing days, and never assumes it runs exactly at midnight.
- Permission is checked on every important entry. If revoked, the dashboard shows "Usage Access is disabled. Enable it to continue tracking your selected apps." with a button to open Settings.

## Onboarding flow

```text
Launch → onboardingComplete?
  NO:  Intro 1 → Intro 2 → Usage Access explanation → Settings → return → verify → App Picker → Daily Limit → Dashboard
  YES: Dashboard
```

The dashboard is never shown before onboarding completes.

## Build order (spec phases)

- 0 Project setup
- 1 Onboarding + Usage Access
- 2 App picker (check package visibility on Android 13; avoid QUERY_ALL_PACKAGES)
- 3 UsageStats integration
- 4 Room + history
- 5 Limit/status engine
- 6 Distance engine
- 7 Rocket engine
- 8 Dashboard
- 9 History + streak
- 10 WorkManager
- 11 Settings
- 12 Real-device validation over 7–14 days

**Development rule:** understand → design → implement a small piece → run → test → show the result → review → next. **Never generate the whole app at once.**

## Tests required (full lists are in the user's spec; key cases)

- **LimitEvaluator:** (60, 0/60) GREEN; 61/75 YELLOW; 76 RED.
- **Planet by fuel:** 0/2 Earth, 3/6 Moon, 7/13 Mars, 14/20 Jupiter, 21/29 Saturn, 30/44 Uranus, 45/59 Neptune, 60 Pluto.
- **Fuel:** never negative. Milestone protection.
- **Streak:**
  - G,G,G gives 3/3
  - G,G,Y,G gives 1/2
  - R,R gives 0/0
  - Today G, yesterday G, day before Y gives current 2
- **Distance** at 100/min: 0→0, 1→100, 10→1000, 60→6000, plus large values.
- **Total includes only selected apps** (Instagram 20 + YouTube 10 = 30, ignoring Facebook and Chrome).
- **App picker:** an installed default is pre-selected, an uninstalled app is not shown, an unchecked app is excluded.
- **Room:** upsert with no duplicates for (date, package), date-range queries, reset keeps history, migration test.
- **ViewModel tests** use fake repositories.
- **UI tests:** onboarding navigation, permission, picker, limit, dashboard, history, settings, reset dialog.
- **Real device:**
  - Accuracy within ±1 min of Digital Wellbeing (a target, not a guarantee).
  - A 7-day run including a reboot and a day without opening the app.
  - Force-stop.
  - The 23:59 → 00:01 boundary (critical).
  - Uninstall and reinstall of a tracked app.

## Deviations from the spec (decided by Claude, recorded in decisions.md)

1. §23 suggests `queryUsageStats(INTERVAL_DAILY)`. We use **`queryEvents()`** to pair foreground/background events, because daily buckets are imprecise at midnight. It stays behind `UsageDataSource`.
2. Settings live in the Room `UserSettingsEntity` as the spec says. This replaces the earlier plan to use DataStore.
3. In Phase 1 there is no Room yet, so `onboardingComplete` isn't persisted. The start screen is chosen by the permission state (granted goes to the post-onboarding placeholder). The real flag arrives with Room in Phase 4.
