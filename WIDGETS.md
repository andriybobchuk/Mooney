# Widgets — What's Shipped, What You Do Next

This is the handoff for the widget feature. All code is on `dev`, CI is green,
Mooley is in the app. The remaining ~30 min of work is Xcode UI clicks that
I literally can't do from a CLI — instructions below.

**Read time: 5 min. Setup time: ~30 min. Device test: ~15 min per platform.**

---

## 1. What's live on `dev`

### Android (fully wired, no manual work)

- **5 widget variants** using Jetpack Glance:
  - `Balance` (net worth) — small 2×2 + medium 4×2
  - `TodaySpending` — medium 4×2
  - `BudgetProgress` — medium 4×2 + large 4×4 (top-3 categories)
  - `Streak` (with Mooley) — small 2×2 + medium 4×2
  - `QuickAdd` — thin 2×1
- **Cross-platform snapshot layer** — `WidgetDataSnapshot` (Kotlin) writes
  JSON to `context.filesDir/widget-snapshot.json` on every transaction change.
  Both Android's Glance readers and iOS's WidgetKit extension consume from
  this snapshot, so business logic (streak calc, mood derivation, budget
  ranking) is shared — the platforms are dumb consumers.
- **Deep-link taps** — every widget tap opens the app with the right route
  extra, fires `widget_tapped` analytics, and lands the user on the matching
  screen. Handled in `MainActivity.maybeHandleWidgetIntent`.
- **Snapshot refresh** — `WidgetSnapshotCoordinator` starts on app boot
  (`MyApp.onCreate` calls `.start()`), observes `AppDataCache`, debounces
  350 ms, then writes + broadcasts a Glance update to every installed
  widget. Force-refresh available via `.requestRefresh()` on the coordinator.

### iOS (Swift files written — you add the Xcode target)

Every Swift file lives at `iosApp/MooneyWidget/`. See section 3 below for
the Xcode setup steps. All widget code mirrors the Android designs so the
two platforms look identical.

### Mooley (the mascot)

- Composable at `composeApp/src/commonMain/.../Mooley.kt` — Canvas-based
  vector paths, no image assets.
- SwiftUI port at `iosApp/MooneyWidget/MooleyView.swift` — identical
  geometry via the shared `MooleyGeometry` ratios.
- **4 moods**: `HAPPY`, `NEUTRAL`, `WORRIED`, `OVER_BUDGET`. Mood is derived
  server-side in `BuildWidgetSnapshotUseCase` from budget + streak state,
  so the widget just reads `snapshot.mooleyMood` and draws.
- **2 accent tints**: Blue (default, primary brand) and Green (positive-vibes).
- **Placeholder art disclaimer**: Mooley is a competent Compose/Canvas
  drawing but he's not a designed character. A real illustration / 3D render
  is a Figma or Blender exercise for a designer. What's shipping is
  good-enough for MVP and lets us measure whether the character concept
  drives retention before investing in production art.

### Analytics (already firing on both platforms)

- `widget_added(kind, size)` — first placement of a widget kind
- `widget_removed(kind, size)` — last instance removed
- `widget_tapped(kind, action)` — every tap (Android via MainActivity,
  iOS via `.onOpenURL` handler)
- `widget_data_refreshed(generation_bucket, mood)` — every successful
  snapshot write; useful for confirming the coordinator is healthy
- `widget_onboarding_shown(platform)` — post-activation bottom sheet render
- `widget_onboarding_action(action)` — dismiss / see-all / later
- **No new user property yet** — deferred `widget_count_bucket` because
  neither Glance nor WidgetKit exposes a first-party API to enumerate
  installed widgets from the app process. Ship it later if needed via
  Google's `GlanceAppWidgetManager.getGlanceIds` + a similar iOS survey.

### Onboarding UX

