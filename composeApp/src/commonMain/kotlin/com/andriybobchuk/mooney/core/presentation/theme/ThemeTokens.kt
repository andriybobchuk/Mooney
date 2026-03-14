package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Color Tokens
object ColorTokens {
    // Modern Blue Theme Colors (like Revolut/Figma)
    object Blue {
        val primary50 = Color(0xFFE6F0FF)
        val primary100 = Color(0xFFB3D4FF)
        val primary200 = Color(0xFF80B8FF)
        val primary300 = Color(0xFF4D9CFF)
        val primary400 = Color(0xFF1A80FF)
        val primary500 = Color(0xFF0066FF)  // Main brand blue
        val primary600 = Color(0xFF0052CC)
        val primary700 = Color(0xFF003D99)
        val primary800 = Color(0xFF002966)
        val primary900 = Color(0xFF001433)
    }
    
    
    // Common semantic colors
    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFF9800)
    val info = Color(0xFF2196F3)
}

// Theme Variants
enum class AppTheme {
    BLUE,      // New modern blue theme
    PURPLE,    // Legacy purple theme
    MINIMAL
}

// Modern Blue Theme (Revolut/Figma inspired)
val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF),  // Vibrant blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F0FF),  // Light blue container
    onPrimaryContainer = Color(0xFF001433),
    secondary = Color(0xFF00D4FF),  // Cyan accent
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE0F7FF),
    onSecondaryContainer = Color(0xFF001F2A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF8FAFB),  // Very light gray for cards
    onSurfaceVariant = Color(0xFF606060),
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF1A1A1A),
    outline = Color(0xFFE1E4E8),
    outlineVariant = Color(0xFFF0F2F5),
    error = Color(0xFFFF3B30),  // iOS red
    onError = Color.White
)

val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4D9CFF),  // Brighter blue for dark mode
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF0052CC),
    onPrimaryContainer = Color(0xFFE6F0FF),
    secondary = Color(0xFF00D4FF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00A8CC),
    onSecondaryContainer = Color(0xFFE0F7FF),
    surface = Color(0xFF0A0A0A),  // Near black surface
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1A1A1A),  // Dark gray for panels
    onSurfaceVariant = Color(0xFFB0B0B0),
    background = Color(0xFF000000),  // Pure black for OLED
    onBackground = Color(0xFFF0F0F0),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF1A1A1A),
    error = Color(0xFFFF453A),  // iOS red dark
    onError = Color.Black
)

// Original Purple Theme (legacy)
val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF512DA8),  // Deep purple for light mode
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),  // Light purple container
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF7E57C2),  // Medium purple accent
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1BEE7),
    onSecondaryContainer = Color(0xFF311B92),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF8F8F8),
    onSurfaceVariant = Color(0xFF616161),
    background = Color.White,
    onBackground = Color(0xFF212121),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C4DFF),  // Deep material purple
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF651FFF),  // Deeper indigo purple
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF9575CD),  // Muted purple
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF7C4DFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),  // Pure black surface
    onSurface = Color(0xFFE0E0E0),  // Slightly softer white
    surfaceVariant = Color(0xFF0A0A0A),  // Very dark gray for panels
    onSurfaceVariant = Color(0xFFA0A0A0),  // Light gray for secondary text
    background = Color(0xFF000000),  // Pure black background
    onBackground = Color(0xFFE0E0E0),  // Slightly softer white
    outline = Color(0xFF2A2A2A),  // Dark gray outline
    outlineVariant = Color(0xFF1A1A1A),  // Very dark outline
    error = Color(0xFFFF5252),
    onError = Color.Black
)


// Minimal Theme with gray topbars and subtle accent
val MinimalLightColorScheme = lightColorScheme(
    primary = Color(0xFF6C757D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAFD),
    onPrimaryContainer = Color(0xFF373F7F),
    secondary = Color(0xFF5A6ACF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAFD),
    onSecondaryContainer = Color(0xFF373F7F),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF8F9FA),
    onSurfaceVariant = Color(0xFF6C757D),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    outline = Color(0xFFE1E4E8),
    outlineVariant = Color(0xFFF1F3F4),
    error = Color(0xFFDC3545),
    onError = Color.White
)

val MinimalDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADB5BD),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF4C5282),
    onPrimaryContainer = Color(0xFFE8EAFD),
    secondary = Color(0xFF7C8CF0),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF4C5282),
    onSecondaryContainer = Color(0xFFE8EAFD),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFADB5BD),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF0F0F0),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF181818),
    error = Color(0xFFFF6B6B),
    onError = Color.Black
)

// Helper function to determine if it's currently night time
@Composable
fun isNightTime(): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.hour
    return hour < 6 || hour >= 18  // Consider 6 PM to 6 AM as night time
}

