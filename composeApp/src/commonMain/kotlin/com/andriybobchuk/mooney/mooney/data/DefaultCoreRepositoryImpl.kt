package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.CategoryUsageDao
import com.andriybobchuk.mooney.core.data.database.CategoryUsageEntity
import com.andriybobchuk.mooney.core.data.database.GoalDao
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.toDomain
import com.andriybobchuk.mooney.core.data.database.toEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultCoreRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryUsageDao: CategoryUsageDao,
    private val goalDao: GoalDao,
) : CoreRepository {

    ///////////////////////////// ACCOUNTS //////////////////////////////////////////////////
    //private val accounts = AccountDataSource.accounts.toMutableList()


    override suspend fun upsertAccount(account: Account) {
        accountDao.upsert(account.toEntity())
    }

    override suspend fun deleteAccount(id: Int) {
        accountDao.delete(id)
    }

    override fun getAllAccounts(): Flow<List<Account?>> {
        return accountDao.getAll().map { it.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Int): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    /////////////////////////////////// TRANSACTIONS /////////////////////////////////
//
//    override suspend fun upsertTransaction(transaction: Transaction) {
//        transactionDao.upsert(transaction.toEntity())
//    }

    override suspend fun upsertTransaction(transaction: Transaction) {
        // Simple data operation - business logic moved to use cases
        transactionDao.upsert(transaction.toEntity())
        // Track category usage when creating/updating transactions
        trackCategoryUsage(transaction.subcategory.id)
    }


    override suspend fun deleteTransaction(id: Int) {
        // Simple data operation - business logic moved to use cases
        transactionDao.delete(id)
    }


    override fun getAllTransactions(): Flow<List<Transaction?>> {
        val accountsFlow = getAllAccounts()
        val categories = getAllCategories()

        return transactionDao.getAll().combine(accountsFlow) { transactionEntities, accounts ->
            transactionEntities.map { transactionEntity ->
                val subcategory = categories.find {
                    it.id == transactionEntity.subcategoryId
                } ?: run {
                    // Handle dynamic transfer categories
                    if (transactionEntity.subcategoryId.startsWith("transfer_to_")) {
                        val destinationAccountId = transactionEntity.subcategoryId.removePrefix("transfer_to_").toIntOrNull()
                        val destinationAccount = accounts.find { it?.id == destinationAccountId }
                        if (destinationAccount != null) {
                            // Create dynamic transfer category
                            val transferParent = categories.find { it.id == "internal_transfer" }
                            Category(
                                id = transactionEntity.subcategoryId,
                                title = "Transfer to ${destinationAccount.title}",
                                type = CategoryType.TRANSFER,
                                emoji = "↔️",
                                parent = transferParent
                            )
                        } else null
                    } else null
                }

                val account = accounts.find {
                    it?.id == transactionEntity.accountId
                }

                if (subcategory != null && account != null) {
                    transactionEntity.toDomain(subcategory, account)
                } else {
                    null // Skip broken/missing data to prevent crash
                }
            }
        }
    }

    override suspend fun getTransactionById(id: Int): Transaction? {
        val entity = transactionDao.getById(id) ?: return null

        val categories = getAllCategories()
        val subcategory = categories.find {
            it.id == entity.subcategoryId
        } ?: run {
            // Handle dynamic transfer categories
            if (entity.subcategoryId.startsWith("transfer_to_")) {
                val destinationAccountId = entity.subcategoryId.removePrefix("transfer_to_").toIntOrNull()
                val accounts = getAllAccounts().first()
                val destinationAccount = accounts.find { it?.id == destinationAccountId }
                if (destinationAccount != null) {
                    val transferParent = categories.find { it.id == "internal_transfer" }
                    Category(
                        id = entity.subcategoryId,
                        title = "Transfer to ${destinationAccount.title}",
                        type = CategoryType.TRANSFER,
                        emoji = "↔️",
                        parent = transferParent
                    )
                } else null
            } else null
        }

        val accounts = getAllAccounts().first()
        val account = accounts.find {
            it?.id == entity.accountId
        }

        return if (subcategory != null && account != null) {
            entity.toDomain(subcategory, account)
        } else null
    }


    ///////////////////////////// CATEGORIES - DO NOT MODIFY //////////////////////
    private val categoriesById = CategoryDataSource.categories.associateBy { it.id }

    override fun getAllCategories(): List<Category> = CategoryDataSource.categories

    override fun getCategoryById(id: String): Category? = categoriesById[id]

    override fun getTopLevelCategories(): List<Category> =
        CategoryDataSource.categories.filter { it.parent == null }

    override fun getSubcategories(parentId: String): List<Category> =
        CategoryDataSource.categories.filter { it.parent?.id == parentId }

    ///////////////////////////// CATEGORY USAGE ///////////////////////////////////////
    
    override suspend fun trackCategoryUsage(categoryId: String) {
        val currentDate = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        val existing = categoryUsageDao.getCategoryUsage(categoryId)
        
        if (existing != null) {
            categoryUsageDao.incrementUsage(categoryId, currentDate)
        } else {
            categoryUsageDao.upsert(
                CategoryUsageEntity(
                    categoryId = categoryId,
                    usageCount = 1,
                    lastUsedDate = currentDate
                )
            )
        }
    }
    
    override suspend fun getMostUsedCategories(limit: Int): List<Category> {
        val usageEntities = categoryUsageDao.getMostUsedCategories(limit)
        return usageEntities.mapNotNull { entity ->
            getCategoryById(entity.categoryId)
        }.filter { category ->
            // Filter out top-level categories (Income/Expense) as they're too generic
            category.parent != null || (category.parent == null && category.type != CategoryType.INCOME && category.type != CategoryType.EXPENSE)
        }
    }

    ///////////////////////////// GOALS /////////////////////////////////////////////////
    
    override suspend fun upsertGoal(goal: Goal) {
        goalDao.upsert(goal.toEntity())
    }

    override suspend fun deleteGoal(id: Int) {
        goalDao.delete(id)
    }

    override fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAll().map { goalEntities ->
            goalEntities.map { it.toDomain() }
        }
    }

    override suspend fun getGoalById(id: Int): Goal? {
        return goalDao.getById(id)?.toDomain()
    }

}
