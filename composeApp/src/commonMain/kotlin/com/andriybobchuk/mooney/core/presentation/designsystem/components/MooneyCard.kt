package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem

/**
 * Card variants for different use cases
 */
enum class CardVariant {
    FILLED,    // Default filled card
    OUTLINED,  // Outlined card with border
    ELEVATED   // Elevated card with shadow
}

/**
 * Primary card component for Mooney
 */
@Composable
fun MooneyCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.FILLED,
    onClick: (() -> Unit)? = null,
    containerColor: Color = when (variant) {
        CardVariant.FILLED -> MaterialTheme.colorScheme.surface
        CardVariant.OUTLINED -> MaterialTheme.colorScheme.surface
        CardVariant.ELEVATED -> MaterialTheme.colorScheme.surface
    },
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }
    
    when (variant) {
        CardVariant.FILLED -> {
            Card(
                modifier = cardModifier,
                shape = MooneyDesignSystem.Shapes.card,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = MooneyDesignSystem.Elevation.card
                )
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(
                        modifier = Modifier.padding(MooneyDesignSystem.Spacing.cardPadding)
                    ) {
                        content()
                    }
                }
            }
        }
        
        CardVariant.OUTLINED -> {
            OutlinedCard(
                modifier = cardModifier,
                shape = MooneyDesignSystem.Shapes.card,
                colors = CardDefaults.outlinedCardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MooneyDesignSystem.Colors.Divider
                )
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(
                        modifier = Modifier.padding(MooneyDesignSystem.Spacing.cardPadding)
                    ) {
                        content()
                    }
                }
            }
        }
        
        CardVariant.ELEVATED -> {
            ElevatedCard(
                modifier = cardModifier,
                shape = MooneyDesignSystem.Shapes.card,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = MooneyDesignSystem.Elevation.level3
                )
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(
                        modifier = Modifier.padding(MooneyDesignSystem.Spacing.cardPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Transaction card component for displaying financial transactions
 */
@Composable
fun MooneyTransactionCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    iconBackgroundColor: Color = MooneyDesignSystem.Colors.Primary.copy(alpha = 0.1f),
    iconTint: Color = MooneyDesignSystem.Colors.Primary,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    MooneyCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.FILLED,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (icon != null || iconPainter != null) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MooneyDesignSystem.Shapes.medium,
                    color = iconBackgroundColor
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        when {
                            icon != null -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = iconTint
                            )
                            iconPainter != null -> Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = iconTint
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(MooneyDesignSystem.Spacing.md))
            }
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MooneyDesignSystem.Typography.TitleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MooneyDesignSystem.Typography.BodySmall,
                        color = MooneyDesignSystem.Colors.Disabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Amount
            Text(
                text = amount,
                style = MooneyDesignSystem.Typography.AmountSmall,
                color = amountColor
            )
            
            // Optional trailing content
            trailing?.invoke()
        }
    }
}

/**
 * Summary card for displaying key metrics
 */
@Composable
fun MooneySummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trend: Float? = null,
    trendLabel: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null
) {
    MooneyCard(
        modifier = modifier,
        variant = CardVariant.FILLED,
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = title,
                    style = MooneyDesignSystem.Typography.LabelMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = value,
                    style = MooneyDesignSystem.Typography.AmountMedium,
                    color = contentColor
                )
                
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MooneyDesignSystem.Typography.BodySmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
                
                if (trend != null && trendLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val trendColor = when {
                            trend > 0 -> MooneyDesignSystem.Colors.Success
                            trend < 0 -> MooneyDesignSystem.Colors.Error
                            else -> contentColor
                        }
                        
                        Text(
                            text = "${if (trend >= 0) "+" else ""}${trend}%",
                            style = MooneyDesignSystem.Typography.LabelSmall,
                            color = trendColor
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = trendLabel,
                            style = MooneyDesignSystem.Typography.LabelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Account balance card
 */
@Composable
fun MooneyAccountCard(
    accountName: String,
    balance: String,
    currency: String,
    modifier: Modifier = Modifier,
    accountNumber: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    val containerColor = if (isSelected) {
        MooneyDesignSystem.Colors.Primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isSelected) {
        MooneyDesignSystem.Colors.Primary
    } else {
        MooneyDesignSystem.Colors.Divider
    }
    
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MooneyDesignSystem.Shapes.card,
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(MooneyDesignSystem.Spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MooneyDesignSystem.Colors.Primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = accountName,
                        style = MooneyDesignSystem.Typography.TitleMedium
                    )
                    if (accountNumber != null) {
                        Text(
                            text = accountNumber,
                            style = MooneyDesignSystem.Typography.BodySmall,
                            color = MooneyDesignSystem.Colors.Disabled
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currency,
                    style = MooneyDesignSystem.Typography.LabelMedium,
                    color = MooneyDesignSystem.Colors.Disabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = balance,
                    style = MooneyDesignSystem.Typography.AmountLarge
                )
            }
        }
    }
}