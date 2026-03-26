package com.andriybobchuk.mooney.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.andriybobchuk.mooney.core.presentation.Icons
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import org.jetbrains.compose.resources.stringResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.*

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val allItems = buildList {
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_transactions), Icons.TransactionsIcon()), Route.Transactions, 0))
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_assets), Icons.AccountsIcon()), Route.Accounts, 1))
        if (FeatureFlags.exchangeEnabled) add(Triple(BottomNavigationItem(stringResource(Res.string.nav_exchange), Icons.ExchangeIcon()), Route.Exchange, 2))
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_analytics), Icons.StatsIcon()), Route.Analytics, 3))
        if (FeatureFlags.goalsEnabled) add(Triple(BottomNavigationItem(stringResource(Res.string.nav_goals), Icons.GoalsIcon()), Route.Goals, 4))
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Floating pill bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 6.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .then(
                    Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(22.dp)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
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
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (isSelected) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp),
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

        // Home indicator space — fixed height for consistency
        Spacer(modifier = Modifier.height(20.dp))
    }
}
