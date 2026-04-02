package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository

class CompleteOnboardingUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val updateUserCurrenciesUseCase: UpdateUserCurrenciesUseCase
) {
    suspend operator fun invoke(baseCurrency: Currency, selectedCurrencies: List<Currency>) {
        preferencesRepository.updateDefaultCurrency(baseCurrency.name)

        // Save selected currencies (clear defaults first, then add selected)
        for ((index, currency) in selectedCurrencies.withIndex()) {
            updateUserCurrenciesUseCase.add(UserCurrency(code = currency.name, sortOrder = index))
        }

        preferencesRepository.markOnboardingCompleted()
    }
}
