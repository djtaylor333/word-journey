package com.djtaylor.wordjourney.ui.timermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.db.InboxItemEntity
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.InboxRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.data.repository.WordRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.GameStatus
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.TileState
import com.djtaylor.wordjourney.domain.usecase.EvaluateGuessUseCase
import com.djtaylor.wordjourney.engine.GameEngine
import com.djtaylor.wordjourney.engine.SubmitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────── Data types ──────────────────────────────────

/**
 * Difficulty level for timer mode.
 * @param wordLength  length of words used in this difficulty
 * @param startTimeSecs  starting timer (seconds)
 * @param engineDifficulty  underlying [Difficulty] for [GameEngine] construction
 */
enum class TimerDifficulty(
    val label: String,
    val emoji: String,
    val wordLength: Int,
    val startTimeSecs: Int,
    val engineDifficulty: Difficulty
) {
    EASY("Easy",    "🟢", 4, 180, Difficulty.EASY),
    REGULAR("Regular", "🟡", 5, 240, Difficulty.REGULAR),
    HARD("Hard",    "🔴", 6, 300, Difficulty.HARD)
}

enum class TimerPhase { SETUP, COUNTDOWN, PLAYING, RECAP }

data class TimerModeUiState(
    val phase: TimerPhase = TimerPhase.SETUP,
    val selectedDifficulty: TimerDifficulty? = null,
    val showRulesDialog: Boolean = false,
    // Countdown (3 → 2 → 1)
    val countdownValue: Int = 3,
    // Live timer
    val remainingMs: Long = 0L,
    // Current word game state (mirrors GameEngine)
    val guesses: List<List<Pair<Char, TileState>>> = emptyList(),
    val currentInput: List<Char> = emptyList(),
    val maxGuesses: Int = 6,
    val letterStates: Map<Char, TileState> = emptyMap(),
    val removedLetters: Set<Char> = emptySet(),
    val prefilledPositions: Map<Int, Char> = emptyMap(),
    val wordStatus: GameStatus = GameStatus.IN_PROGRESS,
    val wordLength: Int = 5,
    val shakeRow: Boolean = false,
    val snackbarMessage: String? = null,
    val definitionHint: String? = null,
    val showDefinitionDialog: Boolean = false,
    val definitionUsedThisWord: Boolean = false,
    // Session stats (accumulated)
    val wordsCorrect: Int = 0,
    val wordsAttempted: Int = 0,
    val livesEarned: Int = 0,
    val totalBonusSecs: Int = 0,
    // Recap data
    val recapBaseTimeSecs: Int = 0,
    val recapTotalSecs: Int = 0,
    val bestLevels: Int = 0,
    val bestTimeSecs: Int = 0,
    // Player resources (visible in toolbar)
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,
    val definitionItems: Int = 0,
    val showLetterItems: Int = 0,
    val isVip: Boolean = false
) {
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val currentRow: Int get() = guesses.size
}

// ─────────────────────────────── ViewModel ────────────────────────────────────

