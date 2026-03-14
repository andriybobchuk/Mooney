package com.andriybobchuk.mooney.core.presentation.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mooney Design System
 * Based on iOS Human Interface Guidelines and Material3 patterns
 * Optimized for financial applications
 */
object MooneyDesignSystem {
    
    /**
     * Color Palette
     * Modern fintech colors inspired by Revolut, Spotify, and iOS design
     */
    object Colors {
        // Primary Brand Colors - Modern Blue Theme
        val Primary = Color(0xFF0066FF)          // Vibrant modern blue (like Revolut)
        val PrimaryVariant = Color(0xFF0052CC)   // Darker blue for emphasis
        val Secondary = Color(0xFF00D4FF)        // Bright cyan accent
        val SecondaryVariant = Color(0xFF00A8CC) // Medium cyan
        
        // Semantic Colors for Finance
        val Success = Color(0xFF34C759)          // iOS Green - Income, positive
        val Warning = Color(0xFFFF9500)          // iOS Orange - Warnings
        val Error = Color(0xFFFF3B30)            // iOS Red - Expenses, negative
        val Info = Color(0xFF5AC8FA)             // iOS Light Blue - Information
        
        // Surface Colors
        val Background = Color(0xFFF2F2F7)       // iOS System Background (Light)
        val Surface = Color(0xFFFFFFFF)          // Card/Component background
        val SurfaceVariant = Color(0xFFF9F9FB)   // Subtle variant for sections
        
        // Dark Mode Colors
        val BackgroundDark = Color(0xFF000000)   // Pure black for OLED
        val SurfaceDark = Color(0xFF1C1C1E)      // iOS Dark Surface
        val SurfaceVariantDark = Color(0xFF2C2C2E) // Dark variant
        
        // Text Colors
        val OnPrimary = Color(0xFFFFFFFF)
        val OnSecondary = Color(0xFFFFFFFF)
        val OnBackground = Color(0xFF000000)
        val OnSurface = Color(0xFF000000)
        val OnBackgroundDark = Color(0xFFFFFFFF)
        val OnSurfaceDark = Color(0xFFFFFFFF)
        
        // Additional UI Colors
        val Divider = Color(0xFFE5E5EA)          // iOS Separator
        val DividerDark = Color(0xFF38383A)      // Dark mode separator
        val Disabled = Color(0xFFAEAEB2)         // Disabled state
        val DisabledDark = Color(0xFF636366)     // Dark mode disabled
        
        // Finance-specific colors
        val Income = Color(0xFF34C759)           // Green for income
        val Expense = Color(0xFFFF3B30)          // Red for expenses
        val Transfer = Color(0xFF007AFF)         // Blue for transfers
        val Investment = Color(0xFF5856D6)       // Purple for investments
        val Savings = Color(0xFFFF9500)          // Orange for savings goals
    }
    
    /**
     * Typography System
     * Modern bold typography inspired by Spotify and Revolut
     */
    object Typography {
        // Display styles - for big numbers and headers (Bold like Spotify)
        val DisplayLarge = TextStyle(
            fontSize = 57.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp
        )
        
        val DisplayMedium = TextStyle(
            fontSize = 45.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        
        val DisplaySmall = TextStyle(
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        
        // Headlines - for sections and cards (Bold like Revolut)
        val HeadlineLarge = TextStyle(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        
        val HeadlineMedium = TextStyle(
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp
        )
        
        val HeadlineSmall = TextStyle(
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        
        // Titles - for lists and components
        val TitleLarge = TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        val TitleMedium = TextStyle(
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp
        )
        
        val TitleSmall = TextStyle(
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )
        
        // Body text - for general content
        val BodyLarge = TextStyle(
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
        
        val BodyMedium = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp
        )
        
        val BodySmall = TextStyle(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp
        )
        
        // Labels - for buttons and inputs
        val LabelLarge = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )
        
        val LabelMedium = TextStyle(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        
        val LabelSmall = TextStyle(
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        
        // Special styles for finance
        val AmountLarge = TextStyle(
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        
        val AmountMedium = TextStyle(
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
        
        val AmountSmall = TextStyle(
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
    }
    
    /**
     * Spacing System
     * Consistent spacing based on 4dp grid
     */
    object Spacing {
        val xxs: Dp = 4.dp
        val xs: Dp = 8.dp
        val sm: Dp = 12.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
        val xxxl: Dp = 64.dp
        
        // Specific spacing for components
        val buttonPadding = 16.dp
        val cardPadding = 16.dp
        val listItemPadding = 12.dp
        val screenPadding = 16.dp
        val sectionSpacing = 24.dp
    }
    
    /**
     * Elevation System
     * Material3 elevation levels
     */
    object Elevation {
        val none: Dp = 0.dp
        val level1: Dp = 1.dp
        val level2: Dp = 3.dp
        val level3: Dp = 6.dp
        val level4: Dp = 8.dp
        val level5: Dp = 12.dp
        
        // Component-specific elevations
        val card: Dp = 2.dp
        val button: Dp = 2.dp
        val dialog: Dp = 24.dp
        val bottomSheet: Dp = 16.dp
        val fab: Dp = 6.dp
    }
    
    /**
     * Shape System
     * Rounded corners for modern look
     */
    object Shapes {
        val none = RoundedCornerShape(0.dp)
        val extraSmall = RoundedCornerShape(4.dp)
        val small = RoundedCornerShape(8.dp)
        val medium = RoundedCornerShape(12.dp)
        val large = RoundedCornerShape(16.dp)
        val extraLarge = RoundedCornerShape(20.dp)
        val full = RoundedCornerShape(100)
        
        // Component-specific shapes
        val button = medium
        val card = large
        val dialog = extraLarge
        val textField = medium
        val chip = full
        val bottomSheet = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    }
    
    /**
     * Animation Durations
     * Consistent timing for animations
     */
    object Animation {
        const val FastDuration = 150
        const val MediumDuration = 300
        const val SlowDuration = 500
        
        // Specific animation timings
        const val FadeIn = 200
        const val FadeOut = 150
        const val Expand = 300
        const val Collapse = 250
        const val PageTransition = 350
    }
    
    /**
     * Icon Sizes
     * Standardized icon dimensions
     */
    object IconSize {
        val small: Dp = 16.dp
        val medium: Dp = 24.dp
        val large: Dp = 32.dp
        val extraLarge: Dp = 48.dp
        
        // Touch target minimum
        val touchTarget: Dp = 48.dp
    }
    
    /**
     * Component Heights
     * Standard heights for UI components
     */
    object ComponentHeight {
        val button: Dp = 48.dp
        val buttonSmall: Dp = 36.dp
        val buttonLarge: Dp = 56.dp
        
        val textField: Dp = 56.dp
        val listItem: Dp = 56.dp
        val listItemSmall: Dp = 48.dp
        val listItemLarge: Dp = 72.dp
        
        val appBar: Dp = 56.dp
        val bottomBar: Dp = 80.dp
        val tab: Dp = 48.dp
        
        val chip: Dp = 32.dp
    }
}