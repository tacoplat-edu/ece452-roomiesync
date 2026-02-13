package com.example.roomiesync.household_onboarding

enum class HouseholdOnboardingStep {
    HOME,
    CREATE,
    JOIN
}

data class HouseholdOnboardingState(
    val isLoading: Boolean = false,
    val currentStep: HouseholdOnboardingStep = HouseholdOnboardingStep.HOME,
    val householdNickname: String = "",
    val householdAddress: String = "",
    val joinCode: String = ""
)
