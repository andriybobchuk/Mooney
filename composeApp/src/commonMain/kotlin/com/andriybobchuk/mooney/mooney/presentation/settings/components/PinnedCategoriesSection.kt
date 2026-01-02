package com.andriybobchuk.mooney.mooney.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsAction
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsState

@Composable
fun PinnedCategoriesSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    Column {
        Text(
            text = "Pinned Categories",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Select up to ${state.maxPinnedCategories} categories to appear as quick shortcuts",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Currently pinned categories
        if (state.pinnedCategories.isNotEmpty()) {
            Text(
                text = "Pinned (${state.pinnedCategories.size}/${state.maxPinnedCategories})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.pinnedCategories) { category ->
                    PinnedCategoryChip(
                        category = category,
                        isSelected = true,
                        onClick = { onAction(SettingsAction.OnCategorySelectionToggle(category)) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // All available categories
        Text(
            text = "Available Categories",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp) // Fixed height for the grid
        ) {
            items(state.allCategories.filter { it.isSubCategory() }) { category ->
                CategorySelectionCard(
                    category = category,
                    isSelected = state.pinnedCategoryIds.contains(category.id),
                    canSelect = state.canAddMorePinned || state.pinnedCategoryIds.contains(category.id),
                    onClick = { onAction(SettingsAction.OnCategorySelectionToggle(category)) }
                )
            }
        }
    }
}

@Composable
private fun PinnedCategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = category.resolveEmoji(),
                fontSize = 16.sp
            )
            Text(
                text = category.title,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CategorySelectionCard(
    category: Category,
    isSelected: Boolean,
    canSelect: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            canSelect -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, 
                MaterialTheme.colorScheme.primary
            )
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canSelect) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = category.resolveEmoji(),
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = category.title,
                fontSize = 12.sp,
                color = if (canSelect) MaterialTheme.colorScheme.onSurfaceVariant 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}