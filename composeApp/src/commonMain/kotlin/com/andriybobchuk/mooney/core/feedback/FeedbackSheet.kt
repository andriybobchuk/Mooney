package com.andriybobchuk.mooney.core.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import com.andriybobchuk.mooney.mooney.domain.feedback.FeedbackKind
import com.andriybobchuk.mooney.mooney.domain.usecase.SubmitFeedbackUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Unified in-app feedback surface. Replaces email/mailto links across the app.
 *
 * Pattern: kind chip at the top, free-text area, submit. On success the form
 * is swapped for a brief "thanks" state, then auto-dismisses. On failure the
 * button re-enables and an error line appears.
 *
 * Call this directly from every feedback entry point — pass `initialKind` to
 * preselect the chip (e.g., WIDGET when opened from "Suggest a widget").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    onDismiss: () -> Unit,
    initialKind: FeedbackKind = FeedbackKind.GENERAL
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val submitFeedback: SubmitFeedbackUseCase = koinInject()
    val analyticsTracker: AnalyticsTracker = koinInject()
    val scope = rememberCoroutineScope()

    var kind by remember { mutableStateOf(initialKind) }
    var body by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var thanksVisible by remember { mutableStateOf(false) }

    LaunchedEffect(thanksVisible) {
        if (thanksVisible) {
            delay(1400)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            EnhancedMeshBackground(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                if (thanksVisible) {
                    FeedbackThanksContent()
                } else {
                    FeedbackFormContent(
                        kind = kind,
                        onKindChange = { kind = it },
                        body = body,
                        onBodyChange = {
                            body = it
                            if (error) error = false
                        },
                        submitting = submitting,
                        error = error,
                        onSubmit = {
                            error = false
                            submitting = true
                            scope.launch {
                                val ok = submitFeedback(kind, body)
                                submitting = false
                                if (ok) {
                                    analyticsTracker.trackEvent(
                                        AnalyticsEvent.FeedbackSubmitted(kind.name)
                                    )
                                    thanksVisible = true
                                } else {
                                    error = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackFormContent(
    kind: FeedbackKind,
    onKindChange: (FeedbackKind) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    submitting: Boolean,
    error: Boolean,
    onSubmit: () -> Unit
) {
    Text(
        text = "What's on your mind?",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Pick a category and tell me anything — bug, idea, frustration. Built solo, every message gets read.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Kind chips — wraps on narrow screens via Arrangement.spacedBy + flow-like Row.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedbackKind.entries.forEach { entry ->
            FeedbackKindChip(
                kind = entry,
                selected = entry == kind,
                onClick = { onKindChange(entry) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        if (body.isEmpty()) {
            Text(
                text = when (kind) {
                    FeedbackKind.BUG -> "What broke? What were you trying to do?"
                    FeedbackKind.FEATURE -> "What's missing? How would you use it?"
                    FeedbackKind.WIDGET -> "Describe the widget — what would it show?"
                    FeedbackKind.GENERAL -> "Your feedback…"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
        BasicTextField(
            value = body,
            onValueChange = onBodyChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }

    if (error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Couldn't send. Check your connection and try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSubmit,
        enabled = body.isNotBlank() && !submitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    ) {
        if (submitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Send",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun FeedbackKindChip(
    kind: FeedbackKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textColor = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = kind.emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = kind.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FeedbackThanksContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🙏", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Got it. Thank you.",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "I read every reply.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}
