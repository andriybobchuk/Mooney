package com.andriybobchuk.mooney.mooney.presentation.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.usecase.CurrencyManagerUseCase
import com.andriybobchuk.mooney.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class ExchangeViewModel(
    private val currencyManagerUseCase: CurrencyManagerUseCase,
    private val repository: CoreRepository
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
            val ratesInBaseCurrency = calculateRatesInBaseCurrency(exchangeRates)
            
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
            // For now, we'll generate sample historical data
            // In a real app, this would fetch from an API
            val historicalData = generateSampleHistoricalData()
            
            _state.update { 
                it.copy(historicalRates = historicalData)
            }
        }
    }

    private fun refreshExchangeRates() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            when (val result = currencyManagerUseCase.refreshExchangeRates()) {
                is Result.Success -> {
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
        loadCurrentRates() // Recalculate rates in new base currency
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

    private fun calculateRatesInBaseCurrency(exchangeRates: ExchangeRates?): Map<Currency, Double> {
        if (exchangeRates == null) return emptyMap()
        
        val baseCurrency = _state.value.displayBaseCurrency
        val rates = mutableMapOf<Currency, Double>()
        
        // Calculate rate for each currency in terms of the display base currency
        Currency.entries.forEach { currency ->
            if (currency != baseCurrency) {
                val rate = exchangeRates.convert(1.0, currency, baseCurrency)
                rates[currency] = rate
            }
        }
        
        return rates
    }

    private fun generateSampleHistoricalData(): Map<Currency, List<HistoricalRate>> {
        val historicalData = mutableMapOf<Currency, List<HistoricalRate>>()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        Currency.entries.forEach { currency ->
            if (currency != GlobalConfig.baseCurrency) {
                val rates = mutableListOf<HistoricalRate>()
                val baseRate = when (currency) {
                    Currency.USD -> 3.67
                    Currency.EUR -> 4.35
                    Currency.UAH -> 0.1
                    Currency.PLN -> 1.0
                }
                
                // Generate weekly data for 12 months back
                for (weeksAgo in 0..52) {
                    val date = today.minus(weeksAgo * 7, DateTimeUnit.DAY)
                    val variation = (kotlin.random.Random.nextDouble() - 0.5) * 0.2 // ±10% variation
                    val rate = baseRate * (1 + variation)
                    rates.add(HistoricalRate(date, rate))
                }
                
                historicalData[currency] = rates.reversed()
            }
        }
        
        return historicalData
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