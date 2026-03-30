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
    INCOME,
    TRANSFER
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
    val emoji: String,
    val assetCategory: AssetCategory = AssetCategory.BANK_ACCOUNT,
    val assetCategoryId: String = assetCategory.name,
    val isPrimary: Boolean = false,
    val isLiability: Boolean = false
)

data class UserCurrency(
    val code: String,
    val sortOrder: Int
)

enum class Currency(val symbol: String) {
    PLN("zł"),
    USD("$"),
    EUR("€"),
    UAH("₴"),
    GBP("£"),
    CHF("Fr"),
    CZK("Kč"),
    SEK("kr"),
    NOK("kr"),
    DKK("kr"),
    JPY("¥"),
    CAD("C$"),
    AUD("A$"),
    TRY("₺"),
    BRL("R$"),
    RUB("₽"),
    AED("د.إ"),
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

enum class GoalTrackingType {
    ACCOUNT,
    NET_WORTH,
    TOTAL_ASSETS
}

data class Goal(
    val id: Int,
    val emoji: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currency: Currency,
    val createdDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val groupName: String = "General",
    val imagePath: String? = null,
    val trackingType: GoalTrackingType = GoalTrackingType.NET_WORTH,
    val accountId: Int? = null
)

data class GoalGroup(
    val id: Int,
    val name: String,
    val emoji: String,
    val color: String,
    val createdDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
)