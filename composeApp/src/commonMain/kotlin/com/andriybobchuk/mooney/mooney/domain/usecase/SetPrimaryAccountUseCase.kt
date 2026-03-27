package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository

class SetPrimaryAccountUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(accountId: Int) {
        repository.setPrimaryAccount(accountId)
    }
}
