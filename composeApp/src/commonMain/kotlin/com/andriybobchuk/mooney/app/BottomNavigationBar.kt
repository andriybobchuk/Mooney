package com.andriybobchuk.mooney.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        // Thin top divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allItems.forEach { (item, route, originalIndex) ->
                val isSelected = selectedItemIndex == originalIndex
                val color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (selectedItemIndex != originalIndex) {
                                navController.navigate(route) { popUpTo(Route.MooneyGraph) }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
