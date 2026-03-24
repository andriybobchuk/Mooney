package com.andriybobchuk.mooney.app

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

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val items = listOf(
        BottomNavigationItem("Transactions", Icons.TransactionsIcon()),
        BottomNavigationItem("Assets", Icons.AccountsIcon()),
        BottomNavigationItem("Exchange", Icons.ExchangeIcon()),
        BottomNavigationItem("Analytics", Icons.StatsIcon()),
        BottomNavigationItem("Goals", Icons.GoalsIcon()),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    if (selectedItemIndex != index) {
                        when (index) {
                            0 -> navController.navigate(Route.Transactions) { popUpTo(Route.MooneyGraph) }
                            1 -> navController.navigate(Route.Accounts) { popUpTo(Route.MooneyGraph) }
                            2 -> navController.navigate(Route.Exchange) { popUpTo(Route.MooneyGraph) }
                            3 -> navController.navigate(Route.Analytics) { popUpTo(Route.MooneyGraph) }
                            4 -> navController.navigate(Route.Goals) { popUpTo(Route.MooneyGraph) }
                        }
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
                        modifier = Modifier.size(20.dp)
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
