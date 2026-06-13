package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.mooney.domain.AlertDirection
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.RateWatchAlert
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.parseAmountInput
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.above
import mooney.composeapp.generated.resources.current_rate_prefix
import mooney.composeapp.generated.resources.alert_me_when_format
import mooney.composeapp.generated.resources.active_alerts_section
import mooney.composeapp.generated.resources.below
import mooney.composeapp.generated.resources.remove
import mooney.composeapp.generated.resources.set_alert
import mooney.composeapp.generated.resources.target_rate_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun RateWatchAlertSheet(
    baseCurrency: Currency,
    targetCurrency: Currency,
    currentRate: Double,
    existingAlerts: List<RateWatchAlert>,
    onSave: (Double, AlertDirection) -> Unit,
    onDelete: (Int) -> Unit
) {
    var targetRate by remember { mutableStateOf(currentRate.toString()) }
    var direction by remember { mutableStateOf(AlertDirection.ABOVE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "${baseCurrency.name} → ${targetCurrency.name}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.current_rate_prefix, "${currentRate.formatWithCommas()} ${targetCurrency.symbol}"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Target rate input
        Text(
            text = stringResource(Res.string.alert_me_when_format, baseCurrency.symbol),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Direction toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DirectionChip(
                label = stringResource(Res.string.above),
                isSelected = direction == AlertDirection.ABOVE,
                onClick = { direction = AlertDirection.ABOVE },
                modifier = Modifier.weight(1f)
            )
            DirectionChip(
                label = stringResource(Res.string.below),
                isSelected = direction == AlertDirection.BELOW,
                onClick = { direction = AlertDirection.BELOW },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rate input
        OutlinedTextField(
            value = targetRate,
            onValueChange = { targetRate = it },
            label = { Text(stringResource(Res.string.target_rate_label, targetCurrency.symbol)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(
            onClick = {
                targetRate.parseAmountInput()?.let { rate ->
                    onSave(rate, direction)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(Res.string.set_alert), modifier = Modifier.padding(vertical = 4.dp))
        }

        // Existing alerts
        val pairAlerts = existingAlerts.filter {
            it.fromCurrency == baseCurrency && it.toCurrency == targetCurrency
        }
        if (pairAlerts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.active_alerts_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            pairAlerts.forEach { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (alert.direction == AlertDirection.ABOVE) "↑" else "↓"} ${alert.direction.name.lowercase()} ${alert.targetRate.formatWithCommas()} ${targetCurrency.symbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.remove),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDelete(alert.id) }
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DirectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
