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

- **Phase:** 3 (UsageStats integration). The code is done and 41 unit tests pass. **Waiting on the phone comparison with Digital Wellbeing.**
- **Last done (2026-09-27):** Phase 2 ✅ verified (33 apps). Built UsageCalculator (queryEvents pairing), GetTodayUsageUseCase, and a home screen showing today's minutes per tracked app.
- **Waiting on:** the user to compare the app's minutes with Digital Wellbeing (target within about ±1 min per app).
- **Next step:** Phase 4, Room history. Add DailyUsage, DailyAppUsage and UserSettings entities via a DB v2 migration, the onboardingComplete flag, and backfill of past days.

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
    │   ├─ domain/calculator/AppSelectionRules.kt
    │   ├─ domain/repository/{UsageAccessRepository, AppRepository}.kt
    │   ├─ domain/model/{UsageEvent, DailyUsageSummary}.kt, domain/calculator/UsageCalculator.kt
    │   ├─ domain/usecase/GetTodayUsageUseCase.kt, domain/repository/UsageRepository.kt
    │   ├─ data/usage/{AndroidUsageAccessRepository, UsageDataSource, AndroidUsageRepository}.kt
    │   ├─ data/local/{ScrollJourneyDatabase, entity/SelectedAppEntity, dao/SelectedAppDao}.kt
    │   ├─ data/apps/{AndroidAppRepository, AppIconSource}.kt
    │   ├─ util/UsageAccessSettings.kt
    │   ├─ navigation/AppNavHost.kt     routes: intro, usage_access, app_picker, home
    │   └─ ui/{theme, startup/StartupViewModel, onboarding/{IntroScreen, UsageAccessScreen}, permission/UsageAccessViewModel,
    │          apppicker/{AppPickerScreen, AppPickerViewModel}, home/{HomePlaceholderScreen, HomePlaceholderViewModel}}
    ├─ schemas/                  Room schema JSON per DB version (committed)
    └─ test/.../                 fakes/, domain/calculator/, ui/apppicker/, ui/permission/ tests
```

Build commands are in `memory/tech-stack.md`.
