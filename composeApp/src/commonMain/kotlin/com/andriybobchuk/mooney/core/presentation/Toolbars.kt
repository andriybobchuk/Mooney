package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Toolbars {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Primary(
        modifier: Modifier = Modifier,
        showBackButton: Boolean = false,
        title: String = "",
        subtitle: String = "",
        titleContent: @Composable (() -> Unit)? = null,
        onBackClick: () -> Unit = {},
        scrollBehavior: TopAppBarScrollBehavior,
        actions: List<ToolBarAction> = emptyList(),
        customContent: @Composable (() -> Unit)? = null,
        // Optional trailing slot for cases where the caller needs to render
        // their own composable inside the actions row (e.g. an IconButton
        // with an attached DropdownMenu that anchors to it). Rendered AFTER
        // [actions], inside the same Row, so the layout stays consistent.
        trailingActionContent: @Composable (() -> Unit)? = null,
        containerColor: Color = MaterialTheme.colorScheme.background
    ) {
        TopAppBar(
            title = {
                if (titleContent != null) {
                    titleContent()
                } else if (title.isNotEmpty() || subtitle.isNotEmpty()) {
                    Column {
                        if (title.isNotEmpty()) {
                            // Bold + 20sp is now the single title style for
                            // every screen — consistent weight + size across
                            // the app so nav transitions don't visually jump.
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            navigationIcon = {
                if (showBackButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Back button uses a larger touch target than other toolbar
                    // actions — it's the most-tapped chrome control and worth the
                    // extra px (44dp box, 22dp icon vs. action buttons' 36/16).
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = Icons.BackIcon(),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            actions = {
                customContent?.invoke()
                actions.take(3).forEach { actionIcon ->
                    when {
                        actionIcon.painter != null -> CircleToolbarButton(
                            painter = actionIcon.painter,
                            contentDescription = actionIcon.contentDescription,
                            onClick = actionIcon.onClick,
                            enabled = actionIcon.enabled
                        )
                        actionIcon.icon != null -> CircleToolbarButton(
                            icon = actionIcon.icon,
                            contentDescription = actionIcon.contentDescription,
                            onClick = actionIcon.onClick,
                            enabled = actionIcon.enabled
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                trailingActionContent?.invoke()
                Spacer(modifier = Modifier.width(8.dp))
            }
        )
    }

    /**
     * Round 36dp button used for every top-bar leading / trailing action.
     * Mirrors the calendar/month-stepper chip on the Transactions screen so
     * every screen reads with the same Apple-style "round control" language.
     */
    @Composable
    fun CircleToolbarButton(
        painter: Painter? = null,
        icon: ImageVector? = null,
        contentDescription: String,
        onClick: () -> Unit,
        enabled: Boolean = true
    ) {
        val bgAlpha = if (enabled) 0.35f else 0.15f
        val tintAlpha = if (enabled) 1f else 0.35f
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = bgAlpha))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                painter != null -> Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = tintAlpha),
                    modifier = Modifier.size(16.dp)
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = tintAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    data class ToolBarAction(
        val icon: ImageVector? = null,
        val painter: Painter? = null,
        val contentDescription: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true
    )
}
