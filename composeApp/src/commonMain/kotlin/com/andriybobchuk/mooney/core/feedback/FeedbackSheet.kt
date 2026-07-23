package com.andriybobchuk.mooney.core.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MooneyBottomSheet
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.andrii_mooney
import mooney.composeapp.generated.resources.contact_intro
import mooney.composeapp.generated.resources.feedback_socials_title
import mooney.composeapp.generated.resources.ic_email
import mooney.composeapp.generated.resources.ic_instagram
import mooney.composeapp.generated.resources.ic_tiktok
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Contact sheet: a friendly "Hey, I'm Andriy" with my profile photo and direct
 * links to the channels I actually reply on. The whole sheet sits on the same
 * mesh background used in onboarding / paywall, so the photo reads as the
 * hero rather than a framed inset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    MooneyBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth()) {
            EnhancedMeshBackground(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.andrii_mooney),
                    contentDescription = "Andrii", // allow-hardcoded (avatar alt)
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.feedback_socials_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(Res.string.contact_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                ContactRow(
                    icon = painterResource(Res.drawable.ic_instagram),
                    label = "Instagram", // allow-hardcoded (brand name)
                    value = "@andriybobchuk.bro",
                    onClick = { uriHandler.openUri("https://instagram.com/andriybobchuk.bro") }
                )
                ContactRow(
                    icon = painterResource(Res.drawable.ic_tiktok),
                    label = "TikTok", // allow-hardcoded (brand name)
                    value = "@mooney_pro",
                    onClick = { uriHandler.openUri("https://www.tiktok.com/@mooney_pro") }
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
}

@Composable
private fun ContactRow(
    icon: Painter,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    // Row background matches the app-wide `background` role instead of the
    // mid-tone `surfaceVariant` grey we used before — so cards read as white
    // in light mode and near-black in dark mode, consistent with the other
    // sheet surfaces.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.background)
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