- **When it fires**: exactly once, after the user hits the activation
  threshold (≥3 transactions across ≥2 distinct days — same threshold as
  the existing `activated` event). Guarded by
  `PreferencesKeys.WIDGET_ONBOARDING_SHOWN` so it never re-appears.
- **What it shows**: Mooley illustration + headline + 3-step
  platform-specific "how to add" instructions + a "See all widgets" CTA.
- **Where the code lives**: `WidgetOnboardingHost` + `WidgetOnboardingSheet`
  in commonMain, hosted from `App.kt` at the top-level Box so it floats
  above every screen.

### Settings entry

- New `Settings → Home screen → Widgets` row. Opens `WidgetsPickerScreen`
  which shows every widget variant with size hints + platform-specific
  "how to add" steps.
- Route registered as `Route.Widgets` in `NavigationHost`.

---

## 2. What you need to do BEFORE testing

### Android

**Nothing.** Just `./gradlew installDebug` and long-press the home screen →
Widgets → find Mooney. Every widget shows up.

### iOS (~10 min in Xcode)

The Swift files are all written but they need a WidgetKit extension target
to live in. Here's the click-by-click:

1. **Open the project**: `open iosApp/iosApp.xcodeproj`
2. **File → New → Target…**
   - Under **iOS → App Extension**, pick **Widget Extension**.
   - Product Name: `MooneyWidget`
   - Team: your existing dev team (2GGQ4M8T47)
   - Bundle Identifier: `com.andriybobchuk.mooney.MooneyWidget` (Xcode
     auto-generates this if you leave the main app selected)
   - Language: Swift · Include Live Activity: **No** · Include
     Configuration App Intent: **No**
   - Finish. Xcode creates a `MooneyWidget/` folder with a boilerplate
     Widget file — **delete everything in that new folder EXCEPT the
     `.entitlements` and `Info.plist` if Xcode generated one**.
3. **Add the widget source files** (right-click `MooneyWidget` in the
   Project navigator → Add Files to "iosApp"…):
   - `MooneyWidgetBundle.swift`
   - `WidgetSnapshot.swift`
   - `WidgetProviders.swift`
   - `WidgetFormatting.swift`
   - `MooleyView.swift`
   - `BalanceWidget.swift`
   - `TodaySpendingWidget.swift`
   - `BudgetProgressWidget.swift`
   - `StreakWidget.swift`
   - `QuickAddWidget.swift`
   - When Xcode prompts, add them **only** to the `MooneyWidget` target
     (NOT the main `iosApp` target — that would double-compile them).
4. **Add the WidgetKitBridge file to the MAIN app target**:
   - `iosApp/WidgetKitBridge.swift` — this one goes to the `iosApp` target
     (checkbox in the file inspector).
5. **App Group entitlement (BOTH targets)**:
   - Select `iosApp` target → Signing & Capabilities → **+ Capability** →
     **App Groups** → **+ (add group)** →
     `group.com.andriybobchuk.mooney.widgets`
   - Repeat for the `MooneyWidget` target. Same group ID.
   - If prompted about provisioning profile updates, let Xcode fix it.
6. **Framework link**: `MooneyWidget` target → Build Phases → Link Binary
   with Libraries → **+** → add `WidgetKit.framework` and `SwiftUI.framework`
   if they're not already there. (Widget Extension template usually adds
   these by default.)
7. **Info.plist**: the pre-written `Info.plist` in the widget folder can
   replace the auto-generated one. Ensure `NSExtension` →
   `NSExtensionPointIdentifier` is `com.apple.widgetkit-extension`.
8. **Build**: `⌘B`. Fix any missing-import errors — if a file complains
   about `WidgetSnapshot` etc., it's not in the target. Check the file
   inspector's Target Membership.

**When it builds**: run the iosApp on a simulator (or your device),
long-press the home screen, tap **+**, search "Mooney", and add a widget.

---

## 3. Constants that MUST stay in sync

If any of these three values drift across the three places they appear, the
widget silently shows the empty state instead of real data:

