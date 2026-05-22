package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.AlertDirection
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.HistoricalRate
import com.andriybobchuk.mooney.mooney.domain.RateWatchAlert
import com.andriybobchuk.mooney.mooney.domain.TriggeredAlert
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatePercentileUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatesInBaseCurrencyUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CheckRateAlertsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.LoadHistoricalRatesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.ManageRateWatchUseCase
import com.andriybobchuk.mooney.core.domain.Result
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class ExchangeViewModel(
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val calculateRatesInBaseCurrencyUseCase: CalculateRatesInBaseCurrencyUseCase,
    private val loadHistoricalRatesUseCase: LoadHistoricalRatesUseCase,
    private val calculateRatePercentileUseCase: CalculateRatePercentileUseCase,
    private val checkRateAlertsUseCase: CheckRateAlertsUseCase,
    private val manageRateWatchUseCase: ManageRateWatchUseCase,
    private val shouldRefreshExchangeRatesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.ShouldRefreshExchangeRatesUseCase,
    private val getUserCurrenciesUseCase: com.andriybobchuk.mooney.mooney.domain.usecase.GetUserCurrenciesUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeState())
    val state: StateFlow<ExchangeState> = _state

    init {
        refreshOnStart()
        observeAlerts()
        observeUserCurrencies()
    }

    private fun refreshOnStart() {
        viewModelScope.launch {
            if (shouldRefreshExchangeRatesUseCase(0L)) {
                refreshExchangeRates()
            } else {
                loadCurrentRates()
                loadHistoricalRates()
            }
        }
    }

    private fun observeUserCurrencies() {
        getUserCurrenciesUseCase().onEach { userCurrencies ->
            val codes = userCurrencies.map { it.code }.toSet()
            _state.update { it.copy(userCurrencyCodes = codes) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: ExchangeAction) {
        when (action) {
            is ExchangeAction.RefreshRates -> refreshExchangeRates()
            is ExchangeAction.SelectCurrency -> selectCurrency(action.currency)
            is ExchangeAction.CycleBaseCurrency -> cycleBaseCurrency()
            is ExchangeAction.ToggleTimeRange -> toggleTimeRange()
            is ExchangeAction.SelectTimeRange -> _state.update { it.copy(timeRange = action.range) }
            is ExchangeAction.OpenAlertSheet -> _state.update { it.copy(alertSheetCurrency = action.currency) }
            is ExchangeAction.CloseAlertSheet -> _state.update { it.copy(alertSheetCurrency = null) }
            is ExchangeAction.SaveAlert -> saveAlert(action.fromCurrency, action.toCurrency, action.targetRate, action.direction)
            is ExchangeAction.DeleteAlert -> deleteAlert(action.id)
            is ExchangeAction.DismissTriggeredAlert -> dismissTriggeredAlert(action.id)
        }
    }

    private fun loadCurrentRates() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val exchangeRates = currencyManagerUseCase.getCurrentExchangeRates()
            val ratesInBaseCurrency = calculateRatesInBaseCurrencyUseCase(
                exchangeRates, _state.value.displayBaseCurrency
            )

            _state.update {
                it.copy(
                    currentRates = ratesInBaseCurrency,
                    lastUpdated = Clock.System.now(),
                    isLoading = false
                )
            }

            checkAlerts()
        }
    }

    private fun loadHistoricalRates() {
        viewModelScope.launch {
            try {
                val displayBase = _state.value.displayBaseCurrency
                val targetCurrencies = Currency.entries.filter { it != displayBase }

                // Fetch from displayBase → targets gives "how many target per 1 displayBase"
                // We want "how many displayBase per 1 target", so invert
                val rawRates = loadHistoricalRatesUseCase(displayBase, targetCurrencies)
                val invertedRates = rawRates.mapValues { (_, rates) ->
                    rates.map { hr ->
                        HistoricalRate(hr.date, if (hr.rate != 0.0) 1.0 / hr.rate else 0.0)
                    }
                }

                _state.update { it.copy(historicalRates = invertedRates) }
                calculatePercentiles(invertedRates)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(historicalRates = emptyMap()) }
            }
        }
    }

    private fun calculatePercentiles(historicalRates: Map<Currency, List<HistoricalRate>>) {
        val currentRates = _state.value.currentRates
        val percentiles = currentRates.mapValues { (currency, currentRate) ->
            val history = historicalRates[currency] ?: return@mapValues 50
            calculateRatePercentileUseCase(currentRate, history)
        }
        _state.update { it.copy(percentiles = percentiles) }
    }

    private fun checkAlerts() {
        viewModelScope.launch {
            try {
                val currentRates = _state.value.currentRates
                val baseCurrency = _state.value.displayBaseCurrency
                val triggered = checkRateAlertsUseCase(currentRates, baseCurrency)
                if (triggered.isNotEmpty()) {
                    _state.update { it.copy(triggeredAlerts = triggered) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun observeAlerts() {
        manageRateWatchUseCase.getAllAlerts()
            .onEach { alerts -> _state.update { it.copy(activeAlerts = alerts) } }
            .launchIn(viewModelScope)
    }

    private fun refreshExchangeRates() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
                    // Breadcrumb for crash diagnosis — not a tracked event.
                    analyticsTracker.log("Exchange rates refreshed")
                    loadCurrentRates()
                    loadHistoricalRates()
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            lastUpdated = Clock.System.now()
                        )
                    }
                }
                is Result.Error -> {
                    analyticsTracker.log("Exchange rate refresh failed: ${result.error}")
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = "Failed to update exchange rates"
                        )
                    }
                }
            }
        }
    }

    private fun selectCurrency(currency: Currency?) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    private fun cycleBaseCurrency() {
        val currencies = Currency.entries
        val currentIndex = currencies.indexOf(_state.value.displayBaseCurrency)
        val nextIndex = (currentIndex + 1) % currencies.size
        val nextCurrency = currencies[nextIndex]

        _state.update { it.copy(displayBaseCurrency = nextCurrency) }
        loadCurrentRates()
        loadHistoricalRates()
    }

    private fun toggleTimeRange() {
        val currentRange = _state.value.timeRange
        val newRange = when (currentRange) {
            TimeRange.ONE_MONTH -> TimeRange.THREE_MONTHS
            TimeRange.THREE_MONTHS -> TimeRange.SIX_MONTHS
            TimeRange.SIX_MONTHS -> TimeRange.ONE_MONTH
        }
        _state.update { it.copy(timeRange = newRange) }
    }

    private fun saveAlert(from: Currency, to: Currency, targetRate: Double, direction: AlertDirection) {
        viewModelScope.launch {
            manageRateWatchUseCase.saveAlert(from, to, targetRate, direction)
            _state.update { it.copy(alertSheetCurrency = null) }
        }
    }

    private fun deleteAlert(id: Int) {
        viewModelScope.launch {
            manageRateWatchUseCase.deleteAlert(id)
        }
    }

    private fun dismissTriggeredAlert(id: Int) {
        viewModelScope.launch {
            manageRateWatchUseCase.deactivateAlert(id)
            _state.update { it.copy(triggeredAlerts = it.triggeredAlerts.filter { t -> t.alert.id != id }) }
        }
    }
}

