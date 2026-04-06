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
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsScreen
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsViewModel
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

@Composable
fun NavigationHost() {
    val getAccountsUseCase: GetAccountsUseCase = koinInject()
    val analyticsTracker: AnalyticsTracker = koinInject()
    val preferencesRepository: PreferencesRepository = koinInject()

    // Determine start destination based on onboarding state
    var startDestination by remember { mutableStateOf<Route?>(null) }
    LaunchedEffect(Unit) {
        val prefs = preferencesRepository.getCurrentPreferences()
        // Set GlobalConfig.baseCurrency from saved preferences
        val savedCurrency = try {
            Currency.valueOf(prefs.defaultCurrency)
        } catch (_: IllegalArgumentException) {
            Currency.PLN
        }
        GlobalConfig.baseCurrency = savedCurrency

        if (prefs.onboardingCompleted) {
            startDestination = Route.MooneyGraph
        } else {
            // Existing user with accounts — silently mark onboarding complete
            val accounts = getAccountsUseCase().first()
            if (accounts.filterNotNull().isNotEmpty()) {
                preferencesRepository.markOnboardingCompleted()
                startDestination = Route.MooneyGraph
            } else {
                startDestination = Route.Onboarding
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

    // Track screen views
    val currentBackStackEntry by navController.currentBackStackEntryFlow
        .collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        val screenName = route.substringAfterLast(".")
        analyticsTracker.trackScreenView(screenName)
    }

    // Set user properties at app start
    val getTransactionsUseCase: GetTransactionsUseCase = koinInject()
    val getUserCurrenciesUseCase: GetUserCurrenciesUseCase = koinInject()
    val getUserPreferencesUseCase: GetUserPreferencesUseCase = koinInject()
    LaunchedEffect(Unit) {
        try {
            val accounts = getAccountsUseCase().first()
            val transactions = getTransactionsUseCase().first()
            val currencies = getUserCurrenciesUseCase().first()
            val prefs = getUserPreferencesUseCase().first()

            analyticsTracker.setUserProperty("account_count", accounts.filterNotNull().size.toString())
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
        } catch (_: Exception) {
            // User properties are best-effort — never crash the app for analytics
        }
    }

    // Sync subscription status on app start
    val premiumManager: com.andriybobchuk.mooney.core.premium.PremiumManager = koinInject()
    LaunchedEffect(Unit) {
        try {
            premiumManager.syncSubscriptionStatus()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort — offline cache remains valid
        }
    }

    // Process recurring transactions on app start
    val processRecurringUseCase: ProcessRecurringTransactionsUseCase = koinInject()
    LaunchedEffect(Unit) {
        try {
            processRecurringUseCase()
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

    NavHost(
        navController = navController,
        startDestination = resolvedStart
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
            composable<Route.Transactions> {
                val viewModel = koinViewModel<TransactionViewModel>()
                TransactionsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 0) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onNavigateToAssets = { navController.navigate(Route.Accounts) { popUpTo(Route.MooneyGraph) } },
                    onNavigateToRecurring = { navController.navigate(Route.RecurringTransactions) }
                )
            }
            composable<Route.Accounts> {
                val viewModel = koinViewModel<AssetsViewModel>()
                AssetsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 1) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onGoalsClick = if (FeatureFlags.goalsEnabled) {
                        { navController.navigate(Route.Goals) }
                    } else null
                )
            }

            if (FeatureFlags.exchangeEnabled) {
                composable<Route.Exchange> {
                    val viewModel = koinViewModel<ExchangeViewModel>()
                    ExchangeScreen(
                        viewModel = viewModel,
                        bottomNavbar = { BottomNavigationBar(navController, 2) },
                        onSettingsClick = { navController.navigate(Route.Settings) }
                    )
                }
            }

            composable<Route.Analytics> {
                val viewModel = koinViewModel<AnalyticsViewModel>()
                AnalyticsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 3) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                    onNavigateToTransactions = {
                        navController.navigate(Route.Transactions) { popUpTo(Route.MooneyGraph) }
                    }
                )
            }

            if (FeatureFlags.goalsEnabled) {
                composable<Route.Goals> {
                    val viewModel = koinViewModel<GoalsViewModel>()
                    GoalsScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }

            composable<Route.RecurringTransactions> {
                val viewModel = koinViewModel<RecurringTransactionsViewModel>()
                RecurringTransactionsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable<Route.Settings> {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigateUp() },
                    onNavigateToTransactionCategories = { navController.navigate(Route.TransactionCategories) },
                    onNavigateToAssetCategories = { navController.navigate(Route.AssetCategories) }
                )
            }

            composable<Route.TransactionCategories> {
                val viewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesViewModel>()
                com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable<Route.AssetCategories> {
                val viewModel = koinViewModel<com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesViewModel>()
                com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }

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
