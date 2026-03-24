package com.andriybobchuk.mooney.app

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.andriybobchuk.mooney.core.presentation.Icons
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val allItems = buildList {
        add(Triple(BottomNavigationItem("Transactions", Icons.TransactionsIcon()), Route.Transactions, 0))
        add(Triple(BottomNavigationItem("Assets", Icons.AccountsIcon()), Route.Accounts, 1))
        if (FeatureFlags.exchangeEnabled) add(Triple(BottomNavigationItem("Exchange", Icons.ExchangeIcon()), Route.Exchange, 2))
        add(Triple(BottomNavigationItem("Analytics", Icons.StatsIcon()), Route.Analytics, 3))
        if (FeatureFlags.goalsEnabled) add(Triple(BottomNavigationItem("Goals", Icons.GoalsIcon()), Route.Goals, 4))
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        allItems.forEachIndexed { _, (item, route, originalIndex) ->
            NavigationBarItem(
                selected = selectedItemIndex == originalIndex,
                onClick = {
                    if (selectedItemIndex != originalIndex) {
                        navController.navigate(route) { popUpTo(Route.MooneyGraph) }
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = true,
                icon = {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
