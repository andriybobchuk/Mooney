package com.andriybobchuk.mooney.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.andriybobchuk.mooney.core.presentation.theme.MooneyTheme
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val themeManager: ThemeManager = koinInject()
    val themeMode by themeManager.themeMode.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()
    
    val darkTheme = themeManager.isSystemInDarkTheme(systemInDarkTheme)
    
    MooneyTheme(
        darkTheme = darkTheme
    ) {
        NavigationHost()
    }
}

