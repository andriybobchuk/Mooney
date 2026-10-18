package com.andriybobchuk.mooney.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.andriybobchuk.mooney.core.data.HttpClientFactory
import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.core.data.database.MooneyDatabaseFactory
import com.andriybobchuk.mooney.core.data.category.BundledCategoryProvider
import com.andriybobchuk.mooney.core.data.category.DefaultCategoryProvider
import com.andriybobchuk.mooney.core.data.category.RemoteCategoryProvider
import com.andriybobchuk.mooney.mooney.data.FrankfurterHistoricalRateProvider
import com.andriybobchuk.mooney.mooney.domain.HistoricalRateProvider
import com.andriybobchuk.mooney.mooney.data.DefaultCoreRepositoryImpl
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

import com.andriybobchuk.mooney.mooney.domain.devtools.DevToolsManager
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.CatchUpNetWorthSnapshotsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.GetNetWorthHistoryUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.SetManualNetWorthUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.networth.WriteNetWorthSnapshotUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.*
import com.andriybobchuk.mooney.mooney.domain.usecase.assets.*
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.data.settings.DataStorePreferencesRepository
import com.andriybobchuk.mooney.core.data.preferences.PreferencesDataStoreFactory
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
import com.andriybobchuk.mooney.core.review.RequestReviewUseCase
import com.andriybobchuk.mooney.core.platform.TransactionIntentHandler
import com.andriybobchuk.mooney.core.review.ReviewPromptManager
import com.andriybobchuk.mooney.mooney.presentation.upkeep.UpkeepReviewViewModel
import com.andriybobchuk.mooney.mooney.presentation.screenshot.ScreenshotImportViewModel
import com.andriybobchuk.mooney.core.platform.ScreenshotOcrParser
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.ParseScreenshotUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.DedupScreenshotCandidatesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.RunScreenshotImportUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.screenshot.CommitScreenshotImportUseCase
import org.koin.core.module.dsl.viewModel
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
import com.andriybobchuk.mooney.core.ads.AdEligibilityUseCase
import com.andriybobchuk.mooney.core.premium.PremiumManager

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    // Application-lifetime coroutine scope. Used by the AppDataCache below
    // to keep its underlying flows alive for the whole process. A
    // SupervisorJob ensures a single failed child flow doesn't tear down
    // the whole cache.
    single<kotlinx.coroutines.CoroutineScope> {
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
        )
    }

    singleOf(::DefaultCoreRepositoryImpl).bind<CoreRepository>()

    // Bottom-nav double-tap → scroll-to-top channel. App-lifetime singleton.
    single { com.andriybobchuk.mooney.app.ScrollToTopBus() }

    // Reminder notification scheduler. Platform-specific actual under the
    // hood; the schedule itself is driven by Settings → Reminders (off /
    // daily / weekly + time picker).
    single { com.andriybobchuk.mooney.core.notifications.ReminderScheduler() }

    // App-wide data snapshot cache. Eager StateFlow over every reactive data
    // source — every screen reads the latest snapshot from here so tab
    // switches paint cached data on the first frame instead of flashing
    // shimmer or empty-state placeholders between identical states.
    single<com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache> {
        com.andriybobchuk.mooney.mooney.data.cache.DefaultAppDataCache(
            getTransactionsUseCase = get(),
            getAccountsUseCase = get(),
            getGoalsUseCase = get(),
            getRecurringTransactionsUseCase = get(),
            getUserCurrenciesUseCase = get(),
            getCategoriesUseCase = get(),
            coreRepository = get(),
            categoryDao = get(),
            assetCategoryDao = get(),
            pendingTransactionDao = get(),
            appScope = get()
        )
    }

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
    single { get<AppDatabase>().historicalRateDao }
    single { get<AppDatabase>().rateWatchAlertDao }
    single { get<AppDatabase>().merchantCategoryHistoryDao }
    single { get<AppDatabase>().netWorthSnapshotDao }

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

