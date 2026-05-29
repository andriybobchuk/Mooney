# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Mooney** is a Kotlin Multiplatform (KMP) personal finance app for iOS and Android. Tracks transactions, manages accounts, and analyzes spending. Compose Multiplatform for UI on both platforms.

## Build Commands

```bash
./gradlew build                                    # All platforms
./gradlew :composeApp:assembleDebug                # Android
./gradlew clean                                    # Clean build
./gradlew :composeApp:kspCommonMainKotlinMetadata  # Generate Room schemas
./gradlew detekt                                   # Lint (static analysis)
./gradlew :composeApp:testDebugUnitTest            # Unit tests (fast, Android)
./gradlew koverHtmlReport                          # Test coverage report
```

## Architecture

### Module Structure

```
Mooney/
├── composeApp/src/              # Main app module (Mooney-specific code)
│   ├── commonMain/kotlin/com/andriybobchuk/mooney/
│   │   ├── app/                     # Navigation, main app composable
│   │   ├── core/
│   │   │   ├── data/database/       # Room DB, entities, DAOs, migrations
│   │   │   ├── data/preferences/    # DataStore
│   │   │   └── presentation/        # Mooney-specific UI components & theme overrides
│   │   ├── di/                      # Koin modules
│   │   └── mooney/                  # Feature code
│   │       ├── data/                # Repositories, data sources, CategoryDataSource
│   │       ├── domain/              # Use cases, business models
│   │       └── presentation/        # ViewModels, screens (one package per feature)
│   ├── androidMain/                 # Android-specific (DatabaseFactory, FileHandler)
│   └── iosMain/                     # iOS-specific (DatabaseFactory, FileHandler)
├── core/                        # ⚡ GIT SUBMODULE → github.com/andriybobchuk/designsystem.kmp
│   └── src/commonMain/          # Shared design system (theme, components, tokens)
└── .claude/                     # Claude Code configuration
```

### Shared Design System (`core/` submodule)

