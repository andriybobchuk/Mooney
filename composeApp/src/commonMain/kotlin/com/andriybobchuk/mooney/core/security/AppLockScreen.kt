package com.andriybobchuk.mooney.core.security

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andriybobchuk.mooney.core.premium.MooneyProIconBadge
import com.andriybobchuk.mooney.core.presentation.designsystem.components.EnhancedMeshBackground
import kotlinx.coroutines.launch
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.app_lock_enter_pin
import mooney.composeapp.generated.resources.app_lock_unlock_to_continue
import mooney.composeapp.generated.resources.app_lock_wrong_pin
import org.jetbrains.compose.resources.stringResource

/**
 * Lock screen shown on app start when App Lock is enabled. Self-dismisses
 * via [onUnlocked] once the entered PIN matches.
 */
@Composable
fun AppLockUnlockScreen(
    manager: AppLockManager,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val wrongPinMsg = stringResource(Res.string.app_lock_wrong_pin)

    LaunchedEffect(pin) {
        if (pin.length == MAX_PIN_LENGTH) {
            val ok = manager.verify(pin)
            if (ok) {
                onUnlocked()
            } else {
                error = wrongPinMsg
                pin = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EnhancedMeshBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MooneyProIconBadge(iconSize = 80)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(Res.string.app_lock_unlock_to_continue),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.app_lock_enter_pin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            PinDots(filled = pin.length, total = MAX_PIN_LENGTH)
            Spacer(modifier = Modifier.height(8.dp))
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            PinKeypad(
                onDigit = { digit ->
                    if (pin.length < MAX_PIN_LENGTH) {
                        pin += digit
                        error = null
                    }
                },
                onBackspace = {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                    }
                }
            )
            // suppress unused-warning on scope by referencing it inside a no-op
            // launch when error is cleared by typing — keeps the field tied to
            // composition so future biometric prompts can be triggered async.
            if (error == null && pin.isEmpty()) {
                LaunchedEffect(Unit) { scope.launch { /* idle */ } }
            }
        }
    }
}

/**
 * PIN setup / change / disable bottom-sheet content.
 *
 * Flow: user enters a new PIN, then confirms; success calls [onSetupComplete]
 * with the chosen PIN so the host can persist it.
 */
@Composable
fun AppLockSetupContent(
    onSetupComplete: (String) -> Unit,
    headerTitle: String,
    confirmTitle: String,
    mismatchMessage: String
) {
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val confirming = firstEntry != null
    val visibleTitle = if (confirming) confirmTitle else headerTitle

    LaunchedEffect(pin) {
        if (pin.length == MAX_PIN_LENGTH) {
            if (!confirming) {
                firstEntry = pin
                pin = ""
            } else {
                if (pin == firstEntry) {
                    onSetupComplete(pin)
                } else {
                    error = mismatchMessage
                    pin = ""
                    firstEntry = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = visibleTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))
        PinDots(filled = pin.length, total = MAX_PIN_LENGTH)
        Spacer(modifier = Modifier.height(8.dp))
        if (error != null) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        PinKeypad(
            onDigit = { digit ->
                if (pin.length < MAX_PIN_LENGTH) {
                    pin += digit
                    error = null
                }
            },
            onBackspace = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(total) { i ->
            val isFilled = i < filled
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    KeypadButton(
                        label = key,
                        onClick = {
                            when (key) {
                                "" -> Unit
                                "⌫" -> onBackspace()
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    val isAction = label == "⌫"
    val isInvisible = label.isEmpty()
    Surface(
        onClick = { if (!isInvisible) onClick() },
        modifier = Modifier.size(70.dp),
        shape = CircleShape,
        color = if (isInvisible) {
            androidx.compose.ui.graphics.Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!isInvisible) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (isAction) FontWeight.Normal else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private const val MAX_PIN_LENGTH = 4
