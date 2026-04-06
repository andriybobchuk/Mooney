package com.andriybobchuk.mooney.testutil

import com.andriybobchuk.mooney.core.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionDao : TransactionDao {
    private val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(transaction: TransactionEntity) {
        val current = transactions.value.toMutableList()
        val existing = current.indexOfFirst { it.id == transaction.id }
        if (existing >= 0) {
            current[existing] = transaction
        } else {
            current.add(transaction.copy(id = if (transaction.id == 0) nextId++ else transaction.id))
        }
        transactions.value = current
    }

    override suspend fun delete(id: Int) {
        transactions.value = transactions.value.filter { it.id != id }
    }

    override fun getAll(): Flow<List<TransactionEntity>> = transactions

    override suspend fun getById(id: Int): TransactionEntity? =
        transactions.value.find { it.id == id }
}

class FakeAccountDao : AccountDao {
    private val accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(account: AccountEntity) {
        val current = accounts.value.toMutableList()
        val existing = current.indexOfFirst { it.id == account.id }
        if (existing >= 0) {
            current[existing] = account
        } else {
            current.add(account.copy(id = if (account.id == 0) nextId++ else account.id))
        }
        accounts.value = current
    }

    override suspend fun delete(id: Int) {
        accounts.value = accounts.value.filter { it.id != id }
    }

    override fun getAll(): Flow<List<AccountEntity>> = accounts

    override suspend fun getById(id: Int): AccountEntity? =
        accounts.value.find { it.id == id }

    override suspend fun clearAllPrimary() {
        accounts.value = accounts.value.map { it.copy(isPrimary = false) }
    }

    override suspend fun setPrimary(id: Int) {
        accounts.value = accounts.value.map {
            if (it.id == id) it.copy(isPrimary = true) else it
        }
    }

    override suspend fun getPrimary(): AccountEntity? =
        accounts.value.find { it.isPrimary }
}

class FakeCategoryUsageDao : CategoryUsageDao {
    private val usages = mutableMapOf<String, CategoryUsageEntity>()

    override suspend fun upsert(categoryUsage: CategoryUsageEntity) {
        usages[categoryUsage.categoryId] = categoryUsage
    }

    override suspend fun getMostUsedCategories(limit: Int): List<CategoryUsageEntity> =
        usages.values.sortedByDescending { it.usageCount }.take(limit)

    override suspend fun getCategoryUsage(categoryId: String): CategoryUsageEntity? =
        usages[categoryId]

    override suspend fun incrementUsage(categoryId: String, date: String) {
        usages[categoryId]?.let {
            usages[categoryId] = it.copy(usageCount = it.usageCount + 1, lastUsedDate = date)
        }
    }
}

class FakeGoalDao : GoalDao {
    private val goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(goal: GoalEntity) {
        val current = goals.value.toMutableList()
        val existing = current.indexOfFirst { it.id == goal.id }
        if (existing >= 0) {
            current[existing] = goal
        } else {
            current.add(goal.copy(id = if (goal.id == 0) nextId++ else goal.id))
        }
        goals.value = current
    }

    override suspend fun delete(id: Int) {
        goals.value = goals.value.filter { it.id != id }
    }

    override fun getAll(): Flow<List<GoalEntity>> = goals

    override suspend fun getById(id: Int): GoalEntity? =
        goals.value.find { it.id == id }
}

class FakeGoalGroupDao : GoalGroupDao {
    private val groups = MutableStateFlow<List<GoalGroupEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(goalGroup: GoalGroupEntity) {
        val current = groups.value.toMutableList()
        val existing = current.indexOfFirst { it.id == goalGroup.id }
        if (existing >= 0) {
            current[existing] = goalGroup
        } else {
            current.add(goalGroup.copy(id = if (goalGroup.id == 0) nextId++ else goalGroup.id))
        }
        groups.value = current
    }

    override suspend fun delete(id: Int) {
        groups.value = groups.value.filter { it.id != id }
    }

    override fun getAll(): Flow<List<GoalGroupEntity>> = groups

    override suspend fun getById(id: Int): GoalGroupEntity? =
        groups.value.find { it.id == id }
}

class FakeCategoryDao : CategoryDao {
    private val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override fun getAll(): Flow<List<CategoryEntity>> = categories

    override suspend fun getById(id: String): CategoryEntity? =
        categories.value.find { it.id == id }

    override suspend fun getByParentId(parentId: String): List<CategoryEntity> =
        categories.value.filter { it.parentId == parentId }

