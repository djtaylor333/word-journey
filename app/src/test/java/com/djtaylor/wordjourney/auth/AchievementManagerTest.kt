package com.djtaylor.wordjourney.auth

import android.app.Activity
import android.content.Context
import com.djtaylor.wordjourney.billing.ProductIds
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for [AchievementManager].
 *
 * Strategy: create a spy of [AchievementManager] and stub out the low-level [unlock] /
 * [setSteps] primitives so they record calls without touching Play Games SDK.
 * Each test then verifies the correct primitive was called with the correct achievement ID.
 *
 * Key invariants tested:
 *  - FIRST_WIN: fires via unlock() on every win (idempotent — already-unlocked is a no-op in SDK)
 *  - Streak achievements (daily + login): use setSteps() so progress is shown (e.g. "4/7 days")
 *    rather than simply "Locked" until threshold is reached.
 *  - setSteps() is NOT called when streak == 0 (API requires steps >= 1).
 *  - onLoginStreakUpdated(): standalone method called from HomeViewModel on each new login day.
 */
class AchievementManagerTest {

    private lateinit var manager: AchievementManager
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        manager = spyk(AchievementManager(context))
        activity = mockk(relaxed = true)

        // Stub primitives so we record calls without invoking real Play Games SDK
        every { manager.unlock(any(), any()) } just Runs
        every { manager.setSteps(any(), any(), any()) } just Runs
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Placeholder guard — low-level helpers
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `unlock skips IDs that start with achievement_`() {
        // Not using spy here — test the real method directly
        val realManager = AchievementManager(mockk(relaxed = true))
        // Should not throw; the guard short-circuits before calling PlayGames
        realManager.unlock(activity, "achievement_placeholder")
    }

    @Test
    fun `setSteps skips IDs that start with achievement_`() {
        val realManager = AchievementManager(mockk(relaxed = true))
        realManager.setSteps(activity, "achievement_placeholder", 50)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. onPuzzleWon — FIRST_WIN
    // Fired via unlock() on ALL wins (idempotent) so players who had wins
    // before achievements were deployed still unlock it retroactively.
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon totalWins=1 unlocks FIRST_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 1, guessCount = 3)
        verify { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    @Test
    fun `onPuzzleWon totalWins=2 still calls unlock FIRST_WIN (idempotent, SDK no-ops if already unlocked)`() {
        manager.onPuzzleWon(activity = activity, totalWins = 2, guessCount = 3)
        verify { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    @Test
    fun `onPuzzleWon totalWins=100 still calls unlock FIRST_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 100, guessCount = 3)
        verify { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    @Test
    fun `onPuzzleWon totalWins=0 does NOT call unlock FIRST_WIN`() {
        // No wins yet — should not fire
        manager.onPuzzleWon(activity = activity, totalWins = 0, guessCount = 3)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. onPuzzleWon — win-count milestones (incremental)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon sets steps for all five win-count milestone achievements`() {
        manager.onPuzzleWon(activity = activity, totalWins = 75, guessCount = 3)
        verify { manager.setSteps(activity, AchievementIds.WIN_10,  75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_50,  75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_100, 75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_250, 75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_500, 75) }
    }

    @Test
    fun `onPuzzleWon totalWins=1 sets steps=1 for all win-count milestones (shows progress)`() {
        manager.onPuzzleWon(activity = activity, totalWins = 1, guessCount = 3)
        verify { manager.setSteps(activity, AchievementIds.WIN_10,  1) }
        verify { manager.setSteps(activity, AchievementIds.WIN_50,  1) }
        verify { manager.setSteps(activity, AchievementIds.WIN_100, 1) }
        verify { manager.setSteps(activity, AchievementIds.WIN_250, 1) }
        verify { manager.setSteps(activity, AchievementIds.WIN_500, 1) }
    }

    @Test
    fun `onPuzzleWon totalWins=500 sets steps=500 unlocking WIN_500`() {
        manager.onPuzzleWon(activity = activity, totalWins = 500, guessCount = 3)
        verify { manager.setSteps(activity, AchievementIds.WIN_500, 500) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. onPuzzleWon — skill achievements
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon guessCount=1 unlocks FIRST_GUESS_WIN and TWO_GUESS_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 1)
        verify { manager.unlock(activity, AchievementIds.FIRST_GUESS_WIN) }
        verify { manager.unlock(activity, AchievementIds.TWO_GUESS_WIN) }
    }

    @Test
    fun `onPuzzleWon guessCount=2 unlocks TWO_GUESS_WIN but not FIRST_GUESS_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 2)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_GUESS_WIN) }
        verify { manager.unlock(activity, AchievementIds.TWO_GUESS_WIN) }
    }

    @Test
    fun `onPuzzleWon guessCount=3 does not unlock FIRST_GUESS_WIN or TWO_GUESS_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_GUESS_WIN) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.TWO_GUESS_WIN) }
    }

    @Test
    fun `onPuzzleWon no power-up unlocks NO_POWERUP_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, usedPowerUp = false)
        verify { manager.unlock(activity, AchievementIds.NO_POWERUP_WIN) }
    }

    @Test
    fun `onPuzzleWon used power-up does NOT unlock NO_POWERUP_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, usedPowerUp = true)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.NO_POWERUP_WIN) }
    }

    @Test
    fun `onPuzzleWon last-guess win unlocks LAST_GUESS_WIN`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 5,
            guessCount = 6, maxGuessesAllowed = 6
        )
        verify { manager.unlock(activity, AchievementIds.LAST_GUESS_WIN) }
    }

    @Test
    fun `onPuzzleWon non-last-guess win does NOT unlock LAST_GUESS_WIN`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 5,
            guessCount = 4, maxGuessesAllowed = 6
        )
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LAST_GUESS_WIN) }
    }

    @Test
    fun `onPuzzleWon maxGuessesAllowed=0 does NOT unlock LAST_GUESS_WIN`() {
        // Guard: maxGuessesAllowed = 0 prevents false positives
        manager.onPuzzleWon(
            activity = activity, totalWins = 5,
            guessCount = 0, maxGuessesAllowed = 0
        )
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LAST_GUESS_WIN) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. onPuzzleWon — level progress
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon level=10 unlocks REACH_LEVEL_10 only`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 10)
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_25) }
    }

    @Test
    fun `onPuzzleWon level=25 unlocks REACH_LEVEL_10 and REACH_LEVEL_25`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 25)
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_25) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_50) }
    }

    @Test
    fun `onPuzzleWon level=50 unlocks up to REACH_LEVEL_50 but not PACK_MASTER`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 50)
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_25) }
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_50) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.PACK_MASTER) }
    }

    @Test
    fun `onPuzzleWon level=100 unlocks all level achievements including PACK_MASTER`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 100)
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_25) }
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_50) }
        verify { manager.unlock(activity, AchievementIds.PACK_MASTER) }
    }

    @Test
    fun `onPuzzleWon level=5 does not unlock any level achievement`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 5)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_25) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_50) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.PACK_MASTER) }
    }

    @Test
    fun `onPuzzleWon level=0 (daily or seasonal default) does not unlock level achievements`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 0)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. onPuzzleWon — seasonal pack
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon isSeasonal=true unlocks SEASONAL_CHAMPION`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, isSeasonal = true)
        verify { manager.unlock(activity, AchievementIds.SEASONAL_CHAMPION) }
    }

    @Test
    fun `onPuzzleWon isSeasonal=false does NOT unlock SEASONAL_CHAMPION`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, isSeasonal = false)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.SEASONAL_CHAMPION) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. onPuzzleWon — daily challenges
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon first daily win unlocks FIRST_DAILY`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 1, guessCount = 3,
            isDaily = true, totalDailyWins = 1
        )
        verify { manager.unlock(activity, AchievementIds.FIRST_DAILY) }
    }

    @Test
    fun `onPuzzleWon second daily win does not re-unlock FIRST_DAILY`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 2, guessCount = 3,
            isDaily = true, totalDailyWins = 2
        )
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_DAILY) }
    }

    @Test
    fun `onPuzzleWon daily sets steps for DAILY_10 and DAILY_100`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 12, guessCount = 3,
            isDaily = true, totalDailyWins = 12
        )
        verify { manager.setSteps(activity, AchievementIds.DAILY_10,  12) }
        verify { manager.setSteps(activity, AchievementIds.DAILY_100, 12) }
    }

    @Test
    fun `onPuzzleWon daily win 1 shows progress on DAILY_10 (1 of 10 steps)`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 1, guessCount = 3,
            isDaily = true, totalDailyWins = 1
        )
        verify { manager.setSteps(activity, AchievementIds.DAILY_10, 1) }
    }

    @Test
    fun `onPuzzleWon non-daily does NOT set DAILY steps`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 5, guessCount = 3,
            isDaily = false, totalDailyWins = 5
        )
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.DAILY_10,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.DAILY_100, any()) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. onPuzzleWon — daily streak (FIXED: setSteps shows progress, not unlock)
    //
    // BEFORE FIX: used unlock() which only fired at exact thresholds.
    //             Players saw "Locked" with no progress indicator.
    // AFTER FIX:  uses setSteps(dailyStreak) so players see e.g. "4/7 days"
    //             and the achievement auto-unlocks when steps reach the max.
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon streak=1 calls setSteps for all streak achievements — shows progress`() {
        manager.onPuzzleWon(activity = activity, totalWins = 1, guessCount = 3, dailyStreak = 1)
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  1) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  1) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 1) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 1) }
    }

    @Test
    fun `onPuzzleWon streak=2 calls setSteps showing 2-of-3 progress on STREAK_3`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 2)
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  2) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  2) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 2) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 2) }
        // Must NOT use the old unlock() path for streak achievements
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_3) }
    }

    @Test
    fun `onPuzzleWon streak=3 calls setSteps with 3 — SDK auto-unlocks STREAK_3`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 3)
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  3) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  3) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 3) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 3) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_3) }
    }

    @Test
    fun `onPuzzleWon streak=4 shows partial progress between STREAK_3 and STREAK_7`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 4)
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  4) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  4) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 4) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 4) }
    }

    @Test
    fun `onPuzzleWon streak=7 calls setSteps with 7 — SDK auto-unlocks STREAK_7`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 7)
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 7) }
    }

    @Test
    fun `onPuzzleWon streak=14 calls setSteps with 14 — SDK auto-unlocks STREAK_14`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 14)
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 14) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 14) }
    }

    @Test
    fun `onPuzzleWon streak=30 calls setSteps with 30 — SDK auto-unlocks STREAK_30`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 30)
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 30) }
    }

    @Test
    fun `onPuzzleWon streak=0 does NOT call setSteps for any streak achievement`() {
        // Guard: setSteps requires >= 1 step; skip when streak is 0
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 0)
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.STREAK_3,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.STREAK_7,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.STREAK_14, any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.STREAK_30, any()) }
    }

    @Test
    fun `streak achievements never use unlock() — only setSteps()`() {
        // Confirms the old unlock() bug is fully removed from the daily streak path
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 30)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_3) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_14) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_30) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. onPuzzleWon — login streak (FIXED: setSteps shows progress)
    //
    // BEFORE FIX: used unlock() — showed "Locked" with no progress indicator.
    // AFTER FIX:  uses setSteps(loginStreak) so players see e.g. "5/7 days".
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon loginStreak=1 calls setSteps showing early progress`() {
        manager.onPuzzleWon(activity = activity, totalWins = 1, guessCount = 3, loginStreak = 1)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  1) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 1) }
    }

    @Test
    fun `onPuzzleWon loginStreak=5 shows 5-of-7 partial progress`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 5)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  5) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 5) }
        // Must NOT use old unlock() path
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
    }

    @Test
    fun `onPuzzleWon loginStreak=7 calls setSteps with 7 — SDK auto-unlocks LOGIN_STREAK_7`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 7)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  7) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
    }

    @Test
    fun `onPuzzleWon loginStreak=30 calls setSteps with 30 — SDK auto-unlocks both login achievements`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 30)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  30) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 30) }
    }

    @Test
    fun `onPuzzleWon loginStreak=0 does NOT call setSteps for login streak achievements`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 0)
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, any()) }
    }

    @Test
    fun `login streak achievements never use unlock() — only setSteps()`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 30)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_30) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. onLoginStreakUpdated — standalone trigger called from HomeViewModel
    //     (NEW) Called each calendar day the player opens the app, so login
    //     streak progress is reflected even without winning a puzzle.
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onLoginStreakUpdated streak=1 calls setSteps for both login achievements`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 1)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  1) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 1) }
    }

    @Test
    fun `onLoginStreakUpdated streak=5 shows 5-of-7 progress`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 5)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  5) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 5) }
    }

    @Test
    fun `onLoginStreakUpdated streak=7 auto-unlocks LOGIN_STREAK_7 via SDK`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 7)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  7) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 7) }
    }

    @Test
    fun `onLoginStreakUpdated streak=30 auto-unlocks both login achievements`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 30)
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  30) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 30) }
    }

    @Test
    fun `onLoginStreakUpdated streak=0 does NOT call setSteps`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 0)
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, any()) }
    }

    @Test
    fun `onLoginStreakUpdated does not use unlock() — setSteps only`() {
        manager.onLoginStreakUpdated(activity, loginStreak = 30)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_30) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. onItemUsed
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onItemUsed totalItemsUsed=1 unlocks FIRST_ITEM_USED`() {
        manager.onItemUsed(activity, totalItemsUsed = 1)
        verify { manager.unlock(activity, AchievementIds.FIRST_ITEM_USED) }
    }

    @Test
    fun `onItemUsed totalItemsUsed=2 does NOT re-unlock FIRST_ITEM_USED`() {
        manager.onItemUsed(activity, totalItemsUsed = 2)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_ITEM_USED) }
    }

    @Test
    fun `onItemUsed always sets ITEMS_USED_50 steps to show progress`() {
        manager.onItemUsed(activity, totalItemsUsed = 25)
        verify { manager.setSteps(activity, AchievementIds.ITEMS_USED_50, 25) }
    }

    @Test
    fun `onItemUsed totalItemsUsed=50 sets max steps for ITEMS_USED_50`() {
        manager.onItemUsed(activity, totalItemsUsed = 50)
        verify { manager.setSteps(activity, AchievementIds.ITEMS_USED_50, 50) }
    }

    @Test
    fun `onItemUsed totalItemsUsed=1 also sets step 1 on ITEMS_USED_50`() {
        manager.onItemUsed(activity, totalItemsUsed = 1)
        verify { manager.setSteps(activity, AchievementIds.ITEMS_USED_50, 1) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. onCoinsEarned
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onCoinsEarned sets COIN_EARNER_10000 steps`() {
        manager.onCoinsEarned(activity, totalCoinsEarned = 5000L)
        verify { manager.setSteps(activity, AchievementIds.COIN_EARNER_10000, 5000) }
    }

    @Test
    fun `onCoinsEarned caps steps at 10000`() {
        manager.onCoinsEarned(activity, totalCoinsEarned = 99_999L)
        verify { manager.setSteps(activity, AchievementIds.COIN_EARNER_10000, 10_000) }
    }

    @Test
    fun `onCoinsEarned with exactly 10000 coins sets max steps`() {
        manager.onCoinsEarned(activity, totalCoinsEarned = 10_000L)
        verify { manager.setSteps(activity, AchievementIds.COIN_EARNER_10000, 10_000) }
    }

    @Test
    fun `onCoinsEarned with small amount shows early progress`() {
        manager.onCoinsEarned(activity, totalCoinsEarned = 500L)
        verify { manager.setSteps(activity, AchievementIds.COIN_EARNER_10000, 500) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 13. onAdWatched
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onAdWatched unlocks FIRST_AD_WATCHED`() {
        manager.onAdWatched(activity)
        verify { manager.unlock(activity, AchievementIds.FIRST_AD_WATCHED) }
    }

    @Test
    fun `onAdWatched calling twice only calls unlock — idempotent in SDK`() {
        manager.onAdWatched(activity)
        manager.onAdWatched(activity)
        verify(exactly = 2) { manager.unlock(activity, AchievementIds.FIRST_AD_WATCHED) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 14. onPurchaseCompleted
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPurchaseCompleted isFirstEverPurchase=true unlocks FIRST_PURCHASE`() {
        manager.onPurchaseCompleted(activity, ProductIds.COINS_500, isFirstEverPurchase = true)
        verify { manager.unlock(activity, AchievementIds.FIRST_PURCHASE) }
    }

    @Test
    fun `onPurchaseCompleted isFirstEverPurchase=false does NOT unlock FIRST_PURCHASE`() {
        manager.onPurchaseCompleted(activity, ProductIds.COINS_500, isFirstEverPurchase = false)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_PURCHASE) }
    }

    @Test
    fun `onPurchaseCompleted STARTER_BUNDLE unlocks BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.STARTER_BUNDLE, isFirstEverPurchase = false)
        verify { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted ADVENTURER_BUNDLE unlocks BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.ADVENTURER_BUNDLE, isFirstEverPurchase = false)
        verify { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted CHAMPION_BUNDLE unlocks BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.CHAMPION_BUNDLE, isFirstEverPurchase = false)
        verify { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted non-bundle does NOT unlock BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.COINS_1500, isFirstEverPurchase = false)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted coins purchase does NOT unlock BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.DIAMONDS_50, isFirstEverPurchase = false)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted VIP subscription does NOT unlock BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.VIP_MONTHLY, isFirstEverPurchase = true)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    @Test
    fun `onPurchaseCompleted first-ever bundle purchase unlocks both FIRST_PURCHASE and BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.STARTER_BUNDLE, isFirstEverPurchase = true)
        verify { manager.unlock(activity, AchievementIds.FIRST_PURCHASE) }
        verify { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 15. onVipPurchased
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onVipPurchased unlocks VIP_SUBSCRIBER`() {
        manager.onVipPurchased(activity)
        verify { manager.unlock(activity, AchievementIds.VIP_SUBSCRIBER) }
    }

    @Test
    fun `onVipPurchased only unlocks VIP_SUBSCRIBER, not other achievements`() {
        manager.onVipPurchased(activity)
        verify(exactly = 1) { manager.unlock(any(), any()) }
        verify { manager.unlock(activity, AchievementIds.VIP_SUBSCRIBER) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 16. Combined scenario — first daily puzzle win
    //     Exercises all achievement pathways simultaneously
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `first daily win fires FIRST_WIN, FIRST_DAILY, daily steps, and STREAK_1 progress`() {
        manager.onPuzzleWon(
            activity          = activity,
            totalWins         = 1,
            guessCount        = 3,
            maxGuessesAllowed = 6,
            usedPowerUp       = false,
            currentLevel      = 0,
            dailyStreak       = 1,
            loginStreak       = 1,
            totalDailyWins    = 1,
            isDaily           = true,
            isSeasonal        = false
        )
        verify { manager.unlock(activity, AchievementIds.FIRST_WIN) }
        verify { manager.unlock(activity, AchievementIds.FIRST_DAILY) }
        verify { manager.unlock(activity, AchievementIds.NO_POWERUP_WIN) }
        verify { manager.setSteps(activity, AchievementIds.WIN_10,  1) }
        verify { manager.setSteps(activity, AchievementIds.DAILY_10, 1) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  1) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  1) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  1) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 1) }
        // Level achievements should NOT fire (currentLevel = 0)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.REACH_LEVEL_10) }
        // Seasonal should NOT fire
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.SEASONAL_CHAMPION) }
    }

    @Test
    fun `7-day daily streak win unlocks STREAK_7 and shows full progress on all streaks`() {
        manager.onPuzzleWon(
            activity      = activity,
            totalWins     = 20,
            guessCount    = 4,
            dailyStreak   = 7,
            loginStreak   = 7,
            isDaily       = true,
            totalDailyWins = 7
        )
        verify { manager.setSteps(activity, AchievementIds.STREAK_3,  7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_7,  7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_14, 7) }
        verify { manager.setSteps(activity, AchievementIds.STREAK_30, 7) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_7,  7) }
        verify { manager.setSteps(activity, AchievementIds.LOGIN_STREAK_30, 7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_3) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_7) }
    }
}
