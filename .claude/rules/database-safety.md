---
description: Room database safety rules - CRITICAL for data preservation
globs: "**/database/**/*.kt"
---

# Database Safety Rules — CRITICAL

The database (`mooney.db`) is at **version 8** with **7 entities**. User has real financial data on device.

## Rules

- **NEVER decrease the database version number** — Room checks identity hashes, mismatch = data inaccessible
- **NEVER remove entities from schema** — even if feature is disabled in UI
- To drop tables, create a FORWARD migration (v8 → v9)
- Both `DatabaseFactory` files (Android + iOS) must register ALL migrations
- Schema JSON files in `composeApp/schemas/` must match compiled entities
- RecurringTransactionEntity and PendingTransactionEntity have no UI — they exist solely for schema integrity

## When Modifying Database Files

1. Increment version in `AppDatabase.kt`
2. Add migration in `Migrations.kt`
3. Register migration in BOTH `DatabaseFactory.android.kt` AND `DatabaseFactory.ios.kt`
4. Build to let KSP regenerate schema JSON
5. Test on device with existing data
