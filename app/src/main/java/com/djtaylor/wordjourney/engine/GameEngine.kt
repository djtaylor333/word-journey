package com.djtaylor.wordjourney.engine

import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.domain.usecase.EvaluateGuessUseCase

/**
 * The result of submitting a guess.
 */
sealed class SubmitResult {
    /** Guess was invalid (not a real word). */
    data class InvalidWord(val guess: String) : SubmitResult()
    /** Guess was valid, evaluated, game continues. */
    data class Evaluated(
        val tiles: List<Pair<Char, TileState>>,
        val isWin: Boolean,
        val isOutOfGuesses: Boolean
    ) : SubmitResult()
    /** Input is not the correct length or game is not in progress. */
    data object NotReady : SubmitResult()
}

/**
 * Pure game engine — no Android dependencies, fully testable.
 *
 * Encapsulates ALL game state and rules for a single Wordle level:
 *  - Typing / deleting letters
 *  - Submitting guesses with validation
 *  - Wordle evaluation (correct/present/absent with duplicate handling)
 *  - Keyboard letter state tracking
 *  - Game status transitions (IN_PROGRESS → WON or WAITING_FOR_LIFE)
 *  - Bonus guess granting
 *  - Letter removal item
 *
 * The ViewModel becomes a thin wrapper that adds navigation, persistence,
 * audio, and player progress on top of this engine.
 */
class GameEngine(
    val difficulty: Difficulty,
    private val targetWord: String,
    private val evaluateGuess: EvaluateGuessUseCase = EvaluateGuessUseCase(),
    private val wordValidator: suspend (String, Int) -> Boolean = { _, _ -> true }
) {
    init {
        require(targetWord.length == difficulty.wordLength) {
            "Target word '${targetWord}' length ${targetWord.length} must equal difficulty word length ${difficulty.wordLength}"
        }
        require(targetWord == targetWord.uppercase()) {
            "Target word must be uppercase"
        }
    }

    // ── Observable state ─────────────────────────────────────────────────────
    var guesses: List<List<Pair<Char, TileState>>> = emptyList()
        private set
    var currentInput: List<Char> = emptyList()
        private set
    var maxGuesses: Int = difficulty.maxGuesses
        private set
    var letterStates: Map<Char, TileState> = emptyMap()
        private set
    var removedLetters: Set<Char> = emptySet()
        private set
    var status: GameStatus = GameStatus.IN_PROGRESS
        private set

    // ── Derived properties ───────────────────────────────────────────────────
    val currentRow: Int get() = guesses.size
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val isInputFull: Boolean get() = currentInput.size == difficulty.wordLength
    val canSubmit: Boolean get() = isInputFull && status == GameStatus.IN_PROGRESS
    val wordLength: Int get() = difficulty.wordLength

    // ── Input ────────────────────────────────────────────────────────────────

    /**
     * Type a letter. Silently ignored if:
     *  - Game is not IN_PROGRESS
     *  - Input is already full
     *  - The letter has been removed by the "Remove Letter" item
     *
     * @return true if the letter was accepted
     */
    fun onKeyPressed(char: Char): Boolean {
        if (status != GameStatus.IN_PROGRESS) return false
        if (currentInput.size >= difficulty.wordLength) return false
        if (char.uppercaseChar() in removedLetters) return false
        currentInput = currentInput + char.uppercaseChar()
        return true
    }

    /**
     * Delete the last typed letter.
     * @return true if a letter was deleted
     */
    fun onDelete(): Boolean {
        if (currentInput.isEmpty()) return false
        currentInput = currentInput.dropLast(1)
        return true
    }

    // ── Submission ───────────────────────────────────────────────────────────

    /**
     * Submit the current input as a guess.
     *
     * The word is validated against the dictionary via [wordValidator].
     * If valid, evaluated using the Wordle algorithm. letter states are updated
     * with highest-priority state per letter (CORRECT > PRESENT > ABSENT).
     *
     * @return a [SubmitResult] indicating the outcome
     */
    suspend fun onSubmit(): SubmitResult {
        if (!canSubmit) return SubmitResult.NotReady

        val guess = currentInput.joinToString("").uppercase()

        if (!wordValidator(guess, difficulty.wordLength)) {
            return SubmitResult.InvalidWord(guess)
        }

        val evaluated = evaluateGuess(guess, targetWord)
        guesses = guesses + listOf(evaluated)
        currentInput = emptyList()
        updateLetterStates(evaluated)

        val isWin = evaluated.all { it.second == TileState.CORRECT }
        if (isWin) {
            status = GameStatus.WON
            return SubmitResult.Evaluated(evaluated, isWin = true, isOutOfGuesses = false)
        }

        val outOfGuesses = guesses.size >= maxGuesses
        if (outOfGuesses) {
            status = GameStatus.WAITING_FOR_LIFE
        }

        return SubmitResult.Evaluated(evaluated, isWin = false, isOutOfGuesses = outOfGuesses)
    }

    // ── Items / bonus ────────────────────────────────────────────────────────

    /**
     * Grant additional guesses (e.g., from spending a life or using an item).
     * Resumes the game if it was WAITING_FOR_LIFE.
     */
    fun addBonusGuesses(count: Int) {
        require(count > 0) { "Bonus guess count must be positive" }
        maxGuesses += count
        if (status == GameStatus.WAITING_FOR_LIFE) {
            status = GameStatus.IN_PROGRESS
        }
    }

    /**
     * Eliminate a letter from the keyboard.
     * The letter must NOT be in the target word (caller must ensure this).
     * Returns true if the letter was successfully removed.
     */
    fun removeLetter(letter: Char): Boolean {
        val ch = letter.uppercaseChar()
        if (ch in removedLetters) return false
        if (ch in targetWord) return false // safety check
        removedLetters = removedLetters + ch
        letterStates = letterStates + (ch to TileState.ABSENT)
        // Also remove from current input if present
        currentInput = currentInput.filter { it != ch }
        return true
    }

    // ── State restoration (for saved games) ──────────────────────────────────

    /**
     * Restore engine state from a saved game.
     */
    fun restore(
        savedGuesses: List<List<Pair<Char, TileState>>>,
        savedInput: List<Char>,
        savedMaxGuesses: Int
    ) {
        guesses = savedGuesses
        currentInput = savedInput
        maxGuesses = savedMaxGuesses
        letterStates = buildLetterMap(savedGuesses)
        status = GameStatus.IN_PROGRESS
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private fun updateLetterStates(evaluated: List<Pair<Char, TileState>>) {
        val updated = letterStates.toMutableMap()
        for ((ch, state) in evaluated) {
            val current = updated[ch]
            if (current == null || state.priority > current.priority) {
                updated[ch] = state
            }
        }
        letterStates = updated
    }

    private fun buildLetterMap(
        allGuesses: List<List<Pair<Char, TileState>>>
    ): Map<Char, TileState> {
        val map = mutableMapOf<Char, TileState>()
        for (row in allGuesses) {
            for ((ch, state) in row) {
                val current = map[ch]
                if (current == null || state.priority > current.priority) {
                    map[ch] = state
                }
            }
        }
        return map
    }

    private val TileState.priority: Int
        get() = when (this) {
            TileState.CORRECT -> 3
            TileState.PRESENT -> 2
            TileState.ABSENT -> 1
            else -> 0
        }
}
