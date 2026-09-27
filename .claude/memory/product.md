# Product

## One-liner

**"Where will your scrolling take you?"** An Android app that turns social-media screen time into a space journey, so people become aware of their scrolling and cut it down.

## Owner

Achyut Pal, a solo developer building it as a potential SaaS/subscription product. They want to learn Android properly, not just copy code.

## Core concept (current, 2026-09-27)

- The user picks which social apps to track (Instagram, YouTube, Facebook, Reddit, and so on).
- The app reads **how many minutes** they spent in those apps each day, using Android `UsageStatsManager`.
- **Awareness hook:** minutes are converted into an **estimated scroll distance** ("You scrolled about 1.2 km of feed today, the height of Burj Khalifa"). This keeps the original scrolling metaphor with reliable data underneath.
- **Game loop, in the wellbeing direction:** staying **under the daily limit** is a "green day." Green days fuel a rocket that travels Earth → Moon → Mars → … → Pluto. Streaks are shown on a calendar.
- Everything stays on the device. No account and no server. Privacy is a selling point.

## History of the idea

1. **2026-09-26, original idea:** count individual *scrolls* in Instagram/YouTube/Facebook with an AccessibilityService (`TYPE_VIEW_SCROLLED`). Scrolls × screen height = distance. A rocket moves from the user's **home location** (captured by GPS) toward planets, and drifts back on days without scrolling.
2. **Assessed:** technically possible but risky:
   - Google Play restricts the Accessibility API for non-accessibility apps (needs a declaration and video, approval not guaranteed).
   - Scroll counts are approximate; Reels and Shorts page one item at a time and may send different events.
   - Onboarding friction: the accessibility warning scares users.
   - OEM battery killers (Xiaomi, Samsung, and others) stop the service.
3. **2026-09-27, pivot:** count **screen time** per selected app instead (UsageStatsManager). Exact data, low Play risk, no always-on service.
4. **Also 2026-09-27:**
   - Dropped home location (privacy friction, not needed).
   - Chose the **wellbeing direction**: reward using the apps less, not more. The original "rocket goes further the more you scroll" rewarded scrolling, which conflicted with the goal.
   - The user was shown 12 alternative concepts (digital garden, island, mountain, pet, RPG, and more). **Chosen:** space journey plus streak calendar. The others are parked as future ideas.

## Positioning and competition

The crowded space includes Android Digital Wellbeing (built in), Forest, ScreenZen, Opal, and one sec.
Our differentiators:
1. The "scroll distance" metaphor, which is shareable and funny.
2. The space-journey progression.
3. On-device only.

## Future ideas (not in the MVP)

- An optional "precise mode" that uses an AccessibilityService for real scroll counts (only if Play policy allows).
- XP, levels, badges, and 30-day challenges.
- A home-screen widget (Glance) showing the rocket.
- A daily summary notification.
- Shareable cards ("I scrolled Everest this week").
- A subscription (Play Billing / RevenueCat).
- "Choose a starting city" instead of GPS home, if a map view is ever wanted.
