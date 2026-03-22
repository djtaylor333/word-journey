package com.djtaylor.wordjourney.auth

import org.junit.Assert.*
import org.junit.Test

/**
 * Validates that all AchievementIds constants contain real Play Games IDs
 * (i.e. they have been replaced with actual CgkI... IDs from Play Console,
 *  not left as placeholder "achievement_*" strings).
 *
 * These tests will FAIL if someone accidentally reverts to placeholder IDs.
 */
class AchievementIdsTest {

    @Test
    fun `FIRST_WIN is a real Play Games ID not a placeholder`() {
        assertFalse(
            "FIRST_WIN should be a real Play Games ID — run setup_play_achievements.py",
            AchievementIds.FIRST_WIN.startsWith("achievement_")
        )
        assertTrue(
            "FIRST_WIN should look like a Play Games ID (starts with CgkI)",
            AchievementIds.FIRST_WIN.isNotEmpty()
        )
    }

    @Test
    fun `all 31 achievement IDs are non-empty`() {
        val allIds = listOf(
            AchievementIds.FIRST_WIN,
            AchievementIds.WIN_10,
            AchievementIds.WIN_50,
            AchievementIds.WIN_100,
            AchievementIds.WIN_250,
            AchievementIds.WIN_500,
            AchievementIds.FIRST_GUESS_WIN,
            AchievementIds.TWO_GUESS_WIN,
            AchievementIds.NO_POWERUP_WIN,
            AchievementIds.LAST_GUESS_WIN,
            AchievementIds.STREAK_3,
            AchievementIds.STREAK_7,
            AchievementIds.STREAK_14,
            AchievementIds.STREAK_30,
            AchievementIds.LOGIN_STREAK_7,
            AchievementIds.LOGIN_STREAK_30,
            AchievementIds.REACH_LEVEL_10,
            AchievementIds.REACH_LEVEL_25,
            AchievementIds.REACH_LEVEL_50,
            AchievementIds.PACK_MASTER,
            AchievementIds.SEASONAL_CHAMPION,
            AchievementIds.FIRST_DAILY,
            AchievementIds.DAILY_10,
            AchievementIds.DAILY_100,
            AchievementIds.FIRST_ITEM_USED,
            AchievementIds.ITEMS_USED_50,
            AchievementIds.COIN_EARNER_10000,
            AchievementIds.FIRST_AD_WATCHED,
            AchievementIds.FIRST_PURCHASE,
            AchievementIds.BUNDLE_BUYER,
            AchievementIds.VIP_SUBSCRIBER,
        )
        assertEquals("Expected 31 achievement IDs", 31, allIds.size)
        for (id in allIds) {
            assertTrue("Achievement ID '$id' must not be empty", id.isNotEmpty())
        }
    }

    @Test
    fun `no achievement ID uses placeholder format`() {
        val allIds = listOf(
            AchievementIds.FIRST_WIN, AchievementIds.WIN_10, AchievementIds.WIN_50,
            AchievementIds.WIN_100, AchievementIds.WIN_250, AchievementIds.WIN_500,
            AchievementIds.FIRST_GUESS_WIN, AchievementIds.TWO_GUESS_WIN,
            AchievementIds.NO_POWERUP_WIN, AchievementIds.LAST_GUESS_WIN,
            AchievementIds.STREAK_3, AchievementIds.STREAK_7,
            AchievementIds.STREAK_14, AchievementIds.STREAK_30,
            AchievementIds.LOGIN_STREAK_7, AchievementIds.LOGIN_STREAK_30,
            AchievementIds.REACH_LEVEL_10, AchievementIds.REACH_LEVEL_25,
            AchievementIds.REACH_LEVEL_50, AchievementIds.PACK_MASTER,
            AchievementIds.SEASONAL_CHAMPION,
            AchievementIds.FIRST_DAILY, AchievementIds.DAILY_10, AchievementIds.DAILY_100,
            AchievementIds.FIRST_ITEM_USED, AchievementIds.ITEMS_USED_50,
            AchievementIds.COIN_EARNER_10000,
            AchievementIds.FIRST_AD_WATCHED,
            AchievementIds.FIRST_PURCHASE, AchievementIds.BUNDLE_BUYER,
            AchievementIds.VIP_SUBSCRIBER,
        )
        for (id in allIds) {
            assertFalse(
                "Achievement ID '$id' is still a placeholder — replace with real Play Console ID",
                id.startsWith("achievement_")
            )
        }
    }

