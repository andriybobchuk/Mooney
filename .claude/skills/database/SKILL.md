---
name: database
description: Room database operations - migrations, entity updates, schema management for Mooney
auto-invocable: When modifying Room entities, creating migrations, or database schema changes
model: claude-sonnet-4-6
allowed-tools: Read,Write,Edit,MultiEdit,Glob,Grep,Bash(./gradlew:*)
---

# Database Operations for Mooney

Expert guidance for Room database operations in the Mooney finance app.

## Current Schema

**Database Version:** Check `AppDatabase.kt` for current version

### Entities:
- `TransactionEntity` - Financial transactions
- `AccountEntity` - User accounts
- `CategoryUsageEntity` - Category tracking
- `GoalEntity` - Financial goals
- `GoalGroupEntity` - Goal groupings
- `RecurringTransactionEntity` - Recurring transactions

## Migration Guidelines

When modifying database schema:

### 1. Update Entity

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: Int,
    val amount: Double,
    val description: String?,
    val categoryId: Int,
    val accountId: Int,
    val date: Long,
    // New field with default value for migration
    val isRecurring: Boolean = false
)
```

### 2. Create Migration

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Add new column with default value
        connection.execSQL(
            "ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

### 3. Update AppDatabase

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        // ... other entities
    ],
    version = 8  // Increment version
)
abstract class AppDatabase : RoomDatabase() {
    // DAOs...
}
```

### 4. Register Migration

In `DatabaseFactory`:

```kotlin
.addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3,
    // ... other migrations
    MIGRATION_7_8  // Add new migration
)
```

## Common Operations

### Adding a New Column

```kotlin
connection.execSQL(
    "ALTER TABLE table_name ADD COLUMN column_name TYPE NOT NULL DEFAULT default_value"
)
```

### Creating Index

```kotlin
connection.execSQL(
    "CREATE INDEX IF NOT EXISTS index_name ON table_name(column_name)"
)
```

### Renaming Column (Complex)

```kotlin
// Room doesn't support RENAME COLUMN, need to recreate table
connection.execSQL("CREATE TABLE new_table (...)")
connection.execSQL("INSERT INTO new_table SELECT ... FROM old_table")
connection.execSQL("DROP TABLE old_table")
connection.execSQL("ALTER TABLE new_table RENAME TO old_table")
```

## Type Converters

For complex types in entities:

```kotlin
class Converters {
    @TypeConverter
    fun fromCurrency(currency: Currency): String = currency.name
    
    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)
    
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toString()
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
```

## Testing Migrations

```kotlin
@Test
fun migrate7To8() {
    val db = helper.createDatabase(TEST_DB, 7).apply {
        // Insert test data for version 7
        execSQL("INSERT INTO transactions ...")
        close()
    }
    
    // Run migration and validate
    db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
    
    // Verify migration worked
    val cursor = db.query("SELECT * FROM transactions")
    // Assert new column exists and has default value
}
```

## Best Practices

1. **ALWAYS provide default values** for new NOT NULL columns
2. **Test migrations** with production-like data
3. **Never modify existing columns** - add new ones instead
4. **Keep migrations idempotent** - safe to run multiple times
5. **Document breaking changes** in migration comments
6. **Use transactions** for multi-step migrations
7. **Validate foreign keys** after migration

## Financial Data Integrity

For financial data, ensure:
- Monetary amounts use `REAL` type (maps to Double)
- Consider using `TEXT` for BigDecimal if precision critical
- Transaction dates stored as `INTEGER` (timestamp)
- Account balances computed, not stored (avoid sync issues)
- Use foreign keys for referential integrity

## Schema Export

Schemas are auto-exported to:
`composeApp/schemas/com.andriybobchuk.mooney.core.data.database.AppDatabase/`

This helps track schema evolution and aids in migration creation.