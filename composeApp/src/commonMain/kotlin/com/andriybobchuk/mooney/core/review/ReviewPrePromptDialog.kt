package com.andriybobchuk.mooney.core.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.enjoying_mooney
import mooney.composeapp.generated.resources.feedback_intro
import mooney.composeapp.generated.resources.not_really
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Two-step gate before the native review prompt. Same mesh-gradient styling as
 * the paywall + flex sheet for visual consistency across all "moment" surfaces.
 * Non-fullscreen — slides up partway and asks one question. Phrasing avoids any
 * "5 stars" language (Apple forbids it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPrePromptDialog(
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    onDismiss: () -> Unit,
    source: String = "unknown"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val analyticsTracker: AnalyticsTracker = koinInject()
    LaunchedEffect(Unit) {
        analyticsTracker.trackEvent(AnalyticsEvent.ReviewPrepromptShown(source))
    }
    val wrappedPositive: () -> Unit = {
        analyticsTracker.trackEvent(AnalyticsEvent.ReviewPrepromptResponse("positive"))
        onPositive()
    }
    val wrappedNegative: () -> Unit = {
        analyticsTracker.trackEvent(AnalyticsEvent.ReviewPrepromptResponse("negative"))
        onNegative()
    }
    val wrappedDismiss: () -> Unit = {
        analyticsTracker.trackEvent(AnalyticsEvent.ReviewPrepromptResponse("dismissed"))
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = wrappedDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // matchParentSize keeps the mesh constrained to the Box's
            // content-driven height — otherwise the canvas would push the
            // sheet to fullscreen.
            EnhancedMeshBackground(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💛",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.enjoying_mooney),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.feedback_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = wrappedPositive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                ) {
                    Text(
                        text = "I love it",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = wrappedNegative,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.not_really),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
