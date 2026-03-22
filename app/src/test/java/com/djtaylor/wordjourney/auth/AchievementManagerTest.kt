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
    // 2. onPuzzleWon — first win
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon totalWins=1 unlocks FIRST_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 1, guessCount = 3)
        verify { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    @Test
    fun `onPuzzleWon totalWins=2 does NOT re-unlock FIRST_WIN`() {
        manager.onPuzzleWon(activity = activity, totalWins = 2, guessCount = 3)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.FIRST_WIN) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. onPuzzleWon — win-count milestones (incremental)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon sets steps for all win-count milestones`() {
        manager.onPuzzleWon(activity = activity, totalWins = 75, guessCount = 3)
        verify { manager.setSteps(activity, AchievementIds.WIN_10,  75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_50,  75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_100, 75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_250, 75) }
        verify { manager.setSteps(activity, AchievementIds.WIN_500, 75) }
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
    fun `onPuzzleWon level=50 unlocks up to REACH_LEVEL_50`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 50)
        verify { manager.unlock(activity, AchievementIds.REACH_LEVEL_50) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.PACK_MASTER) }
    }

    @Test
    fun `onPuzzleWon level=100 unlocks PACK_MASTER`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, currentLevel = 100)
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
    fun `onPuzzleWon non-daily does NOT set DAILY steps`() {
        manager.onPuzzleWon(
            activity = activity, totalWins = 5, guessCount = 3,
            isDaily = false, totalDailyWins = 5
        )
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.DAILY_10,  any()) }
        verify(exactly = 0) { manager.setSteps(activity, AchievementIds.DAILY_100, any()) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. onPuzzleWon — daily streak
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon streak=3 unlocks STREAK_3 only`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 3)
        verify { manager.unlock(activity, AchievementIds.STREAK_3) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_7) }
    }

    @Test
    fun `onPuzzleWon streak=7 unlocks STREAK_3 and STREAK_7`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 7)
        verify { manager.unlock(activity, AchievementIds.STREAK_3) }
        verify { manager.unlock(activity, AchievementIds.STREAK_7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_14) }
    }

    @Test
    fun `onPuzzleWon streak=14 unlocks up to STREAK_14`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 14)
        verify { manager.unlock(activity, AchievementIds.STREAK_14) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_30) }
    }

    @Test
    fun `onPuzzleWon streak=30 unlocks STREAK_30`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 30)
        verify { manager.unlock(activity, AchievementIds.STREAK_30) }
    }

    @Test
    fun `onPuzzleWon streak=2 does not unlock any streak achievement`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, dailyStreak = 2)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.STREAK_3) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. onPuzzleWon — login streak
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onPuzzleWon loginStreak=7 unlocks LOGIN_STREAK_7`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 7)
        verify { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_30) }
    }

    @Test
    fun `onPuzzleWon loginStreak=30 unlocks both login-streak achievements`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 30)
        verify { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
        verify { manager.unlock(activity, AchievementIds.LOGIN_STREAK_30) }
    }

    @Test
    fun `onPuzzleWon loginStreak=3 does not unlock any login-streak achievement`() {
        manager.onPuzzleWon(activity = activity, totalWins = 5, guessCount = 3, loginStreak = 3)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.LOGIN_STREAK_7) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 10. onItemUsed
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
    fun `onItemUsed always sets ITEMS_USED_50 steps`() {
        manager.onItemUsed(activity, totalItemsUsed = 25)
        verify { manager.setSteps(activity, AchievementIds.ITEMS_USED_50, 25) }
    }

    @Test
    fun `onItemUsed totalItemsUsed=50 sets max steps for ITEMS_USED_50`() {
        manager.onItemUsed(activity, totalItemsUsed = 50)
        verify { manager.setSteps(activity, AchievementIds.ITEMS_USED_50, 50) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. onCoinsEarned
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
    fun `onCoinsEarned with exactly 10000 coins`() {
        manager.onCoinsEarned(activity, totalCoinsEarned = 10_000L)
        verify { manager.setSteps(activity, AchievementIds.COIN_EARNER_10000, 10_000) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 12. onAdWatched
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onAdWatched unlocks FIRST_AD_WATCHED`() {
        manager.onAdWatched(activity)
        verify { manager.unlock(activity, AchievementIds.FIRST_AD_WATCHED) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 13. onPurchaseCompleted
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
    fun `onPurchaseCompleted VIP does NOT unlock BUNDLE_BUYER`() {
        manager.onPurchaseCompleted(activity, ProductIds.VIP_MONTHLY, isFirstEverPurchase = true)
        verify(exactly = 0) { manager.unlock(activity, AchievementIds.BUNDLE_BUYER) }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 14. onVipPurchased
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `onVipPurchased unlocks VIP_SUBSCRIBER`() {
        manager.onVipPurchased(activity)
        verify { manager.unlock(activity, AchievementIds.VIP_SUBSCRIBER) }
    }
}
