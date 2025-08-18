package com.andriybobchuk.mooney.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                //titleContentColor = MaterialTheme.colorScheme.onPrimary,
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onPrimary
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
                            tint = MaterialTheme.colorScheme.onPrimary
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
