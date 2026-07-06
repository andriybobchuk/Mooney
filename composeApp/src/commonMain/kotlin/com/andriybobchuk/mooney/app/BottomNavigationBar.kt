package com.andriybobchuk.mooney.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.andriybobchuk.mooney.core.presentation.Icons
import com.andriybobchuk.mooney.core.testing.TestTags
import com.andriybobchuk.mooney.core.testing.mooneyTestTag
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.nav_analytics
import mooney.composeapp.generated.resources.nav_assets
import mooney.composeapp.generated.resources.nav_exchange
import mooney.composeapp.generated.resources.nav_settings
import mooney.composeapp.generated.resources.nav_transactions
import org.jetbrains.compose.resources.stringResource

/**
 * Minimal flat bottom bar. Each tab is an icon stacked above a label with a
 * small horizontal pill at the very top to mark the active tab — no card,
 * no pill backgrounds on individual items, no floating container. Adapts to
 * light and dark themes via `surface` / `onSurface`.
 */
private const val DOUBLE_TAP_WINDOW_MS = 400L

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedItemIndex: Int) {
    val scrollToTopBus: ScrollToTopBus = koinInject()
    // One per slot — we only need the timestamp of the last tap on each tab.
    val lastTap = remember { mutableStateOf(IntArray(5)) }
    val lastTapTime = remember { mutableStateOf(LongArray(5)) }
    @Suppress("UNUSED_VARIABLE")
    val keep = lastTap // suppress unused-warning; the array is used by ref below

    val items = buildList {
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_transactions), Icons.TransactionsIcon(), Icons.TransactionsFilledIcon()), Route.Transactions, 0))
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_assets), Icons.AccountsIcon(), Icons.AccountsFilledIcon()), Route.Accounts, 1))
        if (FeatureFlags.exchangeEnabled) {
            add(Triple(BottomNavigationItem(stringResource(Res.string.nav_exchange), Icons.TransactionsIcon()), Route.Exchange, 2))
        }
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_analytics), Icons.StatsIcon(), Icons.StatsFilledIcon()), Route.Analytics, 3))
        add(Triple(BottomNavigationItem(stringResource(Res.string.nav_settings), Icons.SettingsIcon()), Route.Settings, 4))
    }

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            items.forEach { (item, route, originalIndex) ->
                val isSelected = selectedItemIndex == originalIndex
                val tag = when (originalIndex) {
                    0 -> TestTags.NAV_TRANSACTIONS
                    1 -> TestTags.NAV_ACCOUNTS
                    3 -> TestTags.NAV_ANALYTICS
                    4 -> TestTags.NAV_SETTINGS
                    else -> null
                }
                BottomNavTab(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(route) { popUpTo(Route.MooneyGraph) }
                            lastTapTime.value[originalIndex] = Clock.System.now().toEpochMilliseconds()
                            return@BottomNavTab
                        }
                        // Re-tap on the currently selected tab: detect a quick
                        // second tap (≤400ms) and emit a scroll-to-top event.
                        val now = Clock.System.now().toEpochMilliseconds()
                        val last = lastTapTime.value[originalIndex]
                        lastTapTime.value[originalIndex] = now
                        if (last != 0L && now - last <= DOUBLE_TAP_WINDOW_MS) {
                            val tab = when (originalIndex) {
                                0 -> ScrollToTopBus.Tab.TRANSACTIONS
                                1 -> ScrollToTopBus.Tab.ASSETS
                                3 -> ScrollToTopBus.Tab.ANALYTICS
                                4 -> ScrollToTopBus.Tab.SETTINGS
                                else -> null
                            }
                            tab?.let { scrollToTopBus.fire(it) }
                        }
                    },
                    modifier = Modifier.weight(1f).let { m -> if (tag != null) m.mooneyTestTag(tag) else m }
                )
            }
        }
        // Reserve room for the iOS home indicator so the icons don't sit on it.
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BottomNavTab(
    item: BottomNavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No top indicator pill — the icon/label colour carries the active state
    // on its own. Active is a quieter "onSurface @ 0.85" rather than a hard
    // black/white so the whole strip reads softer.
    val activeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val color = if (isSelected) activeColor else inactiveColor

    Column(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = if (isSelected && item.filledIcon != null) item.filledIcon else item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(22.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}
