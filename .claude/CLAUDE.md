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

- **Phase:** 1 ✅ verified on the device (realme, Android 13). Phase 2 (app picker) not started.
- **Last done (2026-09-27):** Phase 1 passed every device check. Added the `UsageAccess` debug log. realme blocks `adb shell appops set`, so toggle the permission by hand.
- **Waiting on:** the user's approval to start Phase 2, including how to store the app selection (proposed: bring in Room early with only `SelectedAppEntity`).
- **Next step:** Phase 2, the app picker (PackageManager + `<queries>` for launcher apps, pre-select installed social apps, save the selection).

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
    │   ├─ domain/model/UsageAccessState.kt
    │   ├─ domain/repository/UsageAccessRepository.kt
    │   ├─ data/usage/AndroidUsageAccessRepository.kt
    │   ├─ util/UsageAccessSettings.kt
    │   ├─ navigation/AppNavHost.kt     routes: intro, usage_access, home
    │   └─ ui/{theme, onboarding/{IntroScreen, UsageAccessScreen}, permission/UsageAccessViewModel, home/HomePlaceholderScreen}
    └─ test/.../ui/permission/UsageAccessViewModelTest.kt
```

Build commands are in `memory/tech-stack.md`.
