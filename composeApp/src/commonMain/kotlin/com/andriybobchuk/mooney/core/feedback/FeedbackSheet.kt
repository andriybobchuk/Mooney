package com.andriybobchuk.mooney.core.feedback

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
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.feedback_socials_intro
import mooney.composeapp.generated.resources.feedback_socials_title
import mooney.composeapp.generated.resources.ic_email
import mooney.composeapp.generated.resources.ic_instagram
import mooney.composeapp.generated.resources.ic_telegram
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Single feedback path for Mooney: a friendly "Hey, I'm Andriy" sheet with
 * direct links to the channels I actually reply on. Replaces the old
 * Firebase-backed form — DMs convert into conversations, web forms vanish
 * into a queue.
 *
 * Keeping the same `FeedbackSheet` name and entrypoint signature so every
 * call site (Settings, Assets, Transactions, the Analytics request card)
 * stays a one-liner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
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
                text = "👋",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(Res.string.feedback_socials_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = stringResource(Res.string.feedback_socials_intro),
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
