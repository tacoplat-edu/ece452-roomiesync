package com.example.roomiesync.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomiesync.components.BottomNavBar
import com.example.roomiesync.components.NavItem
import io.github.jan.supabase.auth.user.UserInfo

@Composable
fun MainScreen(
    user: UserInfo,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavItem.Home.route) {
                HomeScreen(
                    user = user,
                    onSignOut = onSignOut
                )
            }
            composable(NavItem.Chores.route) {
                Text(text = "Chores")
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