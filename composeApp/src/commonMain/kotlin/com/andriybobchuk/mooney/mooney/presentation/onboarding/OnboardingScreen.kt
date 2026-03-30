package com.andriybobchuk.mooney.mooney.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andriybobchuk.mooney.core.presentation.designsystem.components.MeshGradientBackground
import com.andriybobchuk.mooney.mooney.domain.Currency
import kotlinx.coroutines.launch
import mooney.composeapp.generated.resources.Res
import mooney.composeapp.generated.resources.mooney_icon
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToMain: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    // Controlled fade: entrance + page transitions
    val contentAlpha = remember { Animatable(0f) }
    var displayedPage by remember { mutableStateOf(state.page) }

    // Initial fade-in
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(800, easing = EaseOut))
    }

    // Page transition: fade out → swap content → fade in
    LaunchedEffect(state.page) {
        if (state.page != displayedPage) {
            contentAlpha.animateTo(0f, tween(400, easing = EaseInOut))
            displayedPage = state.page
            contentAlpha.animateTo(1f, tween(600, easing = EaseOut))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(prominent = true)

        Box(modifier = Modifier.fillMaxSize().alpha(contentAlpha.value)) {
            when (displayedPage) {
                OnboardingPage.CURRENCY_PICKER -> CurrencyPickerContent(
                    state = state,
                    onAction = viewModel::onAction
                )
                OnboardingPage.WELCOME -> WelcomeContent(
                    onSwipeUp = { viewModel.onAction(OnboardingAction.EnterApp) }
                )
            }
        }
    }
}

@Composable
private fun CurrencyPickerContent(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.12f))

        Text(
            text = "Welcome to Mooney",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your primary currency",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.currencies) { currency ->
                CurrencyChip(
                    currency = currency,
                    selected = currency == state.selectedCurrency,
                    onClick = { onAction(OnboardingAction.SelectCurrency(currency)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onAction(OnboardingAction.GetStarted) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.selectedCurrency != null && !state.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(0.06f))
    }
}

@Composable
private fun WelcomeContent(
    onSwipeUp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var swiped by remember { mutableStateOf(false) }

    // Swipe-up animation: slide the whole page up + fade out
    val slideOffset = remember { Animatable(0f) }
    val swipeAlpha = remember { Animatable(1f) }

    // Cumulative drag tracking
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(density) { 100.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = slideOffset.value
                alpha = swipeAlpha.value
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (!swiped && dragAmount < 0) {
                            dragAccumulator += dragAmount
                            // Live drag feedback — subtle upward pull
                            scope.launch {
                                slideOffset.snapTo(dragAccumulator * 0.3f)
                                swipeAlpha.snapTo(
                                    (1f + dragAccumulator / swipeThresholdPx * 0.3f).coerceIn(0.4f, 1f)
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        if (!swiped && dragAccumulator < -swipeThresholdPx) {
                            swiped = true
                            scope.launch {
                                // Animate the rest of the slide out
                                launch {
                                    slideOffset.animateTo(
                                        -swipeThresholdPx * 4f,
                                        tween(500, easing = EaseInOut)
                                    )
                                }
                                swipeAlpha.animateTo(0f, tween(400, easing = EaseOut))
                                onSwipeUp()
                            }
                        } else if (!swiped) {
                            // Snap back
                            scope.launch {
                                launch { slideOffset.animateTo(0f, tween(300, easing = EaseOut)) }
                                swipeAlpha.animateTo(1f, tween(300, easing = EaseOut))
                            }
                        }
                    }
                )
            }
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Icon with mesh-like accent glow
        Box(contentAlignment = Alignment.Center) {
            IconGlow()
            Image(
                painter = painterResource(Res.drawable.mooney_icon),
                contentDescription = "Mooney",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.weight(0.25f))

        Text(
            text = "Your new Mooney is ready",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Swipe up to reveal what's\nwaiting for you inside",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.25f))

        SwipeUpIndicator()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Swipe to unwrap the app",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
private fun IconGlow() {
    val accent = MaterialTheme.colorScheme.primary
    val transparent = Color.Transparent

    Canvas(modifier = Modifier.size(240.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Off-center blob top-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.35f), transparent),
                center = Offset(cx + cx * 0.3f, cy - cy * 0.25f),
                radius = cx * 0.7f
            ),
            radius = cx * 0.7f,
            center = Offset(cx + cx * 0.3f, cy - cy * 0.25f)
        )

        // Center warm blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.25f), transparent),
                center = Offset(cx, cy),
                radius = cx * 0.85f
            ),
            radius = cx * 0.85f,
            center = Offset(cx, cy)
        )

        // Off-center blob bottom-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.30f), transparent),
                center = Offset(cx - cx * 0.35f, cy + cy * 0.3f),
                radius = cx * 0.65f
            ),
            radius = cx * 0.65f,
            center = Offset(cx - cx * 0.35f, cy + cy * 0.3f)
        )
    }
}

@Composable
private fun SwipeUpIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Swipe up",
            modifier = Modifier
                .size(36.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) },
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun CurrencyChip(
    currency: Currency,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250, easing = EaseOut)
    )
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(250, easing = EaseOut)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
