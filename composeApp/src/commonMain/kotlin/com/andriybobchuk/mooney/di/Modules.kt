package com.andriybobchuk.mooney.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.mooney.core.data.HttpClientFactory
import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.presentation.account.AccountViewModel
import com.andriybobchuk.mooney.mooney.presentation.analytics.AnalyticsViewModel
import com.andriybobchuk.mooney.mooney.presentation.transaction.TransactionViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase

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
    singleOf(::GetCategoriesUseCase)
    singleOf(::ConvertAccountsToUiUseCase)
    singleOf(::CurrencyManagerUseCase)


    viewModelOf(::AccountViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::AnalyticsViewModel)
}