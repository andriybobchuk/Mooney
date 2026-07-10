# Mooney E2E — session handoff

State of the E2E infrastructure at commit `d5ad5db` on `dev`. CI green.
This document exists so the next person to touch the suite can start on
day zero without re-reading 30 commits.

## What's shipped and working

**Android E2E build variant**
- `composeApp/build.gradle.kts` — dedicated `e2e` build type; own source set at `composeApp/src/androidE2e/kotlin/`; `BuildConfig.IS_E2E` gates runtime behavior.
- Assemble: `./gradlew :composeApp:assembleE2e` → `composeApp/build/outputs/apk/e2e/composeApp-e2e.apk`.
- Same `applicationId` as debug so `google-services.json` resolves; **install alongside impossible — `adb uninstall` first**.

**Bootstrap seam**
- `androidDebug` + `androidRelease` source sets each have a no-op `E2eBootstrap` — the E2E machinery is compiled out of production APKs.
- `androidE2e/kotlin/com/andriybobchuk/mooney/e2e/E2eBootstrap.kt` — real bootstrap. On `MainActivity.onCreate`, before `installSplashScreen`, reads Intent extras (`fixture`, `wipeDb`, `premium`), wipes `mooney_e2e.db` (and its WAL/SHM siblings) if requested, seeds the fixture through the real DAOs, then fires the deferred `warmStartupSingletons`.
- `MyApp` skips its own warm-startup when the e2e bootstrap declares `defersWarmStartup = true`.
- iOS side: `E2eBootstrap.onApplicationLaunch()` seam in `MainViewController` reads `NSProcessInfo.processInfo.arguments`. Body is a stub — Phase 6b.

**Fixtures (Kotlin DSL, `androidE2e/…/e2e/fixtures/`)**
- `Empty`, `SingleAccountUsd`, `TwoAccountsUsd`, `MultiCurrencyUser`, `MidSizeUser`, `RecurringReady`, `NearPaywallLimit`, `PostSpendUsd`.
- Wall-clock-relative dates via `FixtureBuilder.daysAgo(n)` — MidSizeUser stays valid across CI runs regardless of the current month.
- Adding a fixture = 1 `val` in `fixtures/`, 1 map entry in `E2eBootstrap.FIXTURES`, referenced from a flow's `env: { FIXTURE: "your_name" }`.

