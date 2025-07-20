package com.andriybobchuk.time.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

object Toolbars {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Primary(
        modifier: Modifier = Modifier,
        showBackButton: Boolean = false,
        title: String,
        onBackClick: () -> Unit = {},
        scrollBehavior: TopAppBarScrollBehavior,
        actions: List<ToolBarAction> = emptyList(),
        customContent: @Composable (() -> Unit)? = null
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.textColor(),
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            },
            actions = {
                customContent?.invoke()
                actions.take(3).forEach { actionIcon ->
                    IconButton(
                        onClick = actionIcon.onClick,
                        enabled = actionIcon.enabled,
                    ) {
                        Icon(
                            imageVector = actionIcon.icon,
                            contentDescription = actionIcon.contentDescription,
                        )
                    }
                }
            }
        )
    }

    data class ToolBarAction(
        val icon: ImageVector,
        val contentDescription: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true
    )
}
