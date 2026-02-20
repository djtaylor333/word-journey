package com.djtaylor.wordjourney.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.djtaylor.wordjourney.ui.game.GameScreen
import com.djtaylor.wordjourney.ui.home.HomeScreen
import com.djtaylor.wordjourney.ui.levelselect.LevelSelectScreen
import com.djtaylor.wordjourney.ui.settings.SettingsScreen
import com.djtaylor.wordjourney.ui.store.StoreScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLevelSelect = { difficulty ->
                    navController.navigate(Screen.LevelSelect.route(difficulty))
                },
                onNavigateToStore   = { navController.navigate(Screen.Store.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.LevelSelect().route,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStack ->
            val difficultyKey = backStack.arguments?.getString("difficulty") ?: "regular"
            LevelSelectScreen(
                difficultyKey = difficultyKey,
                onNavigateToGame = { difficulty, level ->
                    navController.navigate(Screen.Game.route(difficulty, level))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Game().route,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("level") { type = NavType.IntType }
            )
        ) { backStack ->
            val difficultyKey = backStack.arguments?.getString("difficulty") ?: "regular"
            val level = backStack.arguments?.getInt("level") ?: 1
            GameScreen(
                difficultyKey    = difficultyKey,
                levelArg         = level,
                onNavigateHome   = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onNavigateToStore = { navController.navigate(Screen.Store.route) }
            )
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
