package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Theme variant — kept for compatibility, only BLUE is actively used
enum class AppTheme {
    BLUE,
    PURPLE,
    MINIMAL
}

// ── Light Color Scheme ─────────────────────────────────────────────
val MooneyLightColorScheme = lightColorScheme(
    // Primary = blue accent (used by M3 for FABs, nav indicators, checkboxes, focus)
    primary = Color(0xFF3562F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDFE),
    onPrimaryContainer = Color(0xFF1A3399),

    secondary = Color(0xFF3562F6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDFE),
    onSecondaryContainer = Color(0xFF1A3399),

    tertiary = Color(0xFF3562F6),
    onTertiary = Color.White,

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF6B7280),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),

    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),

    error = Color(0xFFDC2626),
    onError = Color.White,

    // inverseSurface = black — used for primary CTA buttons
    inverseSurface = Color(0xFF111111),
    inverseOnSurface = Color.White,
)

// ── Dark Color Scheme ──────────────────────────────────────────────
val MooneyDarkColorScheme = darkColorScheme(
    // Primary = teal/cyan accent
    primary = Color(0xFF4DD0C8),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF1A3A37),
    onPrimaryContainer = Color(0xFF4DD0C8),

    secondary = Color(0xFF4DD0C8),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF1A3A37),
    onSecondaryContainer = Color(0xFF4DD0C8),

    tertiary = Color(0xFF4DD0C8),
    onTertiary = Color(0xFF003733),

    surface = Color(0xFF111518),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1C2025),
    onSurfaceVariant = Color(0xFF9CA3AF),

    background = Color(0xFF0D1117),
    onBackground = Color(0xFFF0F0F0),

    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),

    error = Color(0xFFEF4444),
    onError = Color.White,

    // inverseSurface = white — used for primary CTA buttons
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF111111),
)

// Extended app colors
@Stable
data class AppColorsExtended(
    val incomeColor: Color,
    val expenseColor: Color,
    val cardBackground: Color,
    val pillBackground: Color,
    val pillBackgroundSecondary: Color,
    val transactionIcon: Color,
    val accent: Color,
    val accentContainer: Color,
)

val LightAppColors = AppColorsExtended(
    incomeColor = Color(0xFF16A34A),
    expenseColor = Color(0xFF374151),
    cardBackground = Color(0xFFF0F1F3),
    pillBackground = Color(0xFFF0F1F3),
    pillBackgroundSecondary = Color(0xFFE8EDFE),
    transactionIcon = Color(0xFFF0F1F3),
    accent = Color(0xFF3562F6),
    accentContainer = Color(0xFFE8EDFE),
)

val DarkAppColors = AppColorsExtended(
    incomeColor = Color(0xFF4ADE80),
    expenseColor = Color(0xFFD1D5DB),
    cardBackground = Color(0xFF1C2025),
    pillBackground = Color(0xFF1C2025),
    pillBackgroundSecondary = Color(0xFF1A3A37),
    transactionIcon = Color(0xFF1C2025),
    accent = Color(0xFF4DD0C8),
    accentContainer = Color(0xFF1A3A37),
)

@Composable
fun getColorSchemeForTheme(theme: AppTheme, isSystemDarkMode: Boolean): ColorScheme {
    return if (isSystemDarkMode) MooneyDarkColorScheme else MooneyLightColorScheme
}

@Composable
fun getAppColorsForTheme(theme: AppTheme, isSystemDarkMode: Boolean): AppColorsExtended {
    return if (isSystemDarkMode) DarkAppColors else LightAppColors
}
