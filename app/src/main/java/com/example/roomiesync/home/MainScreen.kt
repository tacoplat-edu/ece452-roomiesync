package com.example.roomiesync.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.roomiesync.chore.ChoreScreen
import com.example.roomiesync.chore.CreateChoreFormScreen
import com.example.roomiesync.components.BottomNavBar
import com.example.roomiesync.components.NavItem
import com.example.roomiesync.data.Profile
import com.example.roomiesync.household_onboarding.HouseholdOnboardingScreen
import com.example.roomiesync.profile.ProfileScreen
import io.github.jan.supabase.auth.user.UserInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserInfo,
    profile: Profile?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userHasHouse = true // TODO: This should be retrieved from the user's profile

    // Routes where the bottom navigation bar and top bar should be hidden
    // We treat "profile" as a screen without the main bars (similar to Create Chore)
    val routesWithoutBars = setOf("create_chore", "household_onboarding", "profile")

    val showBars by remember(currentRoute) {
        derivedStateOf { currentRoute !in routesWithoutBars }
    }

    val title = when (currentRoute) {
        NavItem.Chores.route -> "Chores"
        NavItem.Calendar.route -> "Calendar"
        NavItem.Bills.route -> "Bills"
        NavItem.Chat.route -> "Chat"
        else -> ""
    }

    val isTitleEmpty = title.isEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = {
                        if (!isTitleEmpty) {
                            Text(
                                text = title,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                navController.navigate("profile") {
                                    launchSingleTop = true
                            }
                        }) {
                            if (profile?.avatarUrl != null) {
                                // Placeholder for image loading
                                // TODO: connect to avatar urls
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5) // Match background color of screens
                    ),
                )
            }
        },
        bottomBar = {
            if (userHasHouse && showBars) {
                BottomNavBar(navController = navController)
            }
        }
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
                ChoreScreen(
                    onAddChoreClick = { navController.navigate("create_chore") }
                )
            }
            composable("create_chore") {
                CreateChoreFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
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
            composable("profile") {
                ProfileScreen(
                    onEditProfileClick = { /* TODO */ },
                    onHouseholdClick = { /* TODO */ },
                    onCloseClick = { navController.popBackStack() }
                )
            }
        }
    }
}
