# Mooney Seeded E2E (Maestro)

Second Maestro suite. Separate from the shimmer/empty-state contract flows
in this folder — see [README.md](./README.md) for those.

Full architecture, flow catalogue, and rollout plan: `.claude/plans/zany-mixing-wilkinson.md`.

## What's different

| | Shimmer contract flows | Seeded E2E flows |
|---|---|---|
| Variant | `debug` build type — installed by hand or from an in-progress dev session | `e2e` build type — dedicated APK you install for the run and uninstall after |
| Pre-state | Whatever's on the device — expects an onboarded user | Deterministic, wiped + seeded by fixture at every launch |
| Purpose | Regression guard for cache-first UI contract | Whole-app financial correctness — balance math, migrations, paywall, etc. |
| Entry | `maestro test .maestro/cold-start.yaml` | `maestro test .maestro/smoke.yaml` |

## Local run

```bash
# 1. Uninstall any existing Mooney build first — e2e uses the same
#    applicationId as debug (so google-services.json resolves) and can't
#    coexist on-device.
adb uninstall com.andriybobchuk.mooney || true

# 2. Assemble and install the E2E build type.
./gradlew :composeApp:assembleE2e
adb install -r composeApp/build/outputs/apk/e2e/composeApp-e2e.apk

# 3. Install the Maestro CLI if you haven't already.
curl -Ls "https://get.maestro.mobile.dev" | bash

# 4. Run the seeded smoke suite.
maestro test .maestro/smoke.yaml
```

Expected: one green flow (`01_seeded_launch_smoke`) in ~30 seconds after a
cold app start.

## How seeding works

The `e2e` build type overlays `composeApp/src/e2e/kotlin/**` on top of
`androidMain`. The same-package `E2eBootstrap` object under `src/e2e/` wins
for the E2E variant, replacing the no-op stub compiled into debug/release.

Sequence:

1. `MyApp.onCreate` inits Koin, then calls `E2eBootstrap.onApplicationCreate`
   (loads the Koin overrides module). Because `defersWarmStartup = true` on
   the real bootstrap, `warmStartupSingletons()` is skipped here.
2. Android launches `MainActivity`. Before `installSplashScreen()`, we call
   `E2eBootstrap.onActivityCreate(intent)`.
3. That reads Intent extras `fixture` and `wipeDb`, deletes the E2E Room DB
   file if requested, and inserts fixture rows through the real DAOs. Then
   it fires the deferred `warmStartupSingletons()`.
4. `super.onCreate` runs; the system splash covers the seeding time (~200 ms).

The E2E build uses `mooney_e2e.db` (see `AppDatabase.DB_NAME_E2E`), so it
never touches your dev or prod data.

## Fixtures

Kotlin DSL under `composeApp/src/e2e/kotlin/com/andriybobchuk/mooney/e2e/fixtures/`.
Each fixture is a `val` compiled against real entity classes — adding a
required column to any entity fails the E2E build at compile time.

Currently registered (`E2eBootstrap.FIXTURES`):

| Key                    | Shape                                                              |
|------------------------|--------------------------------------------------------------------|
| `empty`                | Fresh install, onboarding NOT skipped                              |
| `single_account_usd`   | One USD Checking account at $5,000, onboarding skipped             |
| `two_accounts_usd`     | Checking $3,000 + Savings $10,000 (same-currency transfer flows)   |
| `multi_currency_user`  | USD/EUR/PLN accounts; net worth = $8,500 with stub rates           |
| `mid_size_user`        | 3 accounts + 10 realistic transactions in the last two weeks (wall-clock-relative via `daysAgo(n)`) |
| `recurring_ready`      | 1 account + 1 active monthly recurring on day 15                   |
| `near_paywall_limit`   | 5 accounts (free-tier max — 6th tap triggers paywall)              |

Adding a fixture: create `src/androidE2e/kotlin/com/andriybobchuk/mooney/e2e/fixtures/YourFixture.kt` (Kotlin DSL via `fixture { … }`), register `"your_fixture" to YourFixture` in `E2eBootstrap.FIXTURES`, then reference `env: { FIXTURE: "your_fixture" }` in a flow.

## Flow catalogue

