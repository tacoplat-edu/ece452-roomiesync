package com.example.roomiesync.utils.household_onboarding.validation

private const val JOIN_CODE_MAX_LENGTH = 8
private const val NICKNAME_MAX_LENGTH = 40

private val alphanumericRegex = Regex("^[a-zA-Z0-9]*$")

object JoinCodeFormValidation {
    fun isJoinCodeUpdatable(code: String): Boolean {
        return alphanumericRegex.matches(code) && code.length <= JOIN_CODE_MAX_LENGTH
    }

    fun isJoinCodeValid(code: String): Boolean {
        return alphanumericRegex.matches(code) && code.length == JOIN_CODE_MAX_LENGTH
    }
}

object HouseholdDetailsFormValidation {
    fun isHouseholdDetailsFormValid(nickname: String, address: String): Boolean {
        return nickname.isNotBlank() && address.isNotBlank() && nickname.length <= NICKNAME_MAX_LENGTH
    }

    fun isNicknameFieldUpdatable(nickname: String): Boolean {
        return nickname.length <= NICKNAME_MAX_LENGTH
    }
}