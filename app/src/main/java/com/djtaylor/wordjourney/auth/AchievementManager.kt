package com.djtaylor.wordjourney.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
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
     * Requires an Activity context to access the GamesSignInClient.
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

    // ── Helpers — call from GameViewModel after each win ─────────────────────

    /**
     * Evaluate and trigger all relevant achievements after a puzzle win.
     *
     * @param activity      Current foreground activity (for API calls).
     * @param totalWins     Cumulative wins across all modes.
     * @param guessCount    Number of guesses used this game.
     * @param usedPowerUp   Whether any power-up was used this game.
     * @param currentLevel  The level just completed (regular packs only).
     * @param dailyStreak   Current consecutive daily-challenge streak.
     * @param totalDailyWins Total daily challenges completed.
     */
    fun onPuzzleWon(
        activity: Activity,
        totalWins: Int,
        guessCount: Int,
        usedPowerUp: Boolean = false,
        currentLevel: Int = 0,
        dailyStreak: Int = 0,
        totalDailyWins: Int = 0,
        isDaily: Boolean = false
    ) {
        // First win
        if (totalWins == 1) unlock(activity, AchievementIds.FIRST_WIN)

        // Win count milestones (incremental)
        setSteps(activity, AchievementIds.WIN_10,  totalWins)
        setSteps(activity, AchievementIds.WIN_50,  totalWins)
        setSteps(activity, AchievementIds.WIN_100, totalWins)
        setSteps(activity, AchievementIds.WIN_500, totalWins)

        // Skill-based
        if (guessCount == 1) unlock(activity, AchievementIds.FIRST_GUESS_WIN)
        if (guessCount <= 2) unlock(activity, AchievementIds.TWO_GUESS_WIN)
        if (!usedPowerUp)   unlock(activity, AchievementIds.NO_POWERUP_WIN)

        // Level progress
        if (currentLevel >= 10)  unlock(activity, AchievementIds.REACH_LEVEL_10)
        if (currentLevel >= 50)  unlock(activity, AchievementIds.REACH_LEVEL_50)
        if (currentLevel >= 100) unlock(activity, AchievementIds.PACK_MASTER)

        // Daily challenges
        if (isDaily) {
            if (totalDailyWins == 1) unlock(activity, AchievementIds.FIRST_DAILY)
            setSteps(activity, AchievementIds.DAILY_100, totalDailyWins)
        }

        // Daily streak
        if (dailyStreak >= 3)  unlock(activity, AchievementIds.STREAK_3)
        if (dailyStreak >= 7)  unlock(activity, AchievementIds.STREAK_7)
        if (dailyStreak >= 30) unlock(activity, AchievementIds.STREAK_30)
    }
}
