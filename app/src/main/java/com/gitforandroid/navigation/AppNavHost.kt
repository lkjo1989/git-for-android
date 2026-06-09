package com.gitforandroid.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gitforandroid.ui.gui.home.HomeScreen
import com.gitforandroid.ui.gui.operations.CloneScreen
import com.gitforandroid.ui.gui.status.StatusScreen
import com.gitforandroid.ui.gui.operations.CommitScreen
import com.gitforandroid.ui.gui.commits.LogScreen
import com.gitforandroid.ui.gui.commits.CommitDetailScreen
import com.gitforandroid.ui.gui.branches.BranchScreen
import com.gitforandroid.ui.gui.operations.PushPullScreen
import com.gitforandroid.ui.cli.TerminalScreen
import com.gitforandroid.ui.gui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onRepoClick = { repoId -> navController.navigate(Screen.Status.createRoute(repoId)) },
                    onCloneClick = { navController.navigate(Screen.Clone.route) }
                )
            }

            composable(Screen.Terminal.route) {
                TerminalScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.Clone.route) {
                CloneScreen(
                    onCloneComplete = { navController.popBackStack(Screen.Home.route, false) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Status.ROUTE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                StatusScreen(
                    repoId = repoId,
                    onCommitClick = { navController.navigate(Screen.Commit.createRoute(repoId)) },
                    onLogClick = { navController.navigate(Screen.Log.createRoute(repoId)) },
                    onBranchesClick = { navController.navigate(Screen.Branches.createRoute(repoId)) },
                    onPushPullClick = { navController.navigate(Screen.PushPull.createRoute(repoId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Commit.ROUTE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                CommitScreen(
                    repoId = repoId,
                    onCommitComplete = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Log.ROUTE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                LogScreen(
                    repoId = repoId,
                    onCommitClick = { hash ->
                        navController.navigate(Screen.CommitDetail.createRoute(repoId, hash))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CommitDetail.ROUTE,
                arguments = listOf(
                    navArgument("repoId") { type = NavType.LongType },
                    navArgument("commitHash") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                val commitHash = backStackEntry.arguments?.getString("commitHash") ?: return@composable
                CommitDetailScreen(
                    repoId = repoId,
                    commitHash = commitHash,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Branches.ROUTE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                BranchScreen(
                    repoId = repoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PushPull.ROUTE,
                arguments = listOf(navArgument("repoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getLong("repoId") ?: return@composable
                PushPullScreen(
                    repoId = repoId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
