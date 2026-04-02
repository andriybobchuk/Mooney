package com.andriybobchuk.mooney.mooney.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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

private const val PAGER_PAGE_COUNT = 3 // currency selection, base currency, welcome

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToMain: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { PAGER_PAGE_COUNT })

    // Sync pager with ViewModel state
    LaunchedEffect(state.page) {
        val targetPage = state.page.ordinal
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync ViewModel with pager swipes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetPage = OnboardingPage.entries[page]
            if (state.page != targetPage) {
                when (targetPage) {
                    OnboardingPage.CURRENCY_SELECTION ->
                        viewModel.onAction(OnboardingAction.BackToCurrencySelection)
                    OnboardingPage.BASE_CURRENCY ->
                        if (state.canContinueToBaseCurrency) {
                            viewModel.onAction(OnboardingAction.ContinueToBaseCurrency)
                        } else {
                            // Can't go forward without currencies — snap back
                            pagerState.animateScrollToPage(0)
                        }
                    OnboardingPage.WELCOME -> {} // Welcome reached via GetStarted only
                }
            }
        }
    }

    // Block swiping forward to welcome page (only reachable via button)
    // and block swiping forward to base currency without selections
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { target ->
            if (target == 2 && state.page != OnboardingPage.WELCOME) {
                // Block swipe to welcome — snap back
                pagerState.animateScrollToPage(1)
            }
            if (target == 1 && !state.canContinueToBaseCurrency) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // Initial fade-in
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(800, easing = EaseOut))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(prominent = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha.value)
        ) {
            // Segmented progress (only for setup pages, not welcome)
            val isOnWelcome = pagerState.currentPage == 2 ||
                (pagerState.currentPage == 1 && pagerState.currentPageOffsetFraction > 0.5f)
            if (!isOnWelcome) {
                OnboardingProgressBar(
                    currentStep = pagerState.currentPage,
                    totalSteps = 2,
                    pagerOffset = pagerState.currentPageOffsetFraction
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = state.page != OnboardingPage.WELCOME // disable swiping on welcome
            ) { page ->
                when (page) {
                    0 -> CurrencySelectionContent(
                        state = state,
                        onContinue = {
                            viewModel.onAction(OnboardingAction.ContinueToBaseCurrency)
                        },
                        onToggle = { viewModel.onAction(OnboardingAction.ToggleCurrency(it)) }
                    )
                    1 -> BaseCurrencyContent(
                        state = state,
                        onSelect = { viewModel.onAction(OnboardingAction.SelectBaseCurrency(it)) },
                        onGetStarted = { viewModel.onAction(OnboardingAction.GetStarted) }
                    )
                    2 -> WelcomeContent(
                        onSwipeUp = { viewModel.onAction(OnboardingAction.EnterApp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int,
    pagerOffset: Float
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topInset + 16.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            val fillFraction = when {
                index < currentStep -> 1f
                index == currentStep -> (1f + pagerOffset).coerceIn(0f, 1f)
                index == currentStep + 1 && pagerOffset > 0f -> pagerOffset.coerceIn(0f, 1f)
                else -> 0f
            }
            val animatedFill by animateFloatAsState(
                targetValue = fillFraction,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
            // Soft foreground: onBackground at 55% — visible on both light and dark,
            // feels integrated with the content rather than fighting for attention
            val activeColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            val inactiveColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = inactiveColor)
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = activeColor,
                        size = size.copy(width = size.width * animatedFill)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencySelectionContent(
    state: OnboardingState,
    onContinue: () -> Unit,
    onToggle: (Currency) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.06f))

        Text(
            text = "Welcome to Mooney",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose currencies you use (up to ${state.maxCurrencies})",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.currencies) { currency ->
                val isSelected = state.selectedCurrencies.contains(currency)
                val canSelect = isSelected || state.selectedCurrencies.size < state.maxCurrencies
                CurrencyChip(
                    currency = currency,
                    selected = isSelected,
                    enabled = canSelect,
                    onClick = { onToggle(currency) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.canContinueToBaseCurrency,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BaseCurrencyContent(
    state: OnboardingState,
    onSelect: (Currency) -> Unit,
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.06f))

        Text(
            text = "Primary Currency",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "All totals and analytics will be shown in this currency",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        val selectedList = state.selectedCurrencies.toList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (selectedList.size <= 3) selectedList.size.coerceAtLeast(1) else 3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(selectedList) { currency ->
                CurrencyChip(
                    currency = currency,
                    selected = currency == state.baseCurrency,
                    enabled = true,
                    onClick = { onSelect(currency) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.canGetStarted && !state.isLoading,
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun WelcomeContent(
    onSwipeUp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var swiped by remember { mutableStateOf(false) }

    val slideOffset = remember { Animatable(0f) }
    val swipeAlpha = remember { Animatable(1f) }

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

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.35f), transparent),
                center = Offset(cx + cx * 0.3f, cy - cy * 0.25f),
                radius = cx * 0.7f
            ),
            radius = cx * 0.7f,
            center = Offset(cx + cx * 0.3f, cy - cy * 0.25f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.25f), transparent),
                center = Offset(cx, cy),
                radius = cx * 0.85f
            ),
            radius = cx * 0.85f,
            center = Offset(cx, cy)
        )

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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.25f),
        animationSpec = tween(250, easing = EaseOut)
    )
    val textColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.3f else 0.1f),
        animationSpec = tween(250, easing = EaseOut)
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
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