**IMPORTANT:** The `core/` directory is a **git submodule** pointing to [andriybobchuk/designsystem.kmp](https://github.com/andriybobchuk/designsystem.kmp). It is shared with the Time project. Changes here propagate to all consuming apps.

- **Package:** `com.andriybobchuk.core`
- **Tokens:** `AppDesignSystem` object (Colors, Typography, Spacing, Elevation, Shapes, Animation)
- **Theme:** `AppTheme()`, light/dark color schemes, Poppins typography
- **Components:** `AppButton`, `AppCard`, `AppSummaryCard`, `AppBottomSheet`, `MeshGradientBackground`
- **Helpers:** `UiText`, `rememberPulseAlpha()`

**When modifying shared design system files:**
1. Edit inside `core/`, commit and push from there (`cd core && git add ... && git commit && git push`)
2. Then update submodule reference in Mooney (`cd .. && git add core && git commit`)

**Prefer shared components** (`AppButton`, `AppCard`) over Mooney-specific wrappers (`MooneyButton`, `MooneyCard`) for new code. Use Mooney-specific wrappers only when finance-specific behavior is needed (e.g., `MooneyTransactionCard`, `MooneyAmountField`).

**Setup after clone:** `git submodule update --init --recursive`

### Technology Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0.21 (KMP) |
| UI | Compose Multiplatform 1.7.0, Material3 |
| DI | Koin 4.0.0 |
| Database | Room 2.7.0-alpha11, bundled SQLite driver |
| Network | Ktor 3.0.0 |
| Navigation | Jetpack Navigation Compose |
| Serialization | kotlinx-serialization |

### Pattern: MVVM

- **ViewModel**: Immutable `UiState` (data class), `Action` (sealed interface), `Event` (sealed class)
- **Screen**: Pure Composable receiving `state` + `onAction` callback
- **Use Cases**: `VerbNounUseCase` pattern
- **DI**: `Modules.kt` (`sharedModule` + `platformModule`)

---

## Room Database Schema — CRITICAL

Database `mooney.db` is at **version 8** with **7 entities**. User has real financial data — data safety is the absolute highest priority. See `.claude/rules/database-safety.md` for full rules.

**Entities (ALL must remain):** TransactionEntity, AccountEntity, CategoryUsageEntity, GoalEntity, GoalGroupEntity, RecurringTransactionEntity, PendingTransactionEntity

**Key rules:** NEVER decrease version. NEVER remove entities. Always forward-migrate.

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| ViewModel | `FeatureViewModel` | `AssetsViewModel` |
| UI State | `FeatureState` | `AssetsState` |
| Actions | `FeatureAction` (sealed interface) | `AssetsAction` |
| Events | `FeatureEvent` (sealed class) | `AssetsEvent` |
| Screen | `FeatureScreen` | `AssetsScreen` |
| Use Case | `VerbNounUseCase` | `CalculateNetWorthUseCase` |

---

## Workflow Discipline

- If implementation goes sideways (3+ failed attempts), STOP and re-plan
- Use `/plan` for: new features, navigation changes, refactoring, DB schema changes
- Skip `/plan` for: single-file fixes, string changes, trivial bug fixes
- **ALWAYS remove unused code** after implementation (dead functions, imports, commented-out blocks)
- Self-review like a principal engineer before finishing

### Verification — run the EXACT CI gate before declaring done

CI runs `./gradlew detekt :composeApp:assembleDebug :composeApp:testDebugUnitTest`. Running only `compileDebugKotlinAndroid` locally is NOT enough — it has repeatedly passed locally and then failed CI on **detekt** (e.g. `TooGenericExceptionThrown`, `TooManyFunctions`) or the **iOS** target. Before saying any code change is complete, run `/verify` (or the equivalent below) and paste the actual pass/fail output — never assume green:

```bash
./gradlew detekt :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:compileKotlinIosArm64
```

### Action over explanation

When asked to **run / build / commit / push / deploy / ship**, execute it directly — do not return instructions for the user to run themselves unless they explicitly ask "how". The one exception is interactive commands that genuinely need a TTY (e.g. `gh auth login`), which the user must run via the `!` prompt prefix.

---

## Git Workflow

- **Branching:** `dev` (daily work) → `master` (releases). Pushing to master triggers TestFlight deploy.
- **ALWAYS push to `dev`** unless explicitly releasing. NEVER push to `master` for regular work.
- **Commit after every feature or logical piece of work**
- **NEVER use `git add .`** — always add files explicitly
- **Single-line commit messages** — concise and descriptive
- **No AI/Claude mentions or Co-Authored-By trailers** in commit messages

### Releasing — use `/ship`

Releasing bumps the version in **two** places that must stay in sync, then merges `dev` → `master`:
1. `gradle.properties` → `app.version` (CalVer `YY.MM.PATCH`) **and** `app.versionCode` (increment)
2. `iosApp/iosApp.xcodeproj/project.pbxproj` → all `MARKETING_VERSION` entries (via `sed`)

The `generateVersionFile` Gradle task auto-writes `AppVersion.kt` from `app.version` on build, so Settings shows the right version automatically — no manual edit. Then commit, push `dev`, merge to `master`, push, and watch CI with `gh run list --branch master --limit 1`. The `/ship` skill encodes this whole sequence.

### App Store / TestFlight gotchas (learned the hard way)

- **`Secrets.kt` is gitignored** — CI regenerates it from the workflow template (`.github/workflows/ci.yml`). Any NEW constant added to `Secrets.kt` (e.g. `FIREBASE_PROJECT_ID`) MUST also be added to the three generation blocks in `ci.yml`, or CI fails to compile.
- **Subscriptions must be in a reviewable state for the app to pass review.** A subscription in "Rejected" / "Developer Action Needed" causes a Guideline 2.1(a) "unable to load IAPs" app rejection, because StoreKit can't serve it to the review device. Fix the subscription metadata first; "Waiting for Review" status IS testable in the review sandbox.
- **Subscription review submission** requires the subscription to be added as an explicit item in the App Review submission packet — adding it only to the version page's "In-App Purchases and Subscriptions" section is necessary but not sufficient.
- **Subscription descriptions** must list concrete benefits — vague phrasing like "and other features" gets the localization rejected.
- **dSYM upload to Crashlytics** is NOT done in fastlane (the SwiftPM layout breaks `upload_symbols_to_crashlytics`); dSYMs flow via App Store Connect's Apple-ID connection in the Firebase console.

---

## MCP Servers

| Server | Purpose |
|--------|---------|
| **Figma Remote MCP** | Generate UI from Figma designs. Use `get_design_context` with fileKey + nodeId extracted from Figma URLs. Output is a reference — adapt to Compose Multiplatform + Material3. |

---

## Project Planning

- **Big picture:** Read `ROADMAP.md` when working on features, planning, or the user references the roadmap
- **Refactoring details:** Read `TECH_DEBT.md` when doing architecture cleanup or refactoring
- When you encounter tech debt, add it to `TECH_DEBT.md`. When completing roadmap items, check them off in `ROADMAP.md`.
- Current milestone: **Milestone 1 — Clean Codebase** (remove unused, extract use cases, fix criticals)

---

## New Feature Checklist

1. Domain models in `mooney/domain/`
2. Use cases in `mooney/domain/usecase/`
3. `UiState`, `Actions`, `Events` in `mooney/presentation/feature/`
4. `ViewModel` with `onAction()` method
5. Compose `Screen` with state + onAction
6. Navigation in `NavigationHost.kt`
7. Register in `Modules.kt` (DAO, use cases, ViewModel)
8. Unit tests for ViewModel
9. Verify error/loading states

---

## Agents (`.claude/agents/`)

Use these agents by spawning them when the task matches. They have Mooney-specific context baked in.

| Agent | When to Use |
|-------|-------------|
| **test-writer** | After writing use cases or ViewModels — spawn to write unit tests |
| **ui-builder** | When building UI from Figma designs or feature specs |
| **aso** | App Store / Google Play listing optimization, keyword research, screenshot strategy |
| **product-manager** | Feature prioritization, roadmap decisions, premium conversion strategy |
| **feedback-synthesizer** | Analyzing app reviews, user feedback, or competitor reviews |
| **growth-hacker** | User acquisition strategy, viral mechanics, launch planning |
| **content-creator** | Marketing copy, app store descriptions, social media posts, release notes |
| **linkedin-creator** | LinkedIn posts for Andriy's personal brand (building in public, KMP expertise) |
| **legal-compliance** | Privacy policy review, GDPR/CCPA compliance, App Store policy compliance |
| **reality-checker** | Pre-release quality gate — build verification, code review, regression check |

---

## Key Files

| File | Purpose |
|------|---------|
| `core/` | Shared design system (git submodule → designsystem.kmp) |
| `core/src/.../DesignSystem.kt` | Central design tokens (`AppDesignSystem`) |
| `core/src/.../theme/Theme.kt` | `AppTheme()` composable |
| `composeApp/.../core/data/database/Entity.kt` | All Room entities |
| `composeApp/.../core/data/database/AppDatabase.kt` | Database definition (version, entities) |
| `composeApp/.../core/data/database/Migrations.kt` | All schema migrations |
| `composeApp/.../di/Modules.kt` | Koin dependency injection |
| `composeApp/.../app/NavigationHost.kt` | All screen routes |
| `composeApp/.../app/Route.kt` | Route definitions |
| `composeApp/.../mooney/data/CategoryDataSource.kt` | Hardcoded transaction categories |

---

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless absolutely necessary.
ALWAYS prefer editing existing files.
NEVER proactively create documentation files (*.md).
ALWAYS suggest running `/review` before declaring any implementation task complete.
When modifying shared design system: edit in `core/`, commit+push there, then update submodule ref.