// Exchange Rate Providers — Switchable picks one based on the user's Settings choice.
    single { com.andriybobchuk.mooney.mooney.data.LiveExchangeRateProvider(httpClient = get()) }
    single { com.andriybobchuk.mooney.mooney.data.ExtendedExchangeRateProvider(httpClient = get()) }
    single<com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider> {
        com.andriybobchuk.mooney.mooney.data.SwitchableExchangeRateProvider(
            extended = get(),
            historical = get(),
            dataStore = get()
        )
    }

    // Use Cases — existing
    singleOf(::AddTransactionUseCase)
    singleOf(::TrackFirstEventUseCase)
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
    singleOf(::ReconcileAccountFromBalanceUseCase)

    // Net-worth snapshots — one row per account per month-end.
    // CatchUp fires on app visit to Balance and backfills missing months
    // (reconstructed from current balance + reversed deltas); subsequent
    // months get captured naturally when the user opens the app.
    singleOf(::WriteNetWorthSnapshotUseCase)
    singleOf(::CatchUpNetWorthSnapshotsUseCase)
    singleOf(::GetNetWorthHistoryUseCase)
    singleOf(::SetManualNetWorthUseCase)
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
    singleOf(::ManageTransactionCategoryOrderUseCase)
    single { ReviewPromptManager() }
    singleOf(::RequestReviewUseCase)

    // Settings Use Cases
    singleOf(::GetUserPreferencesUseCase)
    singleOf(::UpdatePinnedCategoriesUseCase)
    singleOf(::GetPinnedCategoriesUseCase)

    // Validation
    singleOf(::ValidateTransactionUseCase)

    // Recurring Transactions
    singleOf(::ProcessRecurringTransactionsUseCase)
    singleOf(::ImportCsvUseCase)
    single { com.andriybobchuk.mooney.mooney.domain.backup.UniversalCsvImporter() }
    singleOf(::AcceptPendingTransactionUseCase)
    singleOf(::GetRecurringTransactionsUseCase)
    singleOf(::SaveRecurringTransactionUseCase)
    singleOf(::DeleteRecurringTransactionUseCase)
    singleOf(::CreateRecurringFromTransactionUseCase)

    // Demo data seeder — for marketing / screen recording. Only usable when
    // the app is empty; will be removed before the real 1.0 release.
    singleOf(::SeedDemoDataUseCase)

    // Platform intent bridge — iOS App Intents / Siri Shortcuts today,
    // Android quick-tiles later. Handler resolves loose Shortcut strings
    // into real domain objects (Category, Account) and persists via the
    // same use case as the in-app Add sheet.
    singleOf(::TransactionIntentHandler)

    // Upkeep review flow — auto-added tx queue + smart category learning.
    singleOf(::GetAutoAddedPendingUseCase)
    singleOf(::GetAutoAddedCountFlowUseCase)
    singleOf(::ReviewAutoAddedUseCase)
    singleOf(::GetSmartCategoryForMerchantUseCase)
    singleOf(::GetFrequentExpenseCategoryIdsUseCase)

    // Screenshot bulk-import — Phase 3 "second line of automation".
    // Parser is platform-actual (Swift PhotosPicker + Vision on iOS,
    // no-op stub on Android for now); the rest is pure common Kotlin so
    // adding ML Kit on Android later only means swapping the parser.
    single { ScreenshotOcrParser() }

    // Account photo-hero picker. iOS binds a real PhotosPicker + JPEG-to-
    // Documents writer via the Swift bridge; Android stub returns null.
    // Shared facade is constructor-less so it lives in common. The real
    // ImageStorage is platform-registered (Android needs Context) in
    // platformModule.
    single { com.andriybobchuk.mooney.core.platform.AccountImagePickerFacade() }
    singleOf(::ParseScreenshotUseCase)
    singleOf(::DedupScreenshotCandidatesUseCase)
    singleOf(::RunScreenshotImportUseCase)
    // Screenshot commit intentionally does NOT depend on smart learning —
    // see CommitScreenshotImportUseCase doc for the reasoning (noisy OCR
    // merchant names would pollute the learning dict).
    singleOf(::CommitScreenshotImportUseCase)

    // Onboarding
    singleOf(::CompleteOnboardingUseCase)

    // Default Category Provider (Remote Config → bundled fallback)
    singleOf(::BundledCategoryProvider)
    singleOf(::RemoteCategoryProvider).bind<DefaultCategoryProvider>()
    singleOf(::SyncDefaultCategoriesUseCase)
    singleOf(::UpdateTransactionCategoriesUseCase)
    singleOf(::ReportCategoryUsageUseCase)

    // Historical Rates & Rate Watch
    singleOf(::FrankfurterHistoricalRateProvider).bind<HistoricalRateProvider>()
    singleOf(::LoadHistoricalRatesUseCase)
    singleOf(::CalculateRatePercentileUseCase)
    singleOf(::CheckRateAlertsUseCase)
    singleOf(::ManageRateWatchUseCase)

    // Dev Tools
    singleOf(::DevToolsManager)

    // Theme
    singleOf(::ThemeManager)

    // Analytics — registered before PremiumManager so its `get()` resolves.
    singleOf(::DefaultAnalyticsTracker).bind<AnalyticsTracker>()

    // Premium
    single { PremiumManager(get(), get(), get()) }

    // App Lock — PIN-gated entry. Premium-only at the entry point.
    single { com.andriybobchuk.mooney.core.security.AppLockManager(get(), get()) }

    // Ads — eligibility/frequency capping. The SDK itself is invoked via
    // `Ads.kt` (expect/actual; iOS bridges to Swift, Android no-op until we
    // wire play-services-ads). See core/ads/Ads.kt.
    singleOf(::AdEligibilityUseCase)

    // Explicit viewModel { } — AssetsViewModel outgrew Koin's viewModelOf
    // reflective builder (capped at 22 constructor params) once we added the
    // net-worth-snapshot use cases. Order must match the primary constructor.
    viewModel {
        AssetsViewModel(
            getAccountsUseCase = get(),
            addAccountUseCase = get(),
            deleteAccountUseCase = get(),
            calculateNetWorthUseCase = get(),
            convertAccountsToUiUseCase = get(),
            currencyManagerUseCase = get(),
            manageAssetCategoryOrderUseCase = get(),
            manageCategoryExpansionUseCase = get(),
            shouldRefreshExchangeRatesUseCase = get(),
            getUserCurrenciesUseCase = get(),
            assetCategoryDao = get(),
            analyticsTracker = get(),
            premiumManager = get(),
            loadHistoricalRatesUseCase = get(),
            calculateRatePercentileUseCase = get(),
            dataStore = get(),
            trackFirstEventUseCase = get(),
            appDataCache = get(),
            reconcileAccountFromBalanceUseCase = get(),
            accountImagePickerFacade = get(),
            imageStorage = get(),
            catchUpNetWorthSnapshotsUseCase = get(),
            getNetWorthHistoryUseCase = get(),
            setManualNetWorthUseCase = get()
        )
    }
    // Manual registration — TransactionViewModel has too many params for `viewModelOf`.
    viewModel {
        TransactionViewModel(
            getTransactionsUseCase = get(),
            addTransactionUseCase = get(),
            deleteTransactionUseCase = get(),
            getAccountsUseCase = get(),
            getCategoriesUseCase = get(),
            calculateTransactionTotalUseCase = get(),
            calculateDailyTotalUseCase = get(),
            convertAccountsToUiUseCase = get(),
            currencyManagerUseCase = get(),
            getPinnedCategoriesUseCase = get(),
            filterTransactionsByMonthUseCase = get(),
            calculateDailyTotalsMapUseCase = get(),
            pendingTransactionDao = get(),
            acceptPendingTransactionUseCase = get(),
            createRecurringFromTransactionUseCase = get(),
            analyticsTracker = get(),
            preferencesRepository = get(),
            assetCategoryDao = get(),
            categoryDao = get(),
            coreRepository = get(),
            manageCategoryExpansionUseCase = get(),
            manageAssetCategoryOrderUseCase = get(),
            manageTransactionCategoryOrderUseCase = get(),
            requestReviewUseCase = get(),
            trackFirstEventUseCase = get(),
            appDataCache = get(),
            getAutoAddedCountFlowUseCase = get()
        )
    }
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::ExchangeViewModel)
    viewModelOf(::GoalsViewModel)
    // Explicit viewModel { } — SettingsViewModel outgrew Koin's viewModelOf
    // reflective builder (capped around 22 constructor params). Order must
    // match the primary constructor exactly.
    viewModel {
        SettingsViewModel(
            getUserPreferencesUseCase = get(),
            updatePinnedCategoriesUseCase = get(),
            getCategoriesUseCase = get(),
            getPinnedCategoriesUseCase = get(),
            preferencesRepository = get(),
            dataExportImportManager = get(),
            getUserCurrenciesUseCase = get(),
            updateUserCurrenciesUseCase = get(),
            assetCategoryDao = get(),
            analyticsTracker = get(),
            premiumManager = get(),
            getAccountsUseCase = get(),
            setPrimaryAccountUseCase = get(),
            updateTransactionCategoriesUseCase = get(),
            currencyManagerUseCase = get(),
            dataStore = get(),
            appDataCache = get(),
            universalCsvImporter = get(),
            importCsvUseCase = get(),
            reminderScheduler = get(),
            seedDemoDataUseCase = get(),
            startupPrefs = get(),
            appRestarter = get()
        )
    }
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::RecurringTransactionsViewModel)
    viewModelOf(::TransactionCategoriesViewModel)
    viewModelOf(::AssetCategoriesViewModel)
    viewModelOf(::UpkeepReviewViewModel)
    viewModelOf(::ScreenshotImportViewModel)
}
