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
