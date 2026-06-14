package com.andriybobchuk.mooney.mooney.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.database.PendingTransactionDao
import com.andriybobchuk.mooney.core.data.database.PendingTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.usecase.*
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetPinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.AccountWithConversion
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.RecurringSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

data class TransactionState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val transactions: List<Transaction?> = emptyList(),
    val accounts: List<AccountWithConversion?> = emptyList(),
    val categories: List<Category> = emptyList(),
    val total: Double = 0.0,
    val totalCurrency: Currency = GlobalConfig.baseCurrency,
    val dailyTotals: Map<Int, Double> = emptyMap(),
    val pendingTransactions: List<PendingTransactionEntity> = emptyList(),
    val pendingCount: Int = 0,
    val assetCategories: List<com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity> = emptyList(),
    val categoryOrder: List<String> = emptyList(),
    val expandedCategories: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    /** True until the first transactions/accounts emission lands. Drives the shimmer. */
    val isInitialLoading: Boolean = true,
    /** User-defined order of top-level transaction categories (ID list). Empty = use natural order. */
    val transactionCategoryOrder: List<String> = emptyList(),
    // Per-month transaction counts across the entire user's history. Powers the
    // count caption under each cell in the month-picker bottom sheet. Computed
    // from the full cache, not from [transactions] (which is month-filtered).
    val monthlyTransactionCounts: Map<MonthKey, Int> = emptyMap()
)

