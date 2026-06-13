package com.andriybobchuk.mooney.app


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsScreen
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsViewModel
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsBreakdownScreen
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsNetIncomeScreen
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsScreen
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.mooney.mooney.presentation.onboarding.OnboardingScreen
import com.andriybobchuk.mooney.mooney.presentation.onboarding.OnboardingViewModel
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ProcessRecurringTransactionsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetUserPreferencesUseCase
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.andriybobchuk.mooney.mooney.presentation.exchange.ExchangeScreen
import com.andriybobchuk.mooney.mooney.presentation.exchange.ExchangeViewModel
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsScreen
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionsScreen
import com.andriybobchuk.mooney.mooney.presentation.recurring.RecurringTransactionsScreen
import com.andriybobchuk.mooney.mooney.presentation.recurring.RecurringTransactionsViewModel
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsScreen
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsViewModel
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val SECONDARY_WORK_DELAY_MS = 1500L

@Suppress("ThrowsCount")
@Composable
fun NavigationHost() {
    val getAccountsUseCase: GetAccountsUseCase = koinInject()
    val analyticsTracker: AnalyticsTracker = koinInject()
    val preferencesRepository: PreferencesRepository = koinInject()
    // Pre-warm the app-wide data cache as early as possible — touching it here
    // forces Koin to construct the singleton, which kicks off its eager
    // SharingStarted.Eagerly collection so the snapshot is populated by the
    // time the first screen renders. Without this, the cache would lazy-init
    // on the first screen's first read, giving us back the shimmer flash.
    @Suppress("UNUSED_VARIABLE")
    val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache = koinInject()

    // Sync-readable mirror — lets the first frame pick a start destination
    // and the right base currency without waiting on DataStore. Returns null
    // only on the very first launch after install/upgrade; the LaunchedEffect
    // below backfills the mirror and corrects the destination if needed.
    val startupPrefs: StartupPrefs = koinInject()
    val cachedOnboardingCompleted = remember { startupPrefs.getOnboardingCompleted() }
    remember {
        startupPrefs.getDefaultCurrency()?.let { code ->
            try {
                GlobalConfig.baseCurrency = Currency.valueOf(code)
            } catch (_: IllegalArgumentException) { /* keep default */ }
        }
        Unit
    }

    var startDestination by remember {
        mutableStateOf<Route?>(
            when (cachedOnboardingCompleted) {
                true -> Route.MooneyGraph
                false -> Route.Onboarding
                null -> null
            }
        )
    }
    LaunchedEffect(Unit) {
        val prefs = preferencesRepository.getCurrentPreferences()
        val savedCurrency = try {
            Currency.valueOf(prefs.defaultCurrency)
        } catch (_: IllegalArgumentException) {
            Currency.USD
        }
        GlobalConfig.baseCurrency = savedCurrency

        if (prefs.onboardingCompleted) {
            if (startDestination != Route.MooneyGraph) startDestination = Route.MooneyGraph
        } else {
            // Existing user with accounts — silently mark onboarding complete
            val accounts = getAccountsUseCase().first()
            if (accounts.filterNotNull().isNotEmpty()) {
                preferencesRepository.markOnboardingCompleted()
                if (startDestination != Route.MooneyGraph) startDestination = Route.MooneyGraph
            } else {
                if (startDestination != Route.Onboarding) startDestination = Route.Onboarding
            }
        }
    }

    val resolvedStart = startDestination
    if (resolvedStart == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val hasAccounts by getAccountsUseCase().map { accounts ->
        accounts.filterNotNull().isNotEmpty()
    }.collectAsStateWithLifecycle(initialValue = true)

    val navController = rememberNavController()

    // Track screen views. Also doubles as our tap-count signal: each navigation
    // change counts as a meaningful tap for interstitial eligibility. The
    // trigger moment is now entering the Analytics tab (NOT Transactions —
    // returning to home should feel calm). Analytics is a "decision moment"
    // where a brief ad break is more tolerable. AdEligibilityUseCase still
    // enforces the cooldown / tap threshold / once-per-session cap.
    val currentBackStackEntry by navController.currentBackStackEntryFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val interstitialEligibility: com.andriybobchuk.mooney.core.ads.AdEligibilityUseCase = koinInject()
    var navTapCount by remember { mutableStateOf(0) }
    // Counts Analytics screen visits this session. The FIRST visit per
    // session is allowed to fire an interstitial (unlike Transactions,
    // Analytics is a deliberate destination, not a "just opened the app"
    // moment).
    var analyticsVisitCount by remember { mutableStateOf(0) }
    // sessionCount is set further down (inside the ads-init LaunchedEffect)
    // — read whatever value it currently has when an interstitial trigger
    // moment fires.
    val sessionCountForInterstitial = remember { mutableStateOf(0) }
    LaunchedEffect(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        val screenName = route.substringAfterLast(".")
        analyticsTracker.trackScreenView(screenName)
        navTapCount += 1
        if (screenName == "Analytics") {
            analyticsVisitCount += 1
            if (interstitialEligibility.isEligible(
                    placement = com.andriybobchuk.mooney.core.ads.AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS,
                    sessionTapCount = navTapCount,
                    sessionCount = sessionCountForInterstitial.value
                )
            ) {
                val shown = com.andriybobchuk.mooney.core.ads.Ads.showInterstitialIfReady()
                if (shown) {
                    interstitialEligibility.markShown(
                        com.andriybobchuk.mooney.core.ads.AdPlacement.INTERSTITIAL_RETURN_TO_TRANSACTIONS
                    )
                }
            }
        }
    }

    // Set user properties at app start.
    // These are user-level cohort dimensions in Firebase Analytics. Pick ones
    // that change rarely and that you actually want to filter funnels by.
    val getTransactionsUseCase: GetTransactionsUseCase = koinInject()
    val getUserCurrenciesUseCase: GetUserCurrenciesUseCase = koinInject()
    val getUserPreferencesUseCase: GetUserPreferencesUseCase = koinInject()
    val getGoalsUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.GetGoalsUseCase = koinInject()
    val premiumManagerProps: com.andriybobchuk.mooney.core.premium.PremiumManager = koinInject()
    val recurringTransactionDao: com.andriybobchuk.mooney.core.data.database.RecurringTransactionDao = koinInject()
    LaunchedEffect(Unit) {
        // Best-effort telemetry — deliberately delayed so it doesn't compete
        // with the first interactive frame. 1.5s is comfortably after any
        // realistic cold-start.
        kotlinx.coroutines.delay(SECONDARY_WORK_DELAY_MS)
        try {
            val accounts = getAccountsUseCase().first()
            val transactions = getTransactionsUseCase().first()
            val currencies = getUserCurrenciesUseCase().first()
            val prefs = getUserPreferencesUseCase().first()
            val goals = getGoalsUseCase().first()
            val isPro = premiumManagerProps.getIsPremium()
            val recurringCount = recurringTransactionDao.getAll().first().size

            val accountCount = accounts.filterNotNull().size
            val accountBucket = when {
                accountCount == 0 -> "0"
                accountCount <= 2 -> "1-2"
                accountCount <= 5 -> "3-5"
                accountCount <= 10 -> "6-10"
                else -> "11+"
            }
            analyticsTracker.setUserProperty("account_count_bucket", accountBucket)

            val txCount = transactions.filterNotNull().size
            val bucket = when {
                txCount == 0 -> "0"
                txCount <= 10 -> "1-10"
                txCount <= 50 -> "11-50"
                txCount <= 200 -> "51-200"
                txCount <= 1000 -> "201-1000"
                else -> "1000+"
            }
            analyticsTracker.setUserProperty("transaction_count_bucket", bucket)
            analyticsTracker.setUserProperty("base_currency", prefs.defaultCurrency)
            analyticsTracker.setUserProperty("theme_mode", prefs.themeMode.name.lowercase())
            analyticsTracker.setUserProperty("app_language", prefs.appLanguage)
            analyticsTracker.setUserProperty("currency_count", currencies.size.toString())
            analyticsTracker.setUserProperty("is_pro", isPro.toString())
            analyticsTracker.setUserProperty("has_recurring", (recurringCount > 0).toString())
            analyticsTracker.setUserProperty("has_goals", goals.isNotEmpty().toString())
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // User properties are best-effort — never crash the app for analytics
        }
    }

    // Sync subscription status on app start — deferred so StoreKit/Billing
    // IPC doesn't fight the first interactive frame.
    val premiumManager: com.andriybobchuk.mooney.core.premium.PremiumManager = koinInject()
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(SECONDARY_WORK_DELAY_MS)
        try {
            premiumManager.syncSubscriptionStatus()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort — offline cache remains valid
        }
    }

    // Process recurring transactions on app start — also deferred.
    val processRecurringUseCase: ProcessRecurringTransactionsUseCase = koinInject()
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(SECONDARY_WORK_DELAY_MS)
        try {
            processRecurringUseCase()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort — never crash the app for recurring processing
        }
    }

    // Navigate to Assets once if user has no accounts (first launch after onboarding)
    LaunchedEffect(Unit) {
        if (!hasAccounts && resolvedStart == Route.MooneyGraph) {
            navController.navigate(Route.Accounts) {
                popUpTo(Route.MooneyGraph) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Ads — session counter + preload. Read APP_OPEN_COUNT once, increment,
    // persist. Drives the eligibility grace window (first 3 sessions = no
    // ads). Preload interstitial + rewarded in parallel so they're warm by
    // the time any eligible placement asks to show one.
    val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = koinInject()
    var sessionCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            val current = dataStore.data.first()[
                com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.APP_OPEN_COUNT
            ] ?: 0
            val next = current + 1
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.APP_OPEN_COUNT] = next
            }
            sessionCount = next
            sessionCountForInterstitial.value = next
            // Eagerly load both ad formats so the first eligible UI moment
            // doesn't have to wait on the network. Both are cheap no-ops
            // when the bridge isn't wired (Android, or pre-SDK iOS builds).
            kotlinx.coroutines.delay(SECONDARY_WORK_DELAY_MS)
            com.andriybobchuk.mooney.core.ads.Ads.preloadInterstitial(
                com.andriybobchuk.mooney.core.ads.AdUnitIds.interstitial
            )
            com.andriybobchuk.mooney.core.ads.Ads.preloadRewarded(
                com.andriybobchuk.mooney.core.ads.AdUnitIds.rewarded
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort — no ads if the count fails to read/write.
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.andriybobchuk.mooney.core.ads.LocalAdSession provides
            com.andriybobchuk.mooney.core.ads.AdSession(
                sessionCount = sessionCount,
                tapCount = 0 // banner placements don't care; only interstitials do
            )
    ) {
    NavHost(
        navController = navController,
        startDestination = resolvedStart,
        // Instant tab switches — the default ~300ms fade caused a visible
        // white flash between bottom-nav destinations. Mooney's screens are
        // independent enough that animating between them is pure overhead.
        enterTransition = { androidx.compose.animation.EnterTransition.None },
        exitTransition = { androidx.compose.animation.ExitTransition.None },
        popEnterTransition = { androidx.compose.animation.EnterTransition.None },
        popExitTransition = { androidx.compose.animation.ExitTransition.None }
    ) {
        composable<Route.Onboarding> {
            val viewModel = koinViewModel<OnboardingViewModel>()
            OnboardingScreen(
                viewModel = viewModel,
                onNavigateToMain = {
                    navController.navigate(Route.MooneyGraph) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                    navController.navigate(Route.Accounts) {
                        popUpTo(Route.MooneyGraph) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        navigation<Route.MooneyGraph>(
            startDestination = Route.Transactions
        ) {
            // Bottom-nav tab ViewModels are scoped to the parent MooneyGraph so
            // they survive tab switching. Without this, each switch recreates
            // the VM, which flashes shimmer / re-renders empty state for a frame
            // even though all data is local. Same pattern as Analytics below.
            composable<Route.Transactions> { entry ->
                val viewModel = entry.sharedKoinViewModel<TransactionViewModel>(navController)
                TransactionsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 0) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onNavigateToAssets = { navController.navigate(Route.Accounts) { popUpTo(Route.MooneyGraph) } },
                    onNavigateToRecurring = { navController.navigate(Route.RecurringTransactions) },
                    onNavigateToTransactionCategories = { navController.navigate(Route.Categories) },
                    onNavigateToGoals = { navController.navigate(Route.Goals) }
                )
            }
            composable<Route.Accounts> { entry ->
                val viewModel = entry.sharedKoinViewModel<AssetsViewModel>(navController)
                AssetsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 1) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onNavigateToAssetCategories = { navController.navigate(Route.Categories) },
                    onGoalsClick = if (FeatureFlags.goalsEnabled) {
                        { navController.navigate(Route.Goals) }
                    } else null
                )
            }

            // Exchange route is registered unconditionally so the Quick-Actions
            // chip on the Transactions screen can route here even when the
            // bottom-nav tab itself is hidden by the feature flag.
            composable<Route.Exchange> { entry ->
                val viewModel = entry.sharedKoinViewModel<ExchangeViewModel>(navController)
                ExchangeScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 2) },
                    onSettingsClick = { navController.navigate(Route.Settings) }
                )
            }

            composable<Route.Analytics> {
                val viewModel = it.sharedKoinViewModel<AnalyticsViewModel>(navController)
                AnalyticsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 3) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onNavigateToTransactions = {
                        navController.navigate(Route.Transactions) { popUpTo(Route.MooneyGraph) }
                    },
                    onNavigateToBreakdown = { type ->
                        when (type) {
                            "REVENUE" -> navController.navigate(Route.AnalyticsRevenue)
                            "OPERATING_COSTS" -> navController.navigate(Route.AnalyticsCosts)
                            "TAXES" -> navController.navigate(Route.AnalyticsTaxes)
                        }
                    },
                    onNavigateToNetIncome = { navController.navigate(Route.AnalyticsNetIncome) },
                    onNavigateToNetWorth = { navController.navigate(Route.NetWorthDetail) }
                )
            }

            composable<Route.AnalyticsRevenue> { entry ->
                val viewModel = entry.sharedKoinViewModel<AnalyticsViewModel>(navController)
                AnalyticsBreakdownScreen(viewModel = viewModel, type = "REVENUE", onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() })
            }

            composable<Route.AnalyticsCosts> { entry ->
                val viewModel = entry.sharedKoinViewModel<AnalyticsViewModel>(navController)
                AnalyticsBreakdownScreen(viewModel = viewModel, type = "OPERATING_COSTS", onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() })
            }

            composable<Route.AnalyticsTaxes> { entry ->
                val viewModel = entry.sharedKoinViewModel<AnalyticsViewModel>(navController)
                AnalyticsBreakdownScreen(viewModel = viewModel, type = "TAXES", onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() })
            }

            composable<Route.AnalyticsNetIncome> { entry ->
                val viewModel = entry.sharedKoinViewModel<AnalyticsViewModel>(navController)
                AnalyticsNetIncomeScreen(viewModel = viewModel, onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() })
            }

            composable<Route.NetWorthDetail> { entry ->
                val viewModel = entry.sharedKoinViewModel<AnalyticsViewModel>(navController)
                com.andriybobchuk.mooney.mooney.presentation.analytics.NetWorthDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                )
            }

            if (FeatureFlags.goalsEnabled) {
                composable<Route.Goals> {
                    val viewModel = koinViewModel<GoalsViewModel>()
                    GoalsScreen(
                        viewModel = viewModel,
                        onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                    )
                }
            }

            composable<Route.RecurringTransactions> {
                val viewModel = koinViewModel<RecurringTransactionsViewModel>()
                RecurringTransactionsScreen(
                    viewModel = viewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                )
            }

            composable<Route.Settings> { entry ->
                // Scoped to MooneyGraph so the VM survives tab switching, just
                // like the other primary tabs.
                val viewModel = entry.sharedKoinViewModel<SettingsViewModel>(navController)
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() },
                    onNavigateToTransactionCategories = { navController.navigate(Route.Categories) },
                    onNavigateToAssetCategories = { navController.navigate(Route.Categories) },
                    onReplayOnboarding = {
                        // Pop everything down to the onboarding destination so
                        // finishing it lands you back on the main graph fresh.
                        navController.navigate(Route.Onboarding) {
                            popUpTo(Route.MooneyGraph) { inclusive = true }
                        }
                    },
                    bottomNavbar = { BottomNavigationBar(navController, 4) }
                )
            }

            composable<Route.TransactionCategories> {
                val viewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesViewModel>()
                com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesScreen(
                    viewModel = viewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                )
            }

            composable<Route.AssetCategories> {
                val viewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesViewModel>()
                com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesScreen(
                    viewModel = viewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                )
            }

            // Unified categories — one screen with tabbed Transactions/Assets.
            // Both ViewModels are pulled at this point so each tab is fully
            // wired without needing to round-trip through the old routes.
            composable<Route.Categories> {
                val txViewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesViewModel>()
                val assetViewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesViewModel>()
                com.andriybobchuk.mooney.mooney.presentation.categories.CategoriesScreen(
                    transactionViewModel = txViewModel,
                    assetViewModel = assetViewModel,
                    onBackClick = { if (navController.previousBackStackEntry != null) navController.navigateUp() }
                )
            }
        }
    }
    } // closes LocalAdSession CompositionLocalProvider

}

@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.sharedKoinViewModel(
    navController: NavController
): T {
    val navGraphRoute = destination.parent?.route ?: return koinViewModel<T>()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return koinViewModel(
        viewModelStoreOwner = parentEntry
    )
}
