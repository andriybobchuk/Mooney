package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.MooneyDesignSystem

/**
 * Button variants for different use cases
 */
enum class ButtonVariant {
    PRIMARY,      // Main CTA buttons
    SECONDARY,    // Secondary actions
    TERTIARY,     // Text-only buttons
    DESTRUCTIVE,  // Delete/dangerous actions
    TONAL         // Filled tonal variant
}

/**
 * Button sizes
 */
enum class ButtonSize {
    SMALL,        // Compact buttons
    MEDIUM,       // Default size
    LARGE         // Prominent CTAs
}

/**
 * Primary Mooney button component with multiple variants and states
 */
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }
    
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
        ButtonSize.SMALL -> MooneyDesignSystem.Typography.LabelMedium
        ButtonSize.MEDIUM -> MooneyDesignSystem.Typography.LabelLarge
        ButtonSize.LARGE -> MooneyDesignSystem.Typography.TitleSmall
    }
    
    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MooneyDesignSystem.Colors.Primary,
                    contentColor = MooneyDesignSystem.Colors.OnPrimary,
                    disabledContainerColor = MooneyDesignSystem.Colors.Disabled,
                    disabledContentColor = MooneyDesignSystem.Colors.OnPrimary.copy(alpha = 0.6f)
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource
            ) {
                ButtonContent(
                    text = text,
                    icon = icon,
                    iconPainter = iconPainter,
                    loading = loading,
                    textStyle = textStyle,
                    size = size
                )
            }
        }
        
        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MooneyDesignSystem.Colors.Primary,
                    disabledContentColor = MooneyDesignSystem.Colors.Disabled
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled && !loading) MooneyDesignSystem.Colors.Primary 
                           else MooneyDesignSystem.Colors.Disabled
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource
            ) {
                ButtonContent(
                    text = text,
                    icon = icon,
                    iconPainter = iconPainter,
                    loading = loading,
                    textStyle = textStyle,
                    size = size
                )
            }
        }
        
        ButtonVariant.TERTIARY -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MooneyDesignSystem.Colors.Primary,
                    disabledContentColor = MooneyDesignSystem.Colors.Disabled
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource
            ) {
                ButtonContent(
                    text = text,
                    icon = icon,
                    iconPainter = iconPainter,
                    loading = loading,
                    textStyle = textStyle,
                    size = size
                )
            }
        }
        
        ButtonVariant.DESTRUCTIVE -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MooneyDesignSystem.Colors.Error,
                    contentColor = MooneyDesignSystem.Colors.OnPrimary,
                    disabledContainerColor = MooneyDesignSystem.Colors.Disabled,
                    disabledContentColor = MooneyDesignSystem.Colors.OnPrimary.copy(alpha = 0.6f)
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource
            ) {
                ButtonContent(
                    text = text,
                    icon = icon,
                    iconPainter = iconPainter,
                    loading = loading,
                    textStyle = textStyle,
                    size = size
                )
            }
        }
        
        ButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier.height(height),
                enabled = enabled && !loading,
                shape = MooneyDesignSystem.Shapes.button,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MooneyDesignSystem.Colors.Primary.copy(alpha = 0.12f),
                    contentColor = MooneyDesignSystem.Colors.Primary,
                    disabledContainerColor = MooneyDesignSystem.Colors.Disabled.copy(alpha = 0.12f),
                    disabledContentColor = MooneyDesignSystem.Colors.Disabled
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource
            ) {
                ButtonContent(
                    text = text,
                    icon = icon,
                    iconPainter = iconPainter,
                    loading = loading,
                    textStyle = textStyle,
                    size = size
                )
            }
        }
    }
}

/**
 * Content inside the button (icon + text + loading)
 */
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
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = LocalContentColor.current,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = text,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Floating Action Button for primary actions
 */
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
            containerColor = MooneyDesignSystem.Colors.Primary,
            contentColor = MooneyDesignSystem.Colors.OnPrimary
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MooneyDesignSystem.Typography.LabelLarge
            )
        }
    } else {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = MooneyDesignSystem.Shapes.large,
            containerColor = MooneyDesignSystem.Colors.Primary,
            contentColor = MooneyDesignSystem.Colors.OnPrimary
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Icon button for toolbar actions and compact interactions
 */
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
            tint = if (enabled) tint else MooneyDesignSystem.Colors.Disabled
        )
    }
}