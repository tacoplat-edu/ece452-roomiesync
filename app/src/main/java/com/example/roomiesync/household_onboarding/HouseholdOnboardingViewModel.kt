package com.example.roomiesync.household_onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.InvalidJoinCodeException
import com.example.roomiesync.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.roomiesync.utils.household_onboarding.validation.JoinCodeFormValidation
import com.example.roomiesync.utils.household_onboarding.validation.HouseholdDetailsFormValidation

class HouseholdOnboardingViewModel(
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository())
) : ViewModel() {
    private val _uiState = MutableStateFlow(HouseholdOnboardingState())
    val uiState: StateFlow<HouseholdOnboardingState> = _uiState.asStateFlow()

    fun onGoToCreate() {
        _uiState.update {
            it.copy(
                currentStep = HouseholdOnboardingStep.CREATE,
                createErrorMessage = null
            )
        }
    }

    fun onGoToJoin() {
        _uiState.update {
            it.copy(
                currentStep = HouseholdOnboardingStep.JOIN,
                joinErrorMessage = null
            )
        }
    }

    fun onGoHome() {
        _uiState.update {
            it.copy(
                currentStep = HouseholdOnboardingStep.HOME,
                createErrorMessage = null,
                joinErrorMessage = null,
                createdInviteCode = null
            )
        }
    }

    fun onCreateHouse() {
        val nickname = _uiState.value.householdNickname
        val address = _uiState.value.householdAddress
        if (!HouseholdDetailsFormValidation.isHouseholdDetailsFormValid(nickname, address)) return
        _uiState.update {
            it.copy(isCreating = true, createErrorMessage = null)
        }
        viewModelScope.launch {
            houseRepository.createHouse(nickname, address)
                .onSuccess { house ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            currentStep = HouseholdOnboardingStep.CREATED,
                            createdInviteCode = house.joinCode,
                            createErrorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createErrorMessage = e.message ?: "Could not create household"
                        )
                    }
                }
        }
    }

    fun onJoinHouse() {
        val code = _uiState.value.joinCode
        if (!JoinCodeFormValidation.isJoinCodeValid(code)) return
        _uiState.update {
            it.copy(isJoining = true, joinErrorMessage = null)
        }
        viewModelScope.launch {
            houseRepository.joinHouseByCode(code)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            joinErrorMessage = null,
                            currentStep = HouseholdOnboardingStep.HOME
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            joinErrorMessage = when (e) {
                                is InvalidJoinCodeException -> "Invalid or expired invite code."
                                else -> e.message ?: "Could not join household"
                            }
                        )
                    }
                }
        }
    }

    fun onDoneFromCreated() {
        _uiState.update {
            it.copy(
                currentStep = HouseholdOnboardingStep.HOME,
                createdInviteCode = null,
                householdNickname = "",
                householdAddress = ""
            )
        }
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
