package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.ui.graphics.Color

// Brand Colors
val Primary = Color(0xFF3E4DBA)
val PrimaryVariant = Color(0xFF2C3A99)
val Secondary = Color(0xFF1976D2)
val SecondaryVariant = Color(0xFF115293)

// Semantic Colors
val Income = Color(0xFF409261)
val IncomeVariant = Color(0xFF357A54)
val Expense = Color(0xFFE53E3E)
val ExpenseVariant = Color(0xFFD53838)

// Light Theme Colors
val LightPrimary = Primary
val LightOnPrimary = Color.White
val LightPrimaryContainer = Color(0xFFE3F2FD)
val LightOnPrimaryContainer = Color(0xFF0D47A1)

val LightSecondary = Secondary
val LightOnSecondary = Color.White
val LightSecondaryContainer = Color(0xFFE1F5FE)
val LightOnSecondaryContainer = Color(0xFF01579B)

val LightBackground = Color.White
val LightOnBackground = Color(0xFF1C1C1E)
val LightSurface = Color.White
val LightOnSurface = Color(0xFF1C1C1E)
val LightSurfaceVariant = Color(0xFFF8F9FF)
val LightOnSurfaceVariant = Color(0xFF6C757D)

val LightOutline = Color(0xFFE5E5E7)
val LightOutlineVariant = Color(0xFFF2F2F7)

// Dark Theme Colors
val DarkPrimary = Color(0xFF5A68D4)
val DarkOnPrimary = Color(0xFF000051)
val DarkPrimaryContainer = Color(0xFF2C3A99)
val DarkOnPrimaryContainer = Color(0xFFE3F2FD)

val DarkSecondary = Color(0xFF64B5F6)
val DarkOnSecondary = Color(0xFF01579B)
val DarkSecondaryContainer = Color(0xFF1565C0)
val DarkOnSecondaryContainer = Color(0xFFE1F5FE)

val DarkBackground = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE5E5E7)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE5E5E7)
val DarkSurfaceVariant = Color(0xFF2A2A2E)
val DarkOnSurfaceVariant = Color(0xFF98989D)

val DarkOutline = Color(0xFF3A3A3C)
val DarkOutlineVariant = Color(0xFF2C2C2E)

// Custom App Colors for Both Themes
object AppColors {
    // Light Theme
    object Light {
        val cardBackground = LightSurfaceVariant
        val pillBackground = Color.White.copy(alpha = 0.5f)
        val pillBackgroundSecondary = LightPrimaryContainer
        val incomeColor = Income
        val expenseColor = Color(0xFF495057)
        val transactionIcon = Color(0xFFF8F9FF)
        val divider = LightOutline
        val success = Income
        val warning = Color(0xFFFF8F00)
        val error = Color(0xFFD32F2F)
    }
    
    // Dark Theme
    object Dark {
        val cardBackground = Color(0xFF2A2A2E)
        val pillBackground = Color(0xFF3A3A3C)
        val pillBackgroundSecondary = DarkSecondaryContainer
        val incomeColor = Color(0xFF66BB6A)
        val expenseColor = Color(0xFFE0E0E0)
        val transactionIcon = Color(0xFF3A3A3C)
        val divider = DarkOutline
        val success = Color(0xFF4CAF50)
        val warning = Color(0xFFFFB74D)
        val error = Color(0xFFEF5350)
    }
}