package com.andriybobchuk.mooney.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.mooney.core.data.HttpClientFactory
import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.*
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.data.settings.DataStorePreferencesRepository
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
import com.andriybobchuk.mooney.mooney.presentation.account.AccountViewModel
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.mooney.mooney.presentation.goals.GoalsViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionViewModel
import com.andriybobchuk.mooney.mooney.presentation.settings.SettingsViewModel
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
    
    // DataStore and Preferences
    single { get<PreferencesDataStoreFactory>().create() }
    singleOf(::DataStorePreferencesRepository).bind<PreferencesRepository>()

    // Feature flags
    single<Boolean>(qualifier = named("use_live_exchange_rates")) { true }
    
    // API Key
    single<String>(qualifier = named("exchangerate_api_key")) { "75b9fafdb8e95b3caea57927" }
    
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

    // Use Cases
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
    singleOf(::AddGoalUseCase)
    singleOf(::DeleteGoalUseCase)
    singleOf(::GetGoalsUseCase)
    singleOf(::CalculateGoalProgressUseCase)
    singleOf(::EstimateGoalCompletionUseCase)
    
    // Settings Use Cases
    singleOf(::GetUserPreferencesUseCase)
    singleOf(::UpdatePinnedCategoriesUseCase)
    singleOf(::GetPinnedCategoriesUseCase)
    
    // Theme
    singleOf(::ThemeManager)

    viewModelOf(::AccountViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::GoalsViewModel)
    viewModelOf(::SettingsViewModel)
}