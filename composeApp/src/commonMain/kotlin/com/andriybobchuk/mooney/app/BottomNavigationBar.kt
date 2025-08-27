package com.andriybobchuk.mooney.app

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.andriybobchuk.mooney.core.presentation.Icons

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val items = listOf(
        BottomNavigationItem("Transactions", Icons.TransactionsIcon()),
        BottomNavigationItem("Accounts", Icons.AccountsIcon()),
        BottomNavigationItem("Analytics", Icons.StatsIcon()),
        BottomNavigationItem("Goals", Icons.GoalsIcon()),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    if (selectedItemIndex != index) {
                        when (index) {
                            0 -> navController.navigate(Route.Transactions) { popUpTo(Route.MooneyGraph) }
                            1 -> navController.navigate(Route.Accounts) { popUpTo(Route.MooneyGraph) }
                            2 -> navController.navigate(Route.Analytics) { popUpTo(Route.MooneyGraph) }
                            3 -> navController.navigate(Route.Goals) { popUpTo(Route.MooneyGraph) }
                        }
                    }
                },
                label = { Text(text = item.title) },
                alwaysShowLabel = true,
                icon = {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(21.dp)
                    )
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
        }
    }
}
