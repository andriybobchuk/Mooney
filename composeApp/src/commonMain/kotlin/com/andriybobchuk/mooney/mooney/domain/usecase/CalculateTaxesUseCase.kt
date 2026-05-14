package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction

class CalculateTaxesUseCase {

    operator fun invoke(
        transactions: List<Transaction>,
        baseCurrency: Currency,
        exchangeRates: ExchangeRates
    ): Double {
        return transactions
            .filter { isTaxTransaction(it) }
            .sumOf { exchangeRates.convert(it.amount, it.account.currency, baseCurrency) }
    }

    companion object {
        private const val TAX_CATEGORY_ID = "tax"
        private val LEGACY_TITLE_KEYWORDS = listOf("ZUS", "PIT")

        fun isTaxTransaction(transaction: Transaction): Boolean {
            if (transaction.subcategory.isRootedInTaxCategory()) return true
            return LEGACY_TITLE_KEYWORDS.any { keyword ->
                transaction.subcategory.title.contains(keyword, ignoreCase = true)
            }
        }

        private fun Category.isRootedInTaxCategory(): Boolean {
            var current: Category? = this
            while (current != null) {
                if (current.id == TAX_CATEGORY_ID) return true
                current = current.parent
            }
            return false
        }
    }
}
