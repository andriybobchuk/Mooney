package com.andriybobchuk.mooney.core.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.mooney_icon
import mooney.composeapp.generated.resources.pro_label
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Mooney icon with a floating PRO chip on the bottom-right. The chip uses
 * green in dark mode, blue in light mode — same accent contrast Apple uses
 * on their hero "Pro" treatments. Shared across paywall, app lock unlock
 * screen, and anywhere else we need to flag premium identity.
 */
@Composable
fun MooneyProIconBadge(iconSize: Int = 72) {
    val isDark = isSystemInDarkTheme()
    val proColor = if (isDark) Color(0xFF00C896) else Color(0xFF3562F6)

    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.mooney_icon),
            contentDescription = "Mooney",
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape((iconSize / 4).dp)),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = proColor,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = stringResource(Res.string.pro_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
