package com.djtaylor.wordjourney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import com.djtaylor.wordjourney.domain.usecase.VipDailyRewardUseCase
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val timerDisplayMs: Long = 0L,         // ms until next life
    val isLoading: Boolean = true,
    val dailyChallengeStreak: Int = 0,
    val vipRewardsMessage: String? = null,
    val newPlayerBonusMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase,
    private val vipDailyRewardUseCase: VipDailyRewardUseCase,
    private val audioManager: WordJourneysAudioManager
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
                var updated = if (regen.livesAdded > 0) {
                    val p = progress.copy(
                        lives = regen.updatedLives,
                        lastLifeRegenTimestamp = regen.updatedTimestamp
                    )
                    playerRepository.saveProgress(p)
                    p
                } else {
                    progress
                }

                // ── New Player Bonus ─────────────────────────────────────
                var newPlayerMsg: String? = null
                if (!updated.hasReceivedNewPlayerBonus) {
                    updated = updated.copy(
                        coins = updated.coins + 500,
                        diamonds = updated.diamonds + 5,
                        addGuessItems = updated.addGuessItems + 3,
                        removeLetterItems = updated.removeLetterItems + 3,
                        definitionItems = updated.definitionItems + 3,
                        showLetterItems = updated.showLetterItems + 3,
                        hasReceivedNewPlayerBonus = true,
                        totalCoinsEarned = updated.totalCoinsEarned + 500
                    )
                    playerRepository.saveProgress(updated)
                    newPlayerMsg = "🎉 Welcome! You received 500 coins, 5 diamonds, and 3 of each item!"
                }

                // ── VIP Daily Rewards ────────────────────────────────────
                var vipMsg: String? = null
                if (updated.isVip) {
                    val reward = vipDailyRewardUseCase.calculateRewards(updated.lastVipRewardDate)
                    if (reward != null) {
                        updated = updated.copy(
                            lives = updated.lives + reward.livesGranted,
                            addGuessItems = updated.addGuessItems + reward.addGuessItemsGranted,
                            removeLetterItems = updated.removeLetterItems + reward.removeLetterItemsGranted,
                            definitionItems = updated.definitionItems + reward.definitionItemsGranted,
                            showLetterItems = updated.showLetterItems + reward.showLetterItemsGranted,
                            lastVipRewardDate = reward.updatedLastRewardDate
                        )
                        playerRepository.saveProgress(updated)
                        if (reward.daysAccumulated > 1) {
                            vipMsg = "👑 VIP: ${reward.daysAccumulated} days of rewards! +${reward.livesGranted} lives, +${reward.addGuessItemsGranted + reward.removeLetterItemsGranted + reward.definitionItemsGranted + reward.showLetterItemsGranted} items"
                        } else {
                            vipMsg = "👑 VIP Daily: +${reward.livesGranted} lives, +${reward.addGuessItemsGranted + reward.removeLetterItemsGranted + reward.definitionItemsGranted + reward.showLetterItemsGranted} items"
                        }
                    }
                }

                // Track login streak
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                if (updated.lastLoginDate != today) {
                    val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val newLoginStreak = if (updated.lastLoginDate == yesterday) {
                        updated.loginStreak + 1
                    } else {
                        1
                    }
                    updated = updated.copy(
                        lastLoginDate = today,
                        loginStreak = newLoginStreak,
                        loginBestStreak = maxOf(updated.loginBestStreak, newLoginStreak)
                    )
                    playerRepository.saveProgress(updated)
                }

                _uiState.update {
                    it.copy(
                        progress = updated,
                        isLoading = false,
                        dailyChallengeStreak = updated.dailyChallengeStreak,
                        vipRewardsMessage = vipMsg,
                        newPlayerBonusMessage = newPlayerMsg
                    )
                }
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

    fun levelForDifficulty(difficulty: Difficulty): Int {
        val progress = _uiState.value.progress
        return when (difficulty) {
            Difficulty.EASY    -> progress.easyLevel
            Difficulty.REGULAR -> progress.regularLevel
            Difficulty.HARD    -> progress.hardLevel
            Difficulty.VIP     -> progress.vipLevel
        }
    }

    fun playButtonClick() {
        audioManager.playSfx(SfxSound.BUTTON_CLICK)
    }
}
