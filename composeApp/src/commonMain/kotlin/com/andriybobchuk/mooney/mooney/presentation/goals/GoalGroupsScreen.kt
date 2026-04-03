package com.andriybobchuk.mooney.mooney.presentation.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars

data class GoalGroup(
    val name: String,
    val emoji: String,
    val goalsCount: Int,
    val totalAmount: Double,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalGroupsScreen(
    viewModel: GoalsViewModel,
    onGroupClick: (String) -> Unit,
    bottomNavbar: @Composable () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // Default colors for groups
    val groupColors = listOf(
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF2196F3), Color(0xFF9C27B0),
        Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF8BC34A), Color(0xFF607D8B),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF673AB7)
    )
    
    // Temporarily use a simple groups list until full goal groups feature is implemented
    val groups = listOf(
        GoalGroup(
            name = "General", 
            emoji = "📌", 
            goalsCount = state.goals.size,
            totalAmount = state.goals.sumOf { it.goal.targetAmount },
            color = Color(0xFF4CAF50)
        )
    )
    
    // Add an "Add Group" card if there are no groups
    val displayGroups = if (groups.isEmpty()) {
        listOf(
            GoalGroup("General", "📌", 0, 0.0, Color(0xFF607D8B))
        )
    } else {
        groups
    }
    
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = {
            Toolbars.Primary(
                title = "Goals",
                scrollBehavior = scrollBehavior,
                actions = listOf(
                    Toolbars.ToolBarAction(
                        painter = com.andriybobchuk.mooney.core.presentation.Icons.SettingsIcon(),
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                )
            )
        },
        bottomBar = bottomNavbar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(GoalsAction.ShowAddGoalSheet) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        Column(Modifier.background(MaterialTheme.colorScheme.primary)) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayGroups) { group ->
                        GoalGroupCard(
                            group = group,
                            onClick = { onGroupClick(group.name) }
                        )
                    }
                }
            }
        }
        
        // Add/Edit Goal Bottom Sheet - Temporarily commented out until full implementation
        /*
        if (state.showAddGoalSheet) {
            AddEditGoalBottomSheet(
                editingGoal = state.editingGoal,
                currentGroup = "General",
                selectedImageBytes = state.selectedImageBytes,
                existingGroups = state.goalsByGroup.keys.toList(),
                onImagePicked = { bytes -> viewModel.onAction(GoalsAction.SetSelectedImage(bytes)) },
                onDismiss = { viewModel.onAction(GoalsAction.HideAddGoalSheet) },
                onSave = { emoji, title, description, amount, currency, group, imageBytes ->
                    viewModel.onAction(
                        GoalsAction.SaveGoal(emoji, title, description, amount, currency, group, imageBytes)
                    )
                }
            )
        }
        */
    }
}

@Composable
private fun GoalGroupCard(
    group: GoalGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            group.color.copy(alpha = 0.15f),
                            group.color.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 300f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = group.emoji,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                if (group.goalsCount > 0) {
                    Text(
                        text = "${group.goalsCount} goal${if (group.goalsCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    if (group.totalAmount > 0) {
                        Text(
                            text = "${group.totalAmount.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = group.color,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No goals yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Badge for goals count
            if (group.goalsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(group.color.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = group.goalsCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = group.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getEmojiForGroup(groupName: String): String {
    return when (groupName.lowercase()) {
        "car", "cars", "vehicle", "vehicles" -> "🚗"
        "house", "home", "real estate", "property" -> "🏠"
        "travel", "vacation", "trip", "holiday" -> "✈️"
        "education", "school", "study", "learning" -> "🎓"
        "health", "fitness", "medical", "wellness" -> "💪"
        "short-term", "urgent", "quick" -> "⏱️"
        "priority", "priorities", "important" -> "⭐"
        "savings", "emergency", "fund" -> "💰"
        "technology", "tech", "gadgets" -> "💻"
        "food", "dining", "restaurant" -> "🍽️"
        "entertainment", "fun", "hobbies" -> "🎬"
        "clothing", "fashion", "style" -> "👕"
        "sports", "exercise", "gym" -> "⚽"
        "business", "investment", "work" -> "💼"
        else -> "📌"
    }
}
