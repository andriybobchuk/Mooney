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

- **Commit after every feature or logical piece of work**
- **NEVER use `git add .`** — always add files explicitly
- **Single-line commit messages** — concise and descriptive
- **No AI/Claude mentions** in commit messages

---

## MCP Servers

| Server | Purpose |
|--------|---------|
| **Figma Remote MCP** | Generate UI from Figma designs. Use `get_design_context` with fileKey + nodeId extracted from Figma URLs. Output is a reference — adapt to Compose Multiplatform + Material3. |

---

## Tech Debt Tracking

Log tech debt in `TECH_DEBT.md` in project root. Check before starting cleanup tasks.

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
