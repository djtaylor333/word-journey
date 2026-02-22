package com.djtaylor.wordjourney.engine

import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.TileState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive test suite for the GameEngine.
 *
 * Game rules verified:
 *  1. Player types letters into a row of tiles (wordLength chars)
 *  2. Only uppercase letters A-Z accepted; removed letters blocked
 *  3. Backspace deletes the last character
 *  4. Submit evaluates against target using Wordle algorithm
 *  5. Invalid words (not in dictionary) are rejected — grid unchanged
 *  6. On correct guess → GameStatus.WON
 *  7. When guesses exhausted → GameStatus.WAITING_FOR_LIFE
 *  8. Bonus guesses resume the game from WAITING_FOR_LIFE
 *  9. Keyboard letter states accumulate (highest priority wins)
 * 10. "Remove Letter" item eliminates letters from keyboard
 * 11. Each difficulty has correct wordLength and maxGuesses
 * 12. State restoration works for saved games
 */
class GameEngineTest {

    // ── Helper factories ─────────────────────────────────────────────────────

    /** Create an engine with all words accepted as valid. */
    private fun engine(
        target: String = "CRANE",
        difficulty: Difficulty = Difficulty.REGULAR,
        validator: suspend (String, Int) -> Boolean = { _, _ -> true }
    ) = GameEngine(
        difficulty = difficulty,
        targetWord = target,
        wordValidator = validator
    )

    private fun easyEngine(target: String = "BAKE") =
        engine(target = target, difficulty = Difficulty.EASY)

    private fun hardEngine(target: String = "BRIDGE") =
        engine(target = target, difficulty = Difficulty.HARD)

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALISATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `new engine starts IN_PROGRESS with empty state`() {
        val e = engine()
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertTrue(e.guesses.isEmpty())
        assertTrue(e.currentInput.isEmpty())
        assertTrue(e.letterStates.isEmpty())
        assertTrue(e.removedLetters.isEmpty())
        assertEquals(0, e.currentRow)
    }

    @Test
    fun `EASY difficulty has wordLength 4 and 6 max guesses`() {
        val e = easyEngine()
        assertEquals(4, e.wordLength)
        assertEquals(6, e.maxGuesses)
    }

    @Test
    fun `REGULAR difficulty has wordLength 5 and 6 max guesses`() {
        val e = engine()
        assertEquals(5, e.wordLength)
        assertEquals(6, e.maxGuesses)
    }

