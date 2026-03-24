package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.mooney.domain.devtools.DevStats
import com.andriybobchuk.mooney.mooney.domain.devtools.DevToolsManager
import kotlinx.coroutines.launch

@Composable
fun DevLabel(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .rotate(-90f)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(Color(0xFF3562F6))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "DEV",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsBottomSheet(
    devToolsManager: DevToolsManager,
    onDismiss: () -> Unit
) {
    var stats by remember { mutableStateOf<DevStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        stats = devToolsManager.getStats()
    }

    MooneyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3562F6))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("DEV", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text("Developer Tools", style = MaterialTheme.typography.titleLarge)
            }

            // DB Stats
            stats?.let { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Accounts", s.accountCount)
                    StatItem("Transactions", s.transactionCount)
                    StatItem("Goals", s.goalCount)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status message
            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                    strokeWidth = 2.dp
                )
            }

            Text(
                "Populate",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            DevActionRow("Populate everything") {
                scope.launch {
                    isLoading = true
                    devToolsManager.populateEverything()
                    stats = devToolsManager.getStats()
                    statusMessage = "Mock data populated"
                    isLoading = false
                }
            }
            DevActionRow("Add mock accounts") {
                scope.launch {
                    isLoading = true
                    devToolsManager.populateMockAccounts()
                    stats = devToolsManager.getStats()
                    statusMessage = "Accounts added"
                    isLoading = false
                }
            }
            DevActionRow("Add mock transactions (6 months)") {
                scope.launch {
                    isLoading = true
                    devToolsManager.populateMockTransactions()
                    stats = devToolsManager.getStats()
                    statusMessage = "Transactions added"
                    isLoading = false
                }
            }
            DevActionRow("Add mock goals") {
                scope.launch {
                    isLoading = true
                    devToolsManager.populateMockGoals()
                    stats = devToolsManager.getStats()
                    statusMessage = "Goals added"
                    isLoading = false
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            DevActionRow("Clear everything", isDestructive = true) {
                scope.launch {
                    isLoading = true
                    devToolsManager.clearEverything()
                    stats = devToolsManager.getStats()
                    statusMessage = "All data cleared"
                    isLoading = false
                }
            }
            DevActionRow("Clear transactions", isDestructive = true) {
                scope.launch {
                    isLoading = true
                    devToolsManager.clearAllTransactions()
                    stats = devToolsManager.getStats()
                    statusMessage = "Transactions cleared"
                    isLoading = false
                }
            }
            DevActionRow("Clear accounts", isDestructive = true) {
                scope.launch {
                    isLoading = true
                    devToolsManager.clearAllAccounts()
                    stats = devToolsManager.getStats()
                    statusMessage = "Accounts cleared"
                    isLoading = false
                }
            }
            DevActionRow("Clear goals", isDestructive = true) {
                scope.launch {
                    isLoading = true
                    devToolsManager.clearAllGoals()
                    stats = devToolsManager.getStats()
                    statusMessage = "Goals cleared"
                    isLoading = false
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Using: mooney_dev.db",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DevActionRow(
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDestructive) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(Modifier.height(6.dp))
}
