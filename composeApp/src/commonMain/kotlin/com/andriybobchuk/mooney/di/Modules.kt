package com.andriybobchuk.mooney.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.mooney.core.data.HttpClientFactory
import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

import com.andriybobchuk.mooney.mooney.domain.devtools.DevToolsManager
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.*
import com.andriybobchuk.mooney.mooney.domain.usecase.assets.*
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.data.settings.DataStorePreferencesRepository
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
import com.andriybobchuk.mooney.mooney.presentation.account.AccountViewModel
import com.andriybobchuk.mooney.mooney.presentation.assets.AssetsViewModel
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.mooney.mooney.presentation.exchange.ExchangeViewModel
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsViewModel
import com.andriybobchuk.mooney.mooney.presentation.categories.AssetCategoriesViewModel
import com.andriybobchuk.mooney.mooney.presentation.categories.TransactionCategoriesViewModel
import com.andriybobchuk.mooney.mooney.presentation.recurring.RecurringTransactionsViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsViewModel
import com.andriybobchuk.mooney.mooney.presentation.onboarding.OnboardingViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.presentation.theme.ThemeManager
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.analytics.DefaultAnalyticsTracker
import com.andriybobchuk.mooney.core.premium.PremiumManager

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    singleOf(::DefaultCoreRepositoryImpl).bind<CoreRepository>()

    single {
        get<MooneyDatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().accountDao }
    single { get<AppDatabase>().transactionDao }
    single { get<AppDatabase>().categoryUsageDao }
    single { get<AppDatabase>().goalDao }
    single { get<AppDatabase>().goalGroupDao }
    single { get<AppDatabase>().recurringTransactionDao }
    single { get<AppDatabase>().pendingTransactionDao }
    single { get<AppDatabase>().categoryDao }
    single { get<AppDatabase>().userCurrencyDao }
    single { get<AppDatabase>().assetCategoryDao }

    // Data Export/Import Manager
    single {
        com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager(
            transactionDao = get(),
            accountDao = get(),
            goalDao = get(),
            goalGroupDao = get(),
            categoryUsageDao = get(),
            categoryDao = get(),
            userCurrencyDao = get(),
            recurringTransactionDao = get(),
            pendingTransactionDao = get(),
            assetCategoryDao = get()
        )
    }

    // DataStore and Preferences
    single { get<PreferencesDataStoreFactory>().create() }
    singleOf(::DataStorePreferencesRepository).bind<PreferencesRepository>()

    // Feature flags
    single<Boolean>(qualifier = named("use_live_exchange_rates")) { true }

    // API Key
    single<String>(qualifier = named("exchangerate_api_key")) { com.andriybobchuk.mooney.core.data.Secrets.EXCHANGE_RATE_API_KEY }

    // Exchange Rate Providers
    single<com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider> {
        val useLiveRates = get<Boolean>(named("use_live_exchange_rates"))
        if (useLiveRates) {
            com.andriybobchuk.mooney.mooney.data.LiveExchangeRateProvider(
                httpClient = get(),
                apiKey = get(named("exchangerate_api_key"))
            )
        } else {
            com.andriybobchuk.mooney.mooney.data.StaticExchangeRateProvider()
        }
    }

    // Use Cases — existing
    singleOf(::AddTransactionUseCase)
    singleOf(::DeleteTransactionUseCase)
    singleOf(::GetTransactionsUseCase)
    singleOf(::AddAccountUseCase)
    singleOf(::DeleteAccountUseCase)
    singleOf(::GetAccountsUseCase)
    singleOf(::CalculateMonthlyAnalyticsUseCase)
    singleOf(::CalculateTransactionTotalUseCase)
    singleOf(::CalculateDailyTotalUseCase)
    singleOf(::CalculateNetWorthUseCase)
    singleOf(::CalculateSubcategoriesUseCase)
    singleOf(::GetCategoriesUseCase)
    singleOf(::GetMostUsedCategoriesUseCase)
    singleOf(::ConvertAccountsToUiUseCase)
    singleOf(::CurrencyManagerUseCase)
    singleOf(::CreateReconciliationUseCase)
    singleOf(::AddGoalUseCase)
    singleOf(::DeleteGoalUseCase)
    singleOf(::GetGoalsUseCase)
    singleOf(::CalculateGoalProgressUseCase)
    singleOf(::EstimateGoalCompletionUseCase)

    // Use Cases — new
    singleOf(::CalculateTaxesUseCase)
    singleOf(::CalculateAnalyticsMetricsUseCase)
    singleOf(::LoadHistoricalAnalyticsUseCase)
    singleOf(::LoadCategoriesForSheetTypeUseCase)
    singleOf(::GetPreviousMonthTransactionsUseCase)
    singleOf(::ReconcileAccountUseCase)
    singleOf(::ShouldRefreshExchangeRatesUseCase)
    singleOf(::FilterTransactionsByMonthUseCase)
    singleOf(::CalculateDailyTotalsMapUseCase)
    singleOf(::EnrichGoalsWithProgressUseCase)
    singleOf(::SaveGoalUseCase)
    singleOf(::CalculateRatesInBaseCurrencyUseCase)

    // Primary Account & Currency Use Cases
    singleOf(::SetPrimaryAccountUseCase)
    singleOf(::GetUserCurrenciesUseCase)
    singleOf(::UpdateUserCurrenciesUseCase)

    // Asset Use Cases
    singleOf(::ManageAssetCategoryOrderUseCase)
    singleOf(::ManageCategoryExpansionUseCase)

    // Settings Use Cases
    singleOf(::GetUserPreferencesUseCase)
    singleOf(::UpdatePinnedCategoriesUseCase)
    singleOf(::GetPinnedCategoriesUseCase)

    // Validation
    singleOf(::ValidateTransactionUseCase)

    // Recurring Transactions
    singleOf(::ProcessRecurringTransactionsUseCase)
    singleOf(::AcceptPendingTransactionUseCase)
    singleOf(::GetRecurringTransactionsUseCase)
    singleOf(::SaveRecurringTransactionUseCase)
    singleOf(::DeleteRecurringTransactionUseCase)
    singleOf(::CreateRecurringFromTransactionUseCase)

    // Onboarding
    singleOf(::CompleteOnboardingUseCase)

    // Dev Tools
    singleOf(::DevToolsManager)

    // Theme
    singleOf(::ThemeManager)

    // Premium
    single { PremiumManager(get(), get()) }

    // Analytics
    singleOf(::DefaultAnalyticsTracker).bind<AnalyticsTracker>()

    viewModelOf(::AccountViewModel)
    viewModelOf(::AssetsViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::ExchangeViewModel)
    viewModelOf(::GoalsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::RecurringTransactionsViewModel)
    viewModelOf(::TransactionCategoriesViewModel)
    viewModelOf(::AssetCategoriesViewModel)
}