    @Test
    fun `HARD difficulty has wordLength 6 and 6 max guesses`() {
        val e = hardEngine()
        assertEquals(6, e.wordLength)
        assertEquals(6, e.maxGuesses)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `target word length mismatch throws`() {
        engine(target = "HI") // 2 chars for REGULAR (needs 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `lowercase target word throws`() {
        engine(target = "crane")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. KEY INPUT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `typing a letter adds it to currentInput`() {
        val e = engine()
        assertTrue(e.onKeyPressed('C'))
        assertEquals(listOf('C'), e.currentInput)
    }

    @Test
    fun `typing fills up to wordLength`() {
        val e = engine() // 5 letters
        "CRANE".forEach { e.onKeyPressed(it) }
        assertEquals(5, e.currentInput.size)
        assertEquals("CRANE".toList(), e.currentInput)
    }

    @Test
    fun `typing beyond wordLength is rejected`() {
        val e = engine()
        "CRANE".forEach { e.onKeyPressed(it) }
        assertFalse(e.onKeyPressed('X'))
        assertEquals(5, e.currentInput.size)
    }

    @Test
    fun `lowercase input is uppercased`() {
        val e = engine()
        e.onKeyPressed('a')
        assertEquals(listOf('A'), e.currentInput)
    }

    @Test
    fun `removed letter cannot be typed`() {
        val e = engine(target = "CRANE")
        e.removeLetter('X') // X not in CRANE
        assertFalse(e.onKeyPressed('X'))
        assertTrue(e.currentInput.isEmpty())
    }

    @Test
    fun `typing when game not IN_PROGRESS is ignored`() = runTest {
        val e = engine()
        // Win the game
        "CRANE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(GameStatus.WON, e.status)
        assertFalse(e.onKeyPressed('A'))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `delete removes last character`() {
        val e = engine()
        e.onKeyPressed('C')
        e.onKeyPressed('R')
        assertTrue(e.onDelete())
        assertEquals(listOf('C'), e.currentInput)
    }

    @Test
    fun `delete on empty input returns false`() {
        val e = engine()
        assertFalse(e.onDelete())
    }

    @Test
    fun `delete all characters returns to empty`() {
        val e = engine()
        e.onKeyPressed('A')
        e.onKeyPressed('B')
        e.onDelete()
        e.onDelete()
        assertTrue(e.currentInput.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. SUBMIT — BASIC
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit with incomplete input returns NotReady`() = runTest {
        val e = engine()
        e.onKeyPressed('C')
        val result = e.onSubmit()
        assertEquals(SubmitResult.NotReady, result)
        assertTrue(e.guesses.isEmpty()) // no guess added
    }

    @Test
    fun `submit correct guess wins the game`() = runTest {
        val e = engine(target = "CRANE")
        "CRANE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit()
        assertTrue(result is SubmitResult.Evaluated)
        val eval = result as SubmitResult.Evaluated
        assertTrue(eval.isWin)
        assertFalse(eval.isOutOfGuesses)
        assertEquals(GameStatus.WON, e.status)
        assertEquals(1, e.guesses.size)
        assertTrue(e.currentInput.isEmpty()) // input cleared after submit
    }

    @Test
    fun `submit wrong guess continues game`() = runTest {
        val e = engine(target = "CRANE")
        "HOUSE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit()
        assertTrue(result is SubmitResult.Evaluated)
        val eval = result as SubmitResult.Evaluated
        assertFalse(eval.isWin)
        assertFalse(eval.isOutOfGuesses)
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertEquals(1, e.guesses.size)
        assertTrue(e.currentInput.isEmpty())
    }

    @Test
    fun `submit clears currentInput`() = runTest {
        val e = engine(target = "CRANE")
        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertTrue(e.currentInput.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. SUBMIT — WORD VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `invalid word is rejected and grid is unchanged`() = runTest {
        val e = engine(
            target = "CRANE",
            validator = { word, _ -> word != "XXXXX" }
        )
        "XXXXX".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit()
        assertTrue(result is SubmitResult.InvalidWord)
        assertEquals("XXXXX", (result as SubmitResult.InvalidWord).guess)
        assertTrue(e.guesses.isEmpty()) // no guess added
        // IMPORTANT: input is NOT cleared on invalid word, allowing user to edit
        assertEquals("XXXXX".toList(), e.currentInput)
    }

    @Test
    fun `valid word is accepted`() = runTest {
        val e = engine(
            target = "CRANE",
            validator = { word, _ -> word == "HOUSE" }
        )
        "HOUSE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit()
        assertTrue(result is SubmitResult.Evaluated)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. SUBMIT — TILE EVALUATION (Wordle algorithm)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `tiles show correct evaluation on submit`() = runTest {
        val e = engine(target = "CRANE")
        "CRATE".forEach { e.onKeyPressed(it) }
        val result = (e.onSubmit() as SubmitResult.Evaluated).tiles
        // C-correct, R-correct, A-correct, T-absent (N), E-correct
        assertEquals(TileState.CORRECT, result[0].second) // C
        assertEquals(TileState.CORRECT, result[1].second) // R
        assertEquals(TileState.CORRECT, result[2].second) // A
        assertEquals(TileState.ABSENT, result[3].second)  // T (N in target)
        assertEquals(TileState.CORRECT, result[4].second) // E
    }

    @Test
    fun `tiles for wrong position show PRESENT`() = runTest {
        val e = engine(target = "CRANE")
        // EARNS: E at 0 (target pos4=PRESENT), A at 1 (target pos2=PRESENT),
        // R at 2 (target pos1=PRESENT), N at 3 (target pos3=CORRECT), S at 4 (ABSENT)
        "EARNS".forEach { e.onKeyPressed(it) }
        val result = (e.onSubmit() as SubmitResult.Evaluated).tiles
        assertEquals(TileState.PRESENT, result[0].second) // E
        assertEquals(TileState.PRESENT, result[1].second) // A
        assertEquals(TileState.PRESENT, result[2].second) // R
        assertEquals(TileState.CORRECT, result[3].second) // N
        assertEquals(TileState.ABSENT, result[4].second)  // S
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. RUNNING OUT OF GUESSES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `game transitions to WAITING_FOR_LIFE when guesses exhausted`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB", "DWELT")
        for (word in wrongWords) {
            word.forEach { e.onKeyPressed(it) }
            val result = e.onSubmit() as SubmitResult.Evaluated
            if (word == wrongWords.last()) {
                assertTrue("Last guess should flag out of guesses", result.isOutOfGuesses)
            }
        }
        assertEquals(GameStatus.WAITING_FOR_LIFE, e.status)
        assertEquals(6, e.guesses.size)
        assertEquals(0, e.remainingGuesses)
    }

    @Test
    fun `cannot type or submit when WAITING_FOR_LIFE`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB", "DWELT")
        wrongWords.forEach { word ->
            word.forEach { e.onKeyPressed(it) }
            e.onSubmit()
        }
        assertFalse(e.onKeyPressed('A'))
        assertEquals(SubmitResult.NotReady, e.onSubmit())
    }

    @Test
    fun `win on last guess does not trigger WAITING_FOR_LIFE`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB")
        wrongWords.forEach { word ->
            word.forEach { e.onKeyPressed(it) }
            e.onSubmit()
        }
        // 6th guess is the correct one
        "CRANE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertFalse(result.isOutOfGuesses)
        assertEquals(GameStatus.WON, e.status)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. BONUS GUESSES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `addBonusGuesses increases maxGuesses`() {
        val e = engine()
        assertEquals(6, e.maxGuesses)
        e.addBonusGuesses(2)
        assertEquals(8, e.maxGuesses)
    }

    @Test
    fun `addBonusGuesses resumes from WAITING_FOR_LIFE`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB", "DWELT")
        wrongWords.forEach { word ->
            word.forEach { e.onKeyPressed(it) }
            e.onSubmit()
        }
        assertEquals(GameStatus.WAITING_FOR_LIFE, e.status)

        e.addBonusGuesses(2) // REGULAR bonus = 2
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertEquals(8, e.maxGuesses)
        assertEquals(2, e.remainingGuesses)
    }

    @Test
    fun `can type and submit after bonus guesses`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB", "DWELT")
        wrongWords.forEach { word ->
            word.forEach { e.onKeyPressed(it) }
            e.onSubmit()
        }
        e.addBonusGuesses(1)
        "CRANE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(GameStatus.WON, e.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addBonusGuesses with zero throws`() {
        engine().addBonusGuesses(0)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. KEYBOARD LETTER STATES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `letter states accumulate after guess`() = runTest {
        val e = engine(target = "CRANE")
        "CRATE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(TileState.CORRECT, e.letterStates['C'])
        assertEquals(TileState.CORRECT, e.letterStates['R'])
        assertEquals(TileState.CORRECT, e.letterStates['A'])
        assertEquals(TileState.ABSENT, e.letterStates['T'])
        assertEquals(TileState.CORRECT, e.letterStates['E'])
    }

    @Test
    fun `CORRECT beats PRESENT for same letter across guesses`() = runTest {
        val e = engine(target = "CRANE")
        // First guess: E at wrong position shows PRESENT
        "EARNS".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(TileState.PRESENT, e.letterStates['E'])

        // Second guess: E at correct position shows CORRECT
        "CRATE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(TileState.CORRECT, e.letterStates['E']) // upgraded
    }

    @Test
    fun `higher priority state wins for same letter across guesses`() = runTest {
        // target=CRANE. Use a guess where a letter in the target appears
        // in the wrong position (PRESENT), then in a second guess at
        // the correct position (CORRECT). CORRECT should win.
        val e = engine(target = "CRANE")

        // Guess 1: EARNS — E at pos 0 (target pos 4) = PRESENT
        "EARNS".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(TileState.PRESENT, e.letterStates['E'])

        // Guess 2: CRATE — E at pos 4 (target pos 4) = CORRECT
        "CRATE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        // E should upgrade from PRESENT to CORRECT
        assertEquals(TileState.CORRECT, e.letterStates['E'])
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. REMOVE LETTER ITEM
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `removeLetter adds to removedLetters and marks ABSENT`() {
        val e = engine(target = "CRANE")
        assertTrue(e.removeLetter('X'))
        assertTrue(e.removedLetters.contains('X'))
        assertEquals(TileState.ABSENT, e.letterStates['X'])
    }

    @Test
    fun `cannot remove letter that is in target word`() {
        val e = engine(target = "CRANE")
        assertFalse(e.removeLetter('C'))
        assertFalse(e.removedLetters.contains('C'))
    }

    @Test
    fun `cannot remove same letter twice`() {
        val e = engine(target = "CRANE")
        assertTrue(e.removeLetter('X'))
        assertFalse(e.removeLetter('X'))
    }

    @Test
    fun `removeLetter strips that letter from currentInput`() {
        val e = engine(target = "CRANE")
        e.onKeyPressed('X')
        e.onKeyPressed('C')
        e.removeLetter('X')
        assertEquals(listOf('C'), e.currentInput)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. DIFFICULTY-SPECIFIC TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `easy mode — 4 letter word gameplay`() = runTest {
        val e = easyEngine(target = "BAKE")
        "BAKE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(4, result.tiles.size)
    }

    @Test
    fun `hard mode — 6 letter word gameplay`() = runTest {
        val e = hardEngine(target = "BRIDGE")
        "BRIDGE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(6, result.tiles.size)
    }

    @Test
    fun `easy mode — input bounded at 4 chars`() {
        val e = easyEngine()
        "BAKES".forEach { e.onKeyPressed(it) } // 5 chars
        assertEquals(4, e.currentInput.size) // only 4 accepted
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. STATE RESTORATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restore sets guesses and letter states`() {
        val e = engine(target = "CRANE")
        val savedGuesses = listOf(
            listOf(
                Pair('H', TileState.ABSENT),
                Pair('O', TileState.ABSENT),
                Pair('U', TileState.ABSENT),
                Pair('S', TileState.ABSENT),
                Pair('E', TileState.CORRECT)
            )
        )
        e.restore(savedGuesses, listOf('C', 'R'), 6)
        assertEquals(1, e.guesses.size)
        assertEquals(listOf('C', 'R'), e.currentInput)
        assertEquals(6, e.maxGuesses)
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertEquals(TileState.CORRECT, e.letterStates['E'])
        assertEquals(TileState.ABSENT, e.letterStates['H'])
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 13. FULL GAME FLOW — Integration
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `full game flow — guess wrong, then guess right`() = runTest {
        val e = engine(target = "CRANE")

        // Guess 1: HOUSE (wrong)
        "HOUSE".forEach { e.onKeyPressed(it) }
        val r1 = e.onSubmit() as SubmitResult.Evaluated
        assertFalse(r1.isWin)
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertEquals(1, e.guesses.size)
        assertEquals(5, e.remainingGuesses)

        // Guess 2: CRANE (correct!)
        "CRANE".forEach { e.onKeyPressed(it) }
        val r2 = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(r2.isWin)
        assertEquals(GameStatus.WON, e.status)
        assertEquals(2, e.guesses.size)
    }

    @Test
    fun `full game flow — exhaust guesses, get bonus, then win`() = runTest {
        val e = engine(target = "CRANE")
        val wrongWords = listOf("HOUSE", "DRINK", "BLAZE", "TOWER", "PLUMB", "DWELT")

        // Use all 6 guesses
        wrongWords.forEach { word ->
            word.forEach { e.onKeyPressed(it) }
            e.onSubmit()
        }
        assertEquals(GameStatus.WAITING_FOR_LIFE, e.status)
        assertEquals(0, e.remainingGuesses)

        // Get bonus guesses from spending a life
        e.addBonusGuesses(2) // REGULAR bonus
        assertEquals(GameStatus.IN_PROGRESS, e.status)
        assertEquals(2, e.remainingGuesses)

        // Guess correctly
        "CRANE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(GameStatus.WON, e.status)
        assertEquals(7, e.guesses.size)
    }

    @Test
    fun `full game flow with items`() = runTest {
        val e = engine(target = "CRANE")

        // Remove a letter
        assertTrue(e.removeLetter('Z'))
        assertEquals(TileState.ABSENT, e.letterStates['Z'])

        // Try to type removed letter — blocked
        assertFalse(e.onKeyPressed('Z'))

        // Type and submit a wrong guess
        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(1, e.guesses.size)

        // Add a bonus guess item
        e.addBonusGuesses(1)
        assertEquals(7, e.maxGuesses)

        // Win
        "CRANE".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 14. REMAINING GUESSES COUNTER
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `remainingGuesses decreases after each guess`() = runTest {
        val e = engine(target = "CRANE")
        assertEquals(6, e.remainingGuesses)

        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(5, e.remainingGuesses)

        "DRINK".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(4, e.remainingGuesses)
    }

    @Test
    fun `currentRow increments after each guess`() = runTest {
        val e = engine(target = "CRANE")
        assertEquals(0, e.currentRow)

        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(1, e.currentRow)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 15. EDGE CASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `submit after game won returns NotReady`() = runTest {
        val e = engine(target = "CRANE")
        "CRANE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(GameStatus.WON, e.status)

        // Try to play again
        val result = e.onSubmit()
        assertEquals(SubmitResult.NotReady, result)
    }

    @Test
    fun `guess is stored as evaluated tiles`() = runTest {
        val e = engine(target = "CRANE")
        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()

        val row = e.guesses[0]
        assertEquals(5, row.size)
        // Verify each tile has the guessed character
        assertEquals('H', row[0].first)
        assertEquals('O', row[1].first)
        assertEquals('U', row[2].first)
        assertEquals('S', row[3].first)
        assertEquals('E', row[4].first)
    }

    @Test
    fun `multiple guesses all stored in order`() = runTest {
        val e = engine(target = "CRANE")
        "HOUSE".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        "DRINK".forEach { e.onKeyPressed(it) }
        e.onSubmit()

        assertEquals(2, e.guesses.size)
        assertEquals('H', e.guesses[0][0].first)
        assertEquals('D', e.guesses[1][0].first)
    }

    @Test
    fun `canSubmit is false when input not full`() {
        val e = engine()
        e.onKeyPressed('A')
        assertFalse(e.canSubmit)
    }

    @Test
    fun `canSubmit is true when input is full and game in progress`() {
        val e = engine()
        "CRANE".forEach { e.onKeyPressed(it) }
        assertTrue(e.canSubmit)
    }

    @Test
    fun `isInputFull matches wordLength`() {
        val e = engine() // 5 letters
        assertFalse(e.isInputFull)
        "CRAN".forEach { e.onKeyPressed(it) }
        assertFalse(e.isInputFull)
        e.onKeyPressed('E')
        assertTrue(e.isInputFull)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 16. VIP DIFFICULTY — VARIABLE WORD LENGTHS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `VIP engine accepts 3-letter word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT"
        )
        assertEquals(3, e.wordLength)
        assertEquals(3, e.effectiveWordLength)
        assertEquals(GameStatus.IN_PROGRESS, e.status)
    }

    @Test
    fun `VIP engine accepts 4-letter word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "BAKE"
        )
        assertEquals(4, e.wordLength)
        assertEquals(4, e.effectiveWordLength)
    }

    @Test
    fun `VIP engine accepts 5-letter word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CRANE"
        )
        assertEquals(5, e.wordLength)
        assertEquals(5, e.effectiveWordLength)
    }

    @Test
    fun `VIP engine accepts 6-letter word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "BRIDGE"
        )
        assertEquals(6, e.wordLength)
        assertEquals(6, e.effectiveWordLength)
    }

    @Test
    fun `VIP engine accepts 7-letter word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "KITCHEN"
        )
        assertEquals(7, e.wordLength)
        assertEquals(7, e.effectiveWordLength)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `VIP engine rejects 2-letter word`() {
        GameEngine(difficulty = Difficulty.VIP, targetWord = "HI")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `VIP engine rejects 8-letter word`() {
        GameEngine(difficulty = Difficulty.VIP, targetWord = "ABSOLUTE")
    }

    @Test
    fun `VIP 3-letter gameplay — input bounded at 3 chars`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT"
        )
        "CATS".forEach { e.onKeyPressed(it) } // try 4 chars
        assertEquals(3, e.currentInput.size) // only 3 accepted
        assertTrue(e.isInputFull)
    }

    @Test
    fun `VIP 3-letter gameplay — submit and win`() = runTest {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT"
        )
        "CAT".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(3, result.tiles.size)
        assertEquals(GameStatus.WON, e.status)
    }

    @Test
    fun `VIP 7-letter gameplay — submit and win`() = runTest {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "KITCHEN"
        )
        "KITCHEN".forEach { e.onKeyPressed(it) }
        val result = e.onSubmit() as SubmitResult.Evaluated
        assertTrue(result.isWin)
        assertEquals(7, result.tiles.size)
        assertEquals(GameStatus.WON, e.status)
    }

    @Test
    fun `VIP 3-letter — word validator receives correct word length`() = runTest {
        var validatedLength = -1
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT",
            wordValidator = { _, len -> validatedLength = len; true }
        )
        "DOG".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(3, validatedLength) // should pass 3, not 5
    }