| # | File                                             | Guards |
|---|--------------------------------------------------|--------|
| 01  | `01_seeded_launch_smoke.yaml`                  | Bootstrap infra end-to-end; fixture reaches UI |
| 01a | `01a_onboarding_complete.yaml`                 | Onboarding flag persistence + navigation |
| 02  | `02_add_transaction_updates_balance.yaml`      | `AddTransactionUseCase` balance math |
| 03  | `03_edit_transaction_reverses_balance.yaml`    | `TransferHandler.reverseThenApply` |
| 04  | `04_delete_transaction_reverses_balance.yaml`  | `DeleteTransactionUseCase` orphan reversal |
| 05  | `05_transfer_same_currency.yaml`               | Atomic dual-side account update |
| 09  | `09_multi_currency_net_worth.yaml`             | `StubExchangeRateProvider` math + net-worth aggregation |
| 10  | `10_change_base_currency_recomputes.yaml`      | Derived-state invalidation on base-currency change |
| 11  | `11_add_account_persists.yaml`                 | Account write reliability across process death |
| 12  | `12_analytics_metric_navigation.yaml`          | Analytics metric card handlers |
| 13  | `13_account_limit_paywall.yaml`                | Free-tier account gate + paywall dismiss stability |
| 14  | `14_goal_create_and_delete.yaml`               | `SaveGoalUseCase` reaches DAO; goal renders |
| 15  | `15_recurring_render_smoke.yaml`               | Recurring transactions render + sheet open |
| 16  | `16_theme_change_persists.yaml`                | DataStore write reliability across process death |
| 17  | `17_theme_cycle.yaml`                          | Theme selector transition mechanic |
| 21  | `21_process_death_mid_add_txn.yaml`            | No phantom-commit on in-flight sheet kill |
| 22  | `22_double_tap_save_no_duplicate.yaml`         | Money-mutating concurrency / debounce |
| 23  | `23_multiple_transactions_aggregate.yaml`      | Balance aggregation associativity |
| 24  | `24_bottom_nav_walk.yaml`                      | All bottom-nav tabs render without crashing |

`smoke.yaml` currently runs {01, 24} for every PR — the flows that reliably pass on the GitHub-hosted KVM emulator. Flow 02 (add-transaction balance math) and other text-input-heavy flows are kept in `full.yaml` only while the Maestro-`inputText`-vs-Compose-`BasicTextField`-in-`ModalBottomSheet` race gets root-caused with an interactive emulator. `full.yaml` runs the full 20-flow set nightly on `dev` and `master`.

## Test doubles (Koin overrides)

Injected via `koinE2eOverridesModule` (loads with Koin 4.x's default `allowOverride = true`).

| Production binding                   | E2E override                               | Effect |
|--------------------------------------|--------------------------------------------|--------|
| `ExchangeRateProvider`               | `StubExchangeRateProvider`                 | Canned USD/EUR/PLN/GBP/CHF/UAH rates; never touches network |
| `BillingManager`                     | `FakeBillingManager`                       | `--premium=true|false` launch arg drives initial state; `purchase()` flips to true |
| `E2eFlags` (new)                     | —                                          | Carries `--premium` between bootstrap and the Koin factory closure |

## testTags

Single source of truth: `composeApp/src/commonMain/kotlin/com/andriybobchuk/mooney/core/testing/TestTags.kt` (23 declared — bottom nav, transaction FAB + form, account FAB + form, paywall, onboarding, settings).

Applied via `Modifier.mooneyTestTag(TestTags.FOO)`. Android surfaces them as resource IDs via the `WithTestTagsAsResourceId` wrapper at `App.kt`'s root. iOS surfacing is Phase 6 (currently `isE2eBuild` returns `false` on iOS).

The dynamic tag `TestTags.txnRow(id)` produces `"txn_row_{id}"`; flows reference these directly.

## Local tooling

```bash
./.claude/scripts/assert-testtags-monotonic.sh   # baseline check; --update to rebase
./.claude/scripts/check-testtag-coverage.sh --warn-only   # unstubbed presentation files
./.claude/scripts/verify-flow-tag-refs.sh        # every `id:` in a flow maps to a declared tag
./.claude/scripts/preflight-testtags.sh          # requires emulator + installed APK
```

## CI

`maestro-android-smoke` job in `.github/workflows/ci.yml` runs after the `check` job.
Currently `continue-on-error: true` during rollout — remove that once flake rate is <5% over two weeks, then wire the `/ship` gate to require this job green.

`tag-lint` and `maestro-tag-preflight` are drafted in `.claude/scripts/` but not yet wired into CI.

## Known limitations

- **iOS**: `isE2eBuild` reads `NSProcessInfo.processInfo.arguments` for `--e2e` but the iOS bootstrap (fixture seeding, Koin overrides) is not yet ported. Full iOS parity is Phase 6.
- **Clock**: no FixedClock injection yet. Fixtures use `daysAgo(n)` helpers to compute wall-clock-relative dates, which stay stable across CI runs except when today's day-of-month is <5. Follow-up: inject `Clock` via Koin so flows can specify `--now=<ISO-8601>`.
- **Reminder / Review**: `ReminderScheduler` and `ReviewPromptManager` are `expect class` in commonMain — no clean Koin subclass override without an interface refactor. In practice both silently no-op on the e2e emulator (no Play Services / no user gesture that triggers them).
- **Migration data-loss guard**: the flagship "run a v8 snapshot through migrations and assert balances survive" flow is deferred to Phase 5. Current guard is a schema-integrity JVM test that catches "forgot to bump the version" but not silent data loss.
- **Text-input flows failing on CI emulator**: `Maestro.inputText` against Compose's `BasicTextField` inside a `ModalBottomSheet` produces silent failures — the field appears to receive the tap but `inputText` doesn't reach the `onValueChange` callback, leaving the ViewModel's amount at `null` and Save disabled. All text-input-driven flows (02, 03, 05, 22, 23, and add-account flows) are currently blocked by this. Debugging strategy: run locally with `maestro studio` for interactive hierarchy inspection, and consider using semantic actions (`Modifier.semantics { setText { … } }`) as a workaround.
