package com.andriybobchuk.mooney.app


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsScreen
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsViewModel
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsScreen
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import com.andriybobchuk.mooney.mooney.presentation.exchange.ExchangeScreen
import com.andriybobchuk.mooney.mooney.presentation.exchange.ExchangeViewModel
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsScreen
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionsScreen
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsScreen
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavigationHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.MooneyGraph
    ) {
        navigation<Route.MooneyGraph>(
            startDestination = Route.Transactions
        ) {
            composable<Route.Transactions> {
                val viewModel = koinViewModel<TransactionViewModel>()
                TransactionsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 0) },
                    onSettingsClick = { navController.navigate(Route.Settings) }
                )
            }
            composable<Route.Accounts> {
                val viewModel = koinViewModel<AssetsViewModel>()
                AssetsScreen(
                    viewModel = viewModel,
                    bottomNavbar = { BottomNavigationBar(navController, 1) },
                    onSettingsClick = { navController.navigate(Route.Settings) }
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
                        bottomNavbar = { BottomNavigationBar(navController, 4) },
                        onSettingsClick = { navController.navigate(Route.Settings) }
                    )
                }
            }

            composable<Route.Settings> {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
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
