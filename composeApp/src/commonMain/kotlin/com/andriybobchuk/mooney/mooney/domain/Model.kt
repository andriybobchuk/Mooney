package com.andriybobchuk.mooney.mooney.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class Category(
    val id: String,
    val title: String,
    val type: CategoryType,
    val emoji: String? = null,
    val parent: Category? = null
) {
    // Category Type
    fun isTypeCategory(): Boolean = parent == null

    // General Category
    fun isGeneralCategory(): Boolean = parent?.isTypeCategory() ?: false

    // Sub Category
    fun isSubCategory(): Boolean = parent?.isGeneralCategory() ?: false

    fun getRoot(): Category = parent?.getRoot() ?: this

    fun resolveEmoji(): String = emoji ?: parent?.emoji ?: parent?.parent?.emoji ?: ""
}

enum class CategoryType {
    EXPENSE,
    INCOME
}

data class Transaction(
    val id: Int,
    val subcategory: Category,
    val amount: Double,
    val account: Account,
    val date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
)

data class Account(
    val id: Int,
    val title: String,
    val amount: Double,
    val currency: Currency,
    val emoji: String
)

enum class Currency(val symbol: String) {
    PLN("zł"),
    USD("$"),
    EUR("€"),
    UAH("₴"),
}

data class ExchangeRates(
    val rates: Map<Currency, Double>
) {
    fun convert(amount: Double, from: Currency, to: Currency): Double {
        val fromRate = rates[from] ?: error("Missing rate for $from")
        val toRate = rates[to] ?: error("Missing rate for $to")
        return amount / fromRate * toRate
    }
}

data class Goal(
    val id: Int,
    val emoji: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currency: Currency,
    val createdDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
)