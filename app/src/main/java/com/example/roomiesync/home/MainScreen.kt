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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.roomiesync.billing.BillingScreen
import com.example.roomiesync.billing.CreateExpenseScreen
import com.example.roomiesync.calendar.CalendarScreen
import com.example.roomiesync.chore.ChoreScreen
import com.example.roomiesync.chore.CreateChoreFormScreen
import com.example.roomiesync.components.BottomNavBar
import com.example.roomiesync.components.NavItem
import com.example.roomiesync.data.Profile
import com.example.roomiesync.household_onboarding.HouseholdOnboardingScreen
import com.example.roomiesync.profile.ProfileScreen
import com.example.roomiesync.profile.EditProfileScreen
import com.example.roomiesync.ui.theme.PrimaryBackground
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
    val createChoreRoute = "create_chore?prefillDateMillis={prefillDateMillis}"

    // Routes where the bottom navigation bar and top bar should be hidden
    // We treat "profile" as a screen without the main bars (similar to Create Chore)
    val routesWithoutBars = setOf(
        "create_chore",
        createChoreRoute,
        "create_expense",
        "household_onboarding",
        "profile",
        "edit_profile"
    )

    val showBars by remember(currentRoute) {
        derivedStateOf { currentRoute !in routesWithoutBars }
    }

    val title = when (currentRoute) {
        NavItem.Home.route -> "RoomieSync"
        NavItem.Chores.route -> "Chores"
        NavItem.Calendar.route -> "Calendar"
        NavItem.Bills.route -> "Bills"
        NavItem.Chat.route -> "Chat"
        else -> ""
    }

    val isTitleEmpty = title.isEmpty()

    Scaffold(
        modifier = Modifier,
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
                        containerColor = PrimaryBackground
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
                )
            }
            composable(NavItem.Chores.route) {
                ChoreScreen(
                    onAddChoreClick = { navController.navigate("create_chore") }
                )
            }
            composable(
                route = createChoreRoute,
                arguments = listOf(
                    navArgument("prefillDateMillis") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val prefillMillis = backStackEntry.arguments
                    ?.getLong("prefillDateMillis")
                    ?.takeIf { it >= 0L }
                CreateChoreFormScreen(
                    prefillDueDateMillis = prefillMillis,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavItem.Calendar.route) {
                CalendarScreen(
                    onCreateChoreForDay = { prefillMillis ->
                        navController.navigate("create_chore?prefillDateMillis=$prefillMillis")
                    }
                )
            }
            composable(NavItem.Bills.route) {
                BillingScreen(
                    onAddExpenseClick = { navController.navigate("create_expense") }
                )
            }
            composable("create_expense") {
                CreateExpenseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavItem.Chat.route) {
                Text(text = "Chat")
            }
            composable("profile") {
                ProfileScreen(
                    onEditProfileClick = { navController.navigate("edit_profile") },
                    onHouseholdClick = { /* TODO */ },
                    onCloseClick = { navController.popBackStack() }
                )
            }
            composable("edit_profile") {
                EditProfileScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
