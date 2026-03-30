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
    CURRENCY_PICKER,
    WELCOME
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.CURRENCY_PICKER,
    val selectedCurrency: Currency? = null,
    val currencies: List<Currency> = Currency.entries,
    val isLoading: Boolean = false
)

sealed interface OnboardingAction {
    data class SelectCurrency(val currency: Currency) : OnboardingAction
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
            is OnboardingAction.SelectCurrency -> {
                _state.update { it.copy(selectedCurrency = action.currency) }
            }
            is OnboardingAction.GetStarted -> completeOnboarding()
            is OnboardingAction.EnterApp -> {
                viewModelScope.launch {
                    _events.emit(OnboardingEvent.NavigateToMain)
                }
            }
        }
    }

    private fun completeOnboarding() {
        val currency = _state.value.selectedCurrency ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                completeOnboardingUseCase(currency)
                GlobalConfig.baseCurrency = currency
                analyticsTracker.trackEvent(AnalyticsEvent.CompleteOnboarding(currency.name))
                _state.update { it.copy(page = OnboardingPage.WELCOME, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
