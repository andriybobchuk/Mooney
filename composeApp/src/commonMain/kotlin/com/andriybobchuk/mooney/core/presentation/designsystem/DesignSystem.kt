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
 * Clean, minimal black/white design with blue accent (Revolut-inspired)
 * Font: Poppins (applied via MaterialTheme typography)
 */
object MooneyDesignSystem {

    object Colors {
        // Primary — black CTAs in light, white in dark
        val Primary = Color(0xFF111111)
        val PrimaryVariant = Color(0xFF333333)
        val OnPrimary = Color.White

        // Accent — Revolut blue (light), teal/cyan (dark)
        val Accent = Color(0xFF3562F6)
        val AccentDark = Color(0xFF4DD0C8)

        // Semantic
        val Success = Color(0xFF16A34A)
        val Warning = Color(0xFFEA580C)
        val Error = Color(0xFFDC2626)
        val Info = Color(0xFF3562F6)

        // Surfaces
        val Background = Color(0xFFFFFFFF)
        val Surface = Color(0xFFFFFFFF)
        val SurfaceVariant = Color(0xFFF0F1F3)
        val BackgroundDark = Color(0xFF0D1117)
        val SurfaceDark = Color(0xFF111518)
        val SurfaceVariantDark = Color(0xFF1C2025)

        // Text
        val OnBackground = Color(0xFF111111)
        val OnSurface = Color(0xFF111111)
        val OnBackgroundDark = Color(0xFFF0F0F0)
        val OnSurfaceDark = Color(0xFFF0F0F0)
        val Secondary = Color(0xFF6B7280)
        val SecondaryDark = Color(0xFF9CA3AF)

        // Borders
        val Divider = Color(0xFFD1D5DB)
        val DividerDark = Color(0xFF374151)
        val Disabled = Color(0xFF9CA3AF)
        val DisabledDark = Color(0xFF4B5563)

        // Finance
        val Income = Color(0xFF16A34A)
        val Expense = Color(0xFFDC2626)
        val Transfer = Color(0xFF3562F6)
    }

    object Typography {
        val DisplayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp)
        val DisplayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        val DisplaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
        val HeadlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
        val HeadlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
        val HeadlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
        val TitleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        val TitleMedium = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
        val TitleSmall = TextStyle(fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        val BodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
        val BodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
        val BodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
        val LabelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        val LabelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        val LabelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        val AmountLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
        val AmountMedium = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        val AmountSmall = TextStyle(fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    }

    object Spacing {
        val xxs: Dp = 4.dp
        val xs: Dp = 8.dp
        val sm: Dp = 12.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
        val xxxl: Dp = 64.dp
        val buttonPadding = 16.dp
        val cardPadding = 16.dp
        val listItemPadding = 12.dp
        val screenPadding = 16.dp
        val sectionSpacing = 24.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val level1: Dp = 1.dp
        val level2: Dp = 3.dp
        val level3: Dp = 6.dp
        val level4: Dp = 8.dp
        val level5: Dp = 12.dp
        val card: Dp = 0.dp       // Flat cards, no shadow
        val button: Dp = 0.dp     // Flat buttons
        val dialog: Dp = 24.dp
        val bottomSheet: Dp = 16.dp
        val fab: Dp = 6.dp
    }

    object Shapes {
        val none = RoundedCornerShape(0.dp)
        val extraSmall = RoundedCornerShape(4.dp)
        val small = RoundedCornerShape(8.dp)
        val medium = RoundedCornerShape(12.dp)
        val large = RoundedCornerShape(16.dp)
        val extraLarge = RoundedCornerShape(20.dp)
        val full = RoundedCornerShape(100)
        val button = medium
        val card = large
        val dialog = extraLarge
        val textField = medium
        val chip = full
        val bottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    }

    object Animation {
        const val FastDuration = 150
        const val MediumDuration = 300
        const val SlowDuration = 500
        const val FadeIn = 200
        const val FadeOut = 150
        const val Expand = 300
        const val Collapse = 250
        const val PageTransition = 350
    }

    object IconSize {
        val small: Dp = 16.dp
        val medium: Dp = 24.dp
        val large: Dp = 32.dp
        val extraLarge: Dp = 48.dp
        val touchTarget: Dp = 48.dp
    }

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
