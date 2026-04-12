package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for [StarChestDefinitions], [StarChest] helpers, and [streakShieldCost].
 *
 * Covers:
 *  - Streak shield gem cost formula correctness
 *  - availableStars() extension
 *  - openedChestsThisMonth() month-key logic
 *  - withChestOpened() deducts correct star cost and records chest ID
 *  - Monthly chest reset (new month → opened list starts fresh)
 *  - Chest count / definition sanity checks
 */
class StarChestDefinitionsTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. STREAK SHIELD COST FORMULA
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `streakShieldCost first use costs 5 gems`() {
        assertEquals(5, streakShieldCost(0))
    }

    @Test
    fun `streakShieldCost second use costs 7 gems`() {
        assertEquals(7, streakShieldCost(1))
    }

    @Test
    fun `streakShieldCost third use costs 10 gems`() {
        assertEquals(10, streakShieldCost(2))
    }

    @Test
    fun `streakShieldCost fourth use costs 14 gems`() {
        assertEquals(14, streakShieldCost(3))
    }

    @Test
    fun `streakShieldCost fifth use costs 19 gems`() {
        assertEquals(19, streakShieldCost(4))
    }

    @Test
    fun `streakShieldCost sixth use costs 25 gems`() {
        assertEquals(25, streakShieldCost(5))
    }

    @Test
    fun `streakShieldCost seventh use costs 32 gems`() {
        assertEquals(32, streakShieldCost(6))
    }

    @Test
    fun `streakShieldCost differences increase by 2 each time`() {
        val costs = (0..9).map { streakShieldCost(it) }
        val diffs = costs.zipWithNext { a, b -> b - a }
        // Expected diffs: 2, 3, 4, 5, 6, 7, 8, 9, 10
        diffs.forEachIndexed { index, diff ->
            assertEquals("diff at index $index", index + 2, diff)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. availableStars()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `availableStars returns totalStarsEarned minus starsSpentOnChests`() {
        val p = PlayerProgress(totalStarsEarned = 50, starsSpentOnChests = 15)
        assertEquals(35, p.availableStars())
    }

    @Test
    fun `availableStars is zero when all stars spent`() {
        val p = PlayerProgress(totalStarsEarned = 20, starsSpentOnChests = 20)
        assertEquals(0, p.availableStars())
    }

    @Test
    fun `availableStars is zero when no stars earned`() {
        val p = PlayerProgress()
        assertEquals(0, p.availableStars())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. openedChestsThisMonth()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `openedChestsThisMonth returns empty when month key differs`() {
        val p = PlayerProgress(
            openedChestsThisMonthKeys = "regular_1,regular_2",
            chestResetMonthKey = "2025-01"
        )
        // Current month is different → no chests opened this month
        assertTrue(p.openedChestsThisMonth("2025-02").isEmpty())
    }

    @Test
    fun `openedChestsThisMonth returns set when month key matches`() {
        val p = PlayerProgress(
            openedChestsThisMonthKeys = "regular_1,regular_3",
            chestResetMonthKey = "2025-03"
        )
        val opened = p.openedChestsThisMonth("2025-03")
        assertEquals(setOf("regular_1", "regular_3"), opened)
    }

    @Test
    fun `openedChestsThisMonth returns empty when keys string is blank`() {
        val p = PlayerProgress(openedChestsThisMonthKeys = "", chestResetMonthKey = "2025-03")
        assertTrue(p.openedChestsThisMonth("2025-03").isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. withChestOpened()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `withChestOpened deducts star cost from starsSpentOnChests`() {
        val p = PlayerProgress(totalStarsEarned = 30, starsSpentOnChests = 0)
        val updated = p.withChestOpened("regular_1", starCost = 3, currentMonthKey = "2025-03")
        assertEquals(3, updated.starsSpentOnChests)
        assertEquals(27, updated.availableStars())
    }

    @Test
    fun `withChestOpened records chest id in openedChestsThisMonthKeys`() {
        val p = PlayerProgress(totalStarsEarned = 30, starsSpentOnChests = 0, chestResetMonthKey = "2025-03")
        val updated = p.withChestOpened("regular_1", starCost = 3, currentMonthKey = "2025-03")
        assertTrue(updated.openedChestsThisMonth("2025-03").contains("regular_1"))
    }

    @Test
    fun `withChestOpened on new month resets opened list`() {
        val p = PlayerProgress(
            totalStarsEarned = 50,
            starsSpentOnChests = 8,
            openedChestsThisMonthKeys = "regular_1,regular_2",
            chestResetMonthKey = "2025-01"        // old month
        )
        val updated = p.withChestOpened("regular_1", starCost = 3, currentMonthKey = "2025-02")
        // The opened list for the NEW month should only contain the just-opened chest
        assertEquals(setOf("regular_1"), updated.openedChestsThisMonth("2025-02"))
        assertEquals("2025-02", updated.chestResetMonthKey)
    }

    @Test
    fun `withChestOpened accumulates starsSpentOnChests across months`() {
        val p = PlayerProgress(totalStarsEarned = 100, starsSpentOnChests = 20)
        val updated = p.withChestOpened("regular_5", starCost = 35, currentMonthKey = "2025-04")
        // starsSpentOnChests never resets (lifetime total)
        assertEquals(55, updated.starsSpentOnChests)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Chest definitions sanity
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `there are 10 regular chests`() {
        assertEquals(10, StarChestDefinitions.regularChests.size)
    }

    @Test
    fun `there are 10 VIP chests`() {
        assertEquals(10, StarChestDefinitions.vipChests.size)
    }

    @Test
    fun `regular chest star costs match expected sequence`() {
        val expected = listOf(3, 8, 15, 24, 35, 48, 63, 80, 99, 120)
        val actual = StarChestDefinitions.regularChests.map { it.starCost }
        assertEquals(expected, actual)
    }

    @Test
    fun `VIP chests are all marked isVip`() {
        assertTrue(StarChestDefinitions.vipChests.all { it.isVip })
    }

    @Test
    fun `regular chests are all not isVip`() {
        assertTrue(StarChestDefinitions.regularChests.none { it.isVip })
    }

    @Test
    fun `all regular chest IDs are unique`() {
        val ids = StarChestDefinitions.regularChests.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `all VIP chest IDs are unique`() {
        val ids = StarChestDefinitions.vipChests.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `toReward extension returns correct fields`() {
        val chest = StarChestDefinitions.regularChests[2]   // regular_3: 400 coins, 1 life, 1 addGuess
        val reward = chest.toReward()
        assertEquals(chest.coins, reward.coins)
        assertEquals(chest.lives, reward.lives)
        assertEquals(chest.addGuessItems, reward.addGuessItems)
        assertEquals(chest.removeLetterItems, reward.removeLetterItems)
        assertEquals(chest.gems, reward.gems)
    }
}
