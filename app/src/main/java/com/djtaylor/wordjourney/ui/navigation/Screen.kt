package com.djtaylor.wordjourney.ui.navigation

sealed class Screen(val route: String) {
    data object Home     : Screen("home")
    data object Settings : Screen("settings")
    data object Store    : Screen("store")

    data class Game(val difficultyKey: String = "{difficulty}") :
        Screen("game/{difficulty}") {

        companion object {
            fun route(difficultyKey: String) = "game/$difficultyKey"
        }
    }
}
