package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    DESTRUCTIVE,
    TONAL
}

enum class ButtonSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Composable
fun MooneyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    fullWidth: Boolean = false
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    val height = when (size) {
        ButtonSize.SMALL -> MooneyDesignSystem.ComponentHeight.buttonSmall
        ButtonSize.MEDIUM -> MooneyDesignSystem.ComponentHeight.button
        ButtonSize.LARGE -> MooneyDesignSystem.ComponentHeight.buttonLarge
    }

    val contentPadding = when (size) {
        ButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ButtonSize.MEDIUM -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    }

    val textStyle = when (size) {
        ButtonSize.SMALL -> MaterialTheme.typography.labelMedium
        ButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        ButtonSize.LARGE -> MaterialTheme.typography.titleSmall
    }

    when (variant) {
        ButtonVariant.PRIMARY -> {
            // Filled black (light) / white (dark) — uses inverseSurface
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPainter, loading, textStyle, size)
            }
        }

        ButtonVariant.SECONDARY -> {
            // Outlined with subtle border
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled && !loading) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPainter, loading, textStyle, size)
            }
        }

        ButtonVariant.TERTIARY -> {
            // Ghost / text-only — no background, no border
            TextButton(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPainter, loading, textStyle, size)
            }
        }

        ButtonVariant.DESTRUCTIVE -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPainter, loading, textStyle, size)
            }
        }

        ButtonVariant.TONAL -> {
            // Subtle filled — uses surfaceVariant
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPainter, loading, textStyle, size)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    iconPainter: Painter?,
    loading: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    size: ButtonSize
) {
    val iconSize = when (size) {
        ButtonSize.SMALL -> 16.dp
        ButtonSize.MEDIUM -> 20.dp
        ButtonSize.LARGE -> 24.dp
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = LocalContentColor.current,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(modifier = Modifier.width(8.dp))
            } else if (iconPainter != null) {
                Icon(painter = iconPainter, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MooneyFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    extended: Boolean = false
) {
    if (extended && text != null) {
        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = MooneyDesignSystem.Shapes.large,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = MooneyDesignSystem.Shapes.large,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun MooneyIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = MooneyDesignSystem.IconSize.touchTarget,
            minHeight = MooneyDesignSystem.IconSize.touchTarget
        ),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(MooneyDesignSystem.IconSize.medium),
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
