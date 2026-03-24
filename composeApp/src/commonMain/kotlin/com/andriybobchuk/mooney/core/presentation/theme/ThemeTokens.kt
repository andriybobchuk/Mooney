package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Theme variant — kept for compatibility but only BLUE is used
enum class AppTheme {
    BLUE,
    PURPLE,
    MINIMAL
}

// ── Light Color Scheme ─────────────────────────────────────────────
val MooneyLightColorScheme = lightColorScheme(
    primary = Color(0xFF111111),             // Black — primary CTAs
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF0F2),    // Light grey container
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF3562F6),           // Revolut-style blue accent
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDFE),  // Light blue container
    onSecondaryContainer = Color(0xFF1A3399),
    tertiary = Color(0xFF3562F6),
    onTertiary = Color.White,
    surface = Color(0xFFFFFFFF),             // Pure white
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF0F1F3),      // Light grey for cards/inputs
    onSurfaceVariant = Color(0xFF6B7280),    // Medium grey text
    background = Color(0xFFFFFFFF),          // White background
    onBackground = Color(0xFF111111),
    outline = Color(0xFFD1D5DB),             // Subtle border grey
    outlineVariant = Color(0xFFE5E7EB),      // Lighter border
    error = Color(0xFFDC2626),               // Red
    onError = Color.White,
    inverseSurface = Color(0xFF111111),
    inverseOnSurface = Color.White,
)

// ── Dark Color Scheme ──────────────────────────────────────────────
val MooneyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),             // White — primary CTAs
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF1E2328),    // Dark grey container
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFF4DD0C8),           // Teal/cyan accent
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF1A3A37),  // Dark teal container
    onSecondaryContainer = Color(0xFF4DD0C8),
    tertiary = Color(0xFF4DD0C8),
    onTertiary = Color(0xFF111111),
    surface = Color(0xFF111518),             // Very dark surface
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1C2025),      // Slightly lighter dark for cards
    onSurfaceVariant = Color(0xFF9CA3AF),    // Medium grey text
    background = Color(0xFF0D1117),          // Near-black with teal tint
    onBackground = Color(0xFFF0F0F0),
    outline = Color(0xFF374151),             // Dark border
    outlineVariant = Color(0xFF1F2937),      // Darker border
    error = Color(0xFFEF4444),               // Bright red
    onError = Color.White,
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF111111),
)

// Extended app colors for finance-specific use
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

// All variants resolve to the single Mooney scheme
@Composable
fun getColorSchemeForTheme(theme: AppTheme, isSystemDarkMode: Boolean): ColorScheme {
    return if (isSystemDarkMode) MooneyDarkColorScheme else MooneyLightColorScheme
}

@Composable
fun getAppColorsForTheme(theme: AppTheme, isSystemDarkMode: Boolean): AppColorsExtended {
    return if (isSystemDarkMode) DarkAppColors else LightAppColors
}
