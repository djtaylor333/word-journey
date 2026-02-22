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
    val showDailyLossDialog: Boolean = false,
    val dailyLossWord: String = "",

    // Win screen data
    val winCoinEarned: Long = 0L,
    val winDefinition: String = "",
    val winWord: String = "",            // word revealed only on win
    val bonusLifeEarned: Boolean = false,
    val starsEarned: Int = 0,            // 1-3 stars awarded on win

    // Player resources displayed in toolbar
    val lives: Int = 10,
    val coins: Long = 0L,
    val diamonds: Int = 5,

    // Item inventory
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,
    val definitionItems: Int = 0,
    val showLetterItems: Int = 0,

    // Eliminated letters by "Remove a Letter" item
    val removedLetters: Set<Char> = emptySet(),

    // Revealed letters by "Show Letter" item: position -> correct char
    val revealedLetters: Map<Int, Char> = emptyMap(),

    // Shake animation trigger (resets to false automatically)
    val shakeCurrentRow: Boolean = false,

    // Snackbar message
    val snackbarMessage: String? = null,

    // Loading
    val isLoading: Boolean = true,

    // Effective word length (may differ from difficulty.wordLength for VIP levels)
    val wordLength: Int = difficulty.wordLength,

    // Replay mode — replaying a completed level (no life cost, no rewards)
    val isReplay: Boolean = false,

    // Daily challenge mode
    val isDailyChallenge: Boolean = false,

    // VIP status (controls item lock in daily challenge)
    val isVip: Boolean = false,

    // Definition item
    val definitionHint: String? = null,
    val showDefinitionDialog: Boolean = false,
    val definitionUsedThisLevel: Boolean = false,

    // Whether the current word has a definition (disables button when false)
    val wordHasDefinition: Boolean = true,

    // Streak milestone reward message shown in win dialog
    val streakRewardMessage: String? = null
) {
    val currentRow: Int get() = guesses.size
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val isInputFull: Boolean get() = currentInput.size + revealedLetters.size >= wordLength
    val canSubmit: Boolean get() = isInputFull && status == GameStatus.IN_PROGRESS
    val regularLives: Int get() = minOf(lives, 10)
    val bonusLives: Int get() = maxOf(lives - 10, 0)
}
