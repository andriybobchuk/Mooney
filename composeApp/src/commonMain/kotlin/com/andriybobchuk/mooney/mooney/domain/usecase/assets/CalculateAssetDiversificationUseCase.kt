package com.andriybobchuk.mooney.mooney.domain.usecase.assets

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

data class AssetDiversification(
    val categoryBreakdown: Map<AssetCategory, CategoryInfo>,
    val totalNetWorth: Double,
    val currency: Currency
)

data class CategoryInfo(
    val totalAmount: Double,
    val percentage: Double,
    val assetCount: Int,
    val accounts: List<Account>
)

class CalculateAssetDiversificationUseCase {
    operator fun invoke(
        accounts: List<Account>,
        exchangeRates: ExchangeRates?,
        targetCurrency: Currency
    ): AssetDiversification {
        if (accounts.isEmpty()) {
            return AssetDiversification(
                categoryBreakdown = emptyMap(),
                totalNetWorth = 0.0,
                currency = targetCurrency
            )
        }

        // Convert all amounts to target currency
        val accountsInTargetCurrency = accounts.map { account ->
            val convertedAmount = if (account.currency != targetCurrency && exchangeRates != null) {
                exchangeRates.convert(account.amount, account.currency, targetCurrency)
            } else {
                account.amount
            }
            account to convertedAmount
        }

        val totalNetWorth = accountsInTargetCurrency.sumOf { it.second }

        // Group by asset category
        val categoryGroups = accountsInTargetCurrency.groupBy { it.first.assetCategory }
        
        val categoryBreakdown = categoryGroups.mapValues { (_, accountPairs) ->
            val categoryTotal = accountPairs.sumOf { it.second }
            val percentage = if (totalNetWorth > 0) (categoryTotal / totalNetWorth) * 100 else 0.0
            
            CategoryInfo(
                totalAmount = categoryTotal,
                percentage = percentage,
                assetCount = accountPairs.size,
                accounts = accountPairs.map { it.first }
            )
        }

        return AssetDiversification(
            categoryBreakdown = categoryBreakdown,
            totalNetWorth = totalNetWorth,
            currency = targetCurrency
        )
    }
}