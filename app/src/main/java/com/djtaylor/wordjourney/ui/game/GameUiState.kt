package com.djtaylor.wordjourney.ui.game

import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameState
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.TileState

data class GameUiState(
    val difficulty: Difficulty = Difficulty.REGULAR,
    val level: Int = 1,

    // Grid state — does NOT include targetWord; tiles only show evaluated states
    val guesses: List<List<Pair<Char, TileState>>> = emptyList(),
    val currentInput: List<Char> = emptyList(),
    val maxGuesses: Int = 6,
    val letterStates: Map<Char, TileState> = emptyMap(),

    // Dialogs / overlay state
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val showWinDialog: Boolean = false,
    val showNeedMoreGuessesDialog: Boolean = false,
    val showNoLivesDialog: Boolean = false,

    // Win screen data
    val winCoinEarned: Long = 0L,
    val winDefinition: String = "",
    val winWord: String = "",            // word revealed only on win
    val bonusLifeEarned: Boolean = false,

    // Player resources displayed in toolbar
    val lives: Int = 10,
    val coins: Long = 0L,
    val diamonds: Int = 5,

    // Item inventory
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,

    // Eliminated letters by "Remove a Letter" item
    val removedLetters: Set<Char> = emptySet(),

    // Shake animation trigger (resets to false automatically)
    val shakeCurrentRow: Boolean = false,

    // Snackbar message
    val snackbarMessage: String? = null,

    // Loading
    val isLoading: Boolean = true
) {
    val currentRow: Int get() = guesses.size
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val isInputFull: Boolean get() = currentInput.size == difficulty.wordLength
    val canSubmit: Boolean get() = isInputFull && status == GameStatus.IN_PROGRESS
}
