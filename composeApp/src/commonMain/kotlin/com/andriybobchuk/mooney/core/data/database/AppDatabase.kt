package com.andriybobchuk.mooney.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class, AccountEntity::class, CategoryUsageEntity::class], version = 2, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val accountDao: AccountDao
    abstract val categoryUsageDao: CategoryUsageDao

    companion object {
        const val DB_NAME = "mooney.db"
    }
}
