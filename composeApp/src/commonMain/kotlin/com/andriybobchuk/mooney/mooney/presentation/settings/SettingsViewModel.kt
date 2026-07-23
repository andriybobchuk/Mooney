package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.premium.PRODUCT_ID_MONTHLY
import com.andriybobchuk.mooney.core.premium.PremiumConfig
import com.andriybobchuk.mooney.core.premium.PremiumManager
import com.andriybobchuk.mooney.core.premium.PurchaseResult
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SetPrimaryAccountUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.UpdateUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetPinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetUserPreferencesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.UpdatePinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.settings.ExchangeRateSource
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class SettingsViewModel(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val updatePinnedCategoriesUseCase: UpdatePinnedCategoriesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getPinnedCategoriesUseCase: GetPinnedCategoriesUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val dataExportImportManager: DataExportImportManager,
    private val getUserCurrenciesUseCase: GetUserCurrenciesUseCase,
    private val updateUserCurrenciesUseCase: UpdateUserCurrenciesUseCase,
    private val assetCategoryDao: AssetCategoryDao,
    private val analyticsTracker: AnalyticsTracker,
    private val premiumManager: PremiumManager,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val setPrimaryAccountUseCase: SetPrimaryAccountUseCase,
    private val updateTransactionCategoriesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.UpdateTransactionCategoriesUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache,
    // CSV import — both injected together so the use case has everything it
    // needs (parsing + DB writes) without the ViewModel having to wire
    // accounts/categories per-call.
    private val universalCsvImporter: com.andriybobchuk.mooney.mooney.domain.backup.UniversalCsvImporter,
    private val importCsvUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.ImportCsvUseCase,
    private val reminderScheduler: com.andriybobchuk.mooney.core.notifications.ReminderScheduler
) : ViewModel() {

    // Seed `isLoading` from the app cache so opening Settings while the cache
    // is warm (the usual case — you reach Settings from a tab, the tabs have
    // already warmed the cache) skips the cold-start spinner entirely.
    private val _state = MutableStateFlow(
        SettingsState(isLoading = !appDataCache.snapshot.value.isReady)
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        observeData()
        observeUserCurrencies()
        observeAssetCategories()
        observeAccounts()
        observeCurrencyInsights()
        observeWidgetPager()
        observeDeveloperOptions()
        observeAdsDisabled()
        observeDevPremiumFlag()
        observeReminderConfig()
    }

    private fun observeReminderConfig() {
        dataStore.data.onEach { prefs ->
            val mode = prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_MODE] ?: "OFF"
            val hour = prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_HOUR]
                ?: com.andriybobchuk.mooney.core.notifications.DEFAULT_REMINDER_HOUR
            val minute = prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_MINUTE]
                ?: com.andriybobchuk.mooney.core.notifications.DEFAULT_REMINDER_MINUTE
            val weekday = prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_WEEKDAY]
                ?: com.andriybobchuk.mooney.core.notifications.DEFAULT_REMINDER_WEEKDAY
            _state.update {
                it.copy(
                    reminderMode = mode,
                    reminderHour = hour,
                    reminderMinute = minute,
                    reminderWeekday = weekday
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeDevPremiumFlag() {
        premiumManager.isPremium.onEach { isPro ->
            _state.update { it.copy(devForcePremium = isPro) }
        }.launchIn(viewModelScope)
    }

    /**
     * Developer Options shortcut — flips the cached Pro flag locally so the
     * paywall and gated screens treat the user as if they had a subscription
     * (or didn't). Doesn't touch any real StoreKit / Play Billing state, so
     * the next `syncSubscriptionStatus()` will overwrite this if the user
     * actually has a live subscription. Test-only.
     */
    fun setDevPlanPro(pro: Boolean) {
        viewModelScope.launch {
            premiumManager.setPremium(pro)
        }
    }

    private fun observeCurrencyInsights() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.CURRENCY_INSIGHTS_ENABLED] ?: false
        }.onEach { enabled ->
            _state.update { it.copy(currencyInsightsEnabled = enabled) }
        }.launchIn(viewModelScope)
    }

    private fun observeWidgetPager() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.WIDGET_PAGER_ENABLED] ?: false
        }.onEach { enabled ->
            _state.update { it.copy(widgetPagerEnabled = enabled) }
        }.launchIn(viewModelScope)
    }

    private fun observeDeveloperOptions() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.DEVELOPER_OPTIONS_ENABLED] ?: false
        }.onEach { enabled ->
            _state.update { it.copy(developerOptionsEnabled = enabled) }
        }.launchIn(viewModelScope)
    }

    private fun observeAdsDisabled() {
        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ADS_DISABLED_DEV] ?: false
        }.onEach { disabled ->
            _state.update { it.copy(adsDisabled = disabled) }
        }.launchIn(viewModelScope)

        dataStore.data.map { prefs ->
            prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ADS_FORCE_SHOW_DEV] ?: false
        }.onEach { forced ->
            _state.update { it.copy(adsForceShow = forced) }
        }.launchIn(viewModelScope)
    }

    fun toggleAdsDisabled(disabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ADS_DISABLED_DEV] = disabled
            }
            _state.update { it.copy(adsDisabled = disabled) }
        }
    }

    fun toggleAdsForceShow(forced: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ADS_FORCE_SHOW_DEV] = forced
            }
            _state.update { it.copy(adsForceShow = forced) }
        }
    }

    fun enableDeveloperOptions() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.DEVELOPER_OPTIONS_ENABLED] = true
            }
            analyticsTracker.trackEvent(AnalyticsEvent.DeveloperOptionsUnlocked)
            _state.update { it.copy(restoreMessage = "Developer options unlocked") }
        }
    }

    /** Fire the one-time app_lock adoption event. Called from Settings after
     *  the PIN is successfully persisted so we count "PIN set" not "PIN screen
     *  opened". */
    fun onAppLockPinConfigured() {
        viewModelScope.launch {
            markFeatureAdoptedOnce("app_lock")
        }
    }

    /** Escape hatch back to the "real user" experience. Section disappears
     *  from Settings; the passcode-gated version-tap unlock still re-enables
     *  it if you need dev tools again. */
    fun disableDeveloperOptions() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.DEVELOPER_OPTIONS_ENABLED] = false
            }
        }
    }

    /**
     * Resets the onboarding flag and signals the UI to navigate to the
     * onboarding flow. Lets us re-test the onboarding screens without
     * reinstalling or wiping all app data. Dev-only.
     */
    fun replayOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ONBOARDING_COMPLETED] = false
            }
            _events.emit(SettingsEvent.ReplayOnboarding)
        }
    }

    private fun observeData() {
        getUserPreferencesUseCase().onEach { preferences ->
            try {
                val allCategories = getCategoriesUseCase()
                val pinnedCategories = getPinnedCategoriesUseCase().first()

                _state.update {
                    it.copy(
                        isLoading = false,
                        allCategories = allCategories,
                        pinnedCategoryIds = preferences.pinnedCategories.toSet(),
                        pinnedCategories = pinnedCategories,
                        currentThemeMode = preferences.themeMode,
                        notificationsEnabled = preferences.notificationsEnabled,
                        defaultCurrency = Currency.entries.firstOrNull { c -> c.name == preferences.defaultCurrency }
                            ?: Currency.USD,
                        appLanguage = preferences.appLanguage,
                        defaultExpenseCategoryId = preferences.defaultExpenseCategory,
                        defaultIncomeCategoryId = preferences.defaultIncomeCategory,
                        excludeTaxesFromTotals = preferences.excludeTaxesFromTotals,
                        exchangeRateSource = preferences.exchangeRateSource,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Throwable) {
                analyticsTracker.recordException(error, "Settings")
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeUserCurrencies() {
        // Cache provides user-currency list; one source of truth.
        appDataCache.snapshot.map { it.userCurrencies }.onEach { currencies ->
            _state.update { it.copy(userCurrencies = currencies) }
        }.launchIn(viewModelScope)
    }

    private fun observeAssetCategories() {
        // Read from the app cache instead of opening another Room subscription.
        appDataCache.snapshot.map { it.assetCategories }.onEach { categories ->
            _state.update { it.copy(assetCategories = categories) }
        }.launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        appDataCache.snapshot.map { it.accounts }.onEach { accounts ->
            val primary = accounts.find { it.isPrimary }
            _state.update { it.copy(accounts = accounts, primaryAccountId = primary?.id) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnCategorySelectionToggle -> handleCategoryToggle(action.category)
            is SettingsAction.OnPinnedCategoriesReorder -> handleReorder(action.fromIndex, action.toIndex)
            is SettingsAction.OnThemeModeChange -> handleThemeModeChange(action.themeMode)
            is SettingsAction.OnNotificationsToggle -> handleNotificationsToggle(action.enabled)
            is SettingsAction.OnDefaultCurrencyChange -> handleDefaultCurrencyChange(action.currency)
            is SettingsAction.OnLanguageChange -> handleLanguageChange(action.language)
            is SettingsAction.OnExportData -> handleExportData()
            is SettingsAction.OnImportData -> handleImportData(action.jsonData)
            is SettingsAction.OnImportCsv -> handleImportCsv(action.csvContent)
            is SettingsAction.OnToggleUserCurrency -> handleToggleUserCurrency(action.currencyCode)
            is SettingsAction.OnDefaultExpenseCategoryChange -> handleDefaultExpenseCategoryChange(action.categoryId)
            is SettingsAction.OnDefaultIncomeCategoryChange -> handleDefaultIncomeCategoryChange(action.categoryId)
            is SettingsAction.OnPrimaryAccountChange -> handlePrimaryAccountChange(action.accountId)
            is SettingsAction.OnExcludeTaxesToggle -> handleExcludeTaxesToggle(action.enabled)
            is SettingsAction.OnExchangeRateSourceChange -> handleExchangeRateSourceChange(action.source)
            is SettingsAction.OnReminderConfigChange -> handleReminderConfigChange(
                mode = action.mode,
                hour = action.hour,
                minute = action.minute,
                weekday = action.weekday
            )
            is SettingsAction.OnBackClick -> {}
        }
    }

    private fun handleReminderConfigChange(mode: String, hour: Int, minute: Int, weekday: Int) {
        viewModelScope.launch {
            try {
                dataStore.edit { prefs ->
                    prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_MODE] = mode
                    prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_HOUR] = hour
                    prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_MINUTE] = minute
                    prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.REMINDER_WEEKDAY] = weekday
                }
                val parsed = com.andriybobchuk.mooney.core.notifications.ReminderMode.fromStorage(mode)
                when (parsed) {
                    com.andriybobchuk.mooney.core.notifications.ReminderMode.DAILY ->
                        reminderScheduler.scheduleDaily(hour, minute)
                    com.andriybobchuk.mooney.core.notifications.ReminderMode.WEEKLY ->
                        reminderScheduler.scheduleWeekly(weekday, hour, minute)
                    com.andriybobchuk.mooney.core.notifications.ReminderMode.OFF ->
                        reminderScheduler.cancel()
                }
                analyticsTracker.setUserProperty("notifications_mode", mode.lowercase())
                analyticsTracker.trackEvent(
                    AnalyticsEvent.NotificationConfigured(
                        mode = mode.lowercase(),
                        timeBucket = timeBucket(hour)
                    )
                )
                if (parsed != com.andriybobchuk.mooney.core.notifications.ReminderMode.OFF) {
                    markFeatureAdoptedOnce("notifications")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun timeBucket(hour: Int): String = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "midday"
        in 17..21 -> "evening"
        else -> "night"
    }

    private suspend fun markFeatureAdoptedOnce(feature: String) {
        try {
            val current = dataStore.data.first()[
                com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ANALYTICS_ADOPTED_FEATURES
            ] ?: emptySet()
            if (feature !in current) {
                analyticsTracker.trackEvent(AnalyticsEvent.FeatureAdopted(feature))
                dataStore.edit {
                    it[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.ANALYTICS_ADOPTED_FEATURES] =
                        current + feature
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            /* best-effort */
        }
    }

    private fun handleExportData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isExporting = true, error = null) }
                val exportData = dataExportImportManager.exportAllData()
                analyticsTracker.trackEvent(AnalyticsEvent.CsvExported)
                _events.emit(SettingsEvent.ExportReady(exportData))
                _state.update { it.copy(isExporting = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Settings", mapOf("action" to "export"))
                _state.update { it.copy(isExporting = false, error = "Export failed: ${e.message}") }
            }
        }
    }

    private fun handleImportCsv(csvContent: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, restoreMessage = null) }
            try {
                val parsed = universalCsvImporter.parse(csvContent)
                if (!parsed.mapping.isComplete) {
                    _state.update {
                        it.copy(
                            isImporting = false,
                            restoreMessage = "Couldn't recognise date / amount columns in this CSV. " +
                                "Try exporting again from the source app, or rename the columns to " +
                                "include 'Date' and 'Amount'."
                        )
                    }
                    return@launch
                }
                val all = universalCsvImporter.parseAll(
                    rows = parsed.rows,
                    mapping = parsed.mapping,
                    dateFormat = parsed.detectedDateFormat,
                    decimalSeparator = parsed.decimalSeparator
                )
                val prefs = preferencesRepository.getCurrentPreferences()
                val summary = importCsvUseCase(
                    transactions = all,
                    defaultExpenseSubcategoryId = prefs.defaultExpenseCategory,
                    defaultIncomeSubcategoryId = prefs.defaultIncomeCategory
                )
                _state.update {
                    it.copy(
                        isImporting = false,
                        restoreMessage = "Imported ${summary.transactionsImported} transactions" +
                            if (summary.accountCreated) " into a new 'Imported' account" else "" +
                                if (summary.firstFailureMessage != null) " · ${summary.firstFailureMessage}" else ""
                    )
                }
                if (summary.transactionsImported > 0) {
                    analyticsTracker.trackEvent(
                        AnalyticsEvent.CsvImported(success = true, transactionCount = summary.transactionsImported)
                    )
                    markFeatureAdoptedOnce("csv_import")
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        restoreMessage = "Couldn't import CSV: ${e.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    private fun handleImportData(jsonData: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isImporting = true, error = null) }

                when (val validation = dataExportImportManager.validateExportData(jsonData)) {
                    is DataExportImportManager.ValidationResult.Valid -> {
                        _events.emit(SettingsEvent.ShowImportConfirmation(
                            transactions = validation.transactions,
                            accounts = validation.accounts,
                            goals = validation.goals
                        ))
                    }
                    is DataExportImportManager.ValidationResult.Invalid -> {
                        _state.update {
                            it.copy(isImporting = false, error = "Invalid import file: ${validation.reason}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Settings", mapOf("action" to "import_validate"))
                _state.update {
                    it.copy(isImporting = false, error = "Import validation failed: ${e.message}")
                }
            }
        }
    }

    fun confirmImport(jsonData: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isImporting = true) }

                when (val result = dataExportImportManager.importData(jsonData, clearExisting = false)) {
                    is DataExportImportManager.ImportResult.Success -> {
                        analyticsTracker.trackEvent(AnalyticsEvent.CsvImported(
                            success = true,
                            transactionCount = result.importedTransactions
                        ))
                        _events.emit(SettingsEvent.ImportSuccess(
                            importedTransactions = result.importedTransactions,
                            importedAccounts = result.importedAccounts,
                            importedGoals = result.importedGoals
                        ))
                        _state.update { it.copy(isImporting = false) }
                    }
                    is DataExportImportManager.ImportResult.Error -> {
                        analyticsTracker.trackEvent(AnalyticsEvent.CsvImported(
                            success = false, transactionCount = 0
                        ))
                        _state.update {
                            it.copy(isImporting = false, error = "Import failed: ${result.message}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Settings", mapOf("action" to "import"))
                _state.update {
                    it.copy(isImporting = false, error = "Import failed: ${e.message}")
                }
            }
        }
    }

    private fun handleCategoryToggle(category: Category) {
        val currentPinnedIds = _state.value.pinnedCategoryIds.toMutableSet()

        if (currentPinnedIds.contains(category.id)) {
            currentPinnedIds.remove(category.id)
        } else if (currentPinnedIds.size < _state.value.maxPinnedCategories) {
            currentPinnedIds.add(category.id)
        } else {
            _state.update {
                it.copy(error = "Cannot pin more than ${it.maxPinnedCategories} categories")
            }
            return
        }

        updatePinnedCategories(currentPinnedIds.toList())
    }

    private fun handleReorder(fromIndex: Int, toIndex: Int) {
        val currentList = _state.value.pinnedCategories.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            updatePinnedCategories(currentList.map { it.id })
        }
    }

    private fun handleThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateThemeMode(themeMode)
                // Theme is tracked as a user property instead of an event —
                // it shows up in cohort filters but doesn't clutter the event log.
                analyticsTracker.setUserProperty("theme_mode", themeMode.name.lowercase())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateNotificationsEnabled(enabled)
                // The dedicated reminder flow (OnReminderConfigChange) handles
                // scheduling now. This toggle just persists the legacy
                // notifications-enabled flag for future use.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleExchangeRateSourceChange(source: ExchangeRateSource) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateExchangeRateSource(source)
                _state.update { it.copy(exchangeRateSource = source) }
                // Refresh now so the new source's rates appear immediately.
                currencyManagerUseCase.refreshExchangeRates()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleExcludeTaxesToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateExcludeTaxesFromTotals(enabled)
                _state.update { it.copy(excludeTaxesFromTotals = enabled) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleDefaultCurrencyChange(currency: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDefaultCurrency(currency)
                // Update global config so all screens pick up the change immediately
                try {
                    com.andriybobchuk.mooney.mooney.data.GlobalConfig.baseCurrency =
                        com.andriybobchuk.mooney.mooney.domain.Currency.valueOf(currency)
                } catch (_: IllegalArgumentException) { }
                analyticsTracker.trackEvent(AnalyticsEvent.ChangeDefaultCurrency(currency))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleLanguageChange(language: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateAppLanguage(language)
                // Language switches are tracked as a user property only —
                // we don't need an event per switch.
                analyticsTracker.setUserProperty("app_language", language)
                _state.update { it.copy(appLanguage = language) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun updatePinnedCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            try {
                updatePinnedCategoriesUseCase(categoryIds)
                clearError()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleDefaultExpenseCategoryChange(categoryId: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDefaultExpenseCategory(categoryId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleDefaultIncomeCategoryChange(categoryId: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDefaultIncomeCategory(categoryId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handlePrimaryAccountChange(accountId: Int) {
        viewModelScope.launch {
            try {
                setPrimaryAccountUseCase(accountId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun handleToggleUserCurrency(currencyCode: String) {
        viewModelScope.launch {
            val current = _state.value.userCurrencies
            val exists = current.any { it.code == currencyCode }
            if (exists) {
                if (current.size <= 1) return@launch
                updateUserCurrenciesUseCase.remove(currencyCode)
            } else {
                val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
                updateUserCurrenciesUseCase.add(UserCurrency(currencyCode, nextOrder))
            }
        }
    }

    fun dismissPaywall() {
        _state.update { it.copy(showPaywall = false, purchaseError = null) }
    }

    fun onSubscribe() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                // Outer timeout — final safety net so the spinner ALWAYS clears
                // even if every downstream layer's timeout fails. 25s is just
                // longer than the billing manager's internal 20s timeout.
                val result = kotlinx.coroutines.withTimeoutOrNull(25_000L) {
                    premiumManager.purchase(PRODUCT_ID_MONTHLY)
                }
                when (result) {
                    is PurchaseResult.Success -> _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                    is PurchaseResult.Cancelled -> _state.update { it.copy(isPurchasing = false) }
                    is PurchaseResult.Error -> _state.update { it.copy(isPurchasing = false, purchaseError = result.message) }
                    null -> _state.update {
                        it.copy(
                            isPurchasing = false,
                            purchaseError = "Purchase didn't respond in time. Please try again."
                        )
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
    }

    fun onRestorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, purchaseError = null, restoreMessage = null) }
            try {
                val restored = premiumManager.restorePurchases()
                if (restored) {
                    _state.update { it.copy(showPaywall = false, isPurchasing = false, restoreMessage = "Purchases restored successfully") }
                } else {
                    _state.update { it.copy(isPurchasing = false, purchaseError = "No active subscription found", restoreMessage = "No active subscription found") }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPurchasing = false, purchaseError = e.message, restoreMessage = e.message ?: "Restore failed") }
            }
        }
    }

    fun clearRestoreMessage() {
        _state.update { it.copy(restoreMessage = null) }
    }

    fun toggleCurrencyInsights(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.CURRENCY_INSIGHTS_ENABLED] = enabled
            }
            _state.update { it.copy(currencyInsightsEnabled = enabled) }
        }
    }

    fun toggleWidgetPager(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys.WIDGET_PAGER_ENABLED] = enabled
            }
            _state.update { it.copy(widgetPagerEnabled = enabled) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun updateTransactionCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingCategories = true) }
            try {
                val result = updateTransactionCategoriesUseCase()
                val parts = buildList {
                    if (result.added > 0) add("${result.added} new")
                    if (result.updated > 0) add("${result.updated} refreshed")
                }
                val message = if (parts.isEmpty()) "Categories are already up to date"
                else "Categories updated: ${parts.joinToString(", ")}"
                _state.update {
                    it.copy(
                        isUpdatingCategories = false,
                        restoreMessage = message
                    )
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isUpdatingCategories = false,
                        restoreMessage = "Failed to update categories: ${e.message}"
                    )
                }
            }
        }
    }
}
