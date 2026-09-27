# Tech stack

**Decided 2026-09-27:** native Android, Kotlin, Jetpack Compose.

## Why native (not Flutter or React Native)

- The core feature is Android-only. iOS doesn't let apps read other apps' usage, so cross-platform gains nothing.
- `UsageStatsManager` is a native API. Flutter or React Native would need a Kotlin bridge anyway, which means two stacks to learn.
- The user wants to understand each piece, and the official docs and samples are written for Kotlin.

## Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM: ViewModel + StateFlow, one-way data flow |
| Usage data | `UsageStatsManager.queryEvents()` (not `queryUsageStats()`, whose buckets are imprecise around midnight) |
| Database | Room (daily per-app minutes) |
| Preferences | DataStore (selected apps, limit, onboarding flag) |
| Background | WorkManager (daily snapshot job) |
| Navigation | Navigation Compose |
| Dependency injection | Manual (constructor injection plus a simple `AppContainer`) at first; Hilt later |
| Visuals | Compose Canvas and built-in animation APIs |
| Tests | JUnit for the domain layer (rocket engine, streaks, distance) |
| Build | Android Studio, Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |
| minSdk | 26 (Android 8.0) |

## Planned package structure

```text
app/src/main/java/<package>/
 ├─ data/     UsageStatsSource, Room (entities, DAO, DB), DataStore, repositories
 ├─ domain/   RocketEngine, StreakCalculator, DistanceEstimator (pure Kotlin, no Android imports)
 ├─ ui/       onboarding/, apppicker/, dashboard/, settings/, theme/
 └─ work/     DailySnapshotWorker
```

The package name isn't chosen yet; ask the user when creating the project.

## Not in the MVP

Firebase, Crashlytics, Play Billing/RevenueCat, Glance widget, Lottie, Vico charts, Hilt, Maps, Location.
