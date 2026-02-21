package com.djtaylor.wordjourney.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val playerRepository: PlayerRepository,
    private val evaluateGuess: EvaluateGuessUseCase,
    private val lifeRegenUseCase: LifeRegenUseCase,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val difficultyKey: String = checkNotNull(savedStateHandle["difficulty"])
    private val difficulty: Difficulty = Difficulty.entries.first { it.saveKey == difficultyKey }
    private val levelArg: Int = savedStateHandle["level"] ?: 1

    // The secret target word — NEVER included in GameUiState until WON
    private var targetWord: String = ""
    private var playerProgress: PlayerProgress = PlayerProgress()
    private var isReplay: Boolean = false

    private val _uiState = MutableStateFlow(
        GameUiState(difficulty = difficulty, isLoading = true)
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initGame()
        }
        // Observe player progress reactively so heart/coin counts stay fresh
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                playerProgress = progress
                _uiState.update { s ->
                    s.copy(
                        lives = progress.lives,
                        coins = progress.coins,
                        diamonds = progress.diamonds,
                        addGuessItems = progress.addGuessItems,
                        removeLetterItems = progress.removeLetterItems,
                        definitionItems = progress.definitionItems
                    )
                }
            }
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────────
    private suspend fun initGame() {
        // Sync life regen
        playerRepository.playerProgressFlow.first().let { progress ->
            val regen = lifeRegenUseCase(progress.lives, progress.lastLifeRegenTimestamp)
            playerProgress = if (regen.livesAdded > 0) {
                val p = progress.copy(
                    lives = regen.updatedLives,
                    lastLifeRegenTimestamp = regen.updatedTimestamp
                )
                playerRepository.saveProgress(p)
                p
            } else progress
        }

        // Determine if this is a replay of a completed level
        val currentLevel = playerProgress.levelFor(difficulty)
        isReplay = levelArg < currentLevel

        // Try to restore in-progress game (only for current level, not replays)
        val saved = if (!isReplay) playerRepository.loadInProgressGame(difficulty) else null
        if (saved != null && saved.level == levelArg) {
            restoreFromSave(saved)
        } else {
            startFreshLevel(levelArg)
        }
    }

    private suspend fun startFreshLevel(level: Int) {
        val word = wordRepository.getWordForLevel(difficulty, level) ?: ""
        targetWord = word
        _uiState.update { s ->
            s.copy(
                level = level,
                guesses = emptyList(),
                currentInput = emptyList(),
                maxGuesses = difficulty.maxGuesses,
                letterStates = emptyMap(),
                removedLetters = emptySet(),
                status = GameStatus.IN_PROGRESS,
                showWinDialog = false,
                showNeedMoreGuessesDialog = false,
                showNoLivesDialog = false,
                lives = playerProgress.lives,
                coins = playerProgress.coins,
                diamonds = playerProgress.diamonds,
                addGuessItems = playerProgress.addGuessItems,
                removeLetterItems = playerProgress.removeLetterItems,
                definitionItems = playerProgress.definitionItems,
                isLoading = false,
                isReplay = isReplay,
                definitionHint = null,
                showDefinitionDialog = false,
                definitionUsedThisLevel = false
            )
        }
        if (!isReplay) persistCurrentState()
    }

    private fun restoreFromSave(saved: SavedGameState) {
        targetWord = saved.targetWord
        val restoredGuesses = saved.completedGuesses.map { row ->
            row.map { (ch, state) ->
                Pair(ch.first(), TileState.valueOf(state))
            }
        }
        val restoredInput = saved.currentInput.map { it.first() }
        val letterMap = buildLetterMap(restoredGuesses)

        _uiState.update { s ->
            s.copy(
                level = saved.level,
                guesses = restoredGuesses,
                currentInput = restoredInput,
                maxGuesses = saved.maxGuesses,
                letterStates = letterMap,
                lives = playerProgress.lives,
                coins = playerProgress.coins,
                diamonds = playerProgress.diamonds,
                addGuessItems = playerProgress.addGuessItems,
                removeLetterItems = playerProgress.removeLetterItems,
                definitionItems = playerProgress.definitionItems,
                status = GameStatus.IN_PROGRESS,
                isLoading = false
            )
        }
    }

    // ── Input handling ────────────────────────────────────────────────────────
    fun onKeyPressed(char: Char) {
        val state = _uiState.value
        if (state.status != GameStatus.IN_PROGRESS) return
        if (state.currentInput.size >= difficulty.wordLength) return
        if (char in state.removedLetters) return  // removed-letter item active
        audioManager.playSfx(SfxSound.KEY_TAP)
        _uiState.update { it.copy(currentInput = it.currentInput + char) }
        persistCurrentState()
    }

    fun onDelete() {
        val state = _uiState.value
        if (state.currentInput.isEmpty()) return
        _uiState.update { it.copy(currentInput = it.currentInput.dropLast(1)) }
        persistCurrentState()
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val guess = state.currentInput.joinToString("").uppercase()

        viewModelScope.launch {
            // Validate word
            if (!wordRepository.isValidWord(guess, difficulty.wordLength)) {
                audioManager.playSfx(SfxSound.INVALID_WORD)
                _uiState.update { it.copy(shakeCurrentRow = true, snackbarMessage = "Not a valid word") }
                delay(600)
                _uiState.update { it.copy(shakeCurrentRow = false, snackbarMessage = null) }
                return@launch
            }

            // Evaluate
            val evaluated = evaluateGuess(guess, targetWord)
            val newGuesses = state.guesses + listOf(evaluated)
            val newLetterStates = buildLetterMap(newGuesses)

            audioManager.playSfx(SfxSound.TILE_FLIP)

            val won = evaluated.all { it.second == TileState.CORRECT }
            if (won) {
                val definition = wordRepository.getDefinition(difficulty, state.level)

                if (isReplay) {
                    // Replay — reveal word but no rewards
                    audioManager.playSfx(SfxSound.WIN)
                    _uiState.update { s ->
                        s.copy(
                            guesses = newGuesses,
                            currentInput = emptyList(),
                            letterStates = newLetterStates,
                            status = GameStatus.WON,
                            showWinDialog = true,
                            winCoinEarned = 0L,
                            winDefinition = definition,
                            winWord = targetWord,
                            bonusLifeEarned = false,
                            isReplay = true
                        )
                    }
                } else {
                    // Normal — coin reward: 100 base + 10 per remaining guess
                    val remaining = state.maxGuesses - newGuesses.size
                    val coinsEarned = 100L + (remaining * 10L)
                    val (updatedProgress, bonusLife) = applyLevelCompletion(coinsEarned)

                    audioManager.playSfx(SfxSound.WIN)
                    audioManager.playSfx(SfxSound.COIN_EARN)

                    _uiState.update { s ->
                        s.copy(
                            guesses = newGuesses,
                            currentInput = emptyList(),
                            letterStates = newLetterStates,
                            status = GameStatus.WON,
                            showWinDialog = true,
                            winCoinEarned = coinsEarned,
                            winDefinition = definition,
                            winWord = targetWord,
                            bonusLifeEarned = bonusLife,
                            lives = updatedProgress.lives,
                            coins = updatedProgress.coins,
                            diamonds = updatedProgress.diamonds
                        )
                    }
                    playerRepository.clearInProgressGame(difficulty)
                }
            } else {
                _uiState.update { s ->
                    s.copy(
                        guesses = newGuesses,
                        currentInput = emptyList(),
                        letterStates = newLetterStates
                    )
                }

                // Check if out of guesses
                if (newGuesses.size >= state.maxGuesses) {
                    handleOutOfGuesses()
                } else {
                    persistCurrentState()
                }
            }
        }
    }

    private fun handleOutOfGuesses() {
        audioManager.playSfx(SfxSound.LEVEL_FAIL)
        val progress = playerProgress
        if (progress.lives > 0) {
            _uiState.update { it.copy(
                status = GameStatus.WAITING_FOR_LIFE,
                showNeedMoreGuessesDialog = true
            )}
        } else {
            audioManager.playSfx(SfxSound.NO_LIVES)
            _uiState.update { it.copy(
                status = GameStatus.WAITING_FOR_LIFE,
                showNoLivesDialog = true
            )}
        }
    }

    // ── Life interactions ─────────────────────────────────────────────────────
    fun useLifeForMoreGuesses() {
        val progress = playerProgress
        if (progress.lives <= 0) {
            _uiState.update { it.copy(
                showNeedMoreGuessesDialog = false,
                showNoLivesDialog = true
            )}
            return
        }
        audioManager.playSfx(SfxSound.LIFE_LOST)
        val updated = progress.copy(lives = progress.lives - 1)
        playerProgress = updated
        viewModelScope.launch { playerRepository.saveProgress(updated) }

        _uiState.update { s ->
            s.copy(
                maxGuesses = s.maxGuesses + difficulty.bonusAttemptsPerLife,
                status = GameStatus.IN_PROGRESS,
                showNeedMoreGuessesDialog = false,
                showNoLivesDialog = false,
                lives = updated.lives
            )
        }
        persistCurrentState()
    }

    fun tradeCoinsForLife() {
        val progress = playerProgress
        val cost = 1000L
        if (progress.coins < cost) {
            _uiState.update { it.copy(snackbarMessage = "Need 1000 coins") }
            return
        }
        audioManager.playSfx(SfxSound.LIFE_GAINED)
        val updated = progress.copy(
            coins = progress.coins - cost,
            lives = progress.lives + 1
        )
        playerProgress = updated
        viewModelScope.launch { playerRepository.saveProgress(updated) }
        _uiState.update { s ->
            s.copy(lives = updated.lives, coins = updated.coins, showNoLivesDialog = false)
        }
        // If we now have a life and were waiting, show the "use for guesses" dialog
        _uiState.update { it.copy(showNeedMoreGuessesDialog = true) }
    }

    fun tradeDiamondsForLife(cost: Int = 3) {
        val progress = playerProgress
        if (progress.diamonds < cost) {
            _uiState.update { it.copy(snackbarMessage = "Need $cost diamonds") }
            return
        }
        audioManager.playSfx(SfxSound.LIFE_GAINED)
        val updated = progress.copy(
            diamonds = progress.diamonds - cost,
            lives = progress.lives + 1
        )
        playerProgress = updated
        viewModelScope.launch { playerRepository.saveProgress(updated) }
        _uiState.update { s ->
            s.copy(lives = updated.lives, diamonds = updated.diamonds, showNoLivesDialog = false)
        }
        _uiState.update { it.copy(showNeedMoreGuessesDialog = true) }
    }

    // ── Items ─────────────────────────────────────────────────────────────────
    fun useAddGuessItem() {
        val progress = playerProgress
        // Use from inventory first, else buy with coins
        if (progress.addGuessItems > 0) {
            audioManager.playSfx(SfxSound.BUTTON_CLICK)
            val updated = progress.copy(addGuessItems = progress.addGuessItems - 1)
            playerProgress = updated
            viewModelScope.launch { playerRepository.saveProgress(updated) }
            _uiState.update { s ->
                s.copy(
                    maxGuesses = s.maxGuesses + 1,
                    status = GameStatus.IN_PROGRESS,
                    showNeedMoreGuessesDialog = false,
                    addGuessItems = updated.addGuessItems
                )
            }
        } else {
            val coinCost = 200L
            if (progress.coins < coinCost) {
                _uiState.update { it.copy(snackbarMessage = "Need 200 coins or buy from Store") }
                return
            }
            audioManager.playSfx(SfxSound.COIN_EARN)
            val updated = progress.copy(coins = progress.coins - coinCost)
            playerProgress = updated
            viewModelScope.launch { playerRepository.saveProgress(updated) }
            _uiState.update { s ->
                s.copy(
                    maxGuesses = s.maxGuesses + 1,
                    status = GameStatus.IN_PROGRESS,
                    showNeedMoreGuessesDialog = false,
                    coins = updated.coins
                )
            }
        }
        persistCurrentState()
    }

    fun useRemoveLetterItem() {
        val progress = playerProgress
        viewModelScope.launch {
            val letter = wordRepository.findAbsentLetter(
                targetWord,
                _uiState.value.removedLetters,
                _uiState.value.letterStates.keys
            ) ?: run {
                _uiState.update { it.copy(snackbarMessage = "No more letters to remove!") }
                return@launch
            }

            // Use from inventory first, else buy with coins
            if (progress.removeLetterItems > 0) {
                audioManager.playSfx(SfxSound.BUTTON_CLICK)
                val updated = progress.copy(removeLetterItems = progress.removeLetterItems - 1)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                _uiState.update { s ->
                    s.copy(
                        removedLetters = s.removedLetters + letter,
                        letterStates = s.letterStates + (letter to TileState.ABSENT),
                        removeLetterItems = updated.removeLetterItems
                    )
                }
            } else {
                val coinCost = 150L
                if (progress.coins < coinCost) {
                    _uiState.update { it.copy(snackbarMessage = "Need 150 coins or buy from Store") }
                    return@launch
                }
                audioManager.playSfx(SfxSound.COIN_EARN)
                val updated = progress.copy(coins = progress.coins - coinCost)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                _uiState.update { s ->
                    s.copy(
                        removedLetters = s.removedLetters + letter,
                        letterStates = s.letterStates + (letter to TileState.ABSENT),
                        coins = updated.coins
                    )
                }
            }
            persistCurrentState()
        }
    }

    // ── Win → Next Level — dismiss dialog; the LevelSelect screen handles navigation
    fun nextLevel() {
        _uiState.update { it.copy(showWinDialog = false) }
        // The calling screen should pop back to level-select or home
    }

    // ── Definition item ───────────────────────────────────────────────────────
    fun useDefinitionItem() {
        if (_uiState.value.definitionUsedThisLevel) {
            _uiState.update { it.copy(snackbarMessage = "Definition already used this level") }
            return
        }
        val progress = playerProgress
        viewModelScope.launch {
            val definition = wordRepository.getDefinition(difficulty, _uiState.value.level)
            val hint = definition.ifBlank { "No definition available" }

            // Use from inventory first, else buy with coins
            if (progress.definitionItems > 0) {
                audioManager.playSfx(SfxSound.BUTTON_CLICK)
                val updated = progress.copy(definitionItems = progress.definitionItems - 1)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                _uiState.update { s ->
                    s.copy(
                        definitionHint = hint,
                        showDefinitionDialog = true,
                        definitionUsedThisLevel = true,
                        definitionItems = updated.definitionItems
                    )
                }
            } else {
                val coinCost = 300L
                if (progress.coins < coinCost) {
                    _uiState.update { it.copy(snackbarMessage = "Need 300 coins or buy from Store") }
                    return@launch
                }
                audioManager.playSfx(SfxSound.COIN_EARN)
                val updated = progress.copy(coins = progress.coins - coinCost)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                _uiState.update { s ->
                    s.copy(
                        coins = updated.coins,
                        definitionHint = hint,
                        showDefinitionDialog = true,
                        definitionUsedThisLevel = true
                    )
                }
            }
        }
    }

    fun dismissDefinitionDialog() {
        _uiState.update { it.copy(showDefinitionDialog = false) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private suspend fun applyLevelCompletion(coinsEarned: Long): Pair<PlayerProgress, Boolean> {
        var p = playerProgress
        val newLevel = _uiState.value.level + 1

        // Advance level
        p = when (difficulty) {
            Difficulty.EASY    -> p.copy(easyLevel = newLevel)
            Difficulty.REGULAR -> p.copy(regularLevel = newLevel)
            Difficulty.HARD    -> p.copy(hardLevel = newLevel)
        }

        // Add coins
        p = p.copy(coins = p.coins + coinsEarned)

        // Check level-completion bonus life
        val counter = when (difficulty) {
            Difficulty.EASY    -> p.easyLevelsCompletedSinceBonusLife
            Difficulty.REGULAR -> p.regularLevelsCompletedSinceBonusLife
            Difficulty.HARD    -> p.hardLevelsCompletedSinceBonusLife
        }
        val newCounter = counter + 1
        val bonusLife = newCounter >= difficulty.levelBonusThreshold

        if (bonusLife) {
            p = p.copy(lives = p.lives + 1)
            p = when (difficulty) {
                Difficulty.EASY    -> p.copy(easyLevelsCompletedSinceBonusLife = 0)
                Difficulty.REGULAR -> p.copy(regularLevelsCompletedSinceBonusLife = 0)
                Difficulty.HARD    -> p.copy(hardLevelsCompletedSinceBonusLife = 0)
            }
        } else {
            p = when (difficulty) {
                Difficulty.EASY    -> p.copy(easyLevelsCompletedSinceBonusLife = newCounter)
                Difficulty.REGULAR -> p.copy(regularLevelsCompletedSinceBonusLife = newCounter)
                Difficulty.HARD    -> p.copy(hardLevelsCompletedSinceBonusLife = newCounter)
            }
        }

        playerProgress = p
        playerRepository.saveProgress(p)
        return Pair(p, bonusLife)
    }

    private fun buildLetterMap(
        guesses: List<List<Pair<Char, TileState>>>
    ): Map<Char, TileState> {
        val map = mutableMapOf<Char, TileState>()
        for (row in guesses) {
            for ((ch, state) in row) {
                val current = map[ch]
                val priority = state.priority
                if (current == null || priority > current.priority) map[ch] = state
            }
        }
        return map
    }

    private fun persistCurrentState() {
        val s = _uiState.value
        if (s.isLoading || targetWord.isEmpty()) return
        viewModelScope.launch {
            val saved = SavedGameState(
                difficultyKey = difficultyKey,
                level = s.level,
                targetWord = targetWord,
                completedGuesses = s.guesses.map { row ->
                    row.map { (ch, state) -> Pair(ch.toString(), state.name) }
                },
                currentInput = s.currentInput.map { it.toString() },
                maxGuesses = s.maxGuesses
            )
            playerRepository.saveInProgressGame(saved)
        }
    }

    private val TileState.priority: Int
        get() = when (this) {
            TileState.CORRECT  -> 3
            TileState.PRESENT  -> 2
            TileState.ABSENT   -> 1
            else               -> 0
        }

    private fun PlayerProgress.levelFor(d: Difficulty) = when (d) {
        Difficulty.EASY    -> easyLevel
        Difficulty.REGULAR -> regularLevel
        Difficulty.HARD    -> hardLevel
    }
}
