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

enum class RecurringFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class RecurringSchedule(
    val frequency: RecurringFrequency,
    val dayOfMonth: Int = 1,
    val weekDay: Int? = null,
    val monthOfYear: Int? = null
) {
    fun toDisplayString(): String {
        return when (frequency) {
            RecurringFrequency.DAILY -> "Every day"
            RecurringFrequency.WEEKLY -> {
                val dayName = weekDay?.let { dayOfWeekName(it) } ?: "Monday"
                "Weekly on $dayName"
            }
            RecurringFrequency.MONTHLY -> "Monthly on the ${ordinal(dayOfMonth)}"
            RecurringFrequency.YEARLY -> {
                val monthName = monthOfYear?.let { monthName(it) } ?: "January"
                "Yearly on $monthName ${ordinal(dayOfMonth)}"
            }
        }
    }
}

data class RecurringTransaction(
    val id: Int,
    val title: String,
    val subcategory: Category?,
    val amount: Double,
    val account: Account?,
    val schedule: RecurringSchedule,
    val isActive: Boolean = true
)

private fun ordinal(day: Int): String {
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

private fun dayOfWeekName(day: Int): String = when (day) {
    0 -> "Monday"
    1 -> "Tuesday"
    2 -> "Wednesday"
    3 -> "Thursday"
    4 -> "Friday"
    5 -> "Saturday"
    6 -> "Sunday"
    else -> "Monday"
}

private fun monthName(month: Int): String = when (month) {
    1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
    5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
    9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
    else -> "January"
}