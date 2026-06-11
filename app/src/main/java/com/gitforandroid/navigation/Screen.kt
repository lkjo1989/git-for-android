package com.gitforandroid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Terminal : Screen("terminal?repoId={repoId}") {
        const val ROUTE = "terminal?repoId={repoId}"
        fun createRoute(repoId: Long? = null) = if (repoId != null) "terminal?repoId=$repoId" else "terminal"
    }
    data object Settings : Screen("settings")
    data object Clone : Screen("clone")
    data class Status(val repoId: Long) : Screen("status/{repoId}") {
        companion object {
            const val ROUTE = "status/{repoId}"
            fun createRoute(repoId: Long) = "status/$repoId"
        }
    }
    data class Commit(val repoId: Long) : Screen("commit/{repoId}") {
        companion object {
            const val ROUTE = "commit/{repoId}"
            fun createRoute(repoId: Long) = "commit/$repoId"
        }
    }
    data class Log(val repoId: Long) : Screen("log/{repoId}") {
        companion object {
            const val ROUTE = "log/{repoId}"
            fun createRoute(repoId: Long) = "log/$repoId"
        }
    }
    data class CommitDetail(val repoId: Long, val commitHash: String) :
        Screen("commit_detail/{repoId}/{commitHash}") {
        companion object {
            const val ROUTE = "commit_detail/{repoId}/{commitHash}"
            fun createRoute(repoId: Long, commitHash: String) = "commit_detail/$repoId/$commitHash"
        }
    }
    data class Branches(val repoId: Long) : Screen("branches/{repoId}") {
        companion object {
            const val ROUTE = "branches/{repoId}"
            fun createRoute(repoId: Long) = "branches/$repoId"
        }
    }
    data class PushPull(val repoId: Long) : Screen("push_pull/{repoId}") {
        companion object {
            const val ROUTE = "push_pull/{repoId}"
            fun createRoute(repoId: Long) = "push_pull/$repoId"
        }
    }
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Repos",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder,
        route = Screen.Home.route
    ),
    BottomNavItem(
        label = "Terminal",
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        route = Screen.Terminal.route
    ),
    BottomNavItem(
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        route = Screen.Settings.route
    )
)
