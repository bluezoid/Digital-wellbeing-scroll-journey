# Research notes (Android facts and pitfalls)

## UsageStatsManager (the core API)

- Needs the `android.permission.PACKAGE_USAGE_STATS` permission (declare it in the manifest with `tools:ignore="ProtectedPermissions"`).
- The user grants it **manually** in Settings → Usage access. There is no runtime dialog. Open it with `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- To check whether it's granted, call `AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, uid, packageName) == MODE_ALLOWED` (`checkOpNoThrow` below API 29).
- For accurate foreground time, pair `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` events (`MOVE_TO_FOREGROUND` / `MOVE_TO_BACKGROUND` on older APIs) from `queryEvents(start, end)`.
  - Handle an app that is still in the foreground at the query end time.
  - Handle a session that started before midnight.
- `queryUsageStats()` buckets can be inaccurate around day boundaries. Avoid them for daily totals.
- Android keeps detailed events only for a limited time (days). Hence the daily snapshot job into Room.
- Validate the numbers against Digital Wellbeing (Settings → Digital Wellbeing). Small differences are normal.

## Package visibility (Android 11+)

- To list launcher apps for the picker, add a `<queries><intent><action MAIN/><category LAUNCHER/></intent></queries>` block to the manifest.
- Avoid `QUERY_ALL_PACKAGES`: Play restricts it.
- Package names:
  - Instagram: `com.instagram.android`
  - YouTube: `com.google.android.youtube`
  - Facebook: `com.facebook.katana`
  - Reddit: `com.reddit.frontpage`
  - X: `com.twitter.android`
  - Snapchat: `com.snapchat.android`

## AccessibilityService (parked; original approach)

- Would receive `AccessibilityEvent.TYPE_VIEW_SCROLLED` with `packageName`. `getScrollDeltaY()` is available on API 28+.
- One swipe produces several events (flings), so it needs debouncing (about 300–500 ms).
- Reels and Shorts page one item at a time and may emit `TYPE_WINDOW_CONTENT_CHANGED` instead.
- **Play policy:** non-accessibility use requires a prominent disclosure and consent, plus the Accessibility API declaration in Play Console with a video. Don't set `isAccessibilityTool="true"`. Approval isn't guaranteed.
- This is why it was parked. It could return later as an optional "precise mode."

## WorkManager

- `PeriodicWorkRequest` with a minimum interval of 15 min. For a daily job use a 24 h period, with an initial delay calculated to just after midnight.
- Timing isn't exact (Doze). The snapshot logic must be idempotent and able to catch up on missed days.

## Misc

- A phone's diagonal size is not the vertical scroll distance. Physical conversion: `pixels / ydpi = inches`.
- OEM battery optimisation doesn't matter much now, since there's no long-running service.

## SceneView / Filament (3D) gotchas (2026-09-28)

- `SphereNode`: pass a **single `materialInstance`**. `materialInstances = listOf(x)` only covers the first primitive (Sphere has one primitive per stack), so the rest render grey.
- Use `materialLoader.createTextureInstance(texture, ...)` (lit, `opaque_textured.filamat`, parameter "texture", uses UV0). `createImageInstance` (unlit) showed stripe artefacts on spheres.
- `ImageTexture.Builder().bitmap(assets, path).build(engine)` gives SRGB8_A8 with mipmaps generated (12 levels for 2048×1024).
- Default Scene environment: black skybox + neutral IBL + a directional light pointing (0, -1, 0).
- SceneView pulls in fuel (networking), so strip `android.permission.INTERNET` with `tools:node="remove"`.
- The realme (Dimensity, Mali-G77 MC9, GLES 3.2) runs about 58 FPS with a 2K-textured sphere.
- **Unlit Filament materials ignore camera exposure**: output = baseColor. Keep intensity around 1 (the Sun 1.4, stars 0.8). Lit materials do use exposure (SceneView's sunny-16).
- Custom materials: write a `.mat`, compile with matc (same version as the runtime, 1.56.0) `-p mobile -a opengl`, load with `materialLoader.createMaterial("materials/x.filamat")`, then `createInstance(material)` and `setTexture("name", tex)` / `setParameter(...)`.
- Sun lighting: a point light at the origin (3e8 lm, falloff 200) + IBL intensity 6000 gives day/night sides that still read on the outer planets.
- **SceneView `lookAt(eye, target)` is buggy** for non-origin targets (double subtraction). Use `dev.romainguy.kotlin.math.lookTowards(eye, target - eye, Float3(0f, 1f, 0f))` for a camera model matrix.
- SceneView hit-testing (`onSingleTapConfirmed` node) misses small/far spheres, so add screen-space picking via `cameraNode.worldToView(pos)` (returns 0..1 with y up; flip y and multiply by the view size).
- SceneView only calls `CameraManipulator.update/getTransform` after the first touch, so drive custom camera animation from `onFrame` and set `cameraNode.transform` yourself.
