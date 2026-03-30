package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository

class CompleteOnboardingUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(currency: Currency) {
        preferencesRepository.updateDefaultCurrency(currency.name)
        preferencesRepository.markOnboardingCompleted()
    }
}