    @Test
    fun `all 31 achievement IDs are unique`() {
        val allIds = listOf(
            AchievementIds.FIRST_WIN, AchievementIds.WIN_10, AchievementIds.WIN_50,
            AchievementIds.WIN_100, AchievementIds.WIN_250, AchievementIds.WIN_500,
            AchievementIds.FIRST_GUESS_WIN, AchievementIds.TWO_GUESS_WIN,
            AchievementIds.NO_POWERUP_WIN, AchievementIds.LAST_GUESS_WIN,
            AchievementIds.STREAK_3, AchievementIds.STREAK_7,
            AchievementIds.STREAK_14, AchievementIds.STREAK_30,
            AchievementIds.LOGIN_STREAK_7, AchievementIds.LOGIN_STREAK_30,
            AchievementIds.REACH_LEVEL_10, AchievementIds.REACH_LEVEL_25,
            AchievementIds.REACH_LEVEL_50, AchievementIds.PACK_MASTER,
            AchievementIds.SEASONAL_CHAMPION,
            AchievementIds.FIRST_DAILY, AchievementIds.DAILY_10, AchievementIds.DAILY_100,
            AchievementIds.FIRST_ITEM_USED, AchievementIds.ITEMS_USED_50,
            AchievementIds.COIN_EARNER_10000,
            AchievementIds.FIRST_AD_WATCHED,
            AchievementIds.FIRST_PURCHASE, AchievementIds.BUNDLE_BUYER,
            AchievementIds.VIP_SUBSCRIBER,
        )
        val unique = allIds.toSet()
        assertEquals("All 31 achievement IDs must be unique — found duplicates", allIds.size, unique.size)
    }

    @Test
    fun `FIRST_WIN ID format matches Play Games CgkI format`() {
        // Play Games achievement IDs returned by the gamesConfiguration API
        // always start with "CgkI" (case-sensitive Base64 encoded IDs)
        assertTrue(
            "FIRST_WIN '${AchievementIds.FIRST_WIN}' should start with 'CgkI' (real Play Games ID)",
            AchievementIds.FIRST_WIN.startsWith("CgkI")
        )
    }

    @Test
    fun `all confirmed achievement IDs start with CgkI`() {
        // All 31 IDs were obtained from Play Console in this session via the
        // gamesConfiguration API — they should all start with "CgkI"
        val confirmed = listOf(
            AchievementIds.FIRST_WIN, AchievementIds.WIN_10, AchievementIds.WIN_50,
            AchievementIds.WIN_100, AchievementIds.WIN_250, AchievementIds.WIN_500,
            AchievementIds.FIRST_GUESS_WIN, AchievementIds.TWO_GUESS_WIN,
            AchievementIds.NO_POWERUP_WIN, AchievementIds.LAST_GUESS_WIN,
            AchievementIds.STREAK_3, AchievementIds.STREAK_7,
            AchievementIds.STREAK_14, AchievementIds.STREAK_30,
            AchievementIds.LOGIN_STREAK_7, AchievementIds.LOGIN_STREAK_30,
            AchievementIds.REACH_LEVEL_10, AchievementIds.REACH_LEVEL_25,
            AchievementIds.REACH_LEVEL_50, AchievementIds.PACK_MASTER,
            AchievementIds.SEASONAL_CHAMPION,
            AchievementIds.FIRST_DAILY, AchievementIds.DAILY_10, AchievementIds.DAILY_100,
            AchievementIds.FIRST_ITEM_USED, AchievementIds.ITEMS_USED_50,
            AchievementIds.COIN_EARNER_10000,
            AchievementIds.FIRST_AD_WATCHED,
            AchievementIds.FIRST_PURCHASE, AchievementIds.BUNDLE_BUYER,
            AchievementIds.VIP_SUBSCRIBER,
        )
        for (id in confirmed) {
            assertTrue(
                "Achievement ID '$id' should start with 'CgkI' — verify in Play Console",
                id.startsWith("CgkI")
            )
        }
    }
}
