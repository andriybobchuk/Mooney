package com.andriybobchuk.mooney.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.core.presentation.designsystem.components.DevLabel
import com.andriybobchuk.mooney.core.presentation.designsystem.components.DevToolsBottomSheet
import com.andriybobchuk.mooney.core.presentation.theme.AppColorsExtended
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.core.presentation.theme.MooneyTypography
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import com.andriybobchuk.mooney.core.presentation.theme.getAppColorsForTheme
import com.andriybobchuk.mooney.core.presentation.theme.getColorSchemeForTheme
import com.andriybobchuk.mooney.core.testing.WithTestTagsAsResourceId
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import com.andriybobchuk.mooney.mooney.domain.devtools.DevToolsManager
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import androidx.compose.runtime.LaunchedEffect
import com.andriybobchuk.mooney.mooney.domain.usecase.ReportCategoryUsageUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SyncDefaultCategoriesUseCase
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
    val syncDefaults: SyncDefaultCategoriesUseCase = koinInject()
    val reportUsage: ReportCategoryUsageUseCase = koinInject()
    val requestReview: com.andriybobchuk.mooney.core.review.RequestReviewUseCase = koinInject()
    LaunchedEffect(Unit) {
        syncDefaults()
        reportUsage()
        // Tracks install date + open count — gates the review prompt later on.
        requestReview.recordAppOpen()
    }

    val themeManager: ThemeManager = koinInject()
    val startupPrefs: StartupPrefs = koinInject()
    // Synchronously seed the theme so the first composition picks the user's
    // chosen mode instead of always flashing SYSTEM until DataStore lands.
    val initialThemeMode = remember {
        startupPrefs.getThemeMode()
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }
    val themeMode by themeManager.themeMode.collectAsState(initial = initialThemeMode)
    val appTheme by themeManager.currentAppTheme.collectAsState(initial = AppTheme.BLUE)
    val systemInDarkTheme = isSystemInDarkTheme()

    val isDarkMode = themeManager.isSystemInDarkTheme(systemInDarkTheme, themeMode)
    val colorScheme = getColorSchemeForTheme(appTheme, isDarkMode)
    val appColors = getAppColorsForTheme(appTheme, isDarkMode)
    val typography = MooneyTypography()

    val layoutDirection = if (com.andriybobchuk.mooney.core.presentation.locale.isCurrentLocaleRtl()) {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalAppColors provides appColors,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            WithTestTagsAsResourceId {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Cap content width on tablets / iPad so screens designed for
                // a phone don't stretch and get cut off at the edges. iPhone
                // viewports stay full-width because widthIn only enforces the
                // upper bound. `fillMaxSize` ensures the inner column actually
                // grabs the full available width up to the cap.
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                ) {
                    val appLockManager: com.andriybobchuk.mooney.core.security.AppLockManager = koinInject()
                    var locked by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        locked = appLockManager.isLockEnabledNow()
                    }
                    when (locked) {
                        null -> Unit // brief gate while we check the prefs
                        true -> com.andriybobchuk.mooney.core.security.AppLockUnlockScreen(
                            manager = appLockManager,
                            onUnlocked = { locked = false }
                        )
                        false -> NavigationHost()
                    }
                }

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
}
