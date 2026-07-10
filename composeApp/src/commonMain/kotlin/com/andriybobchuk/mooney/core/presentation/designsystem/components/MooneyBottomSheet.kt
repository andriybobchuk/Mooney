package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem

/**
 * Standard bottom sheet with consistent styling matching the design system.
 * - surfaceVariant background (light grey in light / dark grey in dark)
 * - 20dp top rounded corners
 * - 0dp tonal elevation (flat)
 *
 * Uses [WindowInsets.navigationBars] as the content inset so the primary
 * action button never sits underneath Android's gesture-nav pill — users
 * were previously bouncing the app or accidentally dismissing the sheet
 * because the CTA overlapped the system swipe area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MooneyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MooneyDesignSystem.Shapes.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets.navigationBars },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        content = content
    )
}
