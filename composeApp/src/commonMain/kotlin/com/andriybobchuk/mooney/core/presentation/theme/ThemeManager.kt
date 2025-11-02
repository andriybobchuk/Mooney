package com.andriybobchuk.mooney.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

class ThemeManager(
    private val preferencesRepository: PreferencesRepository
) {
    val themeMode: Flow<ThemeMode> = preferencesRepository.getUserPreferences()
        .map { it.themeMode }
    
    val currentAppTheme: Flow<AppTheme> = preferencesRepository.getUserPreferences()
        .map { it.appTheme }

    suspend fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.updateThemeMode(mode)
    }
    
    suspend fun setAppTheme(theme: AppTheme) {
        preferencesRepository.updateAppTheme(theme)
    }

    suspend fun toggleTheme() {
        val currentMode = preferencesRepository.getCurrentPreferences().themeMode
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(newMode)
    }

    fun isSystemInDarkTheme(systemInDarkTheme: Boolean, themeMode: ThemeMode): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
    }
    
    companion object {
        fun getThemeDisplayName(theme: AppTheme): String {
            return when (theme) {
                AppTheme.PURPLE -> "Purple"
                AppTheme.MINIMAL -> "Minimal"
            }
        }
        
        fun getThemeDescription(theme: AppTheme): String {
            return when (theme) {
                AppTheme.PURPLE -> "Classic purple theme with light and dark variants"
                AppTheme.MINIMAL -> "Clean minimal theme with gray topbars and subtle accents"
            }
        }
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    return koinInject<ThemeManager>()
}

@Composable
fun rememberCurrentAppTheme(): AppTheme {
    val themeManager = rememberThemeManager()
    val theme by themeManager.currentAppTheme.collectAsState(initial = AppTheme.PURPLE)
    return theme
}

@Composable 
fun rememberCurrentThemeMode(): ThemeMode {
    val themeManager = rememberThemeManager()
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    return themeMode
}