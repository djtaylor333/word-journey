package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for [Difficulty] — covering VIP word-length cycling and word-length helpers.
 */
class DifficultyTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. VIP WORD LENGTH CYCLING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `vipWordLengthForLevel level 1 returns 3`() {
        assertEquals(3, Difficulty.vipWordLengthForLevel(1))
    }

    @Test
    fun `vipWordLengthForLevel level 2 returns 4`() {
        assertEquals(4, Difficulty.vipWordLengthForLevel(2))
    }

    @Test
    fun `vipWordLengthForLevel level 3 returns 5`() {
        assertEquals(5, Difficulty.vipWordLengthForLevel(3))
    }

    @Test
    fun `vipWordLengthForLevel level 4 returns 6`() {
        assertEquals(6, Difficulty.vipWordLengthForLevel(4))
    }

    @Test
    fun `vipWordLengthForLevel level 5 returns 7`() {
        assertEquals(7, Difficulty.vipWordLengthForLevel(5))
    }

    @Test
    fun `vipWordLengthForLevel cycles back at level 6 to 3`() {
        assertEquals(3, Difficulty.vipWordLengthForLevel(6))
    }

    @Test
    fun `vipWordLengthForLevel cycles back at level 7 to 4`() {
        assertEquals(4, Difficulty.vipWordLengthForLevel(7))
    }

    @Test
    fun `vipWordLengthForLevel cycles back at level 10 to 4`() {
        // Level 10: (10-1) % 5 = 4 → lengths[4] = 7
        assertEquals(7, Difficulty.vipWordLengthForLevel(10))
    }

    @Test
    fun `vipWordLengthForLevel returns value in range 3 to 7 for all levels 1 to 50`() {
        for (level in 1..50) {
            val len = Difficulty.vipWordLengthForLevel(level)
            assertTrue("Level $level length $len out of range", len in 3..7)
        }
    }

    @Test
    fun `vipWordLengthForLevel full cycle matches expected pattern`() {
        val expected = listOf(3, 4, 5, 6, 7, 3, 4, 5, 6, 7)
        val actual = (1..10).map { Difficulty.vipWordLengthForLevel(it) }
        assertEquals(expected, actual)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. DIFFICULTY WORD LENGTHS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `EASY difficulty has wordLength 4`() {
        assertEquals(4, Difficulty.EASY.wordLength)
    }

    @Test
    fun `REGULAR difficulty has wordLength 5`() {
        assertEquals(5, Difficulty.REGULAR.wordLength)
    }

    @Test
    fun `HARD difficulty has wordLength 6`() {
        assertEquals(6, Difficulty.HARD.wordLength)
    }

    @Test
    fun `VIP difficulty has wordLength 5 as base`() {
        // VIP base word length is 5 (the middle of the cycle); actual per-level length
        // is determined by vipWordLengthForLevel()
        assertEquals(5, Difficulty.VIP.wordLength)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DIFFICULTY ENUM VALUES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all four difficulties exist`() {
        val values = Difficulty.values()
        assertTrue(values.contains(Difficulty.EASY))
        assertTrue(values.contains(Difficulty.REGULAR))
        assertTrue(values.contains(Difficulty.HARD))
        assertTrue(values.contains(Difficulty.VIP))
    }

    @Test
    fun `difficulty values size is 4`() {
        assertEquals(4, Difficulty.values().size)
    }
}
