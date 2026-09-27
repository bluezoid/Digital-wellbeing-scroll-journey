# Where will your scrolling take you?

An Android app that turns doom-scrolling into a journey. Every scroll in Instagram, YouTube, Facebook, etc. moves a rocket away from your home location toward the Moon, Mars, and beyond. Skip a day and the rocket drifts back.

> **Status:** Research, before the MVP. No code yet.

> **Update 2026-09-27:** The project pivoted from counting scrolls (AccessibilityService) to tracking screen time (UsageStatsManager). The scroll-detection plan below is kept for reference only. See [.claude/memory/decisions.md](.claude/memory/decisions.md) and [.claude/memory/mvp-features.md](.claude/memory/mvp-features.md).

---

## The one question that decides everything

> **Can an Android app reliably detect meaningful scrolling inside *other* apps?**

Android has no public API that says "the user scrolled Instagram 10 times." The only realistic mechanism is an **AccessibilityService** listening for `TYPE_VIEW_SCROLLED` events. If that doesn't work reliably, nothing else in this project matters. If it does, the rest is ordinary Android development.

**Rule:** prove scroll detection first. Build nothing else until it works.

---

## MVP broken into independent problems

```text
 Detect scrolling ──► Count per app ──► Store daily ──► Rocket engine ──► Dashboard
 (Accessibility)      (packageName)     (Room)          (domain logic)    (Compose)
                                                              ▲
                                        Home location ────────┘
                                        (one-time, foreground only)
```

| # | Problem | Android pieces to learn |
|---|---------|-------------------------|
| 1 | Which app is in use? | `AccessibilityEvent.getPackageName()` (primary); `UsageStatsManager`, `UsageEvents`, `queryEvents()` (secondary) |
| 2 | Did the user scroll? ⭐ | `AccessibilityService`, `AccessibilityEvent`, `TYPE_VIEW_SCROLLED`, `accessibility_service_config.xml` |
| 3 | How far did they scroll? | `AccessibilityEvent.getScrollDeltaY()` (API 28+), `WindowMetrics` / `DisplayMetrics` |
| 4 | Remember counts | Room (Entity, DAO, Database), Repository pattern |
| 5 | Daily progress / decay | WorkManager (`PeriodicWorkRequest`) |
| 6 | Home location | `ACCESS_COARSE_LOCATION`, `FusedLocationProviderClient` |
| 7 | UI | Jetpack Compose |
| 8 | Nudges (later) | Notification channels, `POST_NOTIFICATIONS`, `PendingIntent` |

---

## Research notes

### 1. App detection

- The AccessibilityService already gives you `event.packageName` on every event, so for the MVP you probably **don't need UsageStatsManager** to know which app was scrolled.
- UsageStatsManager is still worth learning for "time spent" stats. It needs the special `PACKAGE_USAGE_STATS` permission, which the user grants manually in *Settings → Usage access*. There is no runtime dialog for it.
- Package names to filter:
  - Instagram: `com.instagram.android`
  - YouTube: `com.google.android.youtube`
  - Facebook: `com.facebook.katana`

### 2. Scroll detection ⭐ (critical feasibility test)

Flow:

```text
User swipes in Instagram
   → Android emits AccessibilityEvent (TYPE_VIEW_SCROLLED)
   → our AccessibilityService.onAccessibilityEvent()
   → filter by packageName
   → scrollCount++
```

Things to verify during the spike:

- [ ] Do `TYPE_VIEW_SCROLLED` events fire in the Instagram feed, Instagram Reels, YouTube Shorts, the YouTube home feed, and Facebook?
- [ ] How many events does **one** swipe produce? (Often several, because a fling keeps scrolling.)
- [ ] Is `getScrollDeltaY()` populated, or is it always 0 or -1 in these apps?
- [ ] Reels/Shorts are paged (ViewPager-style). Do they emit scroll events, or only `TYPE_WINDOW_CONTENT_CHANGED`?
- [ ] Effect of `notificationTimeout` in the service config (the OS batches events).
- [ ] Does the service survive app kills, reboots, and battery optimisation on your test device (Xiaomi, Samsung, etc.)?

