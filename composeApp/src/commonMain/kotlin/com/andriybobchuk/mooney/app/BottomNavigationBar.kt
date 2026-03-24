package com.andriybobchuk.mooney.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allItems.forEach { (item, route, originalIndex) ->
                val isSelected = selectedItemIndex == originalIndex

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) else Modifier
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (selectedItemIndex != originalIndex) {
                                navController.navigate(route) { popUpTo(Route.MooneyGraph) }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
