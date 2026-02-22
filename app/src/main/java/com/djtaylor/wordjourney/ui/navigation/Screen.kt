package com.djtaylor.wordjourney.ui.navigation

sealed class Screen(val route: String) {
    data object Home         : Screen("home")
    data object Settings     : Screen("settings")
    data object Store        : Screen("store")
    data object DailyChallenge : Screen("daily_challenge")
    data object ThemedPacks  : Screen("themed_packs")
    data object Statistics   : Screen("statistics")
    data object Achievements : Screen("achievements")
    data object Onboarding   : Screen("onboarding")
    data object Inbox        : Screen("inbox")

    data class LevelSelect(val difficultyKey: String = "{difficulty}") :
        Screen("level_select/{difficulty}") {

        companion object {
            fun route(difficultyKey: String) = "level_select/$difficultyKey"
        }
    }

    data class Game(
        val difficultyKey: String = "{difficulty}",
        val levelArg: Int = 0
    ) : Screen("game/{difficulty}/{level}") {

        companion object {
            fun route(difficultyKey: String, level: Int) = "game/$difficultyKey/$level"
        }
    }

    data class DailyChallengeGame(
        val wordLengthArg: Int = 0
    ) : Screen("daily_game/{wordLength}") {

        companion object {
            fun route(wordLength: Int) = "daily_game/$wordLength"
        }
    }
}
