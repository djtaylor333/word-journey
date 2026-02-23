package com.djtaylor.wordjourney.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.db.StarRatingEntity
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
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
    private val audioManager: WordJourneysAudioManager,
    private val starRatingDao: StarRatingDao,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val difficultyKey: String = checkNotNull(savedStateHandle["difficulty"])
    private val isDailyChallenge: Boolean = difficultyKey.startsWith("daily")
    private val difficulty: Difficulty = if (isDailyChallenge) {
        when (difficultyKey.substringAfter("_").toIntOrNull()) {
            4 -> Difficulty.EASY
            6 -> Difficulty.HARD
            else -> Difficulty.REGULAR
        }
    } else {
        Difficulty.entries.first { it.saveKey == difficultyKey }
    }
    private val levelArg: Int = savedStateHandle["level"] ?: 1

    /** Pure game engine — all game logic is tested via GameEngineTest */
    private var engine: GameEngine? = null
    private var playerProgress: PlayerProgress = PlayerProgress()
    private var isReplay: Boolean = false

    companion object {
        private const val TAG = "GameViewModel"

        /**
         * Returns true if a saved daily-challenge game is from a different day
         * and should be discarded. An empty savedDate is treated as fresh (compatible
         * with saves from before this field was added).
         */
        internal fun isDailySaveStale(savedDate: String, todayDate: String): Boolean {
            return savedDate.isNotEmpty() && savedDate != todayDate
        }
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
                        definitionItems = progress.definitionItems,
                        showLetterItems = progress.showLetterItems
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
            if (isDailyChallenge) {
                isReplay = false
                val saved = playerRepository.loadInProgressGame(difficultyKey)
                if (saved != null) {
                    // If the saved game is from a different day, discard it and start fresh
                    val today = dailyChallengeRepository.todayDateString()
                    if (isDailySaveStale(saved.savedDate, today)) {
                        android.util.Log.d(TAG, "Stale daily challenge save (${saved.savedDate} vs $today), starting fresh")
                        playerRepository.clearInProgressGame(difficultyKey)
                        startFreshLevel(levelArg)
                    } else {
                        try {
                            restoreFromSave(saved)
                        } catch (restoreEx: Exception) {
                            android.util.Log.e(TAG, "Failed to restore saved game, starting fresh", restoreEx)
                            playerRepository.clearInProgressGame(difficultyKey)
                            startFreshLevel(levelArg)
                        }
                    }
                } else {
                    startFreshLevel(levelArg)
                }
            } else {
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
        // Spend 1 life to start a non-replay, non-daily level
        if (!isReplay && !isDailyChallenge) {
            if (playerProgress.lives <= 0) {
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        lives = 0,
                        showNoLivesDialog = true,
                        status = GameStatus.WAITING_FOR_LIFE
                    )
                }
                return
            }
            val updated = playerProgress.copy(lives = playerProgress.lives - 1)
            playerProgress = updated
            playerRepository.saveProgress(updated)
        }

        // For VIP difficulty, word length varies by level
        val effectiveWordLength = if (difficulty == Difficulty.VIP) {
            Difficulty.vipWordLengthForLevel(level)
        } else {
            difficulty.wordLength
        }

        val word = if (isDailyChallenge) {
            dailyChallengeRepository.getDailyWord(difficulty.wordLength)
        } else {
            wordRepository.getWordForLevel(difficulty, level, effectiveWordLength)
        }
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

        // Check if the word has a definition and (for replay) pre-load it
        val defWordLength = if (difficulty == Difficulty.VIP) effectiveWordLength else null
        var defHint: String? = null
        var defUsed = false
        var wordHasDefinition = false
        if (!isDailyChallenge) {
            val definition = wordRepository.getDefinition(difficulty, level, defWordLength)
            wordHasDefinition = definition.isNotBlank()
            if (isReplay && wordHasDefinition) {
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
                revealedLetters = emptyMap(),
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
                showLetterItems = playerProgress.showLetterItems,
                isLoading = false,
                wordLength = effectiveWordLength,
                isReplay = isReplay,
                isDailyChallenge = isDailyChallenge,
                isVip = playerProgress.isVip,
                definitionHint = defHint,
                showDefinitionDialog = false,
                definitionUsedThisLevel = defUsed,
                wordHasDefinition = wordHasDefinition,
                starsEarned = 0
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
        val restoredPrefilled: Map<Int, Char> = saved.revealedLetters.entries
            .mapNotNull { (k, v) ->
                val pos = k.toIntOrNull() ?: return@mapNotNull null
                val ch = v.firstOrNull() ?: return@mapNotNull null
                pos to ch
            }.toMap()
        engine!!.restore(restoredGuesses, restoredInput, saved.maxGuesses, restoredPrefilled)

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
                showLetterItems = playerProgress.showLetterItems,
                isLoading = false,
                wordLength = saved.targetWord.length,
                isDailyChallenge = isDailyChallenge,
                isVip = playerProgress.isVip
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
        val vipWordLen = if (difficulty == Difficulty.VIP) e.effectiveWordLength else null
        val definition = if (isDailyChallenge) "" else wordRepository.getDefinition(difficulty, level, vipWordLen)
        val targetWord = e.guesses.last()
            .joinToString("") { it.first.toString() } // reconstruct from last guess (all CORRECT)
        val guessCount = e.guesses.size

        // Calculate stars: 3★ = 1-2 guesses, 2★ = 3-4 guesses, 1★ = 5+ guesses
        val stars = when {
            guessCount <= 2 -> 3
            guessCount <= 4 -> 2
            else -> 1
        }

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
                    isReplay = true,
                    starsEarned = stars
                )
            }
            // Still save star rating if better
            val existing = starRatingDao.get(difficultyKey, level)
            if (existing == null || stars > existing.stars) {
                starRatingDao.upsert(
                    StarRatingEntity(
                        id = existing?.id ?: 0,
                        difficultyKey = difficultyKey,
                        level = level,
                        stars = stars,
                        guessCount = guessCount
                    )
                )
            }
        } else if (isDailyChallenge) {
            val coinsEarned = 150L + (e.remainingGuesses * 15L)
            audioManager.playSfx(SfxSound.WIN)
            audioManager.playSfx(SfxSound.COIN_EARN)

            // Save daily challenge result
            dailyChallengeRepository.saveResult(
                wordLength = difficulty.wordLength,
                word = targetWord,
                guessCount = guessCount,
                won = true,
                stars = stars
            )

            // Update streak — consecutive days logic
            var p = playerProgress
            val today = dailyChallengeRepository.todayDateString()
            val wordLen = difficulty.wordLength  // 4, 5, or 6

            // Helper: does lastDate immediately precede today?
            fun isYesterday(lastDate: String): Boolean {
                if (lastDate.isEmpty()) return false
                return try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val last = sdf.parse(lastDate) ?: return false
                    val t = sdf.parse(today) ?: return false
                    val diffDays = ((t.time - last.time) / (1000L * 60 * 60 * 24)).toInt()
                    diffDays == 1
                } catch (_: Exception) { false }
            }

            // Overall streak: consecutive days winning any challenge
            val newOverallStreak = when {
                p.dailyChallengeLastDate.isEmpty() -> 1
                p.dailyChallengeLastDate == today  -> p.dailyChallengeStreak // same day replay
                isYesterday(p.dailyChallengeLastDate) -> p.dailyChallengeStreak + 1
                else -> 1 // gap — streak broken
            }

            // Per-length streaks
            fun calcLen(lastDate: String, cur: Int) = when {
                lastDate.isEmpty()   -> 1
                lastDate == today    -> cur
                isYesterday(lastDate) -> cur + 1
                else -> 1
            }
            val newStreak4 = if (wordLen == 4) calcLen(p.dailyLastDate4, p.dailyStreak4) else p.dailyStreak4
            val newStreak5 = if (wordLen == 5) calcLen(p.dailyLastDate5, p.dailyStreak5) else p.dailyStreak5
            val newStreak6 = if (wordLen == 6) calcLen(p.dailyLastDate6, p.dailyStreak6) else p.dailyStreak6

            p = p.copy(
                coins = p.coins + coinsEarned,
                totalCoinsEarned = p.totalCoinsEarned + coinsEarned,
                totalWins = p.totalWins + 1,
                totalGuesses = p.totalGuesses + guessCount,
                totalDailyChallengesCompleted = p.totalDailyChallengesCompleted + 1,
                dailyChallengeLastDate = today,
                dailyChallengeStreak = newOverallStreak,
                dailyChallengeBestStreak = maxOf(p.dailyChallengeBestStreak, newOverallStreak),
                dailyStreak4 = newStreak4,
                dailyBestStreak4 = maxOf(p.dailyBestStreak4, newStreak4),
                dailyLastDate4 = if (wordLen == 4) today else p.dailyLastDate4,
                dailyWins4 = if (wordLen == 4) p.dailyWins4 + 1 else p.dailyWins4,
                dailyStreak5 = newStreak5,
                dailyBestStreak5 = maxOf(p.dailyBestStreak5, newStreak5),
                dailyLastDate5 = if (wordLen == 5) today else p.dailyLastDate5,
                dailyWins5 = if (wordLen == 5) p.dailyWins5 + 1 else p.dailyWins5,
                dailyStreak6 = newStreak6,
                dailyBestStreak6 = maxOf(p.dailyBestStreak6, newStreak6),
                dailyLastDate6 = if (wordLen == 6) today else p.dailyLastDate6,
                dailyWins6 = if (wordLen == 6) p.dailyWins6 + 1 else p.dailyWins6,
                totalDailyChallengesPlayed = p.totalDailyChallengesPlayed + 1
            )
            // Apply streak rewards (2x for normal players, 3x for VIP)
            val (rewardedProgress, streakMsg) = applyStreakRewards(p, p.dailyChallengeStreak)
            p = rewardedProgress
            playerProgress = p
            playerRepository.saveProgress(p)

            _uiState.update { s ->
                s.copy(
                    status = GameStatus.WON,
                    showWinDialog = true,
                    winCoinEarned = coinsEarned,
                    winDefinition = "",
                    winWord = targetWord,
                    bonusLifeEarned = false,
                    starsEarned = stars,
                    lives = p.lives,
                    coins = p.coins,
                    diamonds = p.diamonds,
                    isDailyChallenge = true,
                    streakRewardMessage = streakMsg
                )
            }
            playerRepository.clearInProgressGame("daily")
        } else {
            val remaining = e.remainingGuesses
            val coinsEarned = 100L + (remaining * 10L)
            val (updatedProgress, bonusLife) = applyLevelCompletion(coinsEarned)

            audioManager.playSfx(SfxSound.WIN)
            audioManager.playSfx(SfxSound.COIN_EARN)

            // Save star rating
            val existing = starRatingDao.get(difficultyKey, level)
            if (existing == null || stars > existing.stars) {
                starRatingDao.upsert(
                    StarRatingEntity(
                        id = existing?.id ?: 0,
                        difficultyKey = difficultyKey,
                        level = level,
                        stars = stars,
                        guessCount = guessCount
                    )
                )
            }

            // Update cumulative stats
            var p = updatedProgress
            p = p.copy(
                totalCoinsEarned = p.totalCoinsEarned + coinsEarned,
                totalLevelsCompleted = p.totalLevelsCompleted + 1,
                totalWins = p.totalWins + 1,
                totalGuesses = p.totalGuesses + guessCount
            )
            playerProgress = p
            playerRepository.saveProgress(p)

            _uiState.update { s ->
                s.copy(
                    status = GameStatus.WON,
                    showWinDialog = true,
                    winCoinEarned = coinsEarned,
                    winDefinition = definition,
                    winWord = targetWord,
                    bonusLifeEarned = bonusLife,
                    starsEarned = stars,
                    lives = p.lives,
                    coins = p.coins,
                    diamonds = p.diamonds
                )
            }
            playerRepository.clearInProgressGame(difficulty)
        }
    }

    /** Apply streak rewards at specific milestones.
     * Normal players: 2x the original rewards.
     * VIP players: 3x the original rewards.
     * Returns Pair(updated progress, optional reward message for win dialog).
     */
    private fun applyStreakRewards(progress: PlayerProgress, streak: Int): Pair<PlayerProgress, String?> {
        var p = progress
        val isVip = p.isVip
        val mult = if (isVip) 3 else 2
        val msg: String? = when (streak) {
            3 -> {
                val coins = 100L * mult
                p = p.copy(coins = p.coins + coins, totalCoinsEarned = p.totalCoinsEarned + coins)
                val vipNote = if (isVip) " (VIP 3× bonus!)" else " (2× streak bonus!)"
                "🔥 3-day streak! +${coins} coins$vipNote  Normal: 200 | VIP: 300"
            }
            7 -> {
                val coins = 500L * mult
                val diamonds = 1 * mult
                p = p.copy(coins = p.coins + coins, diamonds = p.diamonds + diamonds, totalCoinsEarned = p.totalCoinsEarned + coins)
                val vipNote = if (isVip) " (VIP 3× bonus!)" else " (2× streak bonus!)"
                "🔥 7-day streak! +${coins} coins +${diamonds}💎$vipNote  Normal: 1000+2💎 | VIP: 1500+3💎"
            }
            14 -> {
                val coins = 1000L * mult
                val diamonds = 3 * mult
                p = p.copy(coins = p.coins + coins, diamonds = p.diamonds + diamonds, totalCoinsEarned = p.totalCoinsEarned + coins)
                val vipNote = if (isVip) " (VIP 3× bonus!)" else " (2× streak bonus!)"
                "🔥 14-day streak! +${coins} coins +${diamonds}💎$vipNote  Normal: 2000+6💎 | VIP: 3000+9💎"
            }
            30 -> {
                val coins = 2000L * mult
                val diamonds = 5 * mult
                val lives = 1 * mult
                p = p.copy(coins = p.coins + coins, diamonds = p.diamonds + diamonds, lives = p.lives + lives, totalCoinsEarned = p.totalCoinsEarned + coins)
                val vipNote = if (isVip) " (VIP 3× bonus!)" else " (2× streak bonus!)"
                "🔥 30-day streak! +${coins} coins +${diamonds}💎 +${lives}❤️$vipNote  Normal: 4000+10💎+2❤️ | VIP: 6000+15💎+3❤️"
            }
            else -> null
        }
        return Pair(p, msg)
    }

    private fun handleOutOfGuesses() {
        audioManager.playSfx(SfxSound.LEVEL_FAIL)
        val e = engine ?: return

        if (isDailyChallenge) {
            // Daily challenge: no second chances — save loss
            viewModelScope.launch {
                dailyChallengeRepository.saveResult(
                    wordLength = difficulty.wordLength,
                    word = _targetWordCache,
                    guessCount = e.guesses.size,
                    won = false,
                    stars = 0
                )
                // Reset daily challenge streak — also reset per-length streak
                val wordLen = difficulty.wordLength
                val p = playerProgress.copy(
                    dailyChallengeStreak = 0,
                    dailyStreak4 = if (wordLen == 4) 0 else playerProgress.dailyStreak4,
                    dailyStreak5 = if (wordLen == 5) 0 else playerProgress.dailyStreak5,
                    dailyStreak6 = if (wordLen == 6) 0 else playerProgress.dailyStreak6,
                    totalGuesses = playerProgress.totalGuesses + e.guesses.size,
                    totalDailyChallengesPlayed = playerProgress.totalDailyChallengesPlayed + 1
                )
                playerProgress = p
                playerRepository.saveProgress(p)
                playerRepository.clearInProgressGame(difficultyKey)
            }
            _uiState.update { it.copy(
                status = GameStatus.LOST,
                showDailyLossDialog = true,
                dailyLossWord = _targetWordCache
            )}
        } else {
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
            val updated = progress.copy(
                addGuessItems = progress.addGuessItems - 1,
                totalItemsUsed = progress.totalItemsUsed + 1
            )
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

    // ── Show Letter item ──────────────────────────────────────────────────────
    fun useShowLetterItem() {
        val e = engine ?: return
        val target = _targetWordCache
        if (target.isEmpty()) return

        // Find positions not yet revealed (by item) and not already CORRECT from guesses
        val revealed = _uiState.value.revealedLetters
        val correctPositions = mutableSetOf<Int>()
        for (guess in e.guesses) {
            for ((idx, pair) in guess.withIndex()) {
                if (pair.second == TileState.CORRECT) correctPositions.add(idx)
            }
        }
        // Always pick the leftmost unrevealed, non-correct position
        val availablePositions = (target.indices).filter { it !in correctPositions && it !in revealed }
        if (availablePositions.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "All letters already revealed!") }
            return
        }
        val pos = availablePositions.first()

        val progress = playerProgress
        if (progress.showLetterItems > 0) {
            audioManager.playSfx(SfxSound.ITEM_USE)
            val updated = progress.copy(
                showLetterItems = progress.showLetterItems - 1,
                totalItemsUsed = progress.totalItemsUsed + 1
            )
            playerProgress = updated
            viewModelScope.launch { playerRepository.saveProgress(updated) }

            e.prefillPosition(pos, target[pos])
            syncEngineToUiState()
            _uiState.update { s -> s.copy(showLetterItems = updated.showLetterItems) }
            persistCurrentState()
        } else {
            val coinCost = 250L
            if (progress.coins < coinCost) {
                _uiState.update { it.copy(snackbarMessage = "Need 250 coins or buy from Store") }
                return
            }
            audioManager.playSfx(SfxSound.COIN_EARN)
            val updated = progress.copy(
                coins = progress.coins - coinCost,
                totalItemsUsed = progress.totalItemsUsed + 1
            )
            playerProgress = updated
            viewModelScope.launch { playerRepository.saveProgress(updated) }

            e.prefillPosition(pos, target[pos])
            syncEngineToUiState()
            _uiState.update { s -> s.copy(coins = updated.coins) }
            persistCurrentState()
        }
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
            val vipWordLen = if (difficulty == Difficulty.VIP) {
                Difficulty.vipWordLengthForLevel(_uiState.value.level)
            } else null
            val definition = wordRepository.getDefinition(difficulty, _uiState.value.level, vipWordLen)
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
                status = e.status,
                wordLength = e.effectiveWordLength,
                revealedLetters = e.prefilledPositions
            )
        }
    }

    /** Get the target word (cached at level start, needed for findAbsentLetter). */
    private fun getTargetWord(): String = _targetWordCache
    private var _targetWordCache: String = ""

    private suspend fun applyLevelCompletion(coinsEarned: Long): Pair<PlayerProgress, Boolean> {
        var p = playerProgress
        val newLevel = _uiState.value.level + 1

        // VIP gets x2 coin rewards
        val effectiveCoins = if (difficulty == Difficulty.VIP) coinsEarned * 2 else coinsEarned

        p = when (difficulty) {
            Difficulty.EASY    -> p.copy(easyLevel = newLevel)
            Difficulty.REGULAR -> p.copy(regularLevel = newLevel)
            Difficulty.HARD    -> p.copy(hardLevel = newLevel)
            Difficulty.VIP     -> p.copy(vipLevel = newLevel)
        }

        p = p.copy(coins = p.coins + effectiveCoins)

        val counter = when (difficulty) {
            Difficulty.EASY    -> p.easyLevelsCompletedSinceBonusLife
            Difficulty.REGULAR -> p.regularLevelsCompletedSinceBonusLife
            Difficulty.HARD    -> p.hardLevelsCompletedSinceBonusLife
            Difficulty.VIP     -> p.vipLevelsCompletedSinceBonusLife
        }
        val newCounter = counter + 1
        val bonusLife = newCounter >= difficulty.levelBonusThreshold

        if (bonusLife) {
            p = p.copy(lives = p.lives + 1)
            p = when (difficulty) {
                Difficulty.EASY    -> p.copy(easyLevelsCompletedSinceBonusLife = 0)
                Difficulty.REGULAR -> p.copy(regularLevelsCompletedSinceBonusLife = 0)
                Difficulty.HARD    -> p.copy(hardLevelsCompletedSinceBonusLife = 0)
                Difficulty.VIP     -> p.copy(vipLevelsCompletedSinceBonusLife = 0)
            }
        } else {
            p = when (difficulty) {
                Difficulty.EASY    -> p.copy(easyLevelsCompletedSinceBonusLife = newCounter)
                Difficulty.REGULAR -> p.copy(regularLevelsCompletedSinceBonusLife = newCounter)
                Difficulty.HARD    -> p.copy(hardLevelsCompletedSinceBonusLife = newCounter)
                Difficulty.VIP     -> p.copy(vipLevelsCompletedSinceBonusLife = newCounter)
            }
        }

        playerProgress = p
        playerRepository.saveProgress(p)
        return Pair(p, bonusLife)
    }

    // ──────────────────────────── Play session time tracking ─────────────────

    private var playSessionStartMs: Long = 0L

    /**
     * Called when the game screen becomes visible/resumed (screen on, level active).
     * Starts accumulating play time for this session.
     */
    fun onPlaySessionResumed() {
        playSessionStartMs = System.currentTimeMillis()
    }

    /**
     * Called when the game screen pauses or the user navigates away.
     * Saves elapsed time to the relevant per-difficulty and total fields.
     */
    fun onPlaySessionPaused() {
        val start = playSessionStartMs
        if (start <= 0L) return
        playSessionStartMs = 0L
        val elapsed = System.currentTimeMillis() - start
        if (elapsed <= 0L) return
        viewModelScope.launch {
            val p = playerProgress
            val updated = when {
                isDailyChallenge -> p.copy(
                    dailyTimePlayedMs = p.dailyTimePlayedMs + elapsed,
                    totalTimePlayedMs = p.totalTimePlayedMs + elapsed
                )
                difficulty == Difficulty.EASY -> p.copy(
                    easyTimePlayedMs  = p.easyTimePlayedMs + elapsed,
                    totalTimePlayedMs = p.totalTimePlayedMs + elapsed
                )
                difficulty == Difficulty.REGULAR -> p.copy(
                    regularTimePlayedMs = p.regularTimePlayedMs + elapsed,
                    totalTimePlayedMs   = p.totalTimePlayedMs + elapsed
                )
                difficulty == Difficulty.HARD -> p.copy(
                    hardTimePlayedMs  = p.hardTimePlayedMs + elapsed,
                    totalTimePlayedMs = p.totalTimePlayedMs + elapsed
                )
                difficulty == Difficulty.VIP -> p.copy(
                    vipTimePlayedMs   = p.vipTimePlayedMs + elapsed,
                    totalTimePlayedMs = p.totalTimePlayedMs + elapsed
                )
                else -> p.copy(totalTimePlayedMs = p.totalTimePlayedMs + elapsed)
            }
            playerRepository.saveProgress(updated)
            playerProgress = updated
        }
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
                maxGuesses = e.maxGuesses,
                revealedLetters = e.prefilledPositions.entries
                    .associate { (k, v) -> k.toString() to v.toString() },
                // For daily challenges, stamp today's date so stale saves can be detected
                savedDate = if (isDailyChallenge) dailyChallengeRepository.todayDateString() else ""
            )
            playerRepository.saveInProgressGame(saved)
        }
    }

    private fun PlayerProgress.levelFor(d: Difficulty) = when (d) {
        Difficulty.EASY    -> easyLevel
        Difficulty.REGULAR -> regularLevel
        Difficulty.HARD    -> hardLevel
        Difficulty.VIP     -> vipLevel
    }
}
