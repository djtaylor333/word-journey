package com.djtaylor.wordjourney.ui.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.auth.AchievementManager
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.data.repository.InboxRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import com.djtaylor.wordjourney.domain.usecase.VipDailyRewardUseCase
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import com.djtaylor.wordjourney.notifications.DailyChallengeReminderWorker
import com.djtaylor.wordjourney.review.InAppReviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val timerDisplayMs: Long = 0L,          // ms until next life
    val isLoading: Boolean = true,
    val dailyChallengeStreak: Int = 0,
    val showVipClaimDialog: Boolean = false, // popup when VIP rewards land in inbox
    val pendingVipDaysMessage: String? = null, // dialog body text
    val newPlayerBonusMessage: String? = null,
    val inboxCount: Int = 0,                // unclaimed inbox items badge
    val nextVipRewardMs: Long = 0L,         // ms until next VIP daily reward available
    val devModeEnabled: Boolean = false,    // unlocked via 10-tap easter egg
    val showReviewPrompt: Boolean = false   // show in-app review prompt after 10 levels
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase,
    private val vipDailyRewardUseCase: VipDailyRewardUseCase,
    private val inboxRepository: InboxRepository,
    private val audioManager: WordJourneysAudioManager,
    private val achievementManager: AchievementManager,
    private val activityProvider: ActivityProvider,
    private val inAppReviewManager: InAppReviewManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Session guard: prevents re-showing the review prompt if flow re-emits after we save. */
    private var reviewPromptShownThisSession = false

    companion object {
        /** 10 minutes of total in-game play time before the review prompt fires. */
        const val REVIEW_TRIGGER_MS = 10L * 60 * 1_000
    }

    init {
        loadProgress()
        startTimerTick()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            // Sync from Play Games cloud save once per session (best-effort, non-blocking).
            // This ensures progress is restored after a reinstall or new device setup.
            try {
                playerRepository.syncFromCloud()
            } catch (_: Exception) {
                // Cloud sync is best-effort — continue with local progress on failure
            }

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

                // ── VIP Daily Rewards → inbox (not applied directly) ────
                var showVipDialog = false
                var vipDialogMsg: String? = null
                if (updated.isVip) {
                    val reward = vipDailyRewardUseCase.calculateRewards(updated.lastVipRewardDate)
                    if (reward != null) {
                        // Use NonCancellable to prevent collectLatest from skipping the inbox
                        // add after saveProgress triggers a new DataStore emission.
                        updated = updated.copy(lastVipRewardDate = reward.updatedLastRewardDate)
                        withContext(NonCancellable) {
                            playerRepository.saveProgress(updated)
                            inboxRepository.addVipDailyRewardIfNeeded(
                                livesGranted = reward.livesGranted,
                                coinsGranted = reward.coinsGranted,
                                addGuessItems = reward.addGuessItemsGranted,
                                removeLetterItems = reward.removeLetterItemsGranted,
                                definitionItems = reward.definitionItemsGranted,
                                showLetterItems = reward.showLetterItemsGranted,
                                daysAccumulated = reward.daysAccumulated
                            )
                        }
                        showVipDialog = true
                        vipDialogMsg = if (reward.daysAccumulated > 1)
                            "You've accumulated ${reward.daysAccumulated} days of VIP rewards! Claim ${reward.livesGranted} lives & ${reward.coinsGranted} coins now."
                        else
                            "Your daily VIP reward is ready — claim ${reward.livesGranted} lives & ${reward.coinsGranted} coins!"
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
                    // Trigger login-streak achievements on each new calendar day
                    activityProvider.currentActivity?.let { activity ->
                        achievementManager.onLoginStreakUpdated(activity, newLoginStreak)
                    }
                }

                // Check whether it's time to show the in-app review prompt.
                // Trigger once per session when the player has >= 10 minutes of total
                // in-game play time and has not already been asked for a review.
                val triggerReview = !reviewPromptShownThisSession &&
                    updated.totalTimePlayedMs >= REVIEW_TRIGGER_MS &&
                    !updated.hasReviewBeenRequested
                if (triggerReview) reviewPromptShownThisSession = true

                val nextVipRewardMs = if (updated.isVip && updated.lastVipRewardDate.isNotBlank()) {
                    val nextMidnight = LocalDate.now().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    (nextMidnight - System.currentTimeMillis()).coerceAtLeast(0L)
                } else 0L

                _uiState.update {
                    it.copy(
                        progress = updated,
                        isLoading = false,
                        dailyChallengeStreak = updated.dailyChallengeStreak,
                        showVipClaimDialog = showVipDialog,
                        pendingVipDaysMessage = vipDialogMsg,
                        newPlayerBonusMessage = newPlayerMsg,
                        inboxCount = inboxRepository.getUnclaimedCount(),
                        devModeEnabled = updated.devModeEnabled,
                        showReviewPrompt = if (triggerReview) true else it.showReviewPrompt,
                        nextVipRewardMs = nextVipRewardMs
                    )
                }

                // Schedule (or cancel) full-lives notification now that we know current lives
                try {
                    LivesFullNotificationWorker.schedule(
                        context              = context,
                        currentLives         = updated.lives,
                        lastRegenTimestamp   = updated.lastLifeRegenTimestamp,
                        notificationsEnabled = updated.notifyLivesFull
                    )
                    // Schedule daily challenge noon reminder
                    DailyChallengeReminderWorker.schedule(
                        context              = context,
                        notificationsEnabled = updated.notifyDailyChallenge
                    )
                } catch (_: Exception) { /* not fatal if WorkManager not initialized in tests */ }
            }
        }
    }

    /** Ticks every second to update the life countdown and VIP reward countdown displays. */
    private fun startTimerTick() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val progress = _uiState.value.progress
                val lifeMs = if (progress.lives < LifeRegenUseCase.TIME_REGEN_CAP) {
                    (lifeRegenUseCase.nextLifeAtMs(progress.lastLifeRegenTimestamp)
                        - System.currentTimeMillis()).coerceAtLeast(0L)
                } else 0L
                val vipMs = if (progress.isVip && progress.lastVipRewardDate.isNotBlank()) {
                    val nextMidnight = LocalDate.now().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    (nextMidnight - System.currentTimeMillis()).coerceAtLeast(0L)
                } else 0L
                _uiState.update { it.copy(timerDisplayMs = lifeMs, nextVipRewardMs = vipMs) }
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

    /** Dismiss the VIP daily reward claim dialog (player chose 'Later'). */
    fun dismissVipClaimDialog() {
        _uiState.update { it.copy(showVipClaimDialog = false) }
    }

    // ── In-App Review ─────────────────────────────────────────────────────────

    /**
     * Called when the review prompt dialog is dismissed without the player engaging
     * with the Play Store review flow (they tapped "Maybe Later").
     * Records that the prompt was shown so it is not repeated.
     */
    fun dismissReviewPrompt() {
        _uiState.update { it.copy(showReviewPrompt = false) }
        viewModelScope.launch {
            val current = _uiState.value.progress
            val updated = current.copy(
                hasReviewBeenRequested = true,
                levelsCompletedForReview = 0
            )
            playerRepository.saveProgress(updated)
        }
    }

    /**
     * Called when the player taps "Rate on Play Store" or "Leave Feedback" in the review dialog.
     * Opens the Play Store listing, drops a reward inbox item (500 coins + 5 lives + 25 gems)
     * so it's waiting when the player returns, and marks the review as completed (one-time only).
     *
     * @param activity The currently resumed Activity.
     */
    fun completeReviewWithReward(activity: Activity) {
        _uiState.update { it.copy(showReviewPrompt = false) }
        viewModelScope.launch {
            // Open Play Store so the player can leave a review / feedback
            inAppReviewManager.openPlayStoreListing(activity)

            // Add reward to inbox — player collects it when they return to the game
            inboxRepository.addItem(
                com.djtaylor.wordjourney.data.db.InboxItemEntity(
                    type           = "review_reward",
                    title          = "⭐ Thanks for the Review!",
                    message        = "You earned 500 coins, 5 lives and 25 gems for supporting Word Journeys!",
                    coinsGranted   = 500L,
                    livesGranted   = 5,
                    diamondsGranted = 25
                )
            )

            // Mark review as completed — reward can only be earned once
            val current = playerRepository.playerProgressFlow.first()
            val updated = current.copy(
                hasReviewBeenRequested = true,
                reviewRewarded = true,
                levelsCompletedForReview = 0
            )
            playerRepository.saveProgress(updated)
            audioManager.playSfx(SfxSound.COIN_EARN)

            // Refresh inbox badge so the reward notification dot appears immediately
            _uiState.update { it.copy(inboxCount = inboxRepository.getUnclaimedCount()) }
        }
    }

    // ── Dev Mode actions ──────────────────────────────────────────────────────

    /** [DEV] Immediately fires the lives-full notification for testing. */
    fun devTriggerLivesFullNotification() {
        try {
            LivesFullNotificationWorker.devDirectFire(context)
        } catch (_: Exception) {}
    }

    /** [DEV] Immediately fires the daily challenge reminder notification. */
    fun devTriggerDailyChallengeNotification() {
        try {
            DailyChallengeReminderWorker.devDirectFire(
                context,
                streak = _uiState.value.dailyChallengeStreak
            )
        } catch (_: Exception) {}
    }

    /** [DEV] Force-inserts a VIP daily reward into the inbox to test the claim popup. */
    fun devTriggerVipDailyReward() {
        viewModelScope.launch {
            inboxRepository.addVipDailyRewardIfNeeded(
                livesGranted = 2, coinsGranted = 167L,
                addGuessItems = 2, removeLetterItems = 1,
                definitionItems = 1, showLetterItems = 1,
                daysAccumulated = 1
            )
            _uiState.update {
                it.copy(
                    showVipClaimDialog = true,
                    pendingVipDaysMessage = "Your daily VIP reward is ready — claim 2 lives & 167 coins! (DEV TEST)",
                    inboxCount = inboxRepository.getUnclaimedCount()
                )
            }
        }
    }

    // ── Achievements ──────────────────────────────────────────────────────────

    /**
     * Opens the Google Play Games achievements overlay.
     * Calls [onIntent] with the intent to launch; the caller is responsible for
     * starting the activity (use [Activity.startActivityForResult] or
     * [ActivityResultLauncher]).
     */
    fun showAchievements(activity: Activity, onIntent: (Intent) -> Unit) {
        playButtonClick()
        achievementManager.showAchievementsIntent(activity, onIntent)
    }
}