| Constant                        | Location 1 (Kotlin)                                         | Location 2 (Swift)                    | Location 3 (Xcode entitlement) |
|---------------------------------|-------------------------------------------------------------|---------------------------------------|--------------------------------|
| App Group ID                    | `WidgetSnapshotStorage.IOS_APP_GROUP`                       | `WidgetGroup.suiteName`               | Both targets' entitlements     |
| Snapshot key                    | `WidgetSnapshotStorage.IOS_SNAPSHOT_KEY`                    | `WidgetGroup.snapshotKey`             | n/a                            |
| JSON field names                | `WidgetDataSnapshot` (Kotlin) — `@Serializable` field names | `WidgetSnapshot` (Swift) — properties | n/a                            |

The JSON layer uses Kotlin's default naming (identical to property names),
so as long as you name Swift `Codable` properties the same as the Kotlin
ones, they line up. `enum WidgetMooleyMood` serializes as its `NAME` string
(`"HAPPY"`, `"NEUTRAL"`, `"WORRIED"`, `"OVER_BUDGET"`) — Swift-side
`MooleyMood.init(rawKotlin:)` matches on those exact strings.

---

## 4. Device test script

Run this on both platforms once you've done the Xcode setup:

### Cold-start flow

1. Fresh install. Complete onboarding.
2. Add 3 accounts, 5 transactions across 2 different days.
3. Trigger `activated` event (VM does this automatically).
4. Kill the app, relaunch. **Expected**: post-activation bottom sheet
   appears offering to add a widget. Dismiss it with "Later".
5. Settings → Home screen → Widgets. **Expected**: 5 variants listed with
   descriptions.

### Home-screen flow

1. Long-press home screen → Widgets → Mooney (or +/search on iOS).
2. Add each of the 5 variants. **Expected**: each renders your real net
   worth, spending, budgets, streak.
3. Tap each widget. **Expected**: opens the app on the right screen, and
   `widget_tapped` fires in Firebase Analytics (visible in the DebugView).

### Data-freshness flow

1. Add a new transaction inside the app.
2. Return to home screen. **Expected**: within a few seconds, the Balance
   / Today Spending / Streak widgets reflect the new number. `Balance`
   should show `oldNetWorth ± transaction.amount`.
3. Kill the app entirely. Verify widgets STILL show the last known snapshot
   (they read from disk, not from a running app process).

### Empty-state flow

1. Wipe app data. Reinstall.
2. Add the widget without opening the app first (long-press home before
   first launch). **Expected**: widget shows the "Empty" placeholder with
   `$0` and a friendly nudge, not a crash or blank.

### Deep-link flow

1. From `QuickAdd` widget → tap. **Expected**: Add Transaction sheet
   opens directly. (This requires NavigationHost to read the `route` extra;
   Android already does, iOS uses `.onOpenURL` — verify both.)

### Firebase confirmation

Open Firebase → Analytics → DebugView with your test device attached
(`adb shell setprop debug.firebase.analytics.app com.andriybobchuk.mooney`
for Android). You should see the widget events land within seconds of
each interaction.

---

## 5. Known trade-offs I made tonight

1. **Placeholder Mooley art**. Vector drawing that looks acceptable but
   isn't the designed character you'd want on a marketing screenshot.
   Ship a real illustration in a follow-up release once a designer has
   time — the character architecture (mood enum, palette, accent) is
   already in place, so swapping the render is a 1-file change.
2. **iOS widget code assumes framework name `ComposeApp`**. If your KMP
   framework builds under a different name, adjust the `import ComposeApp`
   line in `WidgetKitBridge.swift`.
3. **No configurable widgets** (yet). The MVP ships static widgets — every
   Balance widget shows net worth in the base currency, every Budget widget
   shows the top-3. WidgetKit's `IntentConfiguration` (iOS) + Glance's
   `configurationOptions` (Android) are the follow-up path if users ask for
   "let me pick which category to pin."
