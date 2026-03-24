package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.CoreRepository

class ReconcileAccountUseCase(
    private val repository: CoreRepository,
    private val createReconciliationUseCase: CreateReconciliationUseCase,
    private val addAccountUseCase: AddAccountUseCase
) {
    suspend operator fun invoke(
        reconciliationDiff: ReconciliationDifference,
        targetAccount: Account
    ) {
        createReconciliationUseCase.createReconciliationTransaction(reconciliationDiff)
        addAccountUseCase(targetAccount)
    }
}
