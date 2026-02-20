package com.djtaylor.wordjourney.domain.model

/**
 * In-memory game state for a single active round.
 * [targetWord] is NEVER passed to the grid tiles until status == WON,
 * ensuring the word is never revealed to the player during play.
 */
data class GameState(
    val difficulty: Difficulty,
    val level: Int,
    val targetWord: String,                             // hidden from UI until WIN
    val guesses: List<List<Pair<Char, TileState>>> = emptyList(), // completed rows
    val currentInput: List<Char> = emptyList(),         // letters typed in active row
    val maxGuesses: Int = difficulty.maxGuesses,        // can increase with bonus rows
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val invalidShake: Boolean = false                   // triggers shake animation once
) {
    val currentRow: Int get() = guesses.size
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val isInputFull: Boolean get() = currentInput.size == difficulty.wordLength

    /** Best known state for each letter key on the keyboard. */
    val letterStates: Map<Char, TileState> by lazy {
        val map = mutableMapOf<Char, TileState>()
        for (row in guesses) {
            for ((ch, state) in row) {
                val current = map[ch]
                // Green > Yellow > Grey priority; never downgrade a greener state
                if (current == null || state.priority > current.priority) {
                    map[ch] = state
                }
            }
        }
        map
    }
}

private val TileState.priority: Int
    get() = when (this) {
        TileState.CORRECT  -> 3
        TileState.PRESENT  -> 2
        TileState.ABSENT   -> 1
        else               -> 0
    }
