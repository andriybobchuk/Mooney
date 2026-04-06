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
import com.andriybobchuk.mooney.mooney.domain.usecase.GetAccountsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SetPrimaryAccountUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.UpdateUserCurrenciesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetPinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetUserPreferencesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.UpdatePinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList", "TooManyFunctions")
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
    private val setPrimaryAccountUseCase: SetPrimaryAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        observeData()
        observeUserCurrencies()
        observeAssetCategories()
        observeAccounts()
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
        getUserCurrenciesUseCase().onEach { currencies ->
            _state.update { it.copy(userCurrencies = currencies) }
        }.launchIn(viewModelScope)
    }

    private fun observeAssetCategories() {
        assetCategoryDao.getAll().onEach { categories ->
            _state.update { it.copy(assetCategories = categories) }
        }.launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        getAccountsUseCase().onEach { accounts ->
            val nonNull = accounts.filterNotNull()
            val primary = nonNull.find { it.isPrimary }
            _state.update { it.copy(accounts = nonNull, primaryAccountId = primary?.id) }
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
            is SettingsAction.OnToggleUserCurrency -> handleToggleUserCurrency(action.currencyCode)
            is SettingsAction.OnDefaultExpenseCategoryChange -> handleDefaultExpenseCategoryChange(action.categoryId)
            is SettingsAction.OnDefaultIncomeCategoryChange -> handleDefaultIncomeCategoryChange(action.categoryId)
            is SettingsAction.OnPrimaryAccountChange -> handlePrimaryAccountChange(action.accountId)
            is SettingsAction.OnExcludeTaxesToggle -> handleExcludeTaxesToggle(action.enabled)
            is SettingsAction.OnBackClick -> {}
        }
    }

    private fun handleExportData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isExporting = true, error = null) }
                val exportData = dataExportImportManager.exportAllData()
                analyticsTracker.trackEvent(AnalyticsEvent.ExportData)
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
                        analyticsTracker.trackEvent(AnalyticsEvent.ImportData(
                            success = true,
                            transactionCount = result.importedTransactions,
                            accountCount = result.importedAccounts
                        ))
                        _events.emit(SettingsEvent.ImportSuccess(
                            importedTransactions = result.importedTransactions,
                            importedAccounts = result.importedAccounts,
                            importedGoals = result.importedGoals
                        ))
                        _state.update { it.copy(isImporting = false) }
                    }
                    is DataExportImportManager.ImportResult.Error -> {
                        analyticsTracker.trackEvent(AnalyticsEvent.ImportData(
                            success = false, transactionCount = 0, accountCount = 0
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
                analyticsTracker.trackEvent(AnalyticsEvent.ChangeTheme(themeMode.name))
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
                analyticsTracker.trackEvent(AnalyticsEvent.ChangeLanguage(language))
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
                analyticsTracker.trackEvent(AnalyticsEvent.ToggleUserCurrency(currencyCode, enabled = false))
            } else {
                val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
                updateUserCurrenciesUseCase.add(UserCurrency(currencyCode, nextOrder))
                analyticsTracker.trackEvent(AnalyticsEvent.ToggleUserCurrency(currencyCode, enabled = true))
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
                when (val result = premiumManager.purchase(PRODUCT_ID_MONTHLY)) {
                    is PurchaseResult.Success -> _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                    is PurchaseResult.Cancelled -> _state.update { it.copy(isPurchasing = false) }
                    is PurchaseResult.Error -> _state.update { it.copy(isPurchasing = false, purchaseError = result.message) }
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
            _state.update { it.copy(isPurchasing = true, purchaseError = null) }
            try {
                val restored = premiumManager.restorePurchases()
                if (restored) {
                    _state.update { it.copy(showPaywall = false, isPurchasing = false) }
                } else {
                    _state.update { it.copy(isPurchasing = false, purchaseError = "No active subscription found") }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isPurchasing = false, purchaseError = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
