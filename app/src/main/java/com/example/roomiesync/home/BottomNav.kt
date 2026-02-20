package com.example.roomiesync.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.roomiesync.ui.theme.Typography

sealed class NavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : NavItem("home", Icons.Outlined.Home, "Home")
    object Chores : NavItem("chores", Icons.Outlined.CheckCircle, "Chores")
    object Calendar : NavItem("calendar", Icons.Outlined.CalendarToday, "Calendar")
    object Bills : NavItem("bills", Icons.Outlined.AttachMoney, "Bills")
    object Chat : NavItem("chat", Icons.Outlined.ChatBubbleOutline, "Chat")
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavItem.Home,
        NavItem.Chores,
        NavItem.Calendar,
        NavItem.Bills,
        NavItem.Chat
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary,
        windowInsets = WindowInsets(0.dp)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label, style = Typography.labelSmall) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}