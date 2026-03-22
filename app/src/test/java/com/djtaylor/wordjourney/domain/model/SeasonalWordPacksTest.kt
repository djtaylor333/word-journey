package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SeasonalWordPacks — verifies the integrity of all 6 seasonal
 * themed level packs: word count, word length, no duplicates, and retrieval.
 */
class SeasonalWordPacksTest {

    private val allSeasonKeys = listOf(
        "easter", "valentines", "summer", "halloween", "thanksgiving", "christmas"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // 1. PACK AVAILABILITY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all six season packs are available`() {
        val available = SeasonalWordPacks.availableSeasonKeys
        allSeasonKeys.forEach { key ->
            assertTrue("Pack '$key' should be available", available.contains(key))
        }
    }

    @Test
    fun `each pack has exactly 100 levels`() {
        allSeasonKeys.forEach { key ->
            assertEquals("Pack '$key' should have 100 levels", 100, SeasonalWordPacks.packSize(key))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. WORD LENGTH VALIDATION (all words must be exactly 5 letters)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all words in Easter pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("easter")
    }

    @Test
    fun `all words in Valentines pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("valentines")
    }

    @Test
    fun `all words in Summer pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("summer")
    }

    @Test
    fun `all words in Halloween pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("halloween")
    }

    @Test
    fun `all words in Thanksgiving pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("thanksgiving")
    }

    @Test
    fun `all words in Christmas pack are exactly 5 letters`() {
        assertAllExactlyFiveLetters("christmas")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. UNIQUENESS — no duplicates within a pack
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `Easter pack has no duplicate words`() {
        assertNoDuplicates("easter")
    }

    @Test
    fun `Valentines pack has no duplicate words`() {
        assertNoDuplicates("valentines")
    }

    @Test
    fun `Summer pack has no duplicate words`() {
        assertNoDuplicates("summer")
    }

    @Test
    fun `Halloween pack has no duplicate words`() {
        assertNoDuplicates("halloween")
    }

    @Test
    fun `Thanksgiving pack has no duplicate words`() {
        assertNoDuplicates("thanksgiving")
    }

    @Test
    fun `Christmas pack has no duplicate words`() {
        assertNoDuplicates("christmas")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. WORD RETRIEVAL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getWord returns first word for level 1`() {
        allSeasonKeys.forEach { key ->
            val word = SeasonalWordPacks.getWord(key, 1)
            assertTrue("Level 1 word in '$key' should be 5 letters, got '$word'", word.length == 5)
        }
    }

    @Test
    fun `getWord returns last word for level 100`() {
        allSeasonKeys.forEach { key ->
            val word = SeasonalWordPacks.getWord(key, 100)
            assertTrue("Level 100 word in '$key' should be 5 letters, got '$word'", word.length == 5)
        }
    }

    @Test
    fun `getWord wraps around for level beyond 100`() {
        allSeasonKeys.forEach { key ->
            val word1 = SeasonalWordPacks.getWord(key, 1)
            val word101 = SeasonalWordPacks.getWord(key, 101)
            assertEquals("Level 101 should wrap to level 1 word in '$key'", word1, word101)
        }
    }

    @Test
    fun `getWord for unknown season returns fallback 5-letter word`() {
        val word = SeasonalWordPacks.getWord("unknown_season", 1)
        assertEquals("Unknown season fallback word", 5, word.length)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. ALL WORDS ARE UPPERCASE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all words are uppercase`() {
        allSeasonKeys.forEach { key ->
            for (level in 1..SeasonalWordPacks.packSize(key)) {
                val word = SeasonalWordPacks.getWord(key, level)
                assertEquals(
                    "Word at level $level in '$key' should be uppercase: $word",
                    word.uppercase(),
                    word
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. TOTAL WORD COUNT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `total word count across all packs is 600`() {
        val total = allSeasonKeys.sumOf { SeasonalWordPacks.packSize(it) }
        assertEquals("Total words across all packs should be 600", 600, total)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. PLAYER PROGRESS HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `seasonalLevelFor returns correct level for each pack`() {
        val progress = PlayerProgress(
            seasonalEasterLevel = 5,
            seasonalValentinesLevel = 12,
            seasonalSummerLevel = 33,
            seasonalHalloweenLevel = 7,
            seasonalThanksgivingLevel = 50,
            seasonalChristmasLevel = 99
        )
        assertEquals(5, progress.seasonalLevelFor("easter"))
        assertEquals(12, progress.seasonalLevelFor("valentines"))
        assertEquals(33, progress.seasonalLevelFor("summer"))
        assertEquals(7, progress.seasonalLevelFor("halloween"))
        assertEquals(50, progress.seasonalLevelFor("thanksgiving"))
        assertEquals(99, progress.seasonalLevelFor("christmas"))
    }

    @Test
    fun `seasonalLevelFor returns 1 for unknown pack`() {
        val progress = PlayerProgress()
        assertEquals(1, progress.seasonalLevelFor("unknown"))
    }

    @Test
    fun `withSeasonalLevelAdvanced correctly updates progress for each season`() {
        val progress = PlayerProgress()
        val updated = progress
            .withSeasonalLevelAdvanced("easter", 10)
            .withSeasonalLevelAdvanced("valentines", 20)
            .withSeasonalLevelAdvanced("summer", 30)
            .withSeasonalLevelAdvanced("halloween", 40)
            .withSeasonalLevelAdvanced("thanksgiving", 50)
            .withSeasonalLevelAdvanced("christmas", 60)

        assertEquals(10, updated.seasonalEasterLevel)
        assertEquals(20, updated.seasonalValentinesLevel)
        assertEquals(30, updated.seasonalSummerLevel)
        assertEquals(40, updated.seasonalHalloweenLevel)
        assertEquals(50, updated.seasonalThanksgivingLevel)
        assertEquals(60, updated.seasonalChristmasLevel)
    }

    @Test
    fun `withSeasonalLevelAdvanced does not modify other fields`() {
        val progress = PlayerProgress(easyLevel = 5, regularLevel = 10, coins = 999L)
        val updated = progress.withSeasonalLevelAdvanced("easter", 3)

        assertEquals("easyLevel unchanged", 5, updated.easyLevel)
        assertEquals("regularLevel unchanged", 10, updated.regularLevel)
        assertEquals("coins unchanged", 999L, updated.coins)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private fun assertAllExactlyFiveLetters(seasonKey: String) {
        val packSize = SeasonalWordPacks.packSize(seasonKey)
        for (level in 1..packSize) {
            val word = SeasonalWordPacks.getWord(seasonKey, level)
            assertEquals(
                "Word '$word' at level $level in '$seasonKey' pack should be exactly 5 letters",
                5,
                word.length
            )
        }
    }

    private fun assertNoDuplicates(seasonKey: String) {
        val words = (1..SeasonalWordPacks.packSize(seasonKey))
            .map { SeasonalWordPacks.getWord(seasonKey, it) }
        val duplicates = words.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(
            "Pack '$seasonKey' has duplicate words: $duplicates",
            duplicates.isEmpty()
        )
    }
}
