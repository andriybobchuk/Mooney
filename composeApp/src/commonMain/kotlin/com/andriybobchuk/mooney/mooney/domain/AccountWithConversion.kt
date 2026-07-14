package com.andriybobchuk.mooney.mooney.domain

data class AccountWithConversion(
    val id: Int,
    val title: String,
    val emoji: String,
    val originalAmount: Double,
    val originalCurrency: Currency,
    val baseCurrencyAmount: Double,
    val exchangeRate: Double?,
    val assetCategory: AssetCategory = AssetCategory.BANK_ACCOUNT,
    val assetCategoryId: String = assetCategory.name,
    val isPrimary: Boolean = false,
    val isLiability: Boolean = false,
    // Optional "worth today" for cost-basis assets. Kept in the source
    // account currency (same as [originalAmount]) so the UI can compute a
    // gain/loss delta without any FX round-trip.
    val currentMarketValue: Double? = null,
    val includeInNetWorth: Boolean = true
) {
    fun toAccount(): Account = Account(
        id = id,
        title = title,
        amount = originalAmount,
        currency = originalCurrency,
        emoji = emoji,
        assetCategory = assetCategory,
        assetCategoryId = assetCategoryId,
        isPrimary = isPrimary,
        isLiability = isLiability,
        currentMarketValue = currentMarketValue,
        includeInNetWorth = includeInNetWorth
    )
}
