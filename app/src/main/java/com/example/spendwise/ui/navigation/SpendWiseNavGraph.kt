package com.example.spendwise.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.spendwise.data.repository.*
import com.example.spendwise.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Groups : Screen("groups", "Groups", Icons.Default.Person)
    object Friends : Screen("friends", "Friends", Icons.Default.Person)
    object Activity : Screen("activity", "Activity", Icons.Default.Notifications)
    object Account : Screen("account", "Account", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendWiseNavGraph(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    expenseRepository: ExpenseRepository,
    activityRepository: ActivityRepository
) {
    val navController = rememberNavController()
    val currentUser by userRepository.getCurrentUser().collectAsState(initial = null)
    val isLoggedIn = currentUser != null

    if (!isLoggedIn) {
        AuthScreen(authRepository = authRepository, onAuthSuccess = {
            // State flow updates reactively
        })
        return
    }

    val items = listOf(Screen.Groups, Screen.Friends, Screen.Activity, Screen.Account)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Groups.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Groups.route) {
                GroupsScreen(
                    groupRepository = groupRepository,
                    userRepository = userRepository,
                    onGroupClick = { groupId -> navController.navigate("group_detail/$groupId") }
                )
            }
            composable(Screen.Friends.route) {
                FriendsScreen(
                    userRepository = userRepository,
                    onFriendClick = { friendId -> navController.navigate("friend_detail/$friendId") }
                )
            }
            composable(Screen.Activity.route) {
                ActivityScreen(activityRepository = activityRepository)
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    userRepository = userRepository,
                    authRepository = authRepository,
                    onSignOut = {
                        // User state flow updates to null, automatically showing AuthScreen
                    }
                )
            }
            composable(
                route = "group_detail/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupDetailScreen(
                    groupId = groupId,
                    groupRepository = groupRepository,
                    expenseRepository = expenseRepository,
                    userRepository = userRepository,
                    onBack = { navController.popBackStack() },
                    onAddExpense = { gId -> navController.navigate("add_expense/$gId") },
                    onSettleUp = { gId -> navController.navigate("settle_up_group/$gId") }
                )
            }
            composable(
                route = "friend_detail/{friendId}",
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                FriendDetailScreen(
                    friendId = friendId,
                    userRepository = userRepository,
                    expenseRepository = expenseRepository,
                    onBack = { navController.popBackStack() },
                    onSettleUp = { fId -> navController.navigate("settle_up_friend/$fId") }
                )
            }
            composable(
                route = "add_expense/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                AddExpenseScreen(
                    groupId = groupId,
                    expenseRepository = expenseRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "settle_up_group/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                SettleUpScreen(
                    groupId = groupId,
                    friendId = "",
                    expenseRepository = expenseRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "settle_up_friend/{friendId}",
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                SettleUpScreen(
                    groupId = "",
                    friendId = friendId,
                    expenseRepository = expenseRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
