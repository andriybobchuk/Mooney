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

    @Query("UPDATE AccountEntity SET isPrimary = 0 WHERE isPrimary = 1")
    suspend fun clearAllPrimary()

    @Query("UPDATE AccountEntity SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: Int)

    @Query("SELECT * FROM AccountEntity WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(): AccountEntity?
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

@Dao
interface GoalDao {
    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM goals ORDER BY createdDate DESC")
    fun getAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Int): GoalEntity?
}

@Dao
interface GoalGroupDao {
    @Upsert
    suspend fun upsert(goalGroup: GoalGroupEntity)

    @Query("DELETE FROM goal_groups WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM goal_groups ORDER BY createdDate DESC")
    fun getAll(): Flow<List<GoalGroupEntity>>

    @Query("SELECT * FROM goal_groups WHERE id = :id")
    suspend fun getById(id: Int): GoalGroupEntity?
}

@Dao
interface RecurringTransactionDao {
    @Upsert
    suspend fun upsert(recurringTransaction: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY dayOfMonth")
    fun getAllActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions ORDER BY dayOfMonth")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Int): RecurringTransactionEntity?

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Int, isActive: Boolean)

    @Query("UPDATE recurring_transactions SET lastProcessedDate = :date WHERE id = :id")
    suspend fun updateLastProcessedDate(id: Int, date: String)
}

@Dao
interface PendingTransactionDao {
    @Upsert
    suspend fun upsert(pendingTransaction: PendingTransactionEntity)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY scheduledDate")
    fun getAllPending(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions ORDER BY scheduledDate DESC")
    fun getAll(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun getById(id: Int): PendingTransactionEntity?

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("DELETE FROM pending_transactions WHERE status != 'PENDING' AND scheduledDate < :cutoffDate")
    suspend fun cleanupOldProcessed(cutoffDate: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE parentId = :parentId")
    suspend fun getByParentId(parentId: String): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface UserCurrencyDao {
    @Query("SELECT * FROM user_currencies ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<UserCurrencyEntity>>

    @Upsert
    suspend fun upsert(userCurrency: UserCurrencyEntity)

    @Query("DELETE FROM user_currencies WHERE code = :code")
    suspend fun delete(code: String)
}
