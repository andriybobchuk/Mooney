package com.andriybobchuk.mooney.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryUsageEntity::class,
        GoalEntity::class,
        GoalGroupEntity::class,
        RecurringTransactionEntity::class,
        PendingTransactionEntity::class
    ],
    version = 8,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class  AppDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val accountDao: AccountDao
    abstract val categoryUsageDao: CategoryUsageDao
    abstract val goalDao: GoalDao
    abstract val goalGroupDao: GoalGroupDao
    abstract val recurringTransactionDao: RecurringTransactionDao
    abstract val pendingTransactionDao: PendingTransactionDao

    companion object {
        const val DB_NAME = "mooney.db"
    }
}