**Test doubles (`androidE2e/…/e2e/doubles/` — swapped in via `koinE2eOverridesModule` with Koin 4.x's default `allowOverride = true`)**
- `StubExchangeRateProvider` — canned USD/EUR/PLN/GBP/CHF/UAH rates; never touches network.
- `FakeBillingManager` — `--premium=true|false` launch arg drives initial state; `purchase()` flips to `true`.
- `RecordingAnalyticsTracker` — silences Firebase Analytics + Crashlytics I/O; captures events for flows that want to assert on side effects (not yet wired to a flow).
- Ads globally killed via `PreferencesKeys.ADS_DISABLED_DEV = true` written in `E2eBootstrap.seed` — no `AdBannerSlot` retry-eating for any flow.

**testTags**
- Single source of truth: `commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt` — 37 entries across bottom nav, transaction FAB + form + row + delete confirm, account FAB + form, paywall, onboarding, settings, analytics metric cards, goals, recurring.
- `Modifier.mooneyTestTag(tag)` — merges descendants; sets `testTagsAsResourceId = true` on the same node so `ModalBottomSheet` + `Dialog` + `Popup` content also surfaces to Maestro's `id:` selector.
- `Modifier.mooneyTestTagLeaf(tag)` — non-merging variant for `BasicTextField` where merging would hide `SetText` from Maestro's `inputText`.
- `WithTestTagsAsResourceId { … }` wraps the App root so Android's resource-id lookup works from the theme.

**Flows (20 total, `.maestro/flows/`)**
- **Smoke (per PR to `dev`, ~11 min emulator boot + 5 flows)**:
  - `01_seeded_launch_smoke.yaml` — fixture data reaches the UI.
  - `24_bottom_nav_walk.yaml` — every top-level screen renders.
- **Full (nightly on `dev` and `master`, ~20 min)**: all 20 flows — see `README-seeded.md` for the catalogue.

**JVM safety tests (`commonTest/…/DatabaseSchemaIntegrityTest.kt`)**
- `@Database(version)` matches `EXPECTED_VERSION`.
- Entity count matches `EXPECTED_ENTITY_COUNT`.
- `AppDatabase.DB_NAME` / `DB_NAME_DEV` / `DB_NAME_E2E` are distinct non-blank.
- `ALL_MIGRATIONS.size == EXPECTED_VERSION - 1` — forgot-to-append-migration guard.
- `ALL_MIGRATIONS` forms a contiguous chain from 1 to `EXPECTED_VERSION`.

**CI (`.github/workflows/ci.yml`)**
- `Lint, Build & Test` — detekt + assembleDebug + testDebugUnitTest. Required.
- `Maestro E2E (smoke)` — assembles e2e APK, boots KVM emulator via `reactivecircus/android-emulator-runner`, installs, runs `.maestro/smoke.yaml`. `continue-on-error: true` during rollout — remove once flake rate <5% over two weeks and wire `/ship` gate to require green.

## What's parked with a breadcrumb

### 🚧 Blocker #1 — Maestro `inputText` vs Compose `BasicTextField` in `ModalBottomSheet`

**Symptom**: every add-transaction flow (02, 03, 05, 22, 23) fails on the "100.00 not visible" assertion. `inputText: "100"` never lands in the field's `onValueChange`, so the ViewModel's `amount` stays `null`, `Save` stays disabled, and no transaction is written.

**Attempts made** (all failed):
1. Rely on auto-focus (`focusRequester.requestFocus()` inside sheet's `LaunchedEffect { delay(350) }`).
2. Tag the outer padding `Box` and tap it first.
3. Tag the inner `BasicTextField` with merging semantics.
4. Tag the `BasicTextField` with the non-merging `mooneyTestTagLeaf` variant.
5. Tap the placeholder text ("0.00") to focus the field.

**Suspected root cause**: Compose's `ModalBottomSheet` renders in a separate Window, and either (a) IME connection is lost between focus and `inputText`, or (b) Maestro's `input text` ADB command doesn't reach BasicTextField in a bottom sheet. Confirmation needs `maestro studio` live-inspection.

**Next-iteration recipe**:
1. `./gradlew :composeApp:assembleE2e && adb install -r composeApp/build/outputs/apk/e2e/composeApp-e2e.apk`.
2. `maestro studio` → launch app with `arguments: { fixture: "single_account_usd", wipeDb: true }`.
3. Manually open the add-txn sheet, tap the amount field, observe whether the semantics tree shows a focused editable node.
4. Try `Modifier.semantics { setText { … } }` as a workaround if `inputText` really is bypassed.

### 🚧 Blocker #2 — numeric-balance asserts on Assets tab

**Symptom**: `assertVisible: "4,900"` and similar substring-numeric asserts fail even for pure-fixture setups where the balance is definitely rendered as `formatWithCommas() + currency.symbol` → `"4,900.00 $"`.

**Suspected root cause**: the `formatWithCommas` output uses ASCII comma but Compose's text-rendering may split the number into styled spans that Maestro's substring selector can't cross. Or the emulator locale substitutes a non-breaking space.

**Investigation move**: dump the visible hierarchy on the Assets tab (`maestro hierarchy`) and inspect what string the balance actually renders as. If it's split across spans, use a regex-based text selector: `assertVisible: {text: "4[,\s]?900"}`.

### 🚧 iOS Phase 6b — E2eBootstrap body

`E2eBootstrap.onApplicationLaunch()` in `iosMain/…/e2e/E2eBootstrap.kt` reads args and logs but doesn't seed. Mechanical port from Android:

1. Move `Fixture` + `FixtureBuilder` + fixture `val`s from `androidE2e` to `commonMain/…/e2e/` (or a new `iosE2eMain` source set gated by a Gradle property if compile-time exclusion is required).
2. Move `FakeBillingManager` / `StubExchangeRateProvider` / `RecordingAnalyticsTracker` to `commonMain`.
3. Implement the iOS bootstrap: `NSFileManager` DB wipe → seed via DAOs → `loadKoinModules(koinE2eOverridesModule)`.
4. Add `--e2e` and `--fixture=…` launch args to the iOS Maestro flows.

### 🚧 Migration snapshot flagship (flows 20a/b)

Currently deferred. Needs a `./gradlew captureMigrationSnapshot` task that:

1. `git checkout` the previous release tag.
2. Assembles a headless-seeder e2e build.
3. Runs the seeder against `mooney_e2e.db`, `adb pull`s the resulting file.
4. Commits it to `androidE2e/assets/db_snapshots/previous_release.db`.
5. Wire the snapshot into a flow that launches with that DB pre-planted, asserts every seeded account balance survives migration to current.

## File map (where things live)

```
composeApp/src/
  commonMain/kotlin/com/andriybobchuk/mooney/
    core/testing/
      TestTags.kt              # single source of truth for tags
      TestTagModifier.kt       # mooneyTestTag, mooneyTestTagLeaf, WithTestTagsAsResourceId
      BuildKind.kt             # expect val isE2eBuild
    core/data/database/
      Migrations.kt            # ALL_MIGRATIONS list (also DatabaseFactory reads from it)
      AppDatabase.kt           # DB_NAME / DB_NAME_DEV / DB_NAME_E2E
  commonTest/kotlin/…/data/database/
    DatabaseSchemaIntegrityTest.kt  # 5 fast JVM invariants
  androidMain/kotlin/…/core/testing/
    BuildKind.android.kt       # actual val — reads BuildConfig.IS_E2E
    TestTagResourceId.android.kt
  androidDebug/…/e2e/E2eBootstrap.kt   # no-op stub
  androidRelease/…/e2e/E2eBootstrap.kt # no-op stub
  androidE2e/kotlin/com/andriybobchuk/mooney/e2e/
    E2eBootstrap.kt            # real seeding
    E2eOverridesModule.kt      # Koin overrides + E2eFlags
    FixtureBuilder.kt          # DSL
    fixtures/                  # 8 fixture files
    doubles/                   # StubExchangeRateProvider, FakeBillingManager, RecordingAnalyticsTracker
  iosMain/kotlin/…/core/testing/
    BuildKind.ios.kt           # NSProcessInfo args
    TestTagResourceId.ios.kt
  iosMain/kotlin/…/e2e/
    E2eBootstrap.kt            # Phase 6b — seam wired, body empty
.maestro/
  config.yaml                  # existing shimmer-suite config
  smoke.yaml                   # 2-flow smoke — 01 + 24
  full.yaml                    # 20-flow nightly
  shared/launch_seeded.yaml    # reusable fixture-loading launch
  flows/                       # 20 flow files
  README.md                    # existing shimmer docs, includes pointer to README-seeded
  README-seeded.md             # this suite's docs
  HANDOFF.md                   # this file
.claude/scripts/
  assert-testtags-monotonic.sh
  check-testtag-coverage.sh
  preflight-testtags.sh
  verify-flow-tag-refs.sh
  testtags.baseline            # current baseline = 32
.github/workflows/ci.yml       # maestro-android-smoke job wired
```

## Local run

```bash
adb uninstall com.andriybobchuk.mooney || true
./gradlew :composeApp:assembleE2e
adb install -r composeApp/build/outputs/apk/e2e/composeApp-e2e.apk
maestro test .maestro/smoke.yaml
```

## Next actions in priority order

1. Root-cause the `inputText` blocker with `maestro studio` (30 min unblock; promotes 5 flows into smoke).
2. Root-cause the numeric-balance-assert blocker (same session — inspect the hierarchy).
3. Fill in the iOS bootstrap body (mechanical port).
4. Remove `continue-on-error: true` from the CI job after two weeks of stable smoke runs.
5. Add the migration snapshot flow.
