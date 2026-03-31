package com.djtaylor.wordjourney.ui.levelselect

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.billing.IAdManager
import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SeasonalWordPacks
import com.djtaylor.wordjourney.domain.model.seasonalLevelFor
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class LevelSelectUiState(
    val difficulty: Difficulty = Difficulty.REGULAR,
    val currentLevel: Int = 1,        // player's current unsolved level
    val totalLevels: Int = 500,
    val lives: Int = 10,
    val bonusLives: Int = 0,          // lives above 10
    val coins: Long = 0L,
    val diamonds: Int = 5,
    val timerDisplayMs: Long = 0L,
    val isLoading: Boolean = true,
    val showNoLivesDialog: Boolean = false,
    val lifeDeducted: Boolean = false, // triggers heart animation
    val starRatings: Map<Int, Int> = emptyMap(), // level -> stars (1-3)
    val totalStars: Int = 0,
    val journeyTitle: String = "",       // display title: e.g. "Easter Journey 🐣"
    val seasonalPackKey: String? = null, // non-null for seasonal pack screens
    val adIsReady: Boolean = false,      // whether a rewarded ad is loaded and ready
    val adLifeGrantedMessage: String? = null,  // shown briefly after watching an ad for a life
    val seasonalDaysLeft: Int? = null,   // days remaining in the current seasonal event (null if not seasonal)
    val showSeasonInfoDialog: Boolean = false  // show rules/dates info dialog
)

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase,
    private val audioManager: WordJourneysAudioManager,
    private val starRatingDao: StarRatingDao,
    private val adManager: IAdManager,
    private val activityProvider: ActivityProvider
) : ViewModel() {

    private val difficultyKey: String = checkNotNull(savedStateHandle["difficulty"])
    private val isSeasonalLevel: Boolean = difficultyKey.startsWith("seasonal_")
    private val seasonalPackKey: String? = if (isSeasonalLevel) difficultyKey.removePrefix("seasonal_") else null
    private val difficulty: Difficulty = if (isSeasonalLevel) Difficulty.REGULAR
        else Difficulty.entries.first { it.saveKey == difficultyKey }
    private var playerProgress: PlayerProgress = PlayerProgress()

    /** Human-readable title shown in the TopAppBar. */
    private val journeyTitle: String = when (seasonalPackKey) {
        "easter"       -> "Easter Journey 🐣"
        "valentines"   -> "Valentines Journey 💕"
        "summer"       -> "Summer Journey ☀️"
        "halloween"    -> "Halloween Journey 🎃"
        "thanksgiving" -> "Thanksgiving Journey 🦃"
        "christmas"    -> "Christmas Journey 🎄"
        else           -> "${difficulty.displayName} Journey"
    }

    private val _uiState = MutableStateFlow(LevelSelectUiState(
        difficulty = difficulty,
        journeyTitle = journeyTitle,
        seasonalPackKey = seasonalPackKey
    ))
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
        loadStarRatings()
        startTimerTick()
        startAdReadyPoll()
        if (seasonalPackKey != null) {
            _uiState.update { it.copy(seasonalDaysLeft = daysLeftForSeason(seasonalPackKey)) }
        }
    }

    /**
     * Returns the number of calendar days until a seasonal event ends, based on
     * the current year. If the end date has passed this year, reports the end date
     * for next year (so the pack is never shown as "expired").
     */
    private fun daysLeftForSeason(key: String): Int? {
        val (endMonth, endDay) = when (key) {
            "easter"       -> 4 to 20
            "valentines"   -> 2 to 14
            "summer"       -> 8 to 31
            "halloween"    -> 10 to 31
            "thanksgiving" -> 11 to 28
            "christmas"    -> 12 to 31
            else           -> return null
        }
        val today = LocalDate.now()
        var end = today.withMonth(endMonth).withDayOfMonth(endDay)
        if (end.isBefore(today)) end = end.plusYears(1)
        return ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
    }

    /** Poll ad readiness every 2 seconds so the "Watch Ad" button appears as soon as an ad loads. */
    private fun startAdReadyPoll() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(adIsReady = adManager.isRewardedAdReady) }
                delay(2_000L)
            }
        }
    }

    private fun loadStarRatings() {
        viewModelScope.launch {
            val ratings = starRatingDao.getAllForDifficulty(difficultyKey)
            val map = ratings.associate { it.level to it.stars }
            val total = ratings.sumOf { it.stars }
            _uiState.update { it.copy(starRatings = map, totalStars = total) }
        }
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

                val currentLevel = if (isSeasonalLevel)
                    playerProgress.seasonalLevelFor(seasonalPackKey!!)
                else
                    playerProgress.levelFor(difficulty)
                val totalLevels = if (isSeasonalLevel)
                    SeasonalWordPacks.packSize(seasonalPackKey!!)
                else 500
                _uiState.update {
                    it.copy(
                        currentLevel = currentLevel,
                        totalLevels = totalLevels,
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
        val currentLevel = if (isSeasonalLevel) playerProgress.seasonalLevelFor(seasonalPackKey!!)
                           else playerProgress.levelFor(difficulty)
        val isReplay = level < currentLevel
        if (isReplay) return true  // free replay
        return playerProgress.lives > 0
    }

    /**
     * Deduct a life when starting the CURRENT (unsolved) level.
     * Returns true if successful.
     */
    fun deductLifeForLevel(level: Int): Boolean {
        val currentLevel = if (isSeasonalLevel) playerProgress.seasonalLevelFor(seasonalPackKey!!)
                           else playerProgress.levelFor(difficulty)
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

    fun showSeasonInfo() {
        _uiState.update { it.copy(showSeasonInfoDialog = true) }
    }

    fun dismissSeasonInfo() {
        _uiState.update { it.copy(showSeasonInfoDialog = false) }
    }

    /**
     * Watch a rewarded ad to earn 1 bonus life.
     * Uses the current activity from [activityProvider].
     */
    fun watchAdForLife() {
        val activity = activityProvider.currentActivity ?: return
        viewModelScope.launch {
            val result = adManager.showRewardedAd(activity)
            if (result.watched) {
                val updated = playerProgress.copy(lives = playerProgress.lives + 1)
                playerProgress = updated
                playerRepository.saveProgress(updated)
                _uiState.update {
                    it.copy(
                        lives = minOf(updated.lives, 10),
                        bonusLives = maxOf(updated.lives - 10, 0),
                        showNoLivesDialog = false,
                        adLifeGrantedMessage = "You earned a free life! ❤️"
                    )
                }
                // Pre-load next ad
                adManager.loadRewardedAd()
            }
        }
    }

    fun dismissAdLifeMessage() {
        _uiState.update { it.copy(adLifeGrantedMessage = null) }
    }

    fun resetLifeAnimation() {
        _uiState.update { it.copy(lifeDeducted = false) }
    }

    fun playButtonClick() {
        audioManager.playSfx(SfxSound.BUTTON_CLICK)
    }

    private fun PlayerProgress.levelFor(d: Difficulty) = when (d) {
        Difficulty.EASY    -> easyLevel
        Difficulty.REGULAR -> regularLevel
        Difficulty.HARD    -> hardLevel
        Difficulty.VIP     -> vipLevel
    }

    // Expose seasonal info for the UI
    val isSeasonalPack: Boolean get() = isSeasonalLevel
    val seasonPackKey: String? get() = seasonalPackKey
}
