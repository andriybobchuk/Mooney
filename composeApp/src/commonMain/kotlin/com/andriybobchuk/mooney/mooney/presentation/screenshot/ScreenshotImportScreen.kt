package com.andriybobchuk.mooney.mooney.presentation.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.mooney.domain.formatWithCommas
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParsedCandidate
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.screenshot_import_commit_button
import mooney.composeapp.generated.resources.screenshot_import_empty_body
import mooney.composeapp.generated.resources.screenshot_import_empty_title
import mooney.composeapp.generated.resources.screenshot_import_parse_failed
import mooney.composeapp.generated.resources.screenshot_import_possible_duplicate
import mooney.composeapp.generated.resources.screenshot_import_select_all
import mooney.composeapp.generated.resources.screenshot_import_select_none
import mooney.composeapp.generated.resources.screenshot_import_subtitle
import mooney.composeapp.generated.resources.screenshot_import_title
import org.jetbrains.compose.resources.stringResource

/**
 * Preview screen for screenshot bulk-import.
 *
 * Flow:
 *   1. [LaunchedEffect] on entry fires `Load` → VM invokes
 *      [RunScreenshotImportUseCase] which presents the native picker,
 *      runs Vision OCR, and returns parsed candidates.
 *   2. Rows render with a per-row accept toggle. Dups + parse-fails show
 *      inline chips explaining why; dups also expose a "Keep anyway"
 *      affordance for user override.
 *   3. "Add N transactions" at the bottom commits accepted rows. On
 *      success, VM emits a Finished event; the host route pops back to
 *      Transactions. The committed rows arrive there as auto-added
 *      transactions and land in the existing Phase 1 Upkeep queue for
 *      category correction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotImportScreen(
    viewModel: ScreenshotImportViewModel,
    mode: com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParseMode,
    onBackClick: () -> Unit,
    onFinished: (insertedCount: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.configure(mode)
        viewModel.onAction(ScreenshotImportAction.Load)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ScreenshotImportEvent.Finished -> onFinished(event.insertedCount)
                ScreenshotImportEvent.DismissedEmpty -> onBackClick()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ImportTopBar(
                acceptedCount = state.acceptedCount,
                totalCount = state.rows.size,
                onClose = {
                    viewModel.onAction(ScreenshotImportAction.Dismiss)
                }
            )
        }
    ) { padding ->
        when {
            state.isPicking -> LoadingState(padding)
            !state.hasPickedImages -> LoadingState(padding)
            state.rows.isEmpty() -> EmptyState(padding)
            else -> ImportContent(
                state = state,
                padding = padding,
                onAction = viewModel::onAction
            )
        }
    }
}

@Composable
private fun ImportTopBar(
    acceptedCount: Int,
    totalCount: Int,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // Custom top bar — Scaffold doesn't auto-insert status-bar
            // insets for arbitrary topBar content.
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = null)
        }
        Spacer(Modifier.size(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.screenshot_import_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            if (totalCount > 0) {
                Text(
                    text = "$acceptedCount / $totalCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(Res.string.screenshot_import_empty_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.screenshot_import_empty_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ImportContent(
    state: ScreenshotImportState,
    padding: PaddingValues,
    onAction: (ScreenshotImportAction) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.screenshot_import_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            val allSelected = state.rows.all { it.accepted }
            TextButton(
                onClick = {
                    onAction(ScreenshotImportAction.ToggleAll(accepted = !allSelected))
                }
            ) {
                Text(
                    text = if (allSelected) stringResource(Res.string.screenshot_import_select_none)
                    else stringResource(Res.string.screenshot_import_select_all),
                    fontSize = 13.sp
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            // Tighter gap between rows (8 → 2) so more candidates fit on
            // screen — the preview often has 10+ rows and the user needs to
            // scan them all before committing.
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                items = state.rows.withIndex().toList(),
                key = { (_, row) -> row.candidate.sourceImageId }
            ) { (index, row) ->
                CandidateRow(
                    row = row,
                    onToggle = { onAction(ScreenshotImportAction.ToggleRow(index)) }
                )
            }
        }
        CommitBar(state = state, onCommit = { onAction(ScreenshotImportAction.Commit) })
    }
}

@Composable
private fun CandidateRow(
    row: CandidateRow,
    onToggle: () -> Unit
) {
    val c = row.candidate
    val isActionable = c.status != ParsedCandidate.Status.PARSE_FAILED
    val containerColor = if (!isActionable) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(enabled = isActionable, onClick = onToggle)
            // Tightened vertical padding (14 → 6) so more rows fit on screen.
            // Horizontal reduced to 10 to match row rhythm; the Checkbox
            // itself provides enough touch area within.
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        androidx.compose.material3.Checkbox(
            checked = row.accepted,
            onCheckedChange = { if (isActionable) onToggle() },
            enabled = isActionable
        )
        Spacer(Modifier.size(4.dp))
        Column(Modifier.weight(1f)) {
            if (c.status == ParsedCandidate.Status.PARSE_FAILED) {
                Text(
                    text = stringResource(Res.string.screenshot_import_parse_failed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${c.amount.formatWithCommas()} ${c.currency.symbol}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = c.description ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                // Second line — suggested date + optional dup label. User
                // asked that scan previews ALWAYS mention the suggested date
                // so they know what date would be recorded on commit, and
                // that strict amount-match dups show "Possible duplicate".
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = c.date.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (c.status == ParsedCandidate.Status.DUP_EXISTING ||
                        c.status == ParsedCandidate.Status.DUP_BATCH) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.screenshot_import_possible_duplicate),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitBar(
    state: ScreenshotImportState,
    onCommit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // Keep the commit button clear of the home indicator / nav bar.
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Button(
            onClick = onCommit,
            enabled = state.canCommit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (state.isCommitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.screenshot_import_commit_button) +
                        " (${state.acceptedCount})",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
