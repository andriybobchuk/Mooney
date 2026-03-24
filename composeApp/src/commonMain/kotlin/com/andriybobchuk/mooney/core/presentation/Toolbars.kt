package com.andriybobchuk.mooney.core.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        containerColor: Color = MaterialTheme.colorScheme.background
    ) {
        TopAppBar(
            title = {
                if (titleContent != null) {
                    titleContent()
                } else if (title.isNotEmpty() || subtitle.isNotEmpty()) {
                    Column {
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Medium,
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
                            tint = MaterialTheme.colorScheme.onBackground
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
