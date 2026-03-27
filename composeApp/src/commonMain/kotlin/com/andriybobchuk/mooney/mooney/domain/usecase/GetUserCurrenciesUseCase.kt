package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import kotlinx.coroutines.flow.Flow

class GetUserCurrenciesUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): Flow<List<UserCurrency>> {
        return repository.getUserCurrencies()
    }
}
