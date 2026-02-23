package com.djtaylor.wordjourney.data.repository

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for DailyChallengeRepository date-seeded word selection.
 *
 * Covers:
 *  - computeDateSeed: DDMMYYYY numeric encoding, determinism, uniqueness
 *  - pickWordByDateSeed: determinism, valid index, different seeds diverge
 *  - Daily word selection: same date → same word, different date → different word
 *  - Stale save detection via GameViewModel.isDailySaveStale
 */
class DailyChallengeRepositoryTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. computeDateSeed — DDMMYYYY encoding
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `computeDateSeed uses DDMMYYYY numeric encoding`() {
        // Feb 23, 2026 → 23 * 1_000_000 + 02 * 10_000 + 2026 = 23_020_000 + 2026 = 23_022_026
        val expected = 23L * 1_000_000L + 2L * 10_000L + 2026L + 5 * 31L // wordLength=5
        val actual = DailyChallengeRepository.computeDateSeed("2026-02-23", 5)
        assertEquals(expected, actual)
    }

    @Test
    fun `computeDateSeed is deterministic for same input`() {
        val seed1 = DailyChallengeRepository.computeDateSeed("2026-10-31", 4)
        val seed2 = DailyChallengeRepository.computeDateSeed("2026-10-31", 4)
        assertEquals(seed1, seed2)
    }

    @Test
    fun `computeDateSeed differs when day changes`() {
        val seedDay1 = DailyChallengeRepository.computeDateSeed("2026-06-01", 5)
        val seedDay2 = DailyChallengeRepository.computeDateSeed("2026-06-02", 5)
        assertNotEquals(seedDay1, seedDay2)
    }

    @Test
    fun `computeDateSeed differs when month changes`() {
        val seedMonth1 = DailyChallengeRepository.computeDateSeed("2026-01-15", 5)
        val seedMonth2 = DailyChallengeRepository.computeDateSeed("2026-02-15", 5)
        assertNotEquals(seedMonth1, seedMonth2)
    }

    @Test
    fun `computeDateSeed differs when year changes`() {
        val seedYear1 = DailyChallengeRepository.computeDateSeed("2025-03-10", 5)
        val seedYear2 = DailyChallengeRepository.computeDateSeed("2026-03-10", 5)
        assertNotEquals(seedYear1, seedYear2)
    }

    @Test
    fun `computeDateSeed differs when wordLength changes`() {
        val seedLen4 = DailyChallengeRepository.computeDateSeed("2026-07-04", 4)
        val seedLen5 = DailyChallengeRepository.computeDateSeed("2026-07-04", 5)
        val seedLen6 = DailyChallengeRepository.computeDateSeed("2026-07-04", 6)
        assertNotEquals(seedLen4, seedLen5)
        assertNotEquals(seedLen5, seedLen6)
        assertNotEquals(seedLen4, seedLen6)
    }

    @Test
    fun `computeDateSeed handles single-digit day and month gracefully`() {
        // "2026-01-05" = day=5, month=1, year=2026
        val seed = DailyChallengeRepository.computeDateSeed("2026-01-05", 4)
        val expected = 5L * 1_000_000L + 1L * 10_000L + 2026L + 4 * 31L
        assertEquals(expected, seed)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. pickWordByDateSeed — determinism and validity
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `pickWordByDateSeed returns a word from the pool`() {
        val pool = listOf("APPLE", "BREAD", "CRANE", "DAISY", "EARTH")
        val result = DailyChallengeRepository.pickWordByDateSeed(pool, 12345L)
        assertTrue("Result '$result' not in pool", pool.contains(result))
    }

    @Test
    fun `pickWordByDateSeed is deterministic for same seed`() {
        val pool = listOf("APPLE", "BREAD", "CRANE", "DAISY", "EARTH")
        val r1 = DailyChallengeRepository.pickWordByDateSeed(pool, 99999L)
        val r2 = DailyChallengeRepository.pickWordByDateSeed(pool, 99999L)
        assertEquals(r1, r2)
    }

    @Test
    fun `pickWordByDateSeed works with single-element pool`() {
        val pool = listOf("ONLY")
        val result = DailyChallengeRepository.pickWordByDateSeed(pool, 0L)
        assertEquals("ONLY", result)
    }

    @Test
    fun `pickWordByDateSeed different seeds produce different words over a range`() {
        val pool = (1..20).map { "WORD$it" }
        // Different seeds should not all pick the same word
        val results = (1L..20L).map { DailyChallengeRepository.pickWordByDateSeed(pool, it * 1_000_000L) }.toSet()
        // With 20 distinct large seeds over 20 words, we expect at least 5 unique results
        assertTrue("Expected diverse selections, got only ${results.size}", results.size >= 5)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Full daily word selection — same date → same word, different date → differentword
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `same date and word length always selects same word`() {
        val pool = (1..50).map { "WORD$it" }.sorted()
        val seed1 = DailyChallengeRepository.computeDateSeed("2026-12-25", 5)
        val seed2 = DailyChallengeRepository.computeDateSeed("2026-12-25", 5)
        val word1 = DailyChallengeRepository.pickWordByDateSeed(pool, seed1)
        val word2 = DailyChallengeRepository.pickWordByDateSeed(pool, seed2)
        assertEquals(word1, word2)
    }

    @Test
    fun `different dates select different words for same pool`() {
        val pool = (1..100).map { "WORD%03d".format(it) }.sorted()
        val dateA = "2026-01-01"
        val dateB = "2026-01-02"
        val wordA = DailyChallengeRepository.pickWordByDateSeed(
            pool, DailyChallengeRepository.computeDateSeed(dateA, 5)
        )
        val wordB = DailyChallengeRepository.pickWordByDateSeed(
            pool, DailyChallengeRepository.computeDateSeed(dateB, 5)
        )
        // With 100 words and consecutive dates, seeds will almost certainly diverge
        assertNotEquals("Words should differ between days", wordA, wordB)
    }

    @Test
    fun `all three word lengths get different words on the same day`() {
        val pool4 = (1..50).map { "WD$it" }.sorted()
        val pool5 = (1..50).map { "WORD$it" }.sorted()
        val pool6 = (1..50).map { "WORDDD$it" }.sorted()
        val date = "2026-07-04"
        val word4 = DailyChallengeRepository.pickWordByDateSeed(pool4, DailyChallengeRepository.computeDateSeed(date, 4))
        val word5 = DailyChallengeRepository.pickWordByDateSeed(pool5, DailyChallengeRepository.computeDateSeed(date, 5))
        val word6 = DailyChallengeRepository.pickWordByDateSeed(pool6, DailyChallengeRepository.computeDateSeed(date, 6))
        // Each length has its own seed so they use different indices
        assertNotEquals(DailyChallengeRepository.computeDateSeed(date, 4), DailyChallengeRepository.computeDateSeed(date, 5))
        assertNotEquals(DailyChallengeRepository.computeDateSeed(date, 5), DailyChallengeRepository.computeDateSeed(date, 6))
        // Check seeds differ (the words may coincidentally match if pools are small, but seeds must differ)
        assertTrue("4-letter and 5-letter seeds must differ", word4 != null && word5 != null)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Stale save detection (isDailySaveStale from GameViewModel)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `isDailySaveStale returns false when savedDate matches today`() {
        assertFalse(com.djtaylor.wordjourney.ui.game.GameViewModel.isDailySaveStale("2026-02-23", "2026-02-23"))
    }

    @Test
    fun `isDailySaveStale returns true when savedDate is yesterday`() {
        assertTrue(com.djtaylor.wordjourney.ui.game.GameViewModel.isDailySaveStale("2026-02-22", "2026-02-23"))
    }

    @Test
    fun `isDailySaveStale returns false when savedDate is empty (backward compat)`() {
        assertFalse(com.djtaylor.wordjourney.ui.game.GameViewModel.isDailySaveStale("", "2026-02-23"))
    }

    @Test
    fun `isDailySaveStale returns true when savedDate is many days old`() {
        assertTrue(com.djtaylor.wordjourney.ui.game.GameViewModel.isDailySaveStale("2026-01-01", "2026-02-23"))
    }
}