@HiltViewModel
class TimerModeViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val playerRepository: PlayerRepository,
    private val inboxRepository: InboxRepository,
    private val evaluateGuess: EvaluateGuessUseCase,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerModeUiState())
    val uiState: StateFlow<TimerModeUiState> = _uiState.asStateFlow()

    private var playerProgress: PlayerProgress = PlayerProgress()
    private var engine: GameEngine? = null
    private var wordPool: MutableList<String> = mutableListOf()
    private var usedWords: MutableSet<String> = mutableSetOf()
    private var currentWord: String = ""
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                playerProgress = progress
                _uiState.update { s ->
                    s.copy(
                        addGuessItems    = progress.addGuessItems,
                        removeLetterItems = progress.removeLetterItems,
                        definitionItems  = progress.definitionItems,
                        showLetterItems  = progress.showLetterItems,
                        isVip            = progress.isVip
                    )
                }
            }
        }
    }

    // ──────────────────────────── SETUP phase ────────────────────────────────

    fun selectDifficulty(difficulty: TimerDifficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty, showRulesDialog = true) }
    }

    fun dismissRules() {
        _uiState.update { it.copy(showRulesDialog = false) }
    }

    /** Called when the player taps "Begin" in the rules dialog. */
    fun beginSession() {
        val diff = _uiState.value.selectedDifficulty ?: return
        _uiState.update { it.copy(showRulesDialog = false) }
        startCountdown(diff)
    }

    // ──────────────────────────── COUNTDOWN phase ────────────────────────────

    private fun startCountdown(diff: TimerDifficulty) {
        _uiState.update { it.copy(phase = TimerPhase.COUNTDOWN, countdownValue = 3) }
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                _uiState.update { it.copy(countdownValue = n) }
                audioManager.playSfx(SfxSound.KEY_TAP)
                delay(1_000)
            }
            startPlaying(diff)
        }
    }

    // ──────────────────────────── PLAYING phase ──────────────────────────────

    private suspend fun startPlaying(diff: TimerDifficulty) {
        wordPool = dailyChallengeRepository.getTimerWords(diff.wordLength).toMutableList()
        usedWords = mutableSetOf()
        _uiState.update {
            it.copy(
                phase           = TimerPhase.PLAYING,
                remainingMs     = diff.startTimeSecs * 1_000L,
                wordsCorrect    = 0,
                wordsAttempted  = 0,
                livesEarned     = 0,
                totalBonusSecs  = 0,
                recapBaseTimeSecs = diff.startTimeSecs
            )
        }
        loadNextWord(diff)
        startTimer()
    }

    private suspend fun loadNextWord(diff: TimerDifficulty) {
        if (wordPool.isEmpty()) {
            // Reload pool (shouldn't happen, but just in case)
            wordPool = dailyChallengeRepository.getTimerWords(diff.wordLength)
                .filter { it !in usedWords }
                .toMutableList()
            if (wordPool.isEmpty()) wordPool = dailyChallengeRepository.getTimerWords(diff.wordLength).toMutableList()
        }
        val word = wordPool.removeFirst()
        usedWords.add(word)
        currentWord = word

        engine = GameEngine(
            difficulty    = diff.engineDifficulty,
            targetWord    = word,
            evaluateGuess = evaluateGuess,
            wordValidator = { guess, len -> wordRepository.isValidWord(guess, len) }
        )
        _uiState.update {
            it.copy(
                guesses           = emptyList(),
                currentInput      = emptyList(),
                maxGuesses        = 6,
                letterStates      = emptyMap(),
                removedLetters    = emptySet(),
                prefilledPositions= emptyMap(),
                wordStatus        = GameStatus.IN_PROGRESS,
                wordLength        = diff.wordLength,
                definitionHint    = null,
                showDefinitionDialog = false,
                definitionUsedThisWord = false,
                snackbarMessage   = null
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingMs > 0 && _uiState.value.phase == TimerPhase.PLAYING) {
                delay(100)
                val newMs = (_uiState.value.remainingMs - 100).coerceAtLeast(0)
                _uiState.update { it.copy(remainingMs = newMs) }
                if (newMs <= 0L) {
                    endSession()
                    break
                }
            }
        }
    }

    private fun endSession() {
        timerJob?.cancel()
        val s = _uiState.value
        val diff = s.selectedDifficulty ?: return
        val totalSecs = s.recapBaseTimeSecs + s.totalBonusSecs
        audioManager.playSfx(SfxSound.LEVEL_FAIL)

        viewModelScope.launch {
            val p = playerProgress
            val updated = when (diff) {
                TimerDifficulty.EASY -> p.copy(
                    timerBestLevelsEasy  = maxOf(p.timerBestLevelsEasy, s.wordsCorrect),
                    timerBestTimeSecsEasy = maxOf(p.timerBestTimeSecsEasy, totalSecs)
                )
                TimerDifficulty.REGULAR -> p.copy(
                    timerBestLevelsRegular  = maxOf(p.timerBestLevelsRegular, s.wordsCorrect),
                    timerBestTimeSecsRegular = maxOf(p.timerBestTimeSecsRegular, totalSecs)
                )
                TimerDifficulty.HARD -> p.copy(
                    timerBestLevelsHard  = maxOf(p.timerBestLevelsHard, s.wordsCorrect),
                    timerBestTimeSecsHard = maxOf(p.timerBestTimeSecsHard, totalSecs)
                )
            }
            playerRepository.saveProgress(updated)
            playerProgress = updated

            val (bestLevels, bestTime) = when (diff) {
                TimerDifficulty.EASY    -> updated.timerBestLevelsEasy    to updated.timerBestTimeSecsEasy
                TimerDifficulty.REGULAR -> updated.timerBestLevelsRegular to updated.timerBestTimeSecsRegular
                TimerDifficulty.HARD    -> updated.timerBestLevelsHard    to updated.timerBestTimeSecsHard
            }
            _uiState.update {
                it.copy(
                    phase         = TimerPhase.RECAP,
                    recapTotalSecs = totalSecs,
                    bestLevels    = bestLevels,
                    bestTimeSecs  = bestTime
                )
            }
        }
    }

    // ──────────────────────────── Input handlers ─────────────────────────────

    fun onKeyPressed(char: Char) {
        val e = engine ?: return
        if (_uiState.value.phase != TimerPhase.PLAYING) return
        if (_uiState.value.wordStatus != GameStatus.IN_PROGRESS) return
        if (e.onKeyPressed(char)) {
            audioManager.playSfx(SfxSound.KEY_TAP)
            syncEngine()
        }
    }

    fun onDelete() {
        val e = engine ?: return
        if (_uiState.value.phase != TimerPhase.PLAYING) return
        if (_uiState.value.wordStatus != GameStatus.IN_PROGRESS) return
        if (e.onDelete()) syncEngine()
    }

    fun onSubmit() {
        val e = engine ?: return
        if (_uiState.value.phase != TimerPhase.PLAYING) return
        if (_uiState.value.wordStatus != GameStatus.IN_PROGRESS) return
        if (!e.canSubmit) return

        viewModelScope.launch {
            when (val result = e.onSubmit()) {
                is SubmitResult.InvalidWord -> {
                    audioManager.playSfx(SfxSound.INVALID_WORD)
                    _uiState.update { it.copy(shakeRow = true, snackbarMessage = "Not a valid word") }
                    delay(600)
                    _uiState.update { it.copy(shakeRow = false, snackbarMessage = null) }
                }
                is SubmitResult.Evaluated -> {
                    audioManager.playSfx(SfxSound.TILE_FLIP)
                    syncEngine()
                    if (result.isWin) handleWordWin()
                    else if (result.isOutOfGuesses) handleWordFail()
                }
                SubmitResult.NotReady -> { /* skip */ }
            }
        }
    }

    // ──────────────────────────── Word outcome ────────────────────────────────

    private fun handleWordWin() {
        val diff = _uiState.value.selectedDifficulty ?: return
        audioManager.playSfx(SfxSound.WIN)

        val newWordsCorrect  = _uiState.value.wordsCorrect + 1
        val newWordsAttempted = _uiState.value.wordsAttempted + 1
        val bonusSecs = 30
        val newRemaining = _uiState.value.remainingMs + bonusSecs * 1_000L
        var newLivesEarned = _uiState.value.livesEarned

        // Life rewards: Regular = +1 per 5 correct, VIP = +2 per 5 correct
        var livesToInbox = 0
        val vip = _uiState.value.isVip
        if (newWordsCorrect % 5 == 0) livesToInbox += if (vip) 2 else 1

        if (livesToInbox > 0) {
            newLivesEarned += livesToInbox
            viewModelScope.launch {
                inboxRepository.addItem(
                    InboxItemEntity(
                        type = "timer_reward",
                        title = "⏱️ Timer Mode Reward",
                        message = "+$livesToInbox ${if (livesToInbox == 1) "life" else "lives"} " +
                                  "earned at $newWordsCorrect words correct!",
                        livesGranted = livesToInbox
                    )
                )
            }
        }

        _uiState.update {
            it.copy(
                wordsCorrect  = newWordsCorrect,
                wordsAttempted = newWordsAttempted,
                livesEarned   = newLivesEarned,
                remainingMs   = newRemaining,
                totalBonusSecs = it.totalBonusSecs + bonusSecs,
                wordStatus    = GameStatus.WON
            )
        }

        // Brief celebration display, then load next word
        viewModelScope.launch {
            delay(1_200)
            if (_uiState.value.phase == TimerPhase.PLAYING && _uiState.value.remainingMs > 0) {
                loadNextWord(diff)
            }
        }
    }

    private fun handleWordFail() {
        val diff = _uiState.value.selectedDifficulty ?: return
        audioManager.playSfx(SfxSound.LEVEL_FAIL)
        _uiState.update {
            it.copy(
                wordsAttempted = it.wordsAttempted + 1,
                wordStatus     = GameStatus.LOST
            )
        }
        viewModelScope.launch {
            delay(1_500)
            if (_uiState.value.phase == TimerPhase.PLAYING && _uiState.value.remainingMs > 0) {
                loadNextWord(diff)
            }
        }
    }

    // ──────────────────────────── Items ──────────────────────────────────────

    /** Add Guess item — costs 1 item from inventory (not free). */
    fun useAddGuessItem() {
        val e = engine ?: return
        val p = playerProgress
        if (p.addGuessItems <= 0) {
            _uiState.update { it.copy(snackbarMessage = "No Add Guess items — visit the Store!") }
            return
        }
        audioManager.playSfx(SfxSound.BUTTON_CLICK)
        e.addBonusGuesses(1)
        viewModelScope.launch {
            val updated = p.copy(
                addGuessItems = p.addGuessItems - 1,
                totalItemsUsed = p.totalItemsUsed + 1
            )
            playerRepository.saveProgress(updated)
            playerProgress = updated
        }
        syncEngine()
    }

    /** Remove Letter item — costs 1 item from inventory (not free). */
    fun useRemoveLetterItem() {
        val e = engine ?: return
        val p = playerProgress
        if (p.removeLetterItems <= 0) {
            _uiState.update { it.copy(snackbarMessage = "No Remove Letter items — visit the Store!") }
            return
        }
        viewModelScope.launch {
            val absent = wordRepository.findAbsentLetter(
                currentWord,
                e.removedLetters,
                e.letterStates.keys
            ) ?: run {
                _uiState.update { it.copy(snackbarMessage = "No more letters to remove!") }
                return@launch
            }
            audioManager.playSfx(SfxSound.BUTTON_CLICK)
            e.removeLetter(absent)
            val updated = p.copy(
                removeLetterItems = p.removeLetterItems - 1,
                totalItemsUsed = p.totalItemsUsed + 1
            )
            playerRepository.saveProgress(updated)
            playerProgress = updated
            syncEngine()
        }
    }

    /** Define Word item — FREE in timer mode. */
    fun useDefinitionItem() {
        if (_uiState.value.definitionUsedThisWord) {
            // Already fetched — just re-show the dialog
            _uiState.update { it.copy(showDefinitionDialog = true) }
            return
        }
        viewModelScope.launch {
            val def = wordRepository.getDefinitionForWord(currentWord)
            val hint = def.ifBlank { "No definition available for this word." }
            _uiState.update {
                it.copy(
                    definitionHint    = hint,
                    showDefinitionDialog = true,
                    definitionUsedThisWord = true
                )
            }
        }
    }

    fun dismissDefinitionDialog() {
        _uiState.update { it.copy(showDefinitionDialog = false) }
    }

    /** Show Letter item — costs 1 item from inventory (not free). */
    fun useShowLetterItem() {
        val e = engine ?: return
        val p = playerProgress
        if (p.showLetterItems <= 0) {
            _uiState.update { it.copy(snackbarMessage = "No Show Letter items — visit the Store!") }
            return
        }
        val prefilled = e.prefilledPositions
        val correct = e.guesses.flatMapIndexed { _, row ->
            row.mapIndexedNotNull { idx, (_, state) ->
                if (state == TileState.CORRECT) idx else null
            }
        }.toSet()
        val available = currentWord.indices.filter { it !in correct && it !in prefilled }
        if (available.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "All letters already revealed!") }
            return
        }
        val pos = available.first()
        audioManager.playSfx(SfxSound.ITEM_USE)
        e.prefillPosition(pos, currentWord[pos])
        viewModelScope.launch {
            val updated = p.copy(
                showLetterItems = p.showLetterItems - 1,
                totalItemsUsed  = p.totalItemsUsed + 1
            )
            playerRepository.saveProgress(updated)
            playerProgress = updated
        }
        syncEngine()
    }

    // ──────────────────────────── Session time tracking ──────────────────────

    private var playSessionStartMs: Long = 0L

    /**
     * Called when the Timer Mode screen becomes visible/resumed.
     * Starts counting play time.
     */
    fun onSessionResumed() {
        playSessionStartMs = System.currentTimeMillis()
    }

    /**
     * Called when the Timer Mode screen pauses or the user navigates away.
     * Saves elapsed time to [timerTimePlayedMs] and [totalTimePlayedMs].
     */
    fun onSessionPaused() {
        val start = playSessionStartMs
        if (start <= 0L) return
        playSessionStartMs = 0L
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < 0L) return  // allow 0ms saves (test environments)
        viewModelScope.launch {
            val p = playerProgress
            val updated = p.copy(
                timerTimePlayedMs = p.timerTimePlayedMs + elapsed,
                totalTimePlayedMs = p.totalTimePlayedMs + elapsed
            )
            playerRepository.saveProgress(updated)
            playerProgress = updated
        }
    }

    // ──────────────────────────── Recap / retry ───────────────────────────────

    /** Reset back to the difficulty-select screen. */
    fun playAgain() {
        timerJob?.cancel()
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                phase              = TimerPhase.SETUP,
                selectedDifficulty = null,
                showRulesDialog    = false
            )
        }
    }

    // ──────────────────────────── Helpers ────────────────────────────────────

    private fun syncEngine() {
        val e = engine ?: return
        _uiState.update {
            it.copy(
                guesses            = e.guesses,
                currentInput       = e.currentInput,
                maxGuesses         = e.maxGuesses,
                letterStates       = e.letterStates,
                removedLetters     = e.removedLetters,
                prefilledPositions = e.prefilledPositions
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        countdownJob?.cancel()
    }
}
