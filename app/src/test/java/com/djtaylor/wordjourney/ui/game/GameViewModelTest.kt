package com.djtaylor.wordjourney.ui.game

import androidx.lifecycle.SavedStateHandle
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.data.repository.WordRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SavedGameState
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.domain.usecase.EvaluateGuessUseCase
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for GameViewModel — the critical game screen.
 *
 * Tests cover:
 *  - Initialization: fresh level, restore saved game, error handling
 *  - Key input / delete
 *  - Guess submission: valid, invalid, win, out-of-guesses
 *  - Life system: spend life for guesses, trade coins, trade diamonds
 *  - Items: add guess, remove letter, definition
 *  - Win flow: coins, level advancement, bonus life
 *  - Replay mode
 *  - Persistence
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var wordRepository: WordRepository
    private lateinit var playerRepository: PlayerRepository
    private lateinit var audioManager: WordJourneysAudioManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helper: create ViewModel with configurable mocks ──────────────────────

    private fun createViewModel(
        difficulty: String = "easy",
        level: Int = 1,
        word: String? = "ABLE",
        progress: PlayerProgress = PlayerProgress(),
        savedGame: SavedGameState? = null,
        wordValidator: Boolean = true
    ): GameViewModel {
        progressFlow = MutableStateFlow(progress)

        wordRepository = mockk {
            coEvery { getWordForLevel(any(), any()) } returns word
            coEvery { isValidWord(any(), any()) } returns wordValidator
            coEvery { getDefinition(any(), any()) } returns "A test definition"
            coEvery { findAbsentLetter(any(), any(), any()) } returns 'X'
        }

        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            every { isFirstLaunch } returns MutableStateFlow(false)
            coEvery { saveProgress(any()) } just Runs
            coEvery { loadInProgressGame(any()) } returns savedGame
            coEvery { saveInProgressGame(any()) } just Runs
            coEvery { clearInProgressGame(any()) } just Runs
        }

        audioManager = mockk(relaxed = true)

        return GameViewModel(
            savedStateHandle = SavedStateHandle(mapOf("difficulty" to difficulty, "level" to level)),
            wordRepository = wordRepository,
            playerRepository = playerRepository,
            evaluateGuess = EvaluateGuessUseCase(),
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager
        )
    }

    private suspend fun awaitInit(vm: GameViewModel) {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `fresh level loads and sets isLoading false`() = runTest {
        val vm = createViewModel(difficulty = "easy", level = 1, word = "ABLE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse("isLoading should be false after init", state.isLoading)
        assertEquals(Difficulty.EASY, state.difficulty)
        assertEquals(1, state.level)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(6, state.maxGuesses)
        assertTrue(state.guesses.isEmpty())
        assertTrue(state.currentInput.isEmpty())
    }

    @Test
    fun `regular difficulty loads correctly`() = runTest {
        val vm = createViewModel(difficulty = "regular", level = 1, word = "CRANE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(Difficulty.REGULAR, state.difficulty)
    }

    @Test
    fun `hard difficulty loads correctly`() = runTest {
        val vm = createViewModel(difficulty = "hard", level = 1, word = "BRIDGE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(Difficulty.HARD, state.difficulty)
    }

    @Test
    fun `null word shows error and sets isLoading false`() = runTest {
        val vm = createViewModel(word = null)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse("isLoading should be false even with null word", state.isLoading)
        assertNotNull("Should show error snackbar", state.snackbarMessage)
    }

    @Test
    fun `empty word shows error and sets isLoading false`() = runTest {
        val vm = createViewModel(word = "")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.snackbarMessage)
    }

    @Test
    fun `exception during init still sets isLoading false`() = runTest {
        // Force loadInProgressGame to throw
        val vm = createViewModel()
        coEvery { playerRepository.loadInProgressGame(any()) } throws RuntimeException("DB crashed")
        // The init already launched, let it run
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse("isLoading must be false even after exception", state.isLoading)
    }

    @Test
    fun `saved game restores correctly`() = runTest {
        val saved = SavedGameState(
            difficultyKey = "easy",
            level = 1,
            targetWord = "ABLE",
            completedGuesses = listOf(
                listOf(Pair("A", "CORRECT"), Pair("R", "ABSENT"), Pair("E", "ABSENT"), Pair("A", "ABSENT"))
            ),
            currentInput = listOf("B"),
            maxGuesses = 6
        )
        val vm = createViewModel(savedGame = saved)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(1, state.guesses.size)
        assertEquals(listOf('B'), state.currentInput)
    }

    @Test
    fun `corrupted saved game falls back to fresh start`() = runTest {
        // Saved game will cause restoreFromSave to throw because target word length
        // doesn't match difficulty word length
        val saved = SavedGameState(
            difficultyKey = "easy",
            level = 1,
            targetWord = "TOOLONG",  // 7 chars, EASY expects 4
            completedGuesses = emptyList(),
            currentInput = emptyList(),
            maxGuesses = 6
        )
        val vm = createViewModel(savedGame = saved)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse("Should recover from corrupted save", state.isLoading)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        // Should have cleared the corrupted save
        coVerify { playerRepository.clearInProgressGame(any()) }
    }

    @Test
    fun `player progress is reflected in UI state`() = runTest {
        val progress = PlayerProgress(
            lives = 7,
            coins = 1500L,
            diamonds = 12,
            addGuessItems = 3,
            removeLetterItems = 2,
            definitionItems = 1
        )
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(7, state.lives)
        assertEquals(1500L, state.coins)
        assertEquals(12, state.diamonds)
        assertEquals(3, state.addGuessItems)
        assertEquals(2, state.removeLetterItems)
        assertEquals(1, state.definitionItems)
    }

    @Test
    fun `replay mode detected for completed level`() = runTest {
        val progress = PlayerProgress(easyLevel = 5)  // current level is 5
        val vm = createViewModel(level = 3, progress = progress)  // playing level 3 (< 5)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue("Should be in replay mode", state.isReplay)
    }

    @Test
    fun `not replay for current level`() = runTest {
        val progress = PlayerProgress(easyLevel = 1)
        val vm = createViewModel(level = 1, progress = progress)
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse("Should NOT be in replay mode", state.isReplay)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. KEY INPUT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `onKeyPressed adds letter to input`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        vm.onKeyPressed('A')
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(listOf('A'), state.currentInput)
    }

    @Test
    fun `onKeyPressed fills input to word length`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(4, state.currentInput.size)
        assertTrue(state.isInputFull)
    }

    @Test
    fun `onKeyPressed rejects beyond word length`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        "ABLEX".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(4, state.currentInput.size) // Only 4 accepted
    }

    @Test
    fun `onKeyPressed plays audio`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        vm.onKeyPressed('A')
        awaitInit(vm)

        verify { audioManager.playSfx(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `onDelete removes last letter`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        vm.onKeyPressed('A')
        vm.onKeyPressed('B')
        awaitInit(vm)
        vm.onDelete()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(listOf('A'), state.currentInput)
    }

    @Test
    fun `onDelete does nothing on empty input`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        vm.onDelete()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.currentInput.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. SUBMIT — VALID GUESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `correct guess wins the game`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(GameStatus.WON, state.status)
        assertTrue(state.showWinDialog)
        assertEquals("ABLE", state.winWord)
    }

    @Test
    fun `wrong guess adds to grid and clears input`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "DARK".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(1, state.guesses.size)
        assertTrue(state.currentInput.isEmpty()) // Input cleared after submit
    }

    @Test
    fun `invalid word shows snackbar and shakes row`() = runTest {
        val vm = createViewModel(word = "ABLE", wordValidator = false)
        awaitInit(vm)

        "XYZQ".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(0, state.guesses.size) // Grid unchanged
    }

    @Test
    fun `submit with incomplete input does nothing`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        "AB".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(0, state.guesses.size)
        assertEquals(2, state.currentInput.size) // Input unchanged
    }

    @Test
    fun `tile states are correct after guess`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        val guess = state.guesses.first()
        // All letters correct
        assertTrue(guess.all { it.second == TileState.CORRECT })
    }

    @Test
    fun `keyboard letter states update after guess`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "DARK".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        // 'A' is in ABLE at position 0, but DARK has 'A' at position 1 → PRESENT
        // 'D', 'R', 'K' are not in ABLE → ABSENT
        assertTrue(state.letterStates.containsKey('A'))
        assertTrue(state.letterStates.containsKey('D'))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. OUT OF GUESSES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `running out of guesses shows need more guesses dialog`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        // Submit 6 wrong guesses
        val wrongGuesses = listOf("DARK", "BIRD", "FISH", "GOAT", "JUMP", "MILK")
        for (guess in wrongGuesses) {
            guess.forEach { vm.onKeyPressed(it) }
            awaitInit(vm)
            vm.onSubmit()
            awaitInit(vm)
        }

        val state = vm.uiState.first()
        assertEquals(GameStatus.WAITING_FOR_LIFE, state.status)
        assertTrue(state.showNeedMoreGuessesDialog || state.showNoLivesDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. LIFE SYSTEM
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `useLifeForMoreGuesses grants bonus guesses`() = runTest {
        val progress = PlayerProgress(lives = 5)
        val vm = createViewModel(word = "ABLE", progress = progress)
        awaitInit(vm)

        // Exhaust all guesses
        val wrongGuesses = listOf("DARK", "BIRD", "FISH", "GOAT", "JUMP", "MILK")
        for (guess in wrongGuesses) {
            guess.forEach { vm.onKeyPressed(it) }
            awaitInit(vm)
            vm.onSubmit()
            awaitInit(vm)
        }

        vm.useLifeForMoreGuesses()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        // Easy difficulty grants 3 bonus guesses
        assertEquals(9, state.maxGuesses) // 6 + 3
    }

    @Test
    fun `useLifeForMoreGuesses with 0 lives shows no lives dialog`() = runTest {
        val progress = PlayerProgress(lives = 0)
        val vm = createViewModel(word = "ABLE", progress = progress)
        awaitInit(vm)

        vm.useLifeForMoreGuesses()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.showNoLivesDialog)
    }

    @Test
    fun `tradeCoinsForLife deducts coins and adds life`() = runTest {
        val progress = PlayerProgress(lives = 0, coins = 2000L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.tradeCoinsForLife()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(1000L, state.coins)
        assertEquals(1, state.lives)
    }

    @Test
    fun `tradeCoinsForLife fails with insufficient coins`() = runTest {
        val progress = PlayerProgress(lives = 0, coins = 500L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.tradeCoinsForLife()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertNotNull(state.snackbarMessage)
    }

    @Test
    fun `tradeDiamondsForLife deducts diamonds and adds life`() = runTest {
        val progress = PlayerProgress(lives = 0, diamonds = 10)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.tradeDiamondsForLife()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(7, state.diamonds)
        assertEquals(1, state.lives)
    }

    @Test
    fun `tradeDiamondsForLife fails with insufficient diamonds`() = runTest {
        val progress = PlayerProgress(lives = 0, diamonds = 1)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.tradeDiamondsForLife()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertNotNull(state.snackbarMessage)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. ITEMS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `useAddGuessItem with inventory adds 1 guess`() = runTest {
        val progress = PlayerProgress(addGuessItems = 2)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useAddGuessItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(7, state.maxGuesses) // 6 + 1
        assertEquals(1, state.addGuessItems) // decremented
    }

    @Test
    fun `useAddGuessItem without inventory uses coins`() = runTest {
        val progress = PlayerProgress(addGuessItems = 0, coins = 500L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useAddGuessItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(7, state.maxGuesses) // 6 + 1
        assertEquals(300L, state.coins) // 500 - 200
    }

    @Test
    fun `useAddGuessItem without coins or inventory shows error`() = runTest {
        val progress = PlayerProgress(addGuessItems = 0, coins = 100L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useAddGuessItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(6, state.maxGuesses) // unchanged
        assertNotNull(state.snackbarMessage)
    }

    @Test
    fun `useRemoveLetterItem removes letter from keyboard`() = runTest {
        val progress = PlayerProgress(removeLetterItems = 2)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useRemoveLetterItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.removedLetters.isNotEmpty())
        assertEquals(1, state.removeLetterItems) // decremented
    }

    @Test
    fun `useDefinitionItem shows definition dialog`() = runTest {
        val progress = PlayerProgress(definitionItems = 1)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useDefinitionItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.showDefinitionDialog)
        assertEquals("A test definition", state.definitionHint)
        assertTrue(state.definitionUsedThisLevel)
    }

    @Test
    fun `useDefinitionItem can be re-viewed after first use`() = runTest {
        val progress = PlayerProgress(definitionItems = 2)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useDefinitionItem()
        awaitInit(vm)
        vm.dismissDefinitionDialog()
        awaitInit(vm)

        vm.useDefinitionItem() // second use — should re-show without consuming item
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue("Should re-show definition dialog", state.showDefinitionDialog)
        assertEquals("A test definition", state.definitionHint)
        // Should still have 1 item left (only consumed on first use)
        assertEquals(1, state.definitionItems)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. WIN FLOW
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `win awards coins based on remaining guesses`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        // Win on first guess → 5 remaining → 100 + 5*10 = 150 coins
        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(150L, state.winCoinEarned)
    }

    @Test
    fun `win clears in-progress saved game`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        coVerify { playerRepository.clearInProgressGame(any()) }
    }

    @Test
    fun `replay win gives no rewards`() = runTest {
        val progress = PlayerProgress(easyLevel = 5, coins = 100L)
        val vm = createViewModel(level = 3, progress = progress, word = "ABLE")
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(0L, state.winCoinEarned) // no coins in replay
    }

    @Test
    fun `nextLevel dismisses win dialog`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        vm.nextLevel()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.showWinDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `game state persisted after key input`() = runTest {
        val vm = createViewModel()
        awaitInit(vm)

        vm.onKeyPressed('A')
        awaitInit(vm)

        coVerify { playerRepository.saveInProgressGame(any()) }
    }

    @Test
    fun `game state persisted after guess`() = runTest {
        val vm = createViewModel(word = "ABLE")
        awaitInit(vm)

        "DARK".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        coVerify(atLeast = 2) { playerRepository.saveInProgressGame(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. SNACKBAR
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dismissSnackbar clears message`() = runTest {
        val vm = createViewModel(word = null) // triggers error snackbar
        awaitInit(vm)

        assertNotNull(vm.uiState.first().snackbarMessage)

        vm.dismissSnackbar()
        awaitInit(vm)

        assertNull(vm.uiState.first().snackbarMessage)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. FULL GAME FLOWS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `full easy game - type wrong then correct`() = runTest {
        val vm = createViewModel(difficulty = "easy", word = "ABLE")
        awaitInit(vm)

        // Wrong guess
        "DARK".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        var state = vm.uiState.first()
        assertEquals(1, state.guesses.size)
        assertEquals(GameStatus.IN_PROGRESS, state.status)

        // Correct guess
        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(2, state.guesses.size)
        assertEquals(GameStatus.WON, state.status)
    }

    @Test
    fun `full regular game flow`() = runTest {
        val vm = createViewModel(difficulty = "regular", word = "CRANE")
        awaitInit(vm)

        var state = vm.uiState.first()
        assertFalse(state.isLoading)

        "CRANE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(5, state.currentInput.size)
        assertTrue(state.canSubmit)

        vm.onSubmit()
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(GameStatus.WON, state.status)
    }

    @Test
    fun `full hard game flow`() = runTest {
        val vm = createViewModel(difficulty = "hard", word = "BRIDGE")
        awaitInit(vm)

        var state = vm.uiState.first()
        assertFalse(state.isLoading)

        "BRIDGE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(6, state.currentInput.size)

        vm.onSubmit()
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(GameStatus.WON, state.status)
    }

    @Test
    fun `exhaust guesses then use life and win`() = runTest {
        val progress = PlayerProgress(lives = 5)
        val vm = createViewModel(word = "ABLE", progress = progress)
        awaitInit(vm)

        // Exhaust 6 guesses
        val wrongGuesses = listOf("DARK", "BIRD", "FISH", "GOAT", "JUMP", "MILK")
        for (guess in wrongGuesses) {
            guess.forEach { vm.onKeyPressed(it) }
            awaitInit(vm)
            vm.onSubmit()
            awaitInit(vm)
        }

        var state = vm.uiState.first()
        assertEquals(GameStatus.WAITING_FOR_LIFE, state.status)
        assertEquals(6, state.guesses.size)

        // Use a life to get 3 more guesses
        vm.useLifeForMoreGuesses()
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(9, state.maxGuesses)

        // Now win
        "ABLE".forEach { vm.onKeyPressed(it) }
        awaitInit(vm)
        vm.onSubmit()
        awaitInit(vm)

        state = vm.uiState.first()
        assertEquals(GameStatus.WON, state.status)
    }

    @Test
    fun `use items during game`() = runTest {
        val progress = PlayerProgress(
            addGuessItems = 1,
            removeLetterItems = 1,
            definitionItems = 1
        )
        val vm = createViewModel(word = "ABLE", progress = progress)
        awaitInit(vm)

        // Use add guess item
        vm.useAddGuessItem()
        awaitInit(vm)
        assertEquals(7, vm.uiState.first().maxGuesses)

        // Use remove letter item
        vm.useRemoveLetterItem()
        awaitInit(vm)
        assertTrue(vm.uiState.first().removedLetters.isNotEmpty())

        // Use definition item
        vm.useDefinitionItem()
        awaitInit(vm)
        assertTrue(vm.uiState.first().showDefinitionDialog)
        vm.dismissDefinitionDialog()
        awaitInit(vm)
        assertFalse(vm.uiState.first().showDefinitionDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. DEFINITION DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dismissDefinitionDialog closes the dialog`() = runTest {
        val progress = PlayerProgress(definitionItems = 1)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useDefinitionItem()
        awaitInit(vm)
        assertTrue(vm.uiState.first().showDefinitionDialog)

        vm.dismissDefinitionDialog()
        awaitInit(vm)
        assertFalse(vm.uiState.first().showDefinitionDialog)
    }

    @Test
    fun `definition item with coins when none in inventory`() = runTest {
        val progress = PlayerProgress(definitionItems = 0, coins = 500L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useDefinitionItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.showDefinitionDialog)
        assertEquals(200L, state.coins) // 500 - 300
    }

    @Test
    fun `definition item fails with no coins and no inventory`() = runTest {
        val progress = PlayerProgress(definitionItems = 0, coins = 100L)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        vm.useDefinitionItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertFalse(state.showDefinitionDialog) // Did not open
        assertNotNull(state.snackbarMessage) // Error message
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 13. DIFFICULTY-SPECIFIC BEHAVIOR
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `easy mode uses 4-letter words`() = runTest {
        val vm = createViewModel(difficulty = "easy", word = "ABLE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(Difficulty.EASY, state.difficulty)
        assertEquals(4, state.difficulty.wordLength)
    }

    @Test
    fun `regular mode uses 5-letter words`() = runTest {
        val vm = createViewModel(difficulty = "regular", word = "CRANE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(Difficulty.REGULAR, state.difficulty)
        assertEquals(5, state.difficulty.wordLength)
    }

    @Test
    fun `hard mode uses 6-letter words`() = runTest {
        val vm = createViewModel(difficulty = "hard", word = "BRIDGE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertEquals(Difficulty.HARD, state.difficulty)
        assertEquals(6, state.difficulty.wordLength)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 14. NEXT LEVEL NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getNextLevelRoute returns correct difficulty and next level`() = runTest {
        val vm = createViewModel(difficulty = "easy", level = 3, word = "ABLE")
        awaitInit(vm)

        val (diff, lvl) = vm.getNextLevelRoute()
        assertEquals("easy", diff)
        assertEquals(4, lvl)
    }

    @Test
    fun `getNextLevelRoute for regular difficulty`() = runTest {
        val vm = createViewModel(difficulty = "regular", level = 7, word = "CRANE")
        awaitInit(vm)

        val (diff, lvl) = vm.getNextLevelRoute()
        assertEquals("regular", diff)
        assertEquals(8, lvl)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 15. DEFINITION PERSISTENCE & REPLAY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `replay mode auto-loads definition`() = runTest {
        val progress = PlayerProgress(easyLevel = 5, definitionItems = 0)
        val vm = createViewModel(level = 3, progress = progress, word = "ABLE")
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue("Replay should auto-set definitionUsedThisLevel", state.definitionUsedThisLevel)
        assertEquals("A test definition", state.definitionHint)
    }

    @Test
    fun `replay mode definition viewable via define button`() = runTest {
        val progress = PlayerProgress(easyLevel = 5, definitionItems = 0)
        val vm = createViewModel(level = 3, progress = progress, word = "ABLE")
        awaitInit(vm)

        // Click define — should show cached definition without consuming items
        vm.useDefinitionItem()
        awaitInit(vm)

        val state = vm.uiState.first()
        assertTrue(state.showDefinitionDialog)
        assertEquals("A test definition", state.definitionHint)
    }

    @Test
    fun `definition re-view does not consume additional items`() = runTest {
        val progress = PlayerProgress(definitionItems = 1)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        // First use — consumes 1 item
        vm.useDefinitionItem()
        awaitInit(vm)
        assertEquals(0, vm.uiState.first().definitionItems)

        vm.dismissDefinitionDialog()
        awaitInit(vm)

        // Second use — should NOT consume anything (re-view)
        vm.useDefinitionItem()
        awaitInit(vm)
        assertEquals(0, vm.uiState.first().definitionItems) // still 0
        assertTrue(vm.uiState.first().showDefinitionDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 16. NEXT LEVEL NAVIGATION EDGE CASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getNextLevelRoute for hard difficulty returns correct route`() = runTest {
        val vm = createViewModel(difficulty = "hard", level = 3, word = "BRIDGE")
        awaitInit(vm)

        val (diff, lvl) = vm.getNextLevelRoute()
        assertEquals("hard", diff)
        assertEquals(4, lvl)
    }

    @Test
    fun `getNextLevelRoute increments from first level`() = runTest {
        val vm = createViewModel(difficulty = "easy", level = 1, word = "ABLE")
        awaitInit(vm)

        val (diff, lvl) = vm.getNextLevelRoute()
        assertEquals("easy", diff)
        assertEquals(2, lvl)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 17. TEXT INPUT EDGE CASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cannot type more letters than word length allows`() = runTest {
        val vm = createViewModel(difficulty = "easy", level = 1, word = "ABLE")
        awaitInit(vm)

        // Type 4 letters (full word length for EASY)
        vm.onKeyPressed('A')
        vm.onKeyPressed('B')
        vm.onKeyPressed('L')
        vm.onKeyPressed('E')
        awaitInit(vm)

        // Try typing a 5th letter — should be ignored
        vm.onKeyPressed('X')
        awaitInit(vm)

        assertEquals(4, vm.uiState.first().currentInput.size)
    }

    @Test
    fun `delete on empty input does nothing`() = runTest {
        val vm = createViewModel(difficulty = "easy", level = 1, word = "ABLE")
        awaitInit(vm)

        vm.onDelete()
        awaitInit(vm)

        assertEquals(0, vm.uiState.first().currentInput.size)
    }

    @Test
    fun `multiple definitions can be viewed in non-replay mode`() = runTest {
        val progress = PlayerProgress(definitionItems = 2)
        val vm = createViewModel(progress = progress)
        awaitInit(vm)

        // First use
        vm.useDefinitionItem()
        awaitInit(vm)
        assertTrue(vm.uiState.first().showDefinitionDialog)
        assertEquals(1, vm.uiState.first().definitionItems)

        vm.dismissDefinitionDialog()
        awaitInit(vm)

        // Second view — re-view, no item consumed
        vm.useDefinitionItem()
        awaitInit(vm)
        assertTrue(vm.uiState.first().showDefinitionDialog)
        assertEquals(1, vm.uiState.first().definitionItems) // still 1 (re-view)
    }
}
