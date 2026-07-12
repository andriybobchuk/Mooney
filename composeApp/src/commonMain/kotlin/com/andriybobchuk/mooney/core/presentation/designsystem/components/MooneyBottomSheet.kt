package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem
import com.andriybobchuk.mooney.core.presentation.platformBottomSafeInsets

/**
 * Standard bottom sheet with consistent styling matching the design system.
 * - surfaceVariant background (light grey in light / dark grey in dark)
 * - 20dp top rounded corners
 * - 0dp tonal elevation (flat)
 *
 * `contentWindowInsets` comes from [platformBottomSafeInsets]:
 * - Android returns `navigationBars` so the CTA doesn't sit under the
 *   gesture pill on edge-to-edge devices.
 * - iOS returns zero — the ComposeUIViewController already lives inside the
 *   OS safe area, so any extra inset hides the home indicator and doubles
 *   the bottom padding on every sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MooneyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomInsets = platformBottomSafeInsets()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MooneyDesignSystem.Shapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        contentWindowInsets = { bottomInsets },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        content = content
    )
}