    override suspend fun upsert(category: CategoryEntity) {
        val current = categories.value.toMutableList()
        val existing = current.indexOfFirst { it.id == category.id }
        if (existing >= 0) {
            current[existing] = category
        } else {
            current.add(category)
        }
        categories.value = current
    }

    override suspend fun delete(id: String) {
        categories.value = categories.value.filter { it.id != id }
    }
}

class FakeUserCurrencyDao : UserCurrencyDao {
    private val currencies = MutableStateFlow<List<UserCurrencyEntity>>(emptyList())

    override fun getAll(): Flow<List<UserCurrencyEntity>> =
        currencies.map { it.sortedBy { c -> c.sortOrder } }

    override suspend fun upsert(userCurrency: UserCurrencyEntity) {
        val current = currencies.value.toMutableList()
        val existing = current.indexOfFirst { it.code == userCurrency.code }
        if (existing >= 0) {
            current[existing] = userCurrency
        } else {
            current.add(userCurrency)
        }
        currencies.value = current
    }

    override suspend fun delete(code: String) {
        currencies.value = currencies.value.filter { it.code != code }
    }
}

class FakeRecurringTransactionDao : RecurringTransactionDao {
    private val items = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(recurringTransaction: RecurringTransactionEntity) {
        val current = items.value.toMutableList()
        val existing = current.indexOfFirst { it.id == recurringTransaction.id }
        if (existing >= 0) {
            current[existing] = recurringTransaction
        } else {
            current.add(recurringTransaction.copy(id = if (recurringTransaction.id == 0) nextId++ else recurringTransaction.id))
        }
        items.value = current
    }

    override suspend fun delete(id: Int) { items.value = items.value.filter { it.id != id } }
    override fun getAllActive(): Flow<List<RecurringTransactionEntity>> = items.map { it.filter { r -> r.isActive } }
    override fun getAll(): Flow<List<RecurringTransactionEntity>> = items
    override suspend fun getById(id: Int): RecurringTransactionEntity? = items.value.find { it.id == id }
    override suspend fun setActive(id: Int, isActive: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
    }
    override suspend fun updateLastProcessedDate(id: Int, date: String) {
        items.value = items.value.map { if (it.id == id) it.copy(lastProcessedDate = date) else it }
    }
}

class FakePendingTransactionDao : PendingTransactionDao {
    private val items = MutableStateFlow<List<PendingTransactionEntity>>(emptyList())
    private var nextId = 1

    override suspend fun upsert(pendingTransaction: PendingTransactionEntity) {
        val current = items.value.toMutableList()
        val existing = current.indexOfFirst { it.id == pendingTransaction.id }
        if (existing >= 0) {
            current[existing] = pendingTransaction
        } else {
            current.add(pendingTransaction.copy(id = if (pendingTransaction.id == 0) nextId++ else pendingTransaction.id))
        }
        items.value = current
    }

    override suspend fun delete(id: Int) { items.value = items.value.filter { it.id != id } }
    override fun getAllPending(): Flow<List<PendingTransactionEntity>> = items.map { it.filter { p -> p.status == "PENDING" } }
    override fun getAll(): Flow<List<PendingTransactionEntity>> = items
    override suspend fun getById(id: Int): PendingTransactionEntity? = items.value.find { it.id == id }
    override suspend fun updateStatus(id: Int, status: String) {
        items.value = items.value.map { if (it.id == id) it.copy(status = status) else it }
    }
    override fun getPendingCount(): Flow<Int> = items.map { it.count { p -> p.status == "PENDING" } }
    override suspend fun cleanupOldProcessed(cutoffDate: String) {
        items.value = items.value.filter { it.status == "PENDING" || it.scheduledDate >= cutoffDate }
    }
    override suspend fun deletePendingByRecurringId(recurringId: Int) {
        items.value = items.value.filter { !(it.recurringTransactionId == recurringId && it.status == "PENDING") }
    }
}

class FakeAssetCategoryDao : AssetCategoryDao {
    private val categories = MutableStateFlow<List<AssetCategoryEntity>>(emptyList())

    override fun getAll(): Flow<List<AssetCategoryEntity>> =
        categories.map { it.sortedBy { c -> c.sortOrder } }

    override suspend fun getById(id: String): AssetCategoryEntity? =
        categories.value.find { it.id == id }

    override suspend fun upsert(category: AssetCategoryEntity) {
        val current = categories.value.toMutableList()
        val existing = current.indexOfFirst { it.id == category.id }
        if (existing >= 0) {
            current[existing] = category
        } else {
            current.add(category)
        }
        categories.value = current
    }

    override suspend fun delete(id: String) {
        categories.value = categories.value.filter { it.id != id }
    }

    override suspend fun getCount(): Int = categories.value.size
}