@Suppress("LongParameterList", "TooManyFunctions")
class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val calculateTransactionTotalUseCase: CalculateTransactionTotalUseCase,
    private val calculateDailyTotalUseCase: CalculateDailyTotalUseCase,
    private val convertAccountsToUiUseCase: ConvertAccountsToUiUseCase,
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val getPinnedCategoriesUseCase: GetPinnedCategoriesUseCase,
    private val filterTransactionsByMonthUseCase: FilterTransactionsByMonthUseCase,
    private val calculateDailyTotalsMapUseCase: CalculateDailyTotalsMapUseCase,
    private val pendingTransactionDao: PendingTransactionDao,
    private val acceptPendingTransactionUseCase: AcceptPendingTransactionUseCase,
    private val createRecurringFromTransactionUseCase: CreateRecurringFromTransactionUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val preferencesRepository: com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository,
    private val assetCategoryDao: com.andriybobchuk.mooney.core.data.database.AssetCategoryDao,
    private val categoryDao: com.andriybobchuk.mooney.core.data.database.CategoryDao,
    private val coreRepository: com.andriybobchuk.mooney.mooney.domain.CoreRepository,
    private val manageCategoryExpansionUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageCategoryExpansionUseCase,
    private val manageAssetCategoryOrderUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.assets.ManageAssetCategoryOrderUseCase,
    private val manageTransactionCategoryOrderUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.ManageTransactionCategoryOrderUseCase,
    private val requestReviewUseCase: com.andriybobchuk.mooney.core.review.RequestReviewUseCase,
    private val trackFirstEventUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.TrackFirstEventUseCase,
    // App-scoped cache: reading from here means the snapshot's current value
    // is already populated by the time this ViewModel is constructed, so the
    // first state emission carries real data and we never flash an empty
    // shimmer between screens.
    private val appDataCache: com.andriybobchuk.mooney.mooney.domain.cache.AppDataCache
) : ViewModel() {

    private var observeTransactionsJob: Job? = null
    private var excludeTaxes: Boolean = true
    private var allPendingTransactions: List<PendingTransactionEntity> = emptyList()

    // Seed initial state from the app cache so we never start with empty
    // transactions/accounts on a screen revisit. If the cache hasn't warmed
    // yet (true cold start), [TransactionState.isInitialLoading] stays true
    // and the shimmer takes over until the first cache emission lands.
    private val _uiState = MutableStateFlow(seedFromCache())

    private fun seedFromCache(): TransactionState {
        val cached = appDataCache.snapshot.value
        return TransactionState(isInitialLoading = !cached.isReady)
    }

    val state = _uiState
        .onStart {
            observeTransactions(_uiState.value.selectedMonth)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    val frequentCategories = getPinnedCategoriesUseCase()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            emptyList()
        )

    // One-shot signal: emit Unit when the screen should show the review pre-prompt.
    private val _reviewPrePromptRequests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val reviewPrePromptRequests: kotlinx.coroutines.flow.SharedFlow<Unit> = _reviewPrePromptRequests

    fun onMonthSelected(month: MonthKey) {
        _uiState.update { it.copy(selectedMonth = month) }
        observeTransactions(month)
        filterPendingByMonth()
    }

    private fun observeTransactions(month: MonthKey) {
        observeTransactionsJob?.cancel()

        // Read the transactions feed from the app cache instead of going
        // straight to the repository. The cache's StateFlow always has a
        // current value, so the first .onEach invocation receives the
        // already-loaded data immediately — no flash, no shimmer between
        // visits to this screen.
        observeTransactionsJob = appDataCache.snapshot
            .map { it.transactions }
            .map { transactions -> filterTransactionsByMonthUseCase(transactions, month) }
            .onEach { filteredTransactions ->
                val sorted = filteredTransactions.sortedByDescending { it.date }
                // Don't clear isInitialLoading here. If we do, the screen can briefly
                // render with transactions=empty + accounts=empty (because accounts
                // load via a separate flow in loadDataForBottomSheet), which flashes
                // the "Let's get started" onboarding state. Let loadDataForBottomSheet
                // own the loading-complete signal once it has both accounts and categories.
                _uiState.update { it.copy(transactions = sorted) }
                loadTotal()
                loadDailyTotals(filteredTransactions, month)
            }
            .launchIn(viewModelScope)
    }

    init {
        loadDataForBottomSheet()
        observePendingTransactions()
        observeBaseCurrencyChanges()
        observeTaxPreference()
        observeTransactionCategoryOrder()
        observeMonthlyTransactionCounts()
    }

    private fun observeMonthlyTransactionCounts() {
        appDataCache.snapshot
            .map { it.transactions }
            .onEach { transactions ->
                val counts = transactions.groupingBy {
                    MonthKey(it.date.year, it.date.monthNumber)
                }.eachCount()
                _uiState.update { it.copy(monthlyTransactionCounts = counts) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTransactionCategoryOrder() {
        manageTransactionCategoryOrderUseCase.getCategoryOrder().onEach { order ->
            _uiState.update { it.copy(transactionCategoryOrder = order) }
        }.launchIn(viewModelScope)
    }

    fun updateTransactionCategoryOrder(orderedCategoryIds: List<String>) {
        viewModelScope.launch {
            manageTransactionCategoryOrderUseCase.saveCategoryOrder(orderedCategoryIds)
        }
    }

    private fun observeTaxPreference() {
        preferencesRepository.getUserPreferences().onEach { prefs ->
            excludeTaxes = prefs.excludeTaxesFromTotals
            loadTotal()
            val transactions = _uiState.value.transactions.filterNotNull()
            loadDailyTotals(transactions, _uiState.value.selectedMonth)
        }.launchIn(viewModelScope)
    }

    private fun observeBaseCurrencyChanges() {
        GlobalConfig.baseCurrencyFlow.onEach {
            // Re-trigger total recalculation and account conversion when base currency changes
            loadTotal()
            loadDataForBottomSheet()
        }.launchIn(viewModelScope)
    }

    private fun observePendingTransactions() {
        // Pending transactions live in the app cache too; reading from there
        // means we share one DAO subscription with every screen instead of
        // each VM opening its own.
        appDataCache.snapshot.map { it.pendingTransactions }.onEach { pending ->
            allPendingTransactions = pending
            filterPendingByMonth()
        }.launchIn(viewModelScope)
    }

    private fun filterPendingByMonth() {
        val selectedMonth = _uiState.value.selectedMonth
        val filtered = allPendingTransactions.filter { p ->
            try {
                val date = kotlinx.datetime.LocalDate.parse(p.scheduledDate)
                date.year == selectedMonth.year && date.monthNumber == selectedMonth.month
            } catch (_: Exception) { false }
        }
        _uiState.update { it.copy(pendingTransactions = filtered, pendingCount = filtered.size) }
    }

    private fun loadTotal() {
        val result = calculateTransactionTotalUseCase(
            transactions = _uiState.value.transactions,
            selectedCurrency = currencyManagerUseCase.getCurrentCurrency(),
            baseCurrency = GlobalConfig.baseCurrency,
            excludeTaxes = excludeTaxes
        )

        _uiState.update {
            it.copy(
                total = result.total,
                totalCurrency = result.currency
            )
        }
    }

    private fun loadDailyTotals(transactions: List<Transaction>, month: MonthKey) {
        val dailyTotalsMap = calculateDailyTotalsMapUseCase(transactions, month, excludeTaxes)
        _uiState.update { it.copy(dailyTotals = dailyTotalsMap) }
    }

    private fun loadDataForBottomSheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    // Everything that lives in the app cache is read from one
                    // pre-warmed snapshot — accounts, categories, asset
                    // categories all in a single emission. Tab switches see
                    // the latest data on the first frame, no re-query.
                    appDataCache.snapshot,
                    manageAssetCategoryOrderUseCase.getCategoryOrder(),
                    manageCategoryExpansionUseCase.getExpandedCategories()
                ) { snapshot, categoryOrder, expandedCategories ->
                    if (!snapshot.isReady) return@combine
                    val nonLiabilityAccounts = convertAccountsToUiUseCase(snapshot.accounts)
                        .filterNotNull()
                        .filter { !it.isLiability }
                    _uiState.update {
                        it.copy(
                            accounts = nonLiabilityAccounts,
                            categories = snapshot.categories,
                            assetCategories = snapshot.assetCategories,
                            categoryOrder = categoryOrder,
                            expandedCategories = expandedCategories,
                            isLoading = false,
                            isInitialLoading = false
                        )
                    }
                }.launchIn(this)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.recordException(e, "Transactions")
                _uiState.update { it.copy(isError = true, isLoading = false) }
            }
        }
    }

    fun toggleAccountCategoryExpansion(categoryId: String) {
        viewModelScope.launch {
            manageCategoryExpansionUseCase.toggleCategoryExpansion(
                category = categoryId,
                currentExpanded = _uiState.value.expandedCategories
            )
        }
    }

    fun onTotalCurrencyClick() {
        currencyManagerUseCase.cycleToNextCurrency()
        loadTotal()
    }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val wasEdit = transaction.id != 0
            addTransactionUseCase(transaction)
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
            // Only on creates, never on edits — editing is a maintenance act,
            // not a positive moment.
            if (!wasEdit) {
                analyticsTracker.trackEvent(
                    AnalyticsEvent.TransactionAdded(
                        type = transaction.subcategory.type.name,
                        currency = transaction.account.currency.name
                    )
                )
                trackFirstEventUseCase.firstTransaction()
                maybeRequestReviewAfterMilestone()
            }
        }
    }

    /**
     * Asks the screen to show the review pre-prompt after a transaction-creation
     * milestone (10th, 25th, 50th, 100th). Subject to all the gates inside
     * [requestReviewUseCase] (cooldown, install age, etc.).
     */
    private suspend fun maybeRequestReviewAfterMilestone() {
        try {
            val total = _uiState.value.transactions.filterNotNull().size
            if (total in MILESTONES && requestReviewUseCase.shouldShowPrePrompt()) {
                _reviewPrePromptRequests.emit(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort, never break the user flow
        }
    }

    fun onReviewPrePromptPositive() {
        viewModelScope.launch { requestReviewUseCase.confirmReviewRequested() }
    }

    fun onReviewPrePromptNegative() {
        viewModelScope.launch { requestReviewUseCase.markPromptShown() }
    }

    fun onReviewPrePromptDismissed() {
        viewModelScope.launch { requestReviewUseCase.markPromptShown() }
    }

    private companion object {
        val MILESTONES = setOf(10, 25, 50, 100)
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
        }
    }

    fun getDailyTotal(date: kotlinx.datetime.LocalDate): Double {
        val allTransactions = _uiState.value.transactions.filterNotNull()
        return calculateDailyTotalUseCase(allTransactions, date)
    }

    suspend fun getDailyTotalForMonth(date: kotlinx.datetime.LocalDate): Double {
        // Read from the warm cache instead of subscribing to the DAO Flow
        // for one snapshot. Avoids a redundant subscription tear-down.
        val allTransactions = appDataCache.snapshot.value.transactions
        return calculateDailyTotalUseCase(allTransactions, date)
    }

    fun acceptPendingTransaction(pending: PendingTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = getAccountsUseCase(pending.accountId) ?: return@launch
                val category = getCategoriesUseCase(pending.subcategoryId) ?: return@launch
                acceptPendingTransactionUseCase(pending, account, category)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isError = true) }
            }
        }
    }

    fun skipPendingTransaction(pendingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingTransactionDao.updateStatus(pendingId, "SKIPPED")
        }
    }

    fun acceptAllPending() {
        viewModelScope.launch(Dispatchers.IO) {
            val pending = _uiState.value.pendingTransactions
            for (p in pending) {
                try {
                    val account = getAccountsUseCase(p.accountId)
                    val category = getCategoriesUseCase(p.subcategoryId)
                    if (account != null && category != null) {
                        acceptPendingTransactionUseCase(p, account, category)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _uiState.update { it.copy(isError = true) }
                }
            }
        }
    }

    fun createRecurringFromTransaction(
        transaction: Transaction,
        schedule: RecurringSchedule
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            createRecurringFromTransactionUseCase(transaction, schedule)
            analyticsTracker.trackEvent(
                AnalyticsEvent.RecurringAdded(schedule.frequency.name)
            )
        }
    }
}
