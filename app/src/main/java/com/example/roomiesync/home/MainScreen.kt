package com.example.roomiesync.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomiesync.chore.ChoreScreen
import com.example.roomiesync.components.BottomNavBar
import com.example.roomiesync.components.NavItem
import com.example.roomiesync.household_onboarding.HouseholdOnboardingScreen
import io.github.jan.supabase.auth.user.UserInfo

@Composable
fun MainScreen(
    user: UserInfo,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val userHasHouse = true // TODO: This should be retrieved from the user's profile

    Scaffold(
        modifier = Modifier,
        bottomBar = { if (userHasHouse) BottomNavBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (userHasHouse) NavItem.Home.route else "household_onboarding",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("household_onboarding") {
                HouseholdOnboardingScreen()
            }
            composable(NavItem.Home.route) {
                HomeScreen(
                    user = user,
                    onSignOut = onSignOut
                )
            }
            composable(NavItem.Chores.route) {
                ChoreScreen()
            }
            composable(NavItem.Calendar.route) {
                Text(text = "Calendar")
            }
            composable(NavItem.Bills.route) {
                Text(text = "Bills")
            }
            composable(NavItem.Chat.route) {
                Text(text = "Chat")
            }
        }
    }
}