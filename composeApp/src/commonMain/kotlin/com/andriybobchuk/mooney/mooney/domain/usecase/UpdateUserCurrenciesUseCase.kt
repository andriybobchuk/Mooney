package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.UserCurrency

class UpdateUserCurrenciesUseCase(
    private val repository: CoreRepository
) {
    suspend fun add(userCurrency: UserCurrency) {
        repository.upsertUserCurrency(userCurrency)
    }

    suspend fun remove(code: String) {
        repository.deleteUserCurrency(code)
    }
}
