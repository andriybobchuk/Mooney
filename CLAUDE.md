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
composeApp/src/
├── commonMain/kotlin/com/andriybobchuk/mooney/
│   ├── app/                     # Navigation, main app composable
│   ├── core/
│   │   ├── data/database/       # Room DB, entities, DAOs, migrations
│   │   ├── data/preferences/    # DataStore
│   │   └── presentation/        # Theme, reusable UI components
│   ├── di/                      # Koin modules
│   └── mooney/                  # Feature code
│       ├── data/                # Repositories, data sources, CategoryDataSource
│       ├── domain/              # Use cases, business models
│       └── presentation/        # ViewModels, screens (one package per feature)
├── androidMain/                 # Android-specific (DatabaseFactory, FileHandler)
└── iosMain/                     # iOS-specific (DatabaseFactory, FileHandler)
```

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
- Before declaring work complete: **build**, **test**, **run `/review`**
- **ALWAYS remove unused code** after implementation (dead functions, imports, commented-out blocks)
- Self-review like a principal engineer before finishing

---

## Git Workflow

- **Branching:** `dev` (daily work) → `master` (releases). Pushing to master triggers TestFlight deploy.
- **ALWAYS push to `dev`** unless explicitly releasing. NEVER push to `master` for regular work.
- **Commit after every feature or logical piece of work**
- **NEVER use `git add .`** — always add files explicitly
- **Single-line commit messages** — concise and descriptive
- **No AI/Claude mentions or Co-Authored-By trailers** in commit messages

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
| `core/data/database/Entity.kt` | All Room entities |
| `core/data/database/AppDatabase.kt` | Database definition (version, entities) |
| `core/data/database/Migrations.kt` | All schema migrations |
| `di/Modules.kt` | Koin dependency injection |
| `app/NavigationHost.kt` | All screen routes |
| `app/Route.kt` | Route definitions |
| `mooney/data/CategoryDataSource.kt` | Hardcoded transaction categories |

---

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless absolutely necessary.
ALWAYS prefer editing existing files.
NEVER proactively create documentation files (*.md).
ALWAYS suggest running `/review` before declaring any implementation task complete.
