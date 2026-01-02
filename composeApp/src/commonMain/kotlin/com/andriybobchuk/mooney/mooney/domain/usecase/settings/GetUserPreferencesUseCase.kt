package com.andriybobchuk.mooney.mooney.domain.usecase.settings

import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import com.andriybobchuk.mooney.mooney.domain.settings.UserPreferences
import kotlinx.coroutines.flow.Flow

class GetUserPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<UserPreferences> {
        return preferencesRepository.getUserPreferences()
    }
}