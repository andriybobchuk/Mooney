package com.andriybobchuk.mooney.mooney.presentation.reconcile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.app.appColors
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import kotlin.math.abs
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.reconcile_body
import mooney.composeapp.generated.resources.reconcile_cta
import mooney.composeapp.generated.resources.reconcile_delta_label
import mooney.composeapp.generated.resources.reconcile_input_label
import mooney.composeapp.generated.resources.reconcile_mooney_shows
import mooney.composeapp.generated.resources.reconcile_title
import org.jetbrains.compose.resources.stringResource

/**
 * Focused reconciliation wizard. Opens from the Balance tab's
 * AssetDetailSheet when the user taps "Reconcile". The flow is intentionally
 * one-screen with a single text field + comparison line so there's no way to
 * get it wrong.
 *
 * UX contract:
 * - User types the balance they see in their bank app RIGHT NOW.
 * - Live-updating delta row tells them what Mooney will adjust by.
 * - Tapping the primary button fires [onConfirm] with the parsed amount;
 *   the ViewModel inserts the adjustment transaction + rewrites the account
 *   balance in one atomic use case.
 *
 * Layout notes:
 * - `imePadding()` on the root Column lifts the content above the on-screen
 *   keyboard; without it the text field slides offscreen on iOS the moment
 *   you tap it.
 * - `verticalScroll` prevents a short-screen (SE / landscape) crop; the
 *   sheet is small enough it almost never scrolls in practice.
 * - No programmatic focus request on open — doing so from a LaunchedEffect
 *   inside a ModalBottomSheet races the sheet's attach pass and threw
 *   "FocusRequester not initialized" in dev builds, breaking the whole
 *   wizard. User taps the field like any other sheet input; one extra tap
 *   is better than a crash.
 */
@Composable
fun ReconcileAccountSheet(
    account: Account,
    isSubmitting: Boolean,
    onConfirm: (newBalance: Double) -> Unit
) {
    var input by remember { mutableStateOf("") }

    val parsed = input
        .replace(',', '.')
        .toDoubleOrNull()
    val delta = parsed?.let { it - account.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(Res.string.reconcile_title, account.title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.reconcile_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { raw ->
                // Only digits + one separator — keeps the field clean
                // without forcing a numeric keyboard spec that may differ
                // between locales.
                input = raw.filter { it.isDigit() || it == '.' || it == ',' }
            },
            label = { Text(stringResource(Res.string.reconcile_input_label, account.currency.symbol)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(Modifier.height(20.dp))

        // Comparison block — Mooney's current balance on the left, delta on
        // the right. Delta color-coded by sign using the shared appColors
        // tokens, same ones the Transactions list uses.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.reconcile_mooney_shows),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${account.amount.formatWithCommas()} ${account.currency.symbol}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.reconcile_delta_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                val deltaText = when {
                    delta == null -> "—"
                    abs(delta) < 0.01 -> "0 ${account.currency.symbol}"
                    delta > 0 -> "+${delta.formatWithCommas()} ${account.currency.symbol}"
                    else -> "−${abs(delta).formatWithCommas()} ${account.currency.symbol}"
                }
                val deltaColor = when {
                    delta == null || abs(delta) < 0.01 -> MaterialTheme.colorScheme.onSurface
                    delta > 0 -> MaterialTheme.appColors.incomeColor
                    else -> MaterialTheme.appColors.expenseColor
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = deltaColor
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { parsed?.let(onConfirm) },
            enabled = parsed != null && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(Res.string.reconcile_cta),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