**Scroll event ≠ one post.** One swipe can produce many events. We need **debouncing**: collapse events from the same package that arrive within ~300–500 ms into one "meaningful scroll."

### 3. Distance

- The phone's diagonal size (e.g. 6.5") is **not** the vertical distance scrolled.
- MVP simplification: **1 meaningful scroll = 1 screen-height unit.**
- Better later: add up `getScrollDeltaY()` pixels, then convert to physical distance with `pixels / ydpi = inches`.

### 4. Storage (Room)

Draft entity. Keep daily aggregates, not raw events:

```text
DailyScroll
───────────
date          (yyyy-MM-dd)
packageName
scrollCount
distanceUnits
```

### 5. Daily processing (WorkManager)

```text
Once a day
  → total yesterday's scrolls
  → above threshold? rocket moves forward : rocket drifts back
```

Use WorkManager, not a forever-running service. It's also fine to compute this lazily whenever the app opens.

### 6. Home location

- Ask once on first launch → save `homeLatitude` / `homeLongitude` → never ask again unless the user changes home.
- Coarse location is enough. **No background location needed.**

### 7. Rocket engine (our own domain logic)

A gamified progression, not real astronomy:

```text
HOME ──1k──► MOON ──10k──► MARS ──50k──► JUPITER ──► SATURN ──► URANUS ──► NEPTUNE ──► PLUTO
```

Pure Kotlin, with no Android dependencies, so it's easy to unit-test.

---

## ⚠️ Google Play policy risk

The AccessibilityService API is meant for helping users with disabilities. Google Play requires apps that use it for anything else to:

- give a **prominent in-app disclosure** and get the user's consent before enabling the service,
- fill in the **Accessibility API declaration** in Play Console, with a video of how it's used,
- **not** set `isAccessibilityTool="true"` (that flag is only for real accessibility tools).

Digital-wellbeing apps that limit or track social media usage do exist on Play using this API, but approval isn't guaranteed. **Read the current Play policy before investing past the spike.** Sideloading or F-Droid are fallbacks for early testers.

Privacy stance for the product: process events on-device, store only counts, never read or store screen content.

---

## Learning / build order

| Phase | Goal | Done when |
|-------|------|-----------|
| 0 | Android fundamentals: Activity, lifecycle, Intent, permissions, services | Can explain each one |
| 1 | **Scroll spike** ⭐ | Scrolling in Instagram prints `SCROLL DETECTED com.instagram.android` in Logcat |
| 2 | Debounce + per-app counting | On-screen counters: Instagram 127 / YouTube 83 / Facebook 41 |
| 3 | Persistence (Room) | Counts survive closing the app and rebooting |
| 4 | Daily stats (WorkManager) | Today, yesterday, 7-day total, streak |
| 5 | Home location | Coordinates saved once |
| 6 | Rocket engine | Scrolls → distance → current planet, unit-tested |
| 7 | Dashboard (Compose) | Totals, streak calendar, rocket progress, current planet |
| 8 | Notifications, maps, polish | Later |

### Not building yet

Backend, login, Firebase, subscriptions, Google Maps, rocket graphics and animations, AI.

---

## Assignment #1

1. Create a fresh Android project (Kotlin, Empty Activity).
2. Read about: `Activity`, Activity lifecycle, `AccessibilityService`, `AccessibilityEvent`, `TYPE_VIEW_SCROLLED`.
3. Build a bare AccessibilityService that logs every `TYPE_VIEW_SCROLLED` event with its `packageName` and `scrollDeltaY`.
4. Enable it in *Settings → Accessibility*, scroll through Instagram, YouTube, and Facebook, and capture the Logcat output.
5. Write down the answers to the checklist in [§2](#2-scroll-detection--critical-feasibility-test).

**Then stop** and review the results before moving on.

---

## References

- [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AccessibilityEvent](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent)
- [Create your own accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service)
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Google Play: Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491)