data class ExchangeState(
    val currentRates: Map<Currency, Double> = emptyMap(),
    val historicalRates: Map<Currency, List<HistoricalRate>> = emptyMap(),
    val percentiles: Map<Currency, Int> = emptyMap(),
    val selectedCurrency: Currency? = null,
    val displayBaseCurrency: Currency = GlobalConfig.baseCurrency,
    val timeRange: TimeRange = TimeRange.ONE_MONTH,
    val lastUpdated: Instant? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val triggeredAlerts: List<TriggeredAlert> = emptyList(),
    val activeAlerts: List<RateWatchAlert> = emptyList(),
    val alertSheetCurrency: Currency? = null,
    val userCurrencyCodes: Set<String> = emptySet()
)

enum class TimeRange(val months: Int, val label: String) {
    ONE_MONTH(1, "1M"),
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M")
}

sealed interface ExchangeAction {
    data object RefreshRates : ExchangeAction
    data class SelectCurrency(val currency: Currency?) : ExchangeAction
    data object CycleBaseCurrency : ExchangeAction
    data object ToggleTimeRange : ExchangeAction
    data class SelectTimeRange(val range: TimeRange) : ExchangeAction
    data class OpenAlertSheet(val currency: Currency) : ExchangeAction
    data object CloseAlertSheet : ExchangeAction
    data class SaveAlert(val fromCurrency: Currency, val toCurrency: Currency, val targetRate: Double, val direction: AlertDirection) : ExchangeAction
    data class DeleteAlert(val id: Int) : ExchangeAction
    data class DismissTriggeredAlert(val id: Int) : ExchangeAction
}
