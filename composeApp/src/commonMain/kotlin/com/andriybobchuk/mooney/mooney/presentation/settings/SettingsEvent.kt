package com.andriybobchuk.mooney.mooney.presentation.settings

sealed class SettingsEvent {
    data class ExportReady(val jsonData: String) : SettingsEvent()
    
    data class ShowImportConfirmation(
        val transactions: Int,
        val accounts: Int,
        val goals: Int
    ) : SettingsEvent()
    
    data class ImportSuccess(
        val importedTransactions: Int,
        val importedAccounts: Int,
        val importedGoals: Int
    ) : SettingsEvent()
    
    data class ShowError(val message: String) : SettingsEvent()
}