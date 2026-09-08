package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.mooney.core.presentation.Toolbars
import kotlinx.coroutines.launch

/**
 * iOS-only walkthrough that teaches users to wire Mooney into iOS Shortcuts +
 * Siri — including the "closest thing to auto-capture" recipe (bank push
 * notification → Mooney intent). This is a marketing/education surface, so
 * the copy is English-only for now; ~95% of the target audience for
 * Automation-hungry users reads English.
 *
 * Entry point: Settings → "Automate transactions". Gated on iOS platform in
 * SettingsScreen so Android users never see the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsOnboardingScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Toolbars.Primary(
                title = "Automate transactions", // allow-hardcoded (iOS-only edu screen, English fallback)
                showBackButton = true,
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = emptyList()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(pages[pageIndex], uriHandler::openUri)
            }

            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Next" else "Done", // allow-hardcoded (iOS-only)
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    openUri: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.emoji, fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        page.steps?.let { steps ->
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEachIndexed { idx, stepText ->
                    NumberedStep(number = idx + 1, text = stepText)
                }
            }
        }

        page.ctaLabel?.let { label ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = page.ctaUrl != null) {
                        page.ctaUrl?.let(openUri)
                    }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            val selected = i == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 6.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
        }
    }
}

// ---- content ----

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String,
    val steps: List<String>? = null,
    val ctaLabel: String? = null,
    val ctaUrl: String? = null
)

@Composable
private fun rememberOnboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        emoji = "💳",
        title = "Auto-log every Apple Pay purchase", // allow-hardcoded (iOS-only edu screen)
        body = "iOS has a built-in Wallet automation trigger that fires the " +
            "moment you make a card payment. Point it at Mooney's Add " +
            "Transaction action and every tap-to-pay becomes an instant, " +
            "silent log entry — with amount and merchant filled in " +
            "automatically. Setup takes about a minute."
    ),
    OnboardingPage(
        emoji = "⚙️",
        title = "Create the automation", // allow-hardcoded (iOS-only edu screen)
        body = "Follow these exact steps in the Shortcuts app. Once saved, " +
            "the automation runs silently in the background — Mooney logs " +
            "every payment and sends a confirmation notification so you " +
            "know it worked.",
        steps = listOf(
            "Open the Shortcuts app → tap the Automation tab at the bottom.",
            "Tap + (top right) → scroll to New Automation.",
            "Scroll down the trigger list → tap Wallet.",
            "Select which card(s) should trigger the automation (or leave all cards).",
            "Optionally filter by transaction category (Groceries, Transport, etc.).",
            "Tap Next → tap New Blank Automation.",
            "Tap the search bar at the bottom → search Mooney → tap Add Transaction.",
            "The Amount field auto-wires to the Wallet payment amount.",
            "Tap Description → pick Merchant (or Name) from the magic variables.",
            "Toggle Run Immediately ON at the top so it fires without asking."
        )
    ),
    OnboardingPage(
        emoji = "✅",
        title = "Test it and you're done", // allow-hardcoded (iOS-only edu screen)
        body = "Make any card payment via Apple Pay. Within a second or two " +
            "you'll get a Mooney notification: \"Automatically added 12.50 zł " +
            "for Biedronka\". Open Mooney → Transactions to confirm — the " +
            "entry is already there under your default account and category, " +
            "with the merchant name as the description.\n\nAny future " +
            "categorisation tweaks (change category, edit amount) still work " +
            "the same way as manually added transactions."
    )
)
