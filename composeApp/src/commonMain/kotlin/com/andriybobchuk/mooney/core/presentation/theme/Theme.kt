package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

// Custom App Color Compositions
data class CustomAppColors(
    val cardBackground: androidx.compose.ui.graphics.Color,
    val pillBackground: androidx.compose.ui.graphics.Color,
    val pillBackgroundSecondary: androidx.compose.ui.graphics.Color,
    val incomeColor: androidx.compose.ui.graphics.Color,
    val expenseColor: androidx.compose.ui.graphics.Color,
    val transactionIcon: androidx.compose.ui.graphics.Color,
    val divider: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
)

val LocalAppColors = staticCompositionLocalOf {
    CustomAppColors(
        cardBackground = AppColors.Light.cardBackground,
        pillBackground = AppColors.Light.pillBackground,
        pillBackgroundSecondary = AppColors.Light.pillBackgroundSecondary,
        incomeColor = AppColors.Light.incomeColor,
        expenseColor = AppColors.Light.expenseColor,
        transactionIcon = AppColors.Light.transactionIcon,
        divider = AppColors.Light.divider,
        success = AppColors.Light.success,
        warning = AppColors.Light.warning,
        error = AppColors.Light.error,
    )
}

@Composable
fun MooneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val customColors = if (darkTheme) {
        CustomAppColors(
            cardBackground = AppColors.Dark.cardBackground,
            pillBackground = AppColors.Dark.pillBackground,
            pillBackgroundSecondary = AppColors.Dark.pillBackgroundSecondary,
            incomeColor = AppColors.Dark.incomeColor,
            expenseColor = AppColors.Dark.expenseColor,
            transactionIcon = AppColors.Dark.transactionIcon,
            divider = AppColors.Dark.divider,
            success = AppColors.Dark.success,
            warning = AppColors.Dark.warning,
            error = AppColors.Dark.error,
        )
    } else {
        CustomAppColors(
            cardBackground = AppColors.Light.cardBackground,
            pillBackground = AppColors.Light.pillBackground,
            pillBackgroundSecondary = AppColors.Light.pillBackgroundSecondary,
            incomeColor = AppColors.Light.incomeColor,
            expenseColor = AppColors.Light.expenseColor,
            transactionIcon = AppColors.Light.transactionIcon,
            divider = AppColors.Light.divider,
            success = AppColors.Light.success,
            warning = AppColors.Light.warning,
            error = AppColors.Light.error,
        )
    }

    CompositionLocalProvider(LocalAppColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension property to access custom colors easily
val MaterialTheme.appColors: CustomAppColors
    @Composable
    get() = LocalAppColors.current