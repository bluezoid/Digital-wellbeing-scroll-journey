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
