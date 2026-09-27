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

- **Phase:** 0, planning. No app code exists yet.
- **Last done (2026-09-27):** Picked the product direction (screen time instead of scroll counting), picked the tech stack, and proposed the MVP feature list. Created this memory system.
- **Waiting on:** the user's approval of the MVP feature list (`memory/mvp-features.md`) and of the rocket and streak defaults.
- **Next step once approved:** Phase 1. Create the Android Studio project (Empty Activity, Compose), then build the usage-access permission screen and today's per-app minutes list. Check the numbers against Android's Digital Wellbeing.

## Memory index: read on demand

| File | Read when |
|---|---|
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
 ├─ .claude/            project memory (this folder)
 ├─ README.md           original research. The scroll-counting part is superseded; see memory/decisions.md
 └─ idea.txt            empty, the user's scratch file
```