// Theme resolver function
@Composable
fun getColorSchemeForTheme(theme: AppTheme, isSystemDarkMode: Boolean): ColorScheme {
    return when (theme) {
        AppTheme.BLUE -> if (isSystemDarkMode) BlueDarkColorScheme else BlueLightColorScheme
        AppTheme.PURPLE -> if (isSystemDarkMode) PurpleDarkColorScheme else PurpleLightColorScheme
        AppTheme.MINIMAL -> if (isSystemDarkMode) MinimalDarkColorScheme else MinimalLightColorScheme
    }
}

// Extended Material3 color properties for custom app colors
@Stable
data class AppColorsExtended(
    val incomeColor: Color,
    val expenseColor: Color,
    val cardBackground: Color,
    val pillBackground: Color,
    val pillBackgroundSecondary: Color,
    val transactionIcon: Color
)

// Modern Blue Theme App Colors
val BlueLightAppColors = AppColorsExtended(
    incomeColor = Color(0xFF00C853),  // Vibrant green for income
    expenseColor = Color(0xFF424242),  // Dark gray for expenses
    cardBackground = Color(0xFFF8FAFB),  // Light gray for cards
    pillBackground = Color(0xFFE1E4E8).copy(alpha = 0.6f),
    pillBackgroundSecondary = Color(0xFF0066FF).copy(alpha = 0.1f),  // Light blue pill
    transactionIcon = Color(0xFFF8FAFB)  // Match card background
)

val BlueDarkAppColors = AppColorsExtended(
    incomeColor = Color(0xFF00E676),  // Bright green for income
    expenseColor = Color(0xFFE0E0E0),  // Light gray for expense text
    cardBackground = Color(0xFF1A1A1A),  // Dark gray for cards
    pillBackground = Color(0xFF2A2A2A).copy(alpha = 0.8f),
    pillBackgroundSecondary = Color(0xFF4D9CFF).copy(alpha = 0.15f),  // Subtle blue pill
    transactionIcon = Color(0xFF1A1A1A)  // Match card background
)

// Original Purple Theme App Colors
val PurpleLightAppColors = AppColorsExtended(
    incomeColor = Color(0xFF2E7D32),  // Proper green for income
    expenseColor = Color(0xFF424242),  // Dark gray for expenses
    cardBackground = Color(0xFFF8F8F8),  // Very light gray for cards
    pillBackground = Color(0xFFE0E0E0).copy(alpha = 0.6f),
    pillBackgroundSecondary = Color(0xFF512DA8).copy(alpha = 0.1f),  // Light deep purple pill
    transactionIcon = Color(0xFFF8F8F8)  // Match card background
)

val PurpleDarkAppColors = AppColorsExtended(
    incomeColor = Color(0xFF4CAF50),  // Proper green for income
    expenseColor = Color(0xFFE0E0E0),  // Soft white for expense text
    cardBackground = Color(0xFF0A0A0A),  // Very dark gray for cards
    pillBackground = Color(0xFF1A1A1A).copy(alpha = 0.8f),  // Dark pill background
    pillBackgroundSecondary = Color(0xFF7C4DFF).copy(alpha = 0.15f),  // Subtle deep purple pill
    transactionIcon = Color(0xFF0A0A0A)  // Match card background
)

// Minimal Theme App Colors
val MinimalLightAppColors = AppColorsExtended(
    incomeColor = Color(0xFF28A745),
    expenseColor = Color(0xFF666666),
    cardBackground = Color(0xFFFCFCFC),
    pillBackground = Color(0xFFFFFFFF).copy(alpha = 0.25f),
    pillBackgroundSecondary = Color(0xFF5A6ACF).copy(alpha = 0.1f),
    transactionIcon = Color(0xFFF8F9FA)
)

val MinimalDarkAppColors = AppColorsExtended(
    incomeColor = Color(0xFF40C463),
    expenseColor = Color(0xFFADB5BD),
    cardBackground = Color(0xFF0A0A0A),
    pillBackground = Color(0xFF000000).copy(alpha = 0.4f),
    pillBackgroundSecondary = Color(0xFF7C8CF0).copy(alpha = 0.15f),
    transactionIcon = Color(0xFF181818)
)

@Composable
fun getAppColorsForTheme(theme: AppTheme, isSystemDarkMode: Boolean): AppColorsExtended {
    return when (theme) {
        AppTheme.BLUE -> if (isSystemDarkMode) BlueDarkAppColors else BlueLightAppColors
        AppTheme.PURPLE -> if (isSystemDarkMode) PurpleDarkAppColors else PurpleLightAppColors
        AppTheme.MINIMAL -> if (isSystemDarkMode) MinimalDarkAppColors else MinimalLightAppColors
    }
}