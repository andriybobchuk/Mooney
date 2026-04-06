package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryUsageDao
import com.andriybobchuk.mooney.core.data.database.CategoryUsageEntity
import com.andriybobchuk.mooney.core.data.database.GoalDao
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.UserCurrencyDao
import com.andriybobchuk.mooney.core.data.database.UserCurrencyEntity
import com.andriybobchuk.mooney.core.data.database.toDomain
import com.andriybobchuk.mooney.core.data.database.toEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import com.andriybobchuk.mooney.mooney.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking

@Suppress("TooManyFunctions")
class DefaultCoreRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryUsageDao: CategoryUsageDao,
    private val goalDao: GoalDao,
    private val userCurrencyDao: UserCurrencyDao,
    private val categoryDao: CategoryDao,
) : CoreRepository {

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

    override suspend fun upsertTransaction(transaction: Transaction) {
        transactionDao.upsert(transaction.toEntity())
        trackCategoryUsage(transaction.subcategory.id)
    }

    override suspend fun deleteTransaction(id: Int) {
        transactionDao.delete(id)
    }

    override fun getAllTransactions(): Flow<List<Transaction?>> {
        val accountsFlow = getAllAccounts()
        val categories = getAllCategories()

        return transactionDao.getAll().combine(accountsFlow) { transactionEntities, accounts ->
            transactionEntities.map { transactionEntity ->
                val subcategory = categories.find { it.id == transactionEntity.subcategoryId }
                    ?: resolveTransferCategory(transactionEntity.subcategoryId, accounts, categories)

                val account = accounts.find { it?.id == transactionEntity.accountId }

                if (subcategory != null && account != null) {
                    transactionEntity.toDomain(subcategory, account)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun getTransactionById(id: Int): Transaction? {
        val entity = transactionDao.getById(id) ?: return null
        val categories = getAllCategories()
        val accounts = getAllAccounts().first()

        val subcategory = categories.find { it.id == entity.subcategoryId }
            ?: resolveTransferCategory(entity.subcategoryId, accounts, categories)

        val account = accounts.find { it?.id == entity.accountId }

        return if (subcategory != null && account != null) {
            entity.toDomain(subcategory, account)
        } else null
    }

    private fun resolveTransferCategory(
        subcategoryId: String,
        accounts: List<Account?>,
        categories: List<Category>
    ): Category? {
        if (!subcategoryId.startsWith("transfer_to_")) return null
        val destinationAccountId = subcategoryId.removePrefix("transfer_to_").toIntOrNull() ?: return null
        val destinationAccount = accounts.find { it?.id == destinationAccountId } ?: return null
        val transferParent = categories.find { it.id == "internal_transfer" }
        return Category(
            id = subcategoryId,
            title = "Transfer to ${destinationAccount.title}",
            type = CategoryType.TRANSFER,
            emoji = "↔️",
            parent = transferParent
        )
    }

    ///////////////////////////// CATEGORIES //////////////////////

    @Volatile
    private var cachedCategories: List<Category> = emptyList()

    @Volatile
    private var cachedCategoriesById: Map<String, Category> = emptyMap()

    init {
        reloadCategories()
    }

    override fun reloadCategories() {
        val entities = runBlocking(Dispatchers.IO) { categoryDao.getAll().first() }
        val entitiesById = entities.associateBy { it.id }

        fun resolve(entityId: String): Category? {
            val entity = entitiesById[entityId] ?: return null
            val parent = entity.parentId?.let { resolve(it) }
            return entity.toDomain(parent)
        }

        cachedCategories = entities.mapNotNull { resolve(it.id) }
        cachedCategoriesById = cachedCategories.associateBy { it.id }
    }

    override fun getAllCategories(): List<Category> = cachedCategories

    override fun getCategoryById(id: String): Category? = cachedCategoriesById[id]

    override fun getTopLevelCategories(): List<Category> =
        cachedCategories.filter { it.parent == null }

    override fun getSubcategories(parentId: String): List<Category> =
        cachedCategories.filter { it.parent?.id == parentId }

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
            category.parent != null || (category.type != CategoryType.INCOME && category.type != CategoryType.EXPENSE)
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

    ///////////////////////////// PRIMARY ACCOUNT ////////////////////////////////

    override suspend fun setPrimaryAccount(accountId: Int) {
        accountDao.clearAllPrimary()
        accountDao.setPrimary(accountId)
    }

    override suspend fun getPrimaryAccount(): Account? {
        return accountDao.getPrimary()?.toDomain()
    }

    ///////////////////////////// USER CURRENCIES ////////////////////////////////

    override fun getUserCurrencies(): Flow<List<UserCurrency>> {
        return userCurrencyDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertUserCurrency(userCurrency: UserCurrency) {
        userCurrencyDao.upsert(
            UserCurrencyEntity(
                code = userCurrency.code,
                sortOrder = userCurrency.sortOrder
            )
        )
    }

    override suspend fun deleteUserCurrency(code: String) {
        userCurrencyDao.delete(code)
    }
}
