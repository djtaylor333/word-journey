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
import com.djtaylor.wordjourney.engine.GameEngine
import com.djtaylor.wordjourney.engine.SubmitResult
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

    /** Pure game engine — all game logic is tested via GameEngineTest */
    private var engine: GameEngine? = null
    private var playerProgress: PlayerProgress = PlayerProgress()
    private var isReplay: Boolean = false

    companion object {
        private const val TAG = "GameViewModel"
    }

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
        try {
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
                try {
                    restoreFromSave(saved)
                } catch (restoreEx: Exception) {
                    android.util.Log.e(TAG, "Failed to restore saved game, starting fresh", restoreEx)
                    playerRepository.clearInProgressGame(difficulty)
                    startFreshLevel(levelArg)
                }
            } else {
                startFreshLevel(levelArg)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize game", e)
            _uiState.update { s ->
                s.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading game: ${e.message}"
                )
            }
        }
    }

    private suspend fun startFreshLevel(level: Int) {
        val word = wordRepository.getWordForLevel(difficulty, level)
        if (word.isNullOrEmpty()) {
            _uiState.update { s ->
                s.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading level. Please restart the app."
                )
            }
            return
        }

        engine = GameEngine(
            difficulty = difficulty,
            targetWord = word,
            evaluateGuess = evaluateGuess,
            wordValidator = { guess, len -> wordRepository.isValidWord(guess, len) }
        )
        _targetWordCache = word

        // For replay mode, auto-load definition so player can always view it
        var defHint: String? = null
        var defUsed = false
        if (isReplay) {
            val definition = wordRepository.getDefinition(difficulty, level)
            if (definition.isNotBlank()) {
                defHint = definition
                defUsed = true
            }
        }

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
                definitionHint = defHint,
                showDefinitionDialog = false,
                definitionUsedThisLevel = defUsed
            )
        }
        if (!isReplay) persistCurrentState()
    }

    private fun restoreFromSave(saved: SavedGameState) {
        val restoredGuesses = saved.completedGuesses.map { row ->
            row.map { (ch, state) ->
                Pair(ch.first(), TileState.valueOf(state))
            }
        }
        val restoredInput = saved.currentInput.map { it.first() }

        engine = GameEngine(
            difficulty = difficulty,
            targetWord = saved.targetWord,
            evaluateGuess = evaluateGuess,
            wordValidator = { guess, len -> wordRepository.isValidWord(guess, len) }
        )
        _targetWordCache = saved.targetWord
        engine!!.restore(restoredGuesses, restoredInput, saved.maxGuesses)

        syncEngineToUiState()
        _uiState.update { s ->
            s.copy(
                level = saved.level,
                lives = playerProgress.lives,
                coins = playerProgress.coins,
                diamonds = playerProgress.diamonds,
                addGuessItems = playerProgress.addGuessItems,
                removeLetterItems = playerProgress.removeLetterItems,
                definitionItems = playerProgress.definitionItems,
                isLoading = false
            )
        }
    }

    // ── Input handling — delegates to GameEngine ──────────────────────────────
    fun onKeyPressed(char: Char) {
        val e = engine ?: return
        if (e.onKeyPressed(char)) {
            audioManager.playSfx(SfxSound.KEY_TAP)
            syncEngineToUiState()
            persistCurrentState()
        }
    }

    fun onDelete() {
        val e = engine ?: return
        if (e.onDelete()) {
            syncEngineToUiState()
            persistCurrentState()
        }
    }

    fun onSubmit() {
        val e = engine ?: return
        if (!e.canSubmit) return

        viewModelScope.launch {
            when (val result = e.onSubmit()) {
                is SubmitResult.InvalidWord -> {
                    audioManager.playSfx(SfxSound.INVALID_WORD)
                    _uiState.update { it.copy(
                        shakeCurrentRow = true,
                        snackbarMessage = "Not a valid word"
                    )}
                    delay(600)
                    _uiState.update { it.copy(
                        shakeCurrentRow = false,
                        snackbarMessage = null
                    )}
                }

                is SubmitResult.Evaluated -> {
                    audioManager.playSfx(SfxSound.TILE_FLIP)
                    syncEngineToUiState()

                    if (result.isWin) {
                        handleWin()
                    } else if (result.isOutOfGuesses) {
                        handleOutOfGuesses()
                    } else {
                        persistCurrentState()
                    }
                }

                is SubmitResult.NotReady -> { /* shouldn't reach here */ }
            }
        }
    }

    private suspend fun handleWin() {
        val e = engine ?: return
        val level = _uiState.value.level
        val definition = wordRepository.getDefinition(difficulty, level)
        val targetWord = e.guesses.last()
            .joinToString("") { it.first.toString() } // reconstruct from last guess (all CORRECT)

        if (isReplay) {
            audioManager.playSfx(SfxSound.WIN)
            _uiState.update { s ->
                s.copy(
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
            val remaining = e.remainingGuesses
            val coinsEarned = 100L + (remaining * 10L)
            val (updatedProgress, bonusLife) = applyLevelCompletion(coinsEarned)

            audioManager.playSfx(SfxSound.WIN)
            audioManager.playSfx(SfxSound.COIN_EARN)

            _uiState.update { s ->
                s.copy(
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

        val e = engine ?: return
        e.addBonusGuesses(difficulty.bonusAttemptsPerLife)
        syncEngineToUiState()

        _uiState.update { s ->
            s.copy(
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
        val e = engine ?: return
        val progress = playerProgress
        if (progress.addGuessItems > 0) {
            audioManager.playSfx(SfxSound.BUTTON_CLICK)
            val updated = progress.copy(addGuessItems = progress.addGuessItems - 1)
            playerProgress = updated
            viewModelScope.launch { playerRepository.saveProgress(updated) }
            e.addBonusGuesses(1)
            syncEngineToUiState()
            _uiState.update { s ->
                s.copy(
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
            e.addBonusGuesses(1)
            syncEngineToUiState()
            _uiState.update { s ->
                s.copy(
                    showNeedMoreGuessesDialog = false,
                    coins = updated.coins
                )
            }
        }
        persistCurrentState()
    }

    fun useRemoveLetterItem() {
        val e = engine ?: return
        val progress = playerProgress
        viewModelScope.launch {
            val absent = wordRepository.findAbsentLetter(
                getTargetWord(),
                e.removedLetters,
                e.letterStates.keys
            ) ?: run {
                _uiState.update { it.copy(snackbarMessage = "No more letters to remove!") }
                return@launch
            }

            if (progress.removeLetterItems > 0) {
                audioManager.playSfx(SfxSound.BUTTON_CLICK)
                val updated = progress.copy(removeLetterItems = progress.removeLetterItems - 1)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                e.removeLetter(absent)
                syncEngineToUiState()
                _uiState.update { s ->
                    s.copy(removeLetterItems = updated.removeLetterItems)
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
                e.removeLetter(absent)
                syncEngineToUiState()
                _uiState.update { s ->
                    s.copy(coins = updated.coins)
                }
            }
            persistCurrentState()
        }
    }

    // ── Win → Next Level ──────────────────────────────────────────────────────
    fun nextLevel() {
        _uiState.update { it.copy(showWinDialog = false) }
    }

    /** Returns (difficultyKey, nextLevel) for navigation. */
    fun getNextLevelRoute(): Pair<String, Int> {
        return Pair(difficultyKey, _uiState.value.level + 1)
    }

    // ── Definition item ───────────────────────────────────────────────────────
    fun useDefinitionItem() {
        // If definition already used/available this level, just re-show it
        if (_uiState.value.definitionUsedThisLevel && _uiState.value.definitionHint != null) {
            _uiState.update { it.copy(showDefinitionDialog = true) }
            return
        }
        val progress = playerProgress
        viewModelScope.launch {
            val definition = wordRepository.getDefinition(difficulty, _uiState.value.level)
            val hint = definition.ifBlank { "No definition available" }

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

    /** Sync engine state → UI state. Called after every engine mutation. */
    private fun syncEngineToUiState() {
        val e = engine ?: return
        _uiState.update { s ->
            s.copy(
                guesses = e.guesses,
                currentInput = e.currentInput,
                maxGuesses = e.maxGuesses,
                letterStates = e.letterStates,
                removedLetters = e.removedLetters,
                status = e.status
            )
        }
    }

    /** Get the target word (cached at level start, needed for findAbsentLetter). */
    private fun getTargetWord(): String = _targetWordCache
    private var _targetWordCache: String = ""

    private suspend fun applyLevelCompletion(coinsEarned: Long): Pair<PlayerProgress, Boolean> {
        var p = playerProgress
        val newLevel = _uiState.value.level + 1

        p = when (difficulty) {
            Difficulty.EASY    -> p.copy(easyLevel = newLevel)
            Difficulty.REGULAR -> p.copy(regularLevel = newLevel)
            Difficulty.HARD    -> p.copy(hardLevel = newLevel)
        }

        p = p.copy(coins = p.coins + coinsEarned)

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

    private fun persistCurrentState() {
        val e = engine ?: return
        val s = _uiState.value
        if (s.isLoading || _targetWordCache.isEmpty()) return
        viewModelScope.launch {
            val saved = SavedGameState(
                difficultyKey = difficultyKey,
                level = s.level,
                targetWord = _targetWordCache,
                completedGuesses = e.guesses.map { row ->
                    row.map { (ch, state) -> Pair(ch.toString(), state.name) }
                },
                currentInput = e.currentInput.map { it.toString() },
                maxGuesses = e.maxGuesses
            )
            playerRepository.saveInProgressGame(saved)
        }
    }

    private fun PlayerProgress.levelFor(d: Difficulty) = when (d) {
        Difficulty.EASY    -> easyLevel
        Difficulty.REGULAR -> regularLevel
        Difficulty.HARD    -> hardLevel
    }
}
