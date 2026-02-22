package com.djtaylor.wordjourney.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.djtaylor.wordjourney.ui.dailychallenge.DailyChallengeScreen
import com.djtaylor.wordjourney.ui.game.GameScreen
import com.djtaylor.wordjourney.ui.home.HomeScreen
import com.djtaylor.wordjourney.ui.inbox.InboxScreen
import com.djtaylor.wordjourney.ui.levelselect.LevelSelectScreen
import com.djtaylor.wordjourney.ui.onboarding.OnboardingScreen
import com.djtaylor.wordjourney.ui.settings.SettingsScreen
import com.djtaylor.wordjourney.ui.statistics.StatisticsScreen
import com.djtaylor.wordjourney.ui.store.StoreScreen
import com.djtaylor.wordjourney.ui.timermode.TimerModeScreen

private const val ANIM_MS = 350

@Composable
fun AppNavigation(
    navController: NavHostController,
    hasCompletedOnboarding: Boolean = true
) {
    val startDestination = if (hasCompletedOnboarding) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(ANIM_MS)) + slideInHorizontally(tween(ANIM_MS)) { it / 4 } },
        exitTransition = { fadeOut(tween(ANIM_MS)) + slideOutHorizontally(tween(ANIM_MS)) { -it / 4 } },
        popEnterTransition = { fadeIn(tween(ANIM_MS)) + slideInHorizontally(tween(ANIM_MS)) { -it / 4 } },
        popExitTransition = { fadeOut(tween(ANIM_MS)) + slideOutHorizontally(tween(ANIM_MS)) { it / 4 } }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLevelSelect = { difficulty ->
                    navController.navigate(Screen.LevelSelect.route(difficulty))
                },
                onNavigateToStore    = { navController.navigate(Screen.Store.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDailyChallenge = { navController.navigate(Screen.DailyChallenge.route) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                onNavigateToTimerMode = { navController.navigate(Screen.TimerMode.route) }
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
                    if (difficultyKey.startsWith("daily")) {
                        navController.popBackStack()
                    } else {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                },
                onNavigateToStore = { navController.navigate(Screen.Store.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToNextLevel = { diff, nextLevel ->
                    navController.navigate(Screen.Game.route(diff, nextLevel)) {
                        popUpTo("level_select/{difficulty}") { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(
                onNavigateToGame = { wordLength ->
                    val dailyKey = "daily_$wordLength"
                    navController.navigate(Screen.Game.route(dailyKey, 1))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
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

        composable(Screen.Inbox.route) {
            InboxScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TimerMode.route) {
            TimerModeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
