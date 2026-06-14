package com.andriybobchuk.mooney.mooney.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.categories
import mooney.composeapp.generated.resources.nav_assets
import mooney.composeapp.generated.resources.transactions_label
import org.jetbrains.compose.resources.stringResource

/**
 * Unified Categories screen — one place to manage BOTH transaction and asset
 * categories. Two segmented tabs swap the body; each tab embeds the existing
 * category screen (rendered with `embedded = true` to skip its own top bar).
 *
 * Wired so we can strip "Transaction Categories" + "Asset Categories" rows
 * from Settings and offer a single Categories entry point instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    transactionViewModel: TransactionCategoriesViewModel,
    assetViewModel: AssetCategoriesViewModel,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = stringResource(Res.string.categories),
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            com.andriybobchuk.mooney.core.ads.AdBannerSlot(
                placement = com.andriybobchuk.mooney.core.ads.AdPlacement.CATEGORIES_BANNER
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SegmentedTabs(
                tabs = listOf(
                    stringResource(Res.string.transactions_label),
                    stringResource(Res.string.nav_assets)
                ),
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> TransactionCategoriesScreen(
                        viewModel = transactionViewModel,
                        onBackClick = onBackClick,
                        embedded = true
                    )
                    else -> AssetCategoriesScreen(
                        viewModel = assetViewModel,
                        onBackClick = onBackClick,
                        embedded = true
                    )
                }
            }
        }
    }
}

/**
 * Simple two/three-segment pill tab control. Lighter weight than Material's
 * SegmentedButtonRow which forces hairlines and check icons we don't want.
 */
@Composable
private fun SegmentedTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(0.dp))
}
