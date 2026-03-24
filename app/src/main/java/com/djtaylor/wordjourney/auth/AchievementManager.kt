package com.djtaylor.wordjourney.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.djtaylor.wordjourney.billing.ProductIds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Play Games AchievementsClient.
 *
 * All methods silently no-op if the player is not signed in or Play Games is unavailable.
 * Achievements are identified by the IDs in [AchievementIds].
 *
 * Incremental achievements require a "currentSteps" count — this manager always sends
 * the player's cumulative total; the SDK de-duplicates and only fires the achievement
 * unlock when the required number of steps is first reached.
 *
 * IMPORTANT: The IDs in [AchievementIds] must be replaced with real IDs from
 * Play Console → Play Games Services → Achievements after running the setup script
 * (scripts/setup_play_achievements.py).
 */
@Singleton
class AchievementManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AchievementManager"
    }

    // ── Unlock (one-shot achievements) ────────────────────────────────────────

    /**
     * Unlock a one-shot achievement by its Play Games ID.
     * Silently skips placeholder IDs (those that start with "achievement_").
     */
    fun unlock(activity: Activity, achievementId: String) {
        if (achievementId.startsWith("achievement_")) {
            Log.w(TAG, "Achievement ID '$achievementId' looks like a placeholder. " +
                "Replace with the real Play Console ID.")
            return
        }
        try {
            PlayGames.getAchievementsClient(activity).unlock(achievementId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unlock achievement $achievementId: ${e.message}")
        }
    }

    /**
     * Increment a multi-step achievement. Pass the player's *total* cumulative count;
     * the SDK internally tracks steps and unlocks when total >= required steps.
     */
    fun setSteps(activity: Activity, achievementId: String, stepsTotal: Int) {
        if (achievementId.startsWith("achievement_")) {
            Log.w(TAG, "Achievement ID '$achievementId' looks like a placeholder.")
            return
        }
        try {
            PlayGames.getAchievementsClient(activity).setSteps(achievementId, stepsTotal)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set steps for $achievementId: ${e.message}")
        }
    }

    /**
     * Open the Play Games achievements UI overlay.
     */
    fun showAchievementsIntent(activity: Activity, onIntent: (android.content.Intent) -> Unit) {
        try {
            PlayGames.getAchievementsClient(activity)
                .achievementsIntent
                .addOnSuccessListener { intent -> onIntent(intent) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to get achievements intent: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "showAchievementsIntent failed: ${e.message}")
        }
    }

    // ── Primary game-flow trigger — call after each puzzle win ────────────────

    /**
     * Evaluate and trigger all relevant achievements after a puzzle win.
     *
     * @param activity            Current foreground activity (for API calls).
     * @param totalWins           Cumulative wins across all modes.
     * @param guessCount          Number of guesses used this round.
     * @param maxGuessesAllowed   Total guesses the player was allowed (default 6).
     * @param usedPowerUp         Whether any power-up was used this game.
     * @param currentLevel        The level just completed (regular packs only; 0 for daily).
     * @param dailyStreak         Current consecutive daily-challenge streak.
     * @param loginStreak         Current consecutive login streak.
     * @param totalDailyWins      Total daily challenges ever completed.
     * @param isDaily             Whether this was a daily challenge win.
     * @param isSeasonal          Whether this was a seasonal pack level win.
     */
    fun onPuzzleWon(
        activity: Activity,
        totalWins: Int,
        guessCount: Int,
        maxGuessesAllowed: Int = 6,
        usedPowerUp: Boolean = false,
        currentLevel: Int = 0,
        dailyStreak: Int = 0,
        loginStreak: Int = 0,
        totalDailyWins: Int = 0,
        isDaily: Boolean = false,
        isSeasonal: Boolean = false
    ) {
        // ── First win ────────────────────────────────────────────────────────
        // Always call unlock (idempotent) — fires even for players who won before
        // achievements were deployed (their totalWins already exceeded 1 at launch).
        if (totalWins >= 1) unlock(activity, AchievementIds.FIRST_WIN)

        // ── Win count milestones (incremental) ───────────────────────────────
        setSteps(activity, AchievementIds.WIN_10,  totalWins)
        setSteps(activity, AchievementIds.WIN_50,  totalWins)
        setSteps(activity, AchievementIds.WIN_100, totalWins)
        setSteps(activity, AchievementIds.WIN_250, totalWins)
        setSteps(activity, AchievementIds.WIN_500, totalWins)

        // ── Skill ────────────────────────────────────────────────────────────
        if (guessCount == 1)                                    unlock(activity, AchievementIds.FIRST_GUESS_WIN)
        if (guessCount <= 2)                                    unlock(activity, AchievementIds.TWO_GUESS_WIN)
        if (!usedPowerUp)                                       unlock(activity, AchievementIds.NO_POWERUP_WIN)
        if (maxGuessesAllowed > 0 && guessCount == maxGuessesAllowed)
                                                                unlock(activity, AchievementIds.LAST_GUESS_WIN)

        // ── Level pack progress (regular + seasonal both count) ───────────────
        if (currentLevel >= 10)  unlock(activity, AchievementIds.REACH_LEVEL_10)
        if (currentLevel >= 25)  unlock(activity, AchievementIds.REACH_LEVEL_25)
        if (currentLevel >= 50)  unlock(activity, AchievementIds.REACH_LEVEL_50)
        if (currentLevel >= 100) unlock(activity, AchievementIds.PACK_MASTER)

        // ── Seasonal pack ────────────────────────────────────────────────────
        if (isSeasonal) unlock(activity, AchievementIds.SEASONAL_CHAMPION)

        // ── Daily challenges ─────────────────────────────────────────────────
        if (isDaily) {
            if (totalDailyWins == 1) unlock(activity, AchievementIds.FIRST_DAILY)
            setSteps(activity, AchievementIds.DAILY_10,  totalDailyWins)
            setSteps(activity, AchievementIds.DAILY_100, totalDailyWins)
        }

        // ── Daily challenge streak (incremental — shows progress e.g. "4/7") ──────
        // setSteps auto-unlocks when steps reach the achievement's configured max.
        // Guard: only call setSteps when streak > 0 (API requires steps >= 1).
        if (dailyStreak > 0) {
            setSteps(activity, AchievementIds.STREAK_3,  dailyStreak)
            setSteps(activity, AchievementIds.STREAK_7,  dailyStreak)
            setSteps(activity, AchievementIds.STREAK_14, dailyStreak)
            setSteps(activity, AchievementIds.STREAK_30, dailyStreak)
        }

        // ── Login streak (incremental — shows progress e.g. "5/7") ────────────
        if (loginStreak > 0) {
            setSteps(activity, AchievementIds.LOGIN_STREAK_7,  loginStreak)
            setSteps(activity, AchievementIds.LOGIN_STREAK_30, loginStreak)
        }
    }

    // ── Login streak trigger — call from HomeViewModel on each new login day ───

    /**
     * Evaluate login-streak achievements.
     * Call this from HomeViewModel whenever [loginStreak] is updated (i.e. on each
     * new calendar day the player opens the app), so progress is reflected even
     * on days when the player does not complete a puzzle.
     *
     * @param activity     Current foreground activity.
     * @param loginStreak  Updated consecutive login streak count (already incremented).
     */
    fun onLoginStreakUpdated(activity: Activity, loginStreak: Int) {
        if (loginStreak > 0) {
            setSteps(activity, AchievementIds.LOGIN_STREAK_7,  loginStreak)
            setSteps(activity, AchievementIds.LOGIN_STREAK_30, loginStreak)
        }
    }

    // ── Item usage trigger — call after any power-up item is used ─────────────

    /**
     * Evaluate item-usage achievements.
     *
     * @param activity        Current foreground activity.
     * @param totalItemsUsed  Updated cumulative item-usage count (after this use).
     */
    fun onItemUsed(activity: Activity, totalItemsUsed: Int) {
        if (totalItemsUsed == 1) unlock(activity, AchievementIds.FIRST_ITEM_USED)
        setSteps(activity, AchievementIds.ITEMS_USED_50, totalItemsUsed)
    }

    // ── Economy trigger — call whenever total coins earned changes ─────────────

    /**
     * Evaluate coin-earning achievements.
     *
     * @param activity          Current foreground activity.
     * @param totalCoinsEarned  Updated cumulative coins earned (lifetime).
     */
    fun onCoinsEarned(activity: Activity, totalCoinsEarned: Long) {
        setSteps(activity, AchievementIds.COIN_EARNER_10000, totalCoinsEarned.coerceAtMost(10_000L).toInt())
    }

    // ── Ad-watching trigger ────────────────────────────────────────────────────

    /**
     * Unlock achievement for watching the first rewarded ad.
     */
    fun onAdWatched(activity: Activity) {
        unlock(activity, AchievementIds.FIRST_AD_WATCHED)
    }

    // ── Store purchase triggers ────────────────────────────────────────────────

    /**
     * Evaluate purchase-related achievements.
     *
     * @param activity     Current foreground activity.
     * @param productId    The product that was just purchased.
     * @param isFirstEverPurchase  True if the player has never completed a real purchase before.
     */
    fun onPurchaseCompleted(activity: Activity, productId: String, isFirstEverPurchase: Boolean) {
        if (isFirstEverPurchase) unlock(activity, AchievementIds.FIRST_PURCHASE)

        val isBundlePurchase = productId in setOf(
            ProductIds.STARTER_BUNDLE,
            ProductIds.ADVENTURER_BUNDLE,
            ProductIds.CHAMPION_BUNDLE
        )
        if (isBundlePurchase) unlock(activity, AchievementIds.BUNDLE_BUYER)
    }

    /**
     * Unlock VIP achievement when any VIP subscription is activated.
     */
    fun onVipPurchased(activity: Activity) {
        unlock(activity, AchievementIds.VIP_SUBSCRIBER)
    }
}
