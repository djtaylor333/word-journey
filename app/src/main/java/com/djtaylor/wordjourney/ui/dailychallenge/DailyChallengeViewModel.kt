package com.djtaylor.wordjourney.ui.dailychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.db.DailyChallengeResultEntity
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyChallengeUiState(
    val todayResults: List<DailyChallengeResultEntity> = emptyList(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalWins: Int = 0,
    val totalPlayed: Int = 0,
    val lives: Int = 10,
    val timerDisplayMs: Long = 0L,
    val isLoading: Boolean = true,
    // Which word lengths have been played today
    val played4: Boolean = false,
    val played5: Boolean = false,
    val played6: Boolean = false
)

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                // Apply regen
                val regen = lifeRegenUseCase(progress.lives, progress.lastLifeRegenTimestamp)
                val p = if (regen.livesAdded > 0) {
                    progress.copy(
                        lives = regen.updatedLives,
                        lastLifeRegenTimestamp = regen.updatedTimestamp
                    ).also { playerRepository.saveProgress(it) }
                } else progress

                val results = dailyChallengeRepository.getResultsForToday()
                val playedLengths = results.map { it.wordLength }.toSet()

                _uiState.update {
                    it.copy(
                        todayResults = results,
                        streak = p.dailyChallengeStreak,
                        bestStreak = p.dailyChallengeBestStreak,
                        totalWins = dailyChallengeRepository.totalWins(),
                        totalPlayed = dailyChallengeRepository.totalPlayed(),
                        lives = p.lives,
                        played4 = 4 in playedLengths,
                        played5 = 5 in playedLengths,
                        played6 = 6 in playedLengths,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun canPlay(wordLength: Int): Boolean {
        return when (wordLength) {
            4 -> !_uiState.value.played4
            5 -> !_uiState.value.played5
            6 -> !_uiState.value.played6
            else -> false
        }
    }

    fun playButtonClick() {
        audioManager.playSfx(SfxSound.BUTTON_CLICK)
    }
}
