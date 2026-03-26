package com.andriybobchuk.mooney.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.ic_instagram
import mooney.composeapp.generated.resources.ic_telegram
import mooney.composeapp.generated.resources.ic_email
import mooney.composeapp.generated.resources.ic_discord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    MooneyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Get in touch",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Mooney is actively being built. Found a bug? Have an idea? Reach out — your feedback shapes the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            ContactRow(
                icon = painterResource(Res.drawable.ic_instagram),
                label = "Instagram",
                value = "@andriybobchuk.bro",
                onClick = { uriHandler.openUri("https://instagram.com/andriybobchuk.bro") }
            )
            ContactRow(
                icon = painterResource(Res.drawable.ic_telegram),
                label = "Telegram",
                value = "@andriybobchuk",
                onClick = { uriHandler.openUri("https://t.me/andriybobchuk") }
            )
            ContactRow(
                icon = painterResource(Res.drawable.ic_email),
                label = "Email",
                value = "andriybobchuk@gmail.com",
                onClick = { uriHandler.openUri("mailto:andriybobchuk@gmail.com") }
            )
            ContactRow(
                icon = painterResource(Res.drawable.ic_discord),
                label = "Discord",
                value = "andriibobchuk",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContactRow(
    icon: Painter,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
