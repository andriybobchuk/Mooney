package com.andriybobchuk.mooney.testutil

import com.andriybobchuk.mooney.mooney.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCoreRepository : CoreRepository {
    private val accounts = MutableStateFlow<List<Account?>>(emptyList())
    private val transactions = MutableStateFlow<List<Transaction?>>(emptyList())
    private val goals = MutableStateFlow<List<Goal>>(emptyList())
    private val categoryUsages = mutableMapOf<String, Int>()
    private var nextAccountId = 1
    private var nextTransactionId = 1
    private var nextGoalId = 1

    override suspend fun upsertAccount(account: Account) {
        val current = accounts.value.filterNotNull().toMutableList()
        val existing = current.indexOfFirst { it.id == account.id }
        if (existing >= 0) {
            current[existing] = account
        } else {
            val newAccount = if (account.id == 0) account.copy(id = nextAccountId++) else account
            current.add(newAccount)
        }
        accounts.value = current
    }

    override suspend fun deleteAccount(id: Int) {
        accounts.value = accounts.value.filterNotNull().filter { it.id != id }
    }

    override fun getAllAccounts(): Flow<List<Account?>> = accounts

    override suspend fun getAccountById(id: Int): Account? =
        accounts.value.filterNotNull().find { it.id == id }

    override suspend fun upsertTransaction(transaction: Transaction) {
        val current = transactions.value.filterNotNull().toMutableList()
        val existing = current.indexOfFirst { it.id == transaction.id }
        if (existing >= 0) {
            current[existing] = transaction
        } else {
            val newTx = if (transaction.id == 0) transaction.copy(id = nextTransactionId++) else transaction
            current.add(newTx)
        }
        transactions.value = current
        trackCategoryUsage(transaction.subcategory.id)
    }

    override suspend fun deleteTransaction(id: Int) {
        transactions.value = transactions.value.filterNotNull().filter { it.id != id }
    }

    override fun getAllTransactions(): Flow<List<Transaction?>> = transactions

    override suspend fun getTransactionById(id: Int): Transaction? =
        transactions.value.filterNotNull().find { it.id == id }

    override fun getAllCategories(): List<Category> = TestFixtures.allCategories

    override fun getCategoryById(id: String): Category? =
        TestFixtures.allCategories.find { it.id == id }

    override fun getTopLevelCategories(): List<Category> =
        TestFixtures.allCategories.filter { it.parent == null }

    override fun getSubcategories(parentId: String): List<Category> =
        TestFixtures.allCategories.filter { it.parent?.id == parentId }

    override fun reloadCategories() { /* no-op in tests */ }

    override suspend fun trackCategoryUsage(categoryId: String) {
        categoryUsages[categoryId] = (categoryUsages[categoryId] ?: 0) + 1
    }

    override suspend fun getMostUsedCategories(limit: Int): List<Category> =
        categoryUsages.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { getCategoryById(it.key) }

    override suspend fun upsertGoal(goal: Goal) {
        val current = goals.value.toMutableList()
        val existing = current.indexOfFirst { it.id == goal.id }
        if (existing >= 0) {
            current[existing] = goal
        } else {
            val newGoal = if (goal.id == 0) goal.copy(id = nextGoalId++) else goal
            current.add(newGoal)
        }
        goals.value = current
    }

    override suspend fun deleteGoal(id: Int) {
        goals.value = goals.value.filter { it.id != id }
    }

    override fun getAllGoals(): Flow<List<Goal>> = goals

    override suspend fun getGoalById(id: Int): Goal? =
        goals.value.find { it.id == id }

    // Primary Account
    override suspend fun setPrimaryAccount(accountId: Int) {
        accounts.value = accounts.value.filterNotNull().map {
            it.copy(isPrimary = it.id == accountId)
        }
    }

    override suspend fun getPrimaryAccount(): Account? =
        accounts.value.filterNotNull().find { it.isPrimary }

    // User Currencies
    private val userCurrencies = MutableStateFlow<List<UserCurrency>>(emptyList())

    override fun getUserCurrencies(): Flow<List<UserCurrency>> =
        userCurrencies.map { it.sortedBy { c -> c.sortOrder } }

    override suspend fun upsertUserCurrency(userCurrency: UserCurrency) {
        val current = userCurrencies.value.toMutableList()
        val existing = current.indexOfFirst { it.code == userCurrency.code }
        if (existing >= 0) {
            current[existing] = userCurrency
        } else {
            current.add(userCurrency)
        }
        userCurrencies.value = current
    }

    override suspend fun deleteUserCurrency(code: String) {
        userCurrencies.value = userCurrencies.value.filter { it.code != code }
    }
}
