# Decision log

Newest first. Don't re-open a settled decision unless the user asks. Record reversals as new entries.

| Date | Decision | Why | Status |
|---|---|---|---|
| 2026-09-27 | Phase 1: no persisted onboarding flag yet. MainActivity starts at HOME if Usage Access is granted, otherwise INTRO | Room arrives in Phase 4; avoid throwaway storage code | Temporary |
| 2026-09-27 | `UsageAccessState.REVOKED` means "currently off," whether never granted or switched off later | Matches the spec enum; the UI treats both the same | Final |
| 2026-09-27 | Library versions pinned to AGP 8.7.2 / Kotlin 2.1.0 / compileSdk 34 (see tech-stack.md) | Works with the installed SDK and the cached Gradle | Revisit before Play release |
| 2026-09-27 | Use `queryEvents()` rather than the spec §23 `queryUsageStats(INTERVAL_DAILY)` | Daily buckets are imprecise at midnight; hidden behind UsageDataSource anyway | Final |
| 2026-09-27 | Settings stored in Room (UserSettingsEntity), replacing DataStore | Follows the user's spec | Final |
| 2026-09-27 | The user's technical spec adopted as authoritative (saved as memory/spec.md) | The user provided it as the reference | Final |
| 2026-09-27 | MVP F1–F10 and the rocket defaults APPROVED | Approved via the user's spec | Final |
| 2026-09-27 | Git commits carry no Co-Authored-By or AI attribution; the author is Achyut Pal <achyutpal@gmail.com> | The user's explicit instruction | Final |
| 2026-09-27 | A project memory system in `.claude/` (CLAUDE.md plus memory/*.md) | Keeps context across Claude accounts and sessions and saves tokens | Final |
| 2026-09-27 | No code until the user approves the plan | The user's explicit instruction | Final |
| 2026-09-27 | Stack: native Kotlin + Compose, MVVM, Room, DataStore, WorkManager, manual DI, minSdk 26 | Android-only feature, native API, learning goal | Final |
| 2026-09-27 | Wellbeing direction: reward *less* usage; the rocket moves on green days | The original design rewarded scrolling, which contradicted the goal and is harder to monetise | Final |
| 2026-09-27 | Keep the scrolling metaphor with an *estimated* scroll distance from minutes | Keeps the unique hook while using reliable data | Final |
| 2026-09-27 | Dropped home location, GPS and maps from the MVP | Privacy friction, not needed | Final |
| 2026-09-27 | Pivot: screen time (UsageStatsManager) instead of scroll counting (AccessibilityService) | Play policy risk, accuracy, onboarding friction | Final |
