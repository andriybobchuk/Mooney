package com.andriybobchuk.mooney.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.components.DevLabel
import com.andriybobchuk.mooney.core.presentation.designsystem.components.DevToolsBottomSheet
import com.andriybobchuk.mooney.core.presentation.theme.AppColorsExtended
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.core.presentation.theme.MooneyTypography
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import com.andriybobchuk.mooney.core.presentation.theme.getAppColorsForTheme
import com.andriybobchuk.mooney.core.presentation.theme.getColorSchemeForTheme
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import com.andriybobchuk.mooney.mooney.domain.devtools.DevToolsManager
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

val LocalAppColors = staticCompositionLocalOf<AppColorsExtended> {
    error("No AppColors provided")
}

val MaterialTheme.appColors: AppColorsExtended
    @Composable
    get() = LocalAppColors.current

@Composable
@Preview
fun App() {
    val themeManager: ThemeManager = koinInject()
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val appTheme by themeManager.currentAppTheme.collectAsState(initial = AppTheme.BLUE)
    val systemInDarkTheme = isSystemInDarkTheme()

    val isDarkMode = themeManager.isSystemInDarkTheme(systemInDarkTheme, themeMode)
    val colorScheme = getColorSchemeForTheme(appTheme, isDarkMode)
    val appColors = getAppColorsForTheme(appTheme, isDarkMode)
    val typography = MooneyTypography()

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavigationHost()

                // DEV overlay — only in debug builds
                if (FeatureFlags.isDebug) {
                    var showDevTools by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        DevLabel(onClick = { showDevTools = true })
                    }

                    if (showDevTools) {
                        val devToolsManager: DevToolsManager = koinInject()
                        DevToolsBottomSheet(
                            devToolsManager = devToolsManager,
                            onDismiss = { showDevTools = false }
                        )
                    }
                }
            }
        }
    }
}
