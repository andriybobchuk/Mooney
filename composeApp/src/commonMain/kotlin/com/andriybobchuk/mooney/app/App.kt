package com.andriybobchuk.mooney.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.andriybobchuk.mooney.core.presentation.theme.AppColorsExtended
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import com.andriybobchuk.mooney.core.presentation.theme.getAppColorsForTheme
import com.andriybobchuk.mooney.core.presentation.theme.getColorSchemeForTheme
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

val LocalAppColors = staticCompositionLocalOf<AppColorsExtended> {
    error("No AppColors provided")
}

// Extension property to access custom colors easily
val MaterialTheme.appColors: AppColorsExtended
    @Composable
    get() = LocalAppColors.current

@Composable
@Preview
fun App() {
    val themeManager: ThemeManager = koinInject()
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val appTheme by themeManager.currentAppTheme.collectAsState(initial = AppTheme.PURPLE)
    val systemInDarkTheme = isSystemInDarkTheme()
    
    val isDarkMode = themeManager.isSystemInDarkTheme(systemInDarkTheme, themeMode)
    val colorScheme = getColorSchemeForTheme(appTheme, isDarkMode)
    val appColors = getAppColorsForTheme(appTheme, isDarkMode)
    
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            NavigationHost()
        }
    }
}