4. **`widget_count_bucket` user property not shipped**. Enumerating
   installed widgets requires calling `GlanceAppWidgetManager.getGlanceIds`
   for each provider on Android and there's no clean iOS equivalent.
   Deferred to a follow-up so we don't ship a broken cross-platform
   signal.
5. **No unit tests for the widget layer**. `StreakCalculator` and
   `BuildWidgetSnapshotUseCase` are pure functions and easily testable —
   worth adding in a follow-up.

---

## 6. Where the code lives

```
composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/widgets/
├── WidgetDataSnapshot.kt          # The JSON DTO
├── WidgetSnapshotWriter.kt        # Interface + shared storage constants
├── WidgetSnapshotCoordinator.kt   # Observes cache → writes → broadcasts
├── BuildWidgetSnapshotUseCase.kt  # Pure computation
├── StreakCalculator.kt            # Consecutive-days-with-tx calc
├── Mooley.kt                      # The character (Compose Canvas)
├── WidgetOnboardingSheet.kt       # Bottom sheet UI
├── WidgetOnboardingHost.kt        # Post-activation trigger + gate
├── WidgetsPickerScreen.kt         # Settings → Widgets browser
└── WidgetTapTelemetry.kt          # Shared onOpenURL analytics helper

composeApp/src/androidMain/kotlin/com/andriybobchuk/mooney/
├── widgets/
│   ├── WidgetSupport.kt           # Colors, tokens, intent builder
│   ├── BalanceWidget.kt
│   ├── TodaySpendingWidget.kt
│   ├── BudgetProgressWidget.kt
│   ├── StreakWidget.kt            # Includes bitmap-based Mooley render
│   ├── QuickAddWidget.kt
│   └── WidgetReceivers.kt         # AppWidgetProvider registrations
└── core/widgets/
    └── WidgetSnapshotWriter.android.kt   # Writes JSON, pings Glance

composeApp/src/androidMain/res/xml/
├── widget_info_balance.xml
├── widget_info_today.xml
├── widget_info_budget.xml
├── widget_info_streak.xml
└── widget_info_quickadd.xml

composeApp/src/androidMain/res/drawable/
├── widget_preview_balance.xml
├── widget_preview_today.xml
├── widget_preview_budget.xml
├── widget_preview_streak.xml
└── widget_preview_quickadd.xml

composeApp/src/iosMain/kotlin/com/andriybobchuk/mooney/core/widgets/
└── WidgetSnapshotWriter.ios.kt    # Writes to App Group + WidgetKit bridge

iosApp/MooneyWidget/                       # NEW EXTENSION TARGET — you add
├── MooneyWidgetBundle.swift               #   this in Xcode
├── WidgetSnapshot.swift
├── WidgetProviders.swift
├── WidgetFormatting.swift
├── MooleyView.swift
├── BalanceWidget.swift
├── TodaySpendingWidget.swift
├── BudgetProgressWidget.swift
├── StreakWidget.swift
├── QuickAddWidget.swift
├── Info.plist
└── MooneyWidget.entitlements

iosApp/iosApp/
└── WidgetKitBridge.swift          # Main-app bridge (already added to iosApp target)
```

---

## 7. When you're happy — release path

Because iOS needs Xcode target creation before it builds cleanly on CI, the
first release with widgets must be an **Android-only ship**. Then after
the iOS Xcode setup lands:

1. `/ship 26.08.01 [android-only]` — Android widgets go live via Play.
   Users get Mooley on their home screen tomorrow.
2. Do the Xcode target setup (section 2 above).
3. Verify local iOS build.
4. `/ship 26.08.02` — full release, iOS widgets appear on TestFlight,
   promote to App Store when reviewed.

Both stores need updated screenshots featuring the widget + Mooley — that
alone is worth a marketing bump.

Sleep well. 🌙
