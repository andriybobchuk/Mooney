package com.andriybobchuk.mooney.mooney.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.theme.AppTheme
import com.andriybobchuk.mooney.core.presentation.theme.rememberCurrentAppTheme
import com.andriybobchuk.mooney.core.presentation.theme.rememberThemeManager
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsAction
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsState
import com.andriybobchuk.mooney.mooney.presentation.settings.ThemeSwitcherBottomSheet
import com.andriybobchuk.mooney.mooney.presentation.settings.ThemeSwitcherCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val themeManager = rememberThemeManager()
    val currentAppTheme = rememberCurrentAppTheme()
    val scope = rememberCoroutineScope()
    var showThemeSwitcher by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "General Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Theme Mode Setting
        SettingsCard {
            Column {
                Text(
                    text = "Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Choose your preferred theme mode",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { theme ->
                        FilterChip(
                            onClick = { onAction(SettingsAction.OnThemeModeChange(theme)) },
                            label = { 
                                Text(
                                    text = when (theme) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            selected = state.currentThemeMode == theme
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // App Theme Switcher
        ThemeSwitcherCard(
            currentTheme = currentAppTheme,
            onClick = { showThemeSwitcher = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Default Currency Setting
        SettingsCard {
            var showCurrencyDialog by remember { mutableStateOf(false) }
            
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Default Currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Set your primary currency",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "${state.defaultCurrency.symbol} ${state.defaultCurrency.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (showCurrencyDialog) {
                CurrencySelectionDialog(
                    currencies = state.availableCurrencies,
                    selectedCurrency = state.defaultCurrency,
                    onCurrencySelected = { currency ->
                        onAction(SettingsAction.OnDefaultCurrencyChange(currency.name))
                        showCurrencyDialog = false
                    },
                    onDismiss = { showCurrencyDialog = false }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Notifications Setting
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notifications",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Enable app notifications",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { onAction(SettingsAction.OnNotificationsToggle(it)) }
                )
            }
        }
    }
    
    // Theme Switcher Bottom Sheet
    if (showThemeSwitcher) {
        ThemeSwitcherBottomSheet(
            currentTheme = currentAppTheme,
            onThemeSelected = { newTheme ->
                scope.launch {
                    themeManager.setAppTheme(newTheme)
                }
            },
            onDismiss = { showThemeSwitcher = false }
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun CurrencySelectionDialog(
    currencies: List<Currency>,
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Currency",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                currencies.forEach { currency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCurrencySelected(currency) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currency == selectedCurrency,
                            onClick = { onCurrencySelected(currency) }
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "${currency.symbol} ${currency.name}",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}