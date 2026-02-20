package com.djtaylor.wordjourney.ui.navigation

sealed class Screen(val route: String) {
    data object Home     : Screen("home")
    data object Settings : Screen("settings")
    data object Store    : Screen("store")

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
}
