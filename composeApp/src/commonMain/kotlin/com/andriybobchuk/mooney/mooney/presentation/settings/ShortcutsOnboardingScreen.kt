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
        emoji = "⚡️",
        title = "Add transactions without opening Mooney", // allow-hardcoded (iOS-only edu screen)
        body = "Mooney exposes Siri Shortcuts and App Intents on iOS so you can " +
            "log expenses, check balances, or even chain automations that fire " +
            "when your bank app sends a notification. The next few pages walk " +
            "you through the useful setups."
    ),
    OnboardingPage(
        emoji = "🎙️",
        title = "Try it with Siri right now", // allow-hardcoded (iOS-only edu screen)
        body = "Hold your side button (or say \"Hey Siri\") and try one of these " +
            "phrases. Mooney will confirm right in the Siri overlay — no need to " +
            "open the app.",
        steps = listOf(
            "\"Add expense in Mooney\" — Siri asks for the amount, then logs it.",
            "\"How much did I spend in Mooney this month\" — reads back the total.",
            "\"What's my net worth in Mooney\" — reads back the current sum."
        )
    ),
    OnboardingPage(
        emoji = "📲",
        title = "Auto-log from your bank's push notifications", // allow-hardcoded (iOS-only edu screen)
        body = "The closest thing to real automation: every time your bank app " +
            "sends a push notification about a card purchase, iOS fires an " +
            "automation that parses the amount and logs it in Mooney. Setup takes " +
            "~5 minutes and works with any bank whose app sends push notifs.",
        steps = listOf(
            "Open the Shortcuts app → Automation tab → tap +.",
            "Pick \"Notification\" as the trigger, then select your bank's app.",
            "Add action: \"Get Text from Input\" (the notification body).",
            "Add action: \"Match Text\" with regex like ([0-9]+[.,][0-9]+) to pull the amount.",
            "Add action: \"Add Transaction\" (from the Mooney app group) and map Amount → the matched value.",
            "Turn OFF \"Ask Before Running\" so it runs silently."
        )
    ),
    OnboardingPage(
        emoji = "🏷️",
        title = "Tap-to-log with an NFC tag", // allow-hardcoded (iOS-only edu screen)
        body = "Stick an NFC tag on your wallet or your desk. Tap your phone to " +
            "it and Mooney prompts for the amount, then logs the expense — " +
            "great for cash purchases that never leave a digital trace.",
        steps = listOf(
            "Shortcuts → Automation → tap +.",
            "Trigger: NFC → scan a blank tag to bind it.",
            "Action: \"Ask for Input\" → prompt \"Amount?\".",
            "Action: \"Add Transaction\" (Mooney) → Amount = the Provided Input.",
            "Turn OFF \"Ask Before Running\" for a true one-tap flow."
        )
    ),
    OnboardingPage(
        emoji = "🔗",
        title = "Power-user: URL scheme", // allow-hardcoded (iOS-only edu screen)
        body = "For hand-crafted automations or x-callback-url chains, Mooney " +
            "accepts a plain URL:\n\nmooney://add-tx?amount=12.5&type=expense" +
            "&category=coffee&account=Bank&note=Latte\n\nAny of these params " +
            "can be omitted — Mooney falls back to your default expense category " +
            "and primary account. Type is expense/income."
    )
)
