package com.andriybobchuk.mooney.mooney.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class OnboardingPage {
    CURRENCY_SELECTION,
    BASE_CURRENCY,
    WELCOME
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.CURRENCY_SELECTION,
    val selectedCurrencies: Set<Currency> = emptySet(),
    val baseCurrency: Currency? = null,
    val currencies: List<Currency> = Currency.entries,
    val isLoading: Boolean = false
) {
    val maxCurrencies: Int = 6
    val canContinueToBaseCurrency: Boolean = selectedCurrencies.isNotEmpty()
    val canGetStarted: Boolean = baseCurrency != null
}

sealed interface OnboardingAction {
    data class ToggleCurrency(val currency: Currency) : OnboardingAction
    data class SelectBaseCurrency(val currency: Currency) : OnboardingAction
    data object ContinueToBaseCurrency : OnboardingAction
    data object BackToCurrencySelection : OnboardingAction
    data object GetStarted : OnboardingAction
    data object EnterApp : OnboardingAction
}

sealed class OnboardingEvent {
    data object NavigateToMain : OnboardingEvent()
}

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.ToggleCurrency -> toggleCurrency(action.currency)
            is OnboardingAction.SelectBaseCurrency -> {
                _state.update { it.copy(baseCurrency = action.currency) }
            }
            is OnboardingAction.ContinueToBaseCurrency -> {
                val selected = _state.value.selectedCurrencies
                if (selected.isNotEmpty()) {
                    // Auto-select base currency if only one selected
                    val autoBase = if (selected.size == 1) selected.first() else _state.value.baseCurrency
                    _state.update { it.copy(page = OnboardingPage.BASE_CURRENCY, baseCurrency = autoBase) }
                }
            }
            is OnboardingAction.BackToCurrencySelection -> {
                _state.update { it.copy(page = OnboardingPage.CURRENCY_SELECTION) }
            }
            is OnboardingAction.GetStarted -> completeOnboarding()
            is OnboardingAction.EnterApp -> {
                viewModelScope.launch {
                    _events.emit(OnboardingEvent.NavigateToMain)
                }
            }
        }
    }

    private fun toggleCurrency(currency: Currency) {
        _state.update { state ->
            val current = state.selectedCurrencies
            val updated = if (current.contains(currency)) {
                current - currency
            } else if (current.size < state.maxCurrencies) {
                current + currency
            } else {
                current // at max
            }
            // If base currency was deselected, clear it
            val newBase = if (state.baseCurrency != null && !updated.contains(state.baseCurrency)) null else state.baseCurrency
            state.copy(selectedCurrencies = updated, baseCurrency = newBase)
        }
    }

    private fun completeOnboarding() {
        val base = _state.value.baseCurrency ?: return
        val selected = _state.value.selectedCurrencies
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                completeOnboardingUseCase(base, selected.toList())
                GlobalConfig.baseCurrency = base
                analyticsTracker.trackEvent(AnalyticsEvent.CompleteOnboarding(base.name))
                _state.update { it.copy(page = OnboardingPage.WELCOME, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
