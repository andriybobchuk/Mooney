package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.toDomain
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultCoreRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
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

        val subcategory = getAllCategories().find {
            it.id == entity.subcategoryId
        }

        val accounts = getAllAccounts().first()
        val account = accounts.find {
            it?.id == entity.accountId
        }

        return entity.toDomain(subcategory!!, account!!)
    }


    ///////////////////////////// CATEGORIES - DO NOT MODIFY //////////////////////
    private val categoriesById = CategoryDataSource.categories.associateBy { it.id }

    override fun getAllCategories(): List<Category> = CategoryDataSource.categories

    override fun getCategoryById(id: String): Category? = categoriesById[id]

    override fun getTopLevelCategories(): List<Category> =
        CategoryDataSource.categories.filter { it.parent == null }

    override fun getSubcategories(parentId: String): List<Category> =
        CategoryDataSource.categories.filter { it.parent?.id == parentId }

}
