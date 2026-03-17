package com.andriybobchuk.mooney.mooney.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.GetCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetPinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.GetUserPreferencesUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.settings.UpdatePinnedCategoriesUseCase
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.ThemeMode
import com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences
import com.andriybobchuk.mooney.mooney.domain.backup.DataExportImportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val updatePinnedCategoriesUseCase: UpdatePinnedCategoriesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getPinnedCategoriesUseCase: GetPinnedCategoriesUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val dataExportImportManager: DataExportImportManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        getUserPreferencesUseCase().onEach { preferences ->
            try {
                val allCategories = getCategoriesUseCase()
                val pinnedCategories = getPinnedCategoriesUseCase().first()
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    allCategories = allCategories,
                    pinnedCategoryIds = preferences.pinnedCategories.toSet(),
                    pinnedCategories = pinnedCategories,
                    currentThemeMode = preferences.themeMode,
                    notificationsEnabled = preferences.notificationsEnabled,
                    defaultCurrency = Currency.entries.firstOrNull { it.name == preferences.defaultCurrency } 
                        ?: Currency.PLN,
                    error = null
                )
            } catch (error: Throwable) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnCategorySelectionToggle -> handleCategoryToggle(action.category)
            is SettingsAction.OnPinnedCategoriesReorder -> handleReorder(action.fromIndex, action.toIndex)
            is SettingsAction.OnThemeModeChange -> handleThemeModeChange(action.themeMode)
            is SettingsAction.OnNotificationsToggle -> handleNotificationsToggle(action.enabled)
            is SettingsAction.OnDefaultCurrencyChange -> handleDefaultCurrencyChange(action.currency)
            is SettingsAction.OnExportData -> handleExportData()
            is SettingsAction.OnImportData -> handleImportData(action.jsonData)
            is SettingsAction.OnBackClick -> {
                // Handle navigation back - this will be handled by the UI
            }
        }
    }
    
    private fun handleExportData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isExporting = true, error = null)
                val exportData = dataExportImportManager.exportAllData()
                _events.emit(SettingsEvent.ExportReady(exportData))
                _state.value = _state.value.copy(isExporting = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }
    
    private fun handleImportData(jsonData: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isImporting = true, error = null)
                
                // First validate the data
                when (val validation = dataExportImportManager.validateExportData(jsonData)) {
                    is DataExportImportManager.ValidationResult.Valid -> {
                        // Show confirmation dialog
                        _events.emit(SettingsEvent.ShowImportConfirmation(
                            transactions = validation.transactions,
                            accounts = validation.accounts,
                            goals = validation.goals
                        ))
                    }
                    is DataExportImportManager.ValidationResult.Invalid -> {
                        _state.value = _state.value.copy(
                            isImporting = false,
                            error = "Invalid import file: ${validation.reason}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isImporting = false,
                    error = "Import validation failed: ${e.message}"
                )
            }
        }
    }
    
    fun confirmImport(jsonData: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isImporting = true)
                
                when (val result = dataExportImportManager.importData(jsonData, clearExisting = false)) {
                    is DataExportImportManager.ImportResult.Success -> {
                        _events.emit(SettingsEvent.ImportSuccess(
                            importedTransactions = result.importedTransactions,
                            importedAccounts = result.importedAccounts,
                            importedGoals = result.importedGoals
                        ))
                        _state.value = _state.value.copy(isImporting = false)
                    }
                    is DataExportImportManager.ImportResult.Error -> {
                        _state.value = _state.value.copy(
                            isImporting = false,
                            error = "Import failed: ${result.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isImporting = false,
                    error = "Import failed: ${e.message}"
                )
            }
        }
    }

    private fun handleCategoryToggle(category: Category) {
        val currentPinnedIds = _state.value.pinnedCategoryIds.toMutableSet()
        
        if (currentPinnedIds.contains(category.id)) {
            currentPinnedIds.remove(category.id)
        } else if (currentPinnedIds.size < _state.value.maxPinnedCategories) {
            currentPinnedIds.add(category.id)
        } else {
            // Cannot add more than max categories
            _state.value = _state.value.copy(
                error = "Cannot pin more than ${_state.value.maxPinnedCategories} categories"
            )
            return
        }
        
        updatePinnedCategories(currentPinnedIds.toList())
    }

    private fun handleReorder(fromIndex: Int, toIndex: Int) {
        val currentList = _state.value.pinnedCategories.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            
            updatePinnedCategories(currentList.map { it.id })
        }
    }

    private fun handleThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateThemeMode(themeMode)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun handleNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateNotificationsEnabled(enabled)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun handleDefaultCurrencyChange(currency: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.updateDefaultCurrency(currency)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun updatePinnedCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            try {
                updatePinnedCategoriesUseCase(categoryIds)
                clearError()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}