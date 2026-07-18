package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps an asset-category id (the string stored on `AccountEntity.assetCategory`,
 * matching the seed AssetCategory enum names) to a Material Icons vector. The
 * default fallback is [Icons.Outlined.Category] so custom user-added categories
 * still render a reasonable glyph in the list.
 *
 * Outlined variants are used for consistency with the rest of Mooney's chrome
 * (bottom nav, toolbars) which favors line weights over filled glyphs.
 */
fun assetCategoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "BANK_ACCOUNT" -> Icons.Outlined.AccountBalance
    "CASH" -> Icons.Outlined.AttachMoney
    "REAL_ESTATE" -> Icons.Outlined.Home
    "STOCKS" -> Icons.Outlined.TrendingUp
    "BONDS" -> Icons.Outlined.RequestQuote
    "CRYPTO" -> Icons.Outlined.CurrencyBitcoin
    "PRECIOUS_METALS" -> Icons.Outlined.Diamond
    "RETIREMENT" -> Icons.Outlined.BeachAccess
    "VEHICLE" -> Icons.Outlined.DirectionsCar
    "RECEIVABLES" -> Icons.Outlined.ReceiptLong
    "MORTGAGE" -> Icons.Outlined.Domain
    "LOAN" -> Icons.Outlined.Description
    "CREDIT_CARD" -> Icons.Outlined.CreditCard
    "DEBT" -> Icons.Outlined.MoneyOff
    else -> Icons.Outlined.Category
}
