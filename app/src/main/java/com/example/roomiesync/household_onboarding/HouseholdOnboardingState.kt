package com.example.roomiesync.household_onboarding

enum class HouseholdOnboardingStep {
    HOME,
    CREATE,
    CREATED,  // show invite code after successful create
    JOIN
}

data class HouseholdOnboardingState(
    val isLoading: Boolean = false,
    val currentStep: HouseholdOnboardingStep = HouseholdOnboardingStep.HOME,
    val householdNickname: String = "",
    val householdAddress: String = "",
    val joinCode: String = "",
    val createdInviteCode: String? = null,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val createErrorMessage: String? = null,
    val joinErrorMessage: String? = null,
    val isSuccess: Boolean = false
)
