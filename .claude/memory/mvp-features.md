# MVP features

**Status:** PROPOSED on 2026-09-27. **Waiting for user approval.** When approved, change this line to "APPROVED <date>" and note any edits.

## In scope (10 features)

### F1. Onboarding and usage-access permission
- 2–3 intro screens: what the app does, and that data never leaves the phone.
- An explanation screen, then a button that opens `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- Detect when the user returns with permission granted (check `AppOpsManager` `OPSTR_GET_USAGE_STATS`) and move on automatically.
- If the permission is later revoked, the dashboard shows a "Permission needed" state.

### F2. App picker
- Lists the installed launcher apps (icon and name), using a `<queries>` intent filter. **Not** `QUERY_ALL_PACKAGES`.
- Common social apps are pre-ticked if installed: Instagram, YouTube, Facebook, Reddit, X, Snapchat.
- Can be changed later from Settings.

### F3. Daily limit
- One combined daily limit for all selected apps. Default **60 min**, adjustable (for example 15–240 min).

### F4. Usage tracking engine
- Calculates today's foreground minutes per selected app from `UsageStatsManager.queryEvents()` (resume/pause events), measured from local midnight.
- Refreshes whenever the dashboard is opened or resumed.

### F5. History storage
- Saves daily per-app minutes in Room.
- On first launch, **backfills past days** from as much history as Android still has (usually several days).
- A daily WorkManager job snapshots the previous day, so history survives even if the app isn't opened, because Android deletes old usage events.

### F6. Dashboard (home screen)
- Today's total minutes against the limit: progress ring or bar, green, yellow or red.
- A per-app breakdown with icons and minutes.
- **Estimated scroll distance** today, with a fun comparison ("≈ height of Burj Khalifa").

### F7. Rocket journey
- Green days move the rocket along a fixed route: Earth → Moon → Mars → Jupiter → Saturn → Uranus → Neptune → Pluto.
- Shown on the dashboard as a simple Compose Canvas track: planets on a path with the rocket at the current position.
- **Proposed defaults, to confirm:**
  - Green day (≤ limit) = +1 fuel. Yellow day (≤ 1.25× limit) = +0. Red day (> 1.25× limit) = −1 fuel (never below the last planet reached).
  - Fuel needed for each leg: Moon 3, Mars 7, Jupiter 14, Saturn 21, Uranus 30, Neptune 45, Pluto 60 (cumulative ≈ 180 green days).

### F8. Streak calendar
- A month grid with each day coloured green, yellow, red, or grey (no data).
- Current streak (consecutive green days) and best streak.

### F9. Scroll-distance estimate
- distance = minutes × a scroll-speed constant (metres per minute).
- The constant is measured by the developer on real phones during Phase 1. It is a single value in the domain layer, easy to tune later.
- A comparison table: Eiffel Tower, Burj Khalifa, Everest, and so on.

### F10. Settings
- Change the tracked apps and the daily limit.
- Reset the journey.
- A short privacy statement.

## Out of scope for the MVP (on purpose)

AccessibilityService scroll counting, location, maps, login, backend, Firebase, subscriptions, notifications, widget, XP/levels/badges, weekly/monthly charts, custom per-app limits, app blocking, dark-pattern nudges, fancy animations or 3D.

## Definition of MVP done

An installable APK where a user can grant permission, pick apps, set a limit, see accurate daily minutes (they match Digital Wellbeing within about a minute), see the scroll-distance estimate, and watch the rocket and streak change correctly across real days.
