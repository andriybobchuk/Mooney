package com.andriybobchuk.time.app

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
import com.andriybobchuk.time.core.presentation.Icons

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val items = listOf(
        BottomNavigationItem("Transactions", Icons.TransactionsIcon()),
        BottomNavigationItem("Account", Icons.AccountsIcon()),
        BottomNavigationItem("Analytics", Icons.StatsIcon()),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
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
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF3E4DBA),
                    selectedIndicatorColor = Color(0xFF3E4DBA),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    disabledIconColor = Color.Gray,
                    disabledTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun TimeBottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val items = listOf(
        BottomNavigationItem("Blocks", Icons.TransactionsIcon()),
        BottomNavigationItem("Analytics", Icons.StatsIcon()),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    if (selectedItemIndex != index) {
                        when (index) {
                            0 -> navController.navigate(Route.TimeTracking) { popUpTo(Route.TimeGraph) }
                            1 -> navController.navigate(Route.TimeAnalytics) { popUpTo(Route.TimeGraph) }
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
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.Black,
                    selectedIndicatorColor = Color.Black,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    disabledIconColor = Color.Gray,
                    disabledTextColor = Color.Gray
                )
            )
        }
    }
}
