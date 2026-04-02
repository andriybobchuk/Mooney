package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao
import com.andriybobchuk.mooney.mooney.domain.RecurringFrequency
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import com.andriybobchuk.mooney.mooney.domain.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetRecurringTransactionsUseCase(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        val categories = getCategoriesUseCase()
        return recurringTransactionDao.getAll().combine(getAccountsUseCase()) { entities, accounts ->
            val accountMap = accounts.filterNotNull().associateBy { it.id }

            entities.map { entity ->
                val frequency = try {
                    RecurringFrequency.valueOf(entity.frequency)
                } catch (_: IllegalArgumentException) {
                    RecurringFrequency.MONTHLY
                }
                RecurringTransaction(
                    id = entity.id,
                    title = entity.title,
                    subcategory = categories.find { it.id == entity.subcategoryId },
                    amount = entity.amount,
                    account = accountMap[entity.accountId],
                    schedule = RecurringSchedule(
                        frequency = frequency,
                        dayOfMonth = entity.dayOfMonth,
                        weekDay = entity.weekDay,
                        monthOfYear = entity.monthOfYear
                    ),
                    isActive = entity.isActive
                )
            }
        }
    }
}
