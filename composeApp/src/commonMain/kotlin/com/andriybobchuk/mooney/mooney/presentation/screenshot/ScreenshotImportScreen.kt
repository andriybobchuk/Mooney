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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
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
import mooney.composeapp.generated.resources.screenshot_import_dup_batch
import mooney.composeapp.generated.resources.screenshot_import_dup_existing
import mooney.composeapp.generated.resources.screenshot_import_empty_body
import mooney.composeapp.generated.resources.screenshot_import_empty_title
import mooney.composeapp.generated.resources.screenshot_import_keep_anyway
import mooney.composeapp.generated.resources.screenshot_import_low_confidence
import mooney.composeapp.generated.resources.screenshot_import_parse_failed
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
    onBackClick: () -> Unit,
    onFinished: (insertedCount: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.rows.withIndex().toList(),
                key = { (_, row) -> row.candidate.sourceImageId }
            ) { (index, row) ->
                CandidateRow(
                    row = row,
                    onToggle = { onAction(ScreenshotImportAction.ToggleRow(index)) },
                    onKeepAnyway = { onAction(ScreenshotImportAction.KeepAnyway(index)) }
                )
            }
        }
        CommitBar(state = state, onCommit = { onAction(ScreenshotImportAction.Commit) })
    }
}

@Composable
private fun CandidateRow(
    row: CandidateRow,
    onToggle: () -> Unit,
    onKeepAnyway: () -> Unit
) {
    val c = row.candidate
    val isActionable = c.status != ParsedCandidate.Status.PARSE_FAILED
    val containerColor = when {
        !isActionable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        row.accepted -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(enabled = isActionable, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Checkbox(
            checked = row.accepted,
            enabled = isActionable
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            if (c.status == ParsedCandidate.Status.PARSE_FAILED) {
                Text(
                    text = stringResource(Res.string.screenshot_import_parse_failed),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${c.amount.formatWithCommas()} ${c.currency.symbol}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = c.description ?: "—",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatusChips(c, row.accepted, onKeepAnyway)
            }
        }
    }
}

@Composable
private fun Checkbox(checked: Boolean, enabled: Boolean) {
    val background = if (checked && enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (checked && enabled) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun StatusChips(
    c: ParsedCandidate,
    accepted: Boolean,
    onKeepAnyway: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (c.status) {
            ParsedCandidate.Status.DUP_EXISTING -> {
                InfoChip(
                    icon = Icons.Filled.Info,
                    text = stringResource(Res.string.screenshot_import_dup_existing),
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (!accepted) {
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onKeepAnyway) {
                        Text(
                            text = stringResource(Res.string.screenshot_import_keep_anyway),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            ParsedCandidate.Status.DUP_BATCH -> {
                InfoChip(
                    icon = Icons.Filled.Info,
                    text = stringResource(Res.string.screenshot_import_dup_batch),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            ParsedCandidate.Status.NEW -> {
                if (c.confidence < 0.5) {
                    InfoChip(
                        icon = Icons.Filled.Warning,
                        text = stringResource(Res.string.screenshot_import_low_confidence),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            ParsedCandidate.Status.PARSE_FAILED -> Unit
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.size(4.dp))
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
