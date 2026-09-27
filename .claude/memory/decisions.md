# Decision log

Newest first. Don't re-open a settled decision unless the user asks. Record reversals as new entries.

| Date | Decision | Why | Status |
|---|---|---|---|
| 2026-09-27 | Git commits carry no Co-Authored-By or AI attribution; the author is Achyut Pal <achyutpal@gmail.com> | The user's explicit instruction | Final |
| 2026-09-27 | A project memory system in `.claude/` (CLAUDE.md plus memory/*.md) | Keeps context across Claude accounts and sessions and saves tokens | Final |
| 2026-09-27 | No code until the user approves the plan | The user's explicit instruction | Final |
| 2026-09-27 | MVP feature list F1–F10 (see mvp-features.md) | Smallest version that proves the idea | **Proposed, awaiting approval** |
| 2026-09-27 | Rocket and streak defaults (green/yellow/red thresholds, fuel per leg) | Simple, tunable | **Proposed, awaiting approval** |
| 2026-09-27 | Stack: native Kotlin + Compose, MVVM, Room, DataStore, WorkManager, manual DI, minSdk 26 | Android-only feature, native API, learning goal | Final |
| 2026-09-27 | Wellbeing direction: reward *less* usage; the rocket moves on green days | The original design rewarded scrolling, which contradicted the goal and is harder to monetise | Final |
| 2026-09-27 | Keep the scrolling metaphor with an *estimated* scroll distance from minutes | Keeps the unique hook while using reliable data | Final |
| 2026-09-27 | Dropped home location, GPS and maps from the MVP | Privacy friction, not needed | Final |
| 2026-09-27 | Pivot: screen time (UsageStatsManager) instead of scroll counting (AccessibilityService) | Play policy risk, accuracy, onboarding friction | Final |
