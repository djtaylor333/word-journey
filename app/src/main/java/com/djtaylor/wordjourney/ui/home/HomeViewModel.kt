package com.djtaylor.wordjourney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val timerDisplayMs: Long = 0L,         // ms until next life
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
        startTimerTick()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            // Apply any passive life regen since last open
            playerRepository.playerProgressFlow.collectLatest { progress ->
                val regen = lifeRegenUseCase(
                    progress.lives,
                    progress.lastLifeRegenTimestamp
                )
                val updated = if (regen.livesAdded > 0) {
                    val p = progress.copy(
                        lives = regen.updatedLives,
                        lastLifeRegenTimestamp = regen.updatedTimestamp
                    )
                    playerRepository.saveProgress(p)
                    p
                } else {
                    progress
                }
                _uiState.update { it.copy(progress = updated, isLoading = false) }
            }
        }
    }

    /** Ticks every second to update the life countdown display. */
    private fun startTimerTick() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val progress = _uiState.value.progress
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

    /** Called when the player taps a difficulty card to start a level. */
    fun enterLevel(difficulty: Difficulty): EnterLevelResult {
        val progress = _uiState.value.progress
        return if (progress.lives <= 0) {
            EnterLevelResult.NoLives
        } else {
            viewModelScope.launch {
                val updated = progress.copy(lives = progress.lives - 1)
                // Restart regen timer if lives just dropped below cap
                val regenTs = if (updated.lives < LifeRegenUseCase.TIME_REGEN_CAP &&
                    updated.lastLifeRegenTimestamp == 0L) {
                    System.currentTimeMillis()
                } else {
                    updated.lastLifeRegenTimestamp
                }
                val final = updated.copy(lastLifeRegenTimestamp = regenTs)
                playerRepository.saveProgress(final)
            }
            EnterLevelResult.Ok
        }
    }

    fun levelForDifficulty(difficulty: Difficulty): Int {
        val progress = _uiState.value.progress
        return when (difficulty) {
            Difficulty.EASY    -> progress.easyLevel
            Difficulty.REGULAR -> progress.regularLevel
            Difficulty.HARD    -> progress.hardLevel
        }
    }
}

enum class EnterLevelResult { Ok, NoLives }
