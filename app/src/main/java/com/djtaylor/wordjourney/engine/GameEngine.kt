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
    /**
     * Effective word length for this game instance.
     * For VIP difficulty this varies per level (3-7); for others it equals difficulty.wordLength.
     */
    val effectiveWordLength: Int = targetWord.length

    init {
        if (difficulty == Difficulty.VIP) {
            require(targetWord.length in 3..7) {
                "VIP target word length must be 3-7, got ${targetWord.length}"
            }
        } else {
            require(targetWord.length == difficulty.wordLength) {
                "Target word '${targetWord}' length ${targetWord.length} must equal difficulty word length ${difficulty.wordLength}"
            }
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
    /**
     * Positions pre-filled by the "Show Letter" item (position → char).
     * These persist for the lifetime of the level and count toward [isInputFull].
     */
    var prefilledPositions: Map<Int, Char> = emptyMap()
        private set

    // ── Derived properties ───────────────────────────────────────────────────
    val currentRow: Int get() = guesses.size
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    /** Number of positions the user must type (total − pre-filled by item). */
    val freePositionCount: Int get() = effectiveWordLength - prefilledPositions.size
    val isInputFull: Boolean get() = currentInput.size >= freePositionCount
    val canSubmit: Boolean get() = isInputFull && status == GameStatus.IN_PROGRESS
    val wordLength: Int get() = effectiveWordLength

    /**
     * Positional view of the current row, merging [prefilledPositions] and [currentInput].
     * Null means the cell is empty (not yet typed). Used by [GameGrid] for display.
     */
    val displayInput: List<Char?>
        get() {
            val result = arrayOfNulls<Char>(effectiveWordLength)
            prefilledPositions.forEach { (pos, ch) -> result[pos] = ch }
            var userIdx = 0
            for (pos in 0 until effectiveWordLength) {
                if (result[pos] == null && userIdx < currentInput.size) {
                    result[pos] = currentInput[userIdx++]
                }
            }
            return result.toList()
        }

    // ── Input ────────────────────────────────────────────────────────────────

    /**
     * Type a letter. Silently ignored if:
     *  - Game is not IN_PROGRESS
     *  - Input is already full (all free positions filled)
     *  - The letter has been removed by the "Remove Letter" item
     *
     * @return true if the letter was accepted
     */
    fun onKeyPressed(char: Char): Boolean {
        if (status != GameStatus.IN_PROGRESS) return false
        if (currentInput.size >= freePositionCount) return false
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
     * Builds the full word by merging [prefilledPositions] (from the Show Letter item)
     * with [currentInput] (typed by the user into the remaining free positions).
     * The word is validated against the dictionary via [wordValidator].
     * If valid, evaluated using the Wordle algorithm. Letter states are updated
     * with highest-priority state per letter (CORRECT > PRESENT > ABSENT).
     *
     * @return a [SubmitResult] indicating the outcome
     */
    suspend fun onSubmit(): SubmitResult {
        if (!canSubmit) return SubmitResult.NotReady

        // Build the full guess by merging pre-filled and user-typed positions
        val freePositions = (0 until effectiveWordLength).filter { it !in prefilledPositions }
        val fullGuess = Array(effectiveWordLength) { ' ' }
        prefilledPositions.forEach { (pos, ch) -> fullGuess[pos] = ch }
        freePositions.forEachIndexed { idx, pos ->
            if (idx < currentInput.size) fullGuess[pos] = currentInput[idx]
        }
        val guess = fullGuess.joinToString("").uppercase()

        if (!wordValidator(guess, effectiveWordLength)) {
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
     * Pre-fill a position with a revealed letter (from the "Show Letter" item).
     * This position is excluded from user typing and counts towards [isInputFull].
     * Pre-fills persist for the lifetime of the level.
     */
    fun prefillPosition(pos: Int, char: Char) {
        require(pos in 0 until effectiveWordLength) { "Position $pos out of range" }
        prefilledPositions = prefilledPositions + (pos to char.uppercaseChar())
    }

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
        savedMaxGuesses: Int,
        savedPrefilled: Map<Int, Char> = emptyMap()
    ) {
        guesses = savedGuesses
        currentInput = savedInput
        maxGuesses = savedMaxGuesses
        prefilledPositions = savedPrefilled
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
