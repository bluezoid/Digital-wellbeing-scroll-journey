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

**Update 2026-09-27:** preferences go in a Room `UserSettingsEntity` (per the spec), not DataStore. See spec.md for the full package layout.

## Actual build setup (Phase 0, 2026-09-27)

Pinned to what builds offline-friendly on this machine. Android Studio will suggest upgrades; that's fine, but upgrade deliberately.

| Item | Version |
|---|---|
| Gradle wrapper | 8.10.2 |
| AGP | 8.7.2 |
| Kotlin + Compose compiler plugin | 2.1.0 |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |
| Compose BOM | 2024.09.00 (material3 1.3) |
| activity-compose | 1.9.2 |
| lifecycle (runtime-compose, viewmodel-compose) | 2.8.6 |
| navigation-compose | 2.8.0 (string routes, not type-safe) |
| core-ktx | 1.13.1 |
| JUnit | 4.13.2 |
| JDK | 17 (Corretto) |
| SDK | `~/Android/Sdk` (platforms 34, 36) |

- Versions live in `gradle/libs.versions.toml`.
- **Before a Play Store release**, raise targetSdk to Play's current requirement (lint `OldTargetApi` flags it). This needs a newer AGP and compileSdk 35/36.

Commands (run from the repo root):
- `./gradlew assembleDebug`: APK at `app/build/outputs/apk/debug/app-debug.apk`
- `./gradlew testDebugUnitTest`: JVM unit tests
- `./gradlew lintDebug`
- `./gradlew installDebug`: install on the connected phone (check `adb devices` first)
- If resources get renamed and the build says "resource not found," run `./gradlew clean` (stale incremental cache).

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

## Versions after the 2026-10-10 upgrade (authoritative; older numbers above are history)
compileSdk/targetSdk 36, minSdk 26, AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, KSP 2.2.21-2.0.5, Compose BOM 2026.03.00, Room 2.8.4, WorkManager 2.10.5, Navigation 2.9.5, Lifecycle 2.9.4, SceneView 2.3.0 (Filament 1.56), kotlinx-serialization-core constrained to 1.8.1. JVM target 17 via `kotlin { compilerOptions { } }`.
