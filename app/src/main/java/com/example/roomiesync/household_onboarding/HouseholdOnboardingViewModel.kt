package com.example.roomiesync.household_onboarding

import androidx.lifecycle.ViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.roomiesync.utils.household_onboarding.validation.JoinCodeFormValidation
import com.example.roomiesync.utils.household_onboarding.validation.HouseholdDetailsFormValidation

class HouseholdOnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HouseholdOnboardingState())
    val uiState: StateFlow<HouseholdOnboardingState> = _uiState.asStateFlow()

    fun onGoToCreate() {
        _uiState.update { it.copy(currentStep = HouseholdOnboardingStep.CREATE) }
    }

    fun onGoToJoin() {
        _uiState.update { it.copy(currentStep = HouseholdOnboardingStep.JOIN) }
    }

    fun onGoHome() {
        _uiState.update { it.copy(currentStep = HouseholdOnboardingStep.HOME) }
    }

    fun updateHouseholdNickname(nickname: String) {
        if (!HouseholdDetailsFormValidation.isNicknameFieldUpdatable(nickname)) { return }
        _uiState.update { it.copy(householdNickname = nickname) }
    }

    fun updateHouseholdAddress(address: String) {
        _uiState.update { it.copy(householdAddress = address) }
    }

    fun updateJoinCode(code: String) {
        if (!JoinCodeFormValidation.isJoinCodeUpdatable(code)) { return }
        _uiState.update { it.copy(joinCode = code.uppercase()) }
    }
}
