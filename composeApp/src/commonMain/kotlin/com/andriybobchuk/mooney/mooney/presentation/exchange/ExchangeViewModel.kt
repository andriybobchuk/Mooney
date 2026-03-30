package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateRatesInBaseCurrencyUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class ExchangeViewModel(
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val calculateRatesInBaseCurrencyUseCase: CalculateRatesInBaseCurrencyUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeState())
    val state: StateFlow<ExchangeState> = _state

    init {
        loadCurrentRates()
        loadHistoricalRates()
    }

    fun onAction(action: ExchangeAction) {
        when (action) {
            is ExchangeAction.RefreshRates -> refreshExchangeRates()
            is ExchangeAction.SelectCurrency -> selectCurrency(action.currency)
            is ExchangeAction.CycleBaseCurrency -> cycleBaseCurrency()
            is ExchangeAction.ToggleTimeRange -> toggleTimeRange()
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
        }
    }

    private fun loadHistoricalRates() {
        viewModelScope.launch {
            _state.update { it.copy(historicalRates = emptyMap()) }
        }
    }

    private fun refreshExchangeRates() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
                    analyticsTracker.trackEvent(AnalyticsEvent.RefreshExchangeRates)
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
        analyticsTracker.trackEvent(AnalyticsEvent.CycleCurrencyDisplay(nextCurrency.name))
        loadCurrentRates()
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
}

data class ExchangeState(
    val currentRates: Map<Currency, Double> = emptyMap(),
    val historicalRates: Map<Currency, List<HistoricalRate>> = emptyMap(),
    val selectedCurrency: Currency? = null,
    val displayBaseCurrency: Currency = GlobalConfig.baseCurrency,
    val timeRange: TimeRange = TimeRange.ONE_MONTH,
    val lastUpdated: Instant? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

data class HistoricalRate(
    val date: LocalDate,
    val rate: Double
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
}
