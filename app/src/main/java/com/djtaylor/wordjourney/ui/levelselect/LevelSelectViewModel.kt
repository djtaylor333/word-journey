package com.djtaylor.wordjourney.ui.levelselect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelSelectUiState(
    val difficulty: Difficulty = Difficulty.REGULAR,
    val currentLevel: Int = 1,        // player's current unsolved level
    val totalLevels: Int = 100,
    val lives: Int = 10,
    val bonusLives: Int = 0,          // lives above 10
    val coins: Long = 0L,
    val diamonds: Int = 5,
    val timerDisplayMs: Long = 0L,
    val isLoading: Boolean = true,
    val showNoLivesDialog: Boolean = false,
    val lifeDeducted: Boolean = false, // triggers heart animation
)

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase
) : ViewModel() {

    private val difficultyKey: String = checkNotNull(savedStateHandle["difficulty"])
    private val difficulty: Difficulty = Difficulty.entries.first { it.saveKey == difficultyKey }
    private var playerProgress: PlayerProgress = PlayerProgress()

    private val _uiState = MutableStateFlow(LevelSelectUiState(difficulty = difficulty))
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
        startTimerTick()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                val regen = lifeRegenUseCase(progress.lives, progress.lastLifeRegenTimestamp)
                playerProgress = if (regen.livesAdded > 0) {
                    val p = progress.copy(
                        lives = regen.updatedLives,
                        lastLifeRegenTimestamp = regen.updatedTimestamp
                    )
                    playerRepository.saveProgress(p)
                    p
                } else progress

                _uiState.update {
                    it.copy(
                        currentLevel = playerProgress.levelFor(difficulty),
                        lives = minOf(playerProgress.lives, 10),
                        bonusLives = maxOf(playerProgress.lives - 10, 0),
                        coins = playerProgress.coins,
                        diamonds = playerProgress.diamonds,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun startTimerTick() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val progress = playerProgress
                if (progress.lives < LifeRegenUseCase.TIME_REGEN_CAP) {
                    val ms = lifeRegenUseCase.nextLifeAtMs(
                        progress.lastLifeRegenTimestamp
                    ) - System.currentTimeMillis()
                    _uiState.update { it.copy(timerDisplayMs = ms.coerceAtLeast(0L)) }
                } else {
                    _uiState.update { it.copy(timerDisplayMs = 0L) }
                }
            }
        }
    }

    /**
     * Returns true if the level can be started, false if no lives.
     * For completed levels (replay), no life is deducted.
     */
    fun canStartLevel(level: Int): Boolean {
        val currentLevel = playerProgress.levelFor(difficulty)
        val isReplay = level < currentLevel
        if (isReplay) return true  // free replay
        return playerProgress.lives > 0
    }

    /**
     * Deduct a life when starting the CURRENT (unsolved) level.
     * Returns true if successful.
     */
    fun deductLifeForLevel(level: Int): Boolean {
        val currentLevel = playerProgress.levelFor(difficulty)
        if (level < currentLevel) return true  // replay, no cost

        if (playerProgress.lives <= 0) {
            _uiState.update { it.copy(showNoLivesDialog = true) }
            return false
        }

        val updated = playerProgress.copy(lives = playerProgress.lives - 1)
        // Restart regen timer if needed
        val regenTs = if (updated.lives < LifeRegenUseCase.TIME_REGEN_CAP &&
            updated.lastLifeRegenTimestamp == 0L
        ) System.currentTimeMillis() else updated.lastLifeRegenTimestamp
        val final = updated.copy(lastLifeRegenTimestamp = regenTs)
        playerProgress = final
        viewModelScope.launch { playerRepository.saveProgress(final) }

        _uiState.update {
            it.copy(
                lives = minOf(final.lives, 10),
                bonusLives = maxOf(final.lives - 10, 0),
                lifeDeducted = true
            )
        }
        return true
    }

    fun dismissNoLivesDialog() {
        _uiState.update { it.copy(showNoLivesDialog = false) }
    }

    fun resetLifeAnimation() {
        _uiState.update { it.copy(lifeDeducted = false) }
    }

    private fun PlayerProgress.levelFor(d: Difficulty) = when (d) {
        Difficulty.EASY    -> easyLevel
        Difficulty.REGULAR -> regularLevel
        Difficulty.HARD    -> hardLevel
    }
}
