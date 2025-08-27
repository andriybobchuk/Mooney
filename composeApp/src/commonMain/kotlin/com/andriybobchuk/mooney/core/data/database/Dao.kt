package com.andriybobchuk.mooney.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("DELETE FROM TransactionEntity WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM TransactionEntity")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM TransactionEntity WHERE id = :id")
    suspend fun getById(id: Int): TransactionEntity?
}

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM AccountEntity WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM AccountEntity")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM AccountEntity WHERE id = :id")
    suspend fun getById(id: Int): AccountEntity?
}

@Dao
interface CategoryUsageDao {
    @Upsert
    suspend fun upsert(categoryUsage: CategoryUsageEntity)

    @Query("SELECT * FROM category_usage ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedCategories(limit: Int): List<CategoryUsageEntity>

    @Query("SELECT * FROM category_usage WHERE categoryId = :categoryId")
    suspend fun getCategoryUsage(categoryId: String): CategoryUsageEntity?

    @Query("UPDATE category_usage SET usageCount = usageCount + 1, lastUsedDate = :date WHERE categoryId = :categoryId")
    suspend fun incrementUsage(categoryId: String, date: String)
}