    @Test
    fun `VIP 7-letter — word validator receives correct word length`() = runTest {
        var validatedLength = -1
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "KITCHEN",
            wordValidator = { _, len -> validatedLength = len; true }
        )
        "POULTRY".forEach { e.onKeyPressed(it) }
        e.onSubmit()
        assertEquals(7, validatedLength) // should pass 7, not 5
    }

    @Test
    fun `VIP word length cycling pattern`() {
        // Verify the cycling pattern: 3, 4, 5, 6, 7, 3, 4, 5, ...
        assertEquals(3, Difficulty.vipWordLengthForLevel(1))
        assertEquals(4, Difficulty.vipWordLengthForLevel(2))
        assertEquals(5, Difficulty.vipWordLengthForLevel(3))
        assertEquals(6, Difficulty.vipWordLengthForLevel(4))
        assertEquals(7, Difficulty.vipWordLengthForLevel(5))
        assertEquals(3, Difficulty.vipWordLengthForLevel(6))  // wraps
        assertEquals(4, Difficulty.vipWordLengthForLevel(7))
        assertEquals(7, Difficulty.vipWordLengthForLevel(100)) // (100-1) % 5 = 4 → lengths[4] = 7
    }

    @Test
    fun `VIP engine — restore with non-default word length`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT"
        )
        val savedGuess = listOf(
            Pair('D', TileState.ABSENT),
            Pair('O', TileState.ABSENT),
            Pair('G', TileState.ABSENT)
        )
        e.restore(listOf(savedGuess), listOf('C'), 6)
        assertEquals(1, e.guesses.size)
        assertEquals(listOf('C'), e.currentInput)
        assertEquals(3, e.wordLength)
        assertEquals(GameStatus.IN_PROGRESS, e.status)
    }

    @Test
    fun `VIP engine — removeLetter works with short word`() {
        val e = GameEngine(
            difficulty = Difficulty.VIP,
            targetWord = "CAT"
        )
        assertTrue(e.removeLetter('Z'))
        assertFalse(e.onKeyPressed('Z')) // blocked
        assertFalse(e.removeLetter('C')) // in target
    }
}
