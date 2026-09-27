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

- **Phase:** 7 (rocket engine). The code is done; 100 unit + 16 on-device tests pass; DB v4 migrated on the phone. **Waiting on the user's phone check.**
- **Last done (2026-09-27):** Phase 6 ✅ (1 m/min final). Planet thresholds, RocketProgressCalculator, exactly-once fuel application in a transaction, journey start date (the onboarding day, or "today" for existing users), a rocket card and fuel badges on home.
- **Waiting on:** the user to check the rocket card (it should show Earth, journey started 27 Sep). Real fuel first appears on 28 Sep when 27 Sep is finalized.
- **Next step:** Phase 8, the real Dashboard (single DashboardUiState; replaces HomePlaceholderScreen; rocket visuals).
- **Never run `connectedAndroidTest`** (it uninstalls the app). See progress-log for the manual on-device test commands, and the schema-bump gotcha.

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
 ├─ README.md                    original research; the scroll-counting part is superseded
 ├─ idea.txt                     the user's scratch file
 ├─ settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew
 └─ app/src/
    ├─ main/AndroidManifest.xml  PACKAGE_USAGE_STATS declared
    ├─ main/res/                 strings, themes, colors, adaptive rocket icon
    ├─ main/java/com/achyutpal/scrolljourney/
    │   ├─ ScrollJourneyApp.kt   Application → AppContainer
    │   ├─ MainActivity.kt       picks the start route, hosts navigation
    │   ├─ di/AppContainer.kt    manual DI
    │   ├─ domain/model/{UsageAccessState, InstalledApp, SelectedApp}.kt
    │   ├─ domain/calculator/{AppSelectionRules, LimitEvaluator, DailyLimitRules, DistanceConfig, DistanceCalculator, DistanceComparisons, RocketProgressCalculator}.kt
    │   ├─ domain/repository/{UsageAccessRepository, AppRepository}.kt
    │   ├─ domain/model/{UsageEvent, DailyUsageSummary}.kt, domain/calculator/UsageCalculator.kt
    │   ├─ domain/usecase/{SyncUsageHistoryUseCase, ApplyJourneyFuelUseCase, TodayUsageResult}.kt
    │   ├─ domain/repository/{Usage,History,Settings,Journey}Repository.kt, domain/model/{DayStatus,Planet,RocketState,UserSettings,DailyRecord,ForegroundUsage}.kt
    │   ├─ data/usage/{AndroidUsageAccessRepository, UsageDataSource, AndroidUsageRepository}.kt
    │   ├─ data/local/ScrollJourneyDatabase.kt (v4)  entity/{SelectedApp,DailyUsage,DailyAppUsage,UserSettings}Entity  dao/*
    │   ├─ data/history/RoomHistoryRepository.kt, data/settings/RoomSettingsRepository.kt, data/journey/RoomJourneyRepository.kt
    │   ├─ data/apps/{AndroidAppRepository, AppIconSource}.kt
    │   ├─ util/UsageAccessSettings.kt
    │   ├─ navigation/AppNavHost.kt     routes: intro, usage_access, app_picker?onboarding=, daily_limit?onboarding=, home
    │   └─ ui/{theme, startup/StartupViewModel, onboarding/{IntroScreen, UsageAccessScreen}, permission/UsageAccessViewModel,
    │          apppicker/{AppPickerScreen, AppPickerViewModel}, dailylimit/{DailyLimitScreen, DailyLimitViewModel},
    │          home/{HomePlaceholderScreen, HomePlaceholderViewModel}, common/{DurationFormat, DistanceFormat}, theme/StatusColors}
    ├─ schemas/                  Room schema JSON per DB version (committed)
    ├─ test/.../                 JVM unit tests + fakes/
    └─ androidTest/.../data/local/   on-device Room + migration tests
```

Build commands are in `memory/tech-stack.md`.
