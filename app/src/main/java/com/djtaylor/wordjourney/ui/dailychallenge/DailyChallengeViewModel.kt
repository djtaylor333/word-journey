package com.djtaylor.wordjourney.ui.dailychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.db.DailyChallengeResultEntity
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.streakShieldCost
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val played6: Boolean = false,
    // Per-length consecutive-day streaks
    val streak4: Int = 0,
    val streak5: Int = 0,
    val streak6: Int = 0,
    val bestStreak4: Int = 0,
    val bestStreak5: Int = 0,
    val bestStreak6: Int = 0,
    val wins4: Int = 0,
    val wins5: Int = 0,
    val wins6: Int = 0,
    // Streak shield state
    val showStreakShieldDialog: Boolean = false,
    val streakBeforeBreak: Int = 0,        // streak value before the 1-day gap
    val streakShieldCostGems: Int = 5,     // gems required to restore
    val streakShieldApplied: Boolean = false
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
                val today = dailyChallengeRepository.todayDateString()

                // Streak shield detection:
                // Show dialog if player had a streak > 0 AND missed exactly 1 day AND hasn't played today
                val streakBeforeBreak = p.dailyChallengeStreak
                val missedExactlyOneDay = isMissedExactlyOneDay(p.dailyChallengeLastDate, today)
                val hasntPlayedToday = p.dailyChallengeLastDate != today
                val shieldAlreadyApplied = _uiState.value.streakShieldApplied
                val showShieldDialog = !shieldAlreadyApplied &&
                    streakBeforeBreak >= 1 &&
                    missedExactlyOneDay &&
                    hasntPlayedToday &&
                    playedLengths.isEmpty() // only show before they play today

                val currentMonthKey = today.substring(0, 7) // YYYY-MM
                // Reset monthly counter if month changed
                val shieldUsedThisMonth = if (p.streakShieldMonthKey == currentMonthKey) {
                    p.streakShieldUsedThisMonth
                } else 0
                val shieldCost = streakShieldCost(shieldUsedThisMonth)

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
                        streak4 = p.dailyStreak4,
                        streak5 = p.dailyStreak5,
                        streak6 = p.dailyStreak6,
                        bestStreak4 = p.dailyBestStreak4,
                        bestStreak5 = p.dailyBestStreak5,
                        bestStreak6 = p.dailyBestStreak6,
                        wins4 = p.dailyWins4,
                        wins5 = p.dailyWins5,
                        wins6 = p.dailyWins6,
                        showStreakShieldDialog = showShieldDialog,
                        streakBeforeBreak = streakBeforeBreak,
                        streakShieldCostGems = shieldCost,
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

    fun dismissStreakShieldDialog() {
        _uiState.update { it.copy(showStreakShieldDialog = false) }
    }

    /** Spends gems to restore the broken streak. Returns true on success; false if not enough gems. */
    fun activateStreakShield(): Boolean {
        val state = _uiState.value
        if (state.streakShieldCostGems <= 0) return false

        viewModelScope.launch {
            val progress = playerRepository.playerProgressFlow.first()
            if (progress.diamonds < state.streakShieldCostGems) {
                _uiState.update { it.copy(showStreakShieldDialog = false) }
                return@launch
            }
            val today = dailyChallengeRepository.todayDateString()
            val yesterday = yesterdayString()
            val currentMonthKey = today.substring(0, 7)
            val shieldUsedThisMonth = if (progress.streakShieldMonthKey == currentMonthKey) {
                progress.streakShieldUsedThisMonth
            } else 0

            val updated = progress.copy(
                diamonds = progress.diamonds - state.streakShieldCostGems,
                // Move lastDate back to yesterday so the GameViewModel sees a consecutive day
                dailyChallengeLastDate = yesterday,
                dailyChallengeStreak = state.streakBeforeBreak,
                streakShieldUsedThisMonth = shieldUsedThisMonth + 1,
                streakShieldMonthKey = currentMonthKey
            )
            playerRepository.saveProgress(updated)
            audioManager.playSfx(SfxSound.LIFE_GAINED)
            _uiState.update {
                it.copy(
                    showStreakShieldDialog = false,
                    streakShieldApplied = true,
                    streak = state.streakBeforeBreak
                )
            }
        }
        return true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if [lastDate] is exactly 2 days before [today]
     * (meaning the player skipped exactly 1 day — yesterday).
     */
    internal fun isMissedExactlyOneDay(lastDate: String, today: String): Boolean {
        if (lastDate.isEmpty()) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val last = sdf.parse(lastDate) ?: return false
            val t = sdf.parse(today) ?: return false
            val diffDays = ((t.time - last.time) / (1000L * 60 * 60 * 24)).toInt()
            diffDays == 2
        } catch (_: Exception) { false }
    }

    private fun yesterdayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }
}
