package com.andriybobchuk.mooney.mooney.presentation.assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.CurrencyRateChart
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.edit
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssetDetailSheet(
    asset: AccountWithConversion,
    baseCurrency: Currency,
    historicalRates: List<HistoricalRate>,
    currentRate: Double?,
    percentile: Int?,
    onEdit: () -> Unit
) {
    val isForeign = asset.originalCurrency != baseCurrency

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Account name (secondary)
        Text(
            text = asset.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))

        // Big bold value
        Text(
            text = "${asset.originalAmount.formatWithCommas()} ${asset.originalCurrency.symbol}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Estimated value in base currency
        if (isForeign) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "≈ ${asset.baseCurrencyAmount.formatWithCommas()} ${baseCurrency.symbol}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Chart (foreign currency only)
        if (isForeign && historicalRates.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            CurrencyRateChart(
                chartCurrency = asset.originalCurrency,
                baseCurrency = baseCurrency,
                currentRate = currentRate,
                historicalRates = historicalRates,
                percentile = percentile,
                assetAmount = asset.originalAmount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // Edit button
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(Res.string.edit), modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}
