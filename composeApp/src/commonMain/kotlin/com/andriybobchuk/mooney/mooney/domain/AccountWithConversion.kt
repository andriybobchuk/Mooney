package com.andriybobchuk.mooney.mooney.domain

data class AccountWithConversion(
    val id: Int,
    val title: String,
    val emoji: String,
    val originalAmount: Double,
    val originalCurrency: Currency,
    val baseCurrencyAmount: Double,
    val exchangeRate: Double?,
    val assetCategory: AssetCategory = AssetCategory.BANK_ACCOUNT
) {
    fun toAccount(): Account = Account(
        id = id,
        title = title,
        amount = originalAmount,
        currency = originalCurrency,
        emoji = emoji,
        assetCategory = assetCategory
    )
}
