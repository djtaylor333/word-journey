package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests that verify the difficulty grid-layout grouping logic used in HomeScreen.
 *
 * HomeScreen displays Adventure mode cards in a 2-column grid using
 * `chunked(2)` on the non-VIP difficulties. These tests confirm the
 * grouping produces the expected rows.
 */
class DifficultyGridLayoutTest {

    private val adventureDifficulties = Difficulty.entries.filter { it != Difficulty.VIP }

    @Test
    fun `adventure difficulties excludes VIP`() {
        assertFalse("VIP should not be in adventure list",
            adventureDifficulties.contains(Difficulty.VIP))
    }

    @Test
    fun `adventure difficulties contains Easy Regular Hard`() {
        assertTrue(adventureDifficulties.contains(Difficulty.EASY))
        assertTrue(adventureDifficulties.contains(Difficulty.REGULAR))
        assertTrue(adventureDifficulties.contains(Difficulty.HARD))
    }

    @Test
    fun `adventure difficulties ordered Easy then Regular then Hard`() {
        val names = adventureDifficulties.map { it.displayName }
        assertEquals(listOf("Easy", "Regular", "Hard"), names)
    }

    @Test
    fun `chunked into 2 columns produces 2 rows for 3 difficulties`() {
        val rows = adventureDifficulties.chunked(2)
        assertEquals("Expected 2 row chunks for 3 difficulties", 2, rows.size)
    }

    @Test
    fun `first row contains Easy and Regular`() {
        val rows = adventureDifficulties.chunked(2)
        assertEquals(2, rows[0].size)
        assertEquals(Difficulty.EASY, rows[0][0])
        assertEquals(Difficulty.REGULAR, rows[0][1])
    }

    @Test
    fun `second row contains only Hard`() {
        val rows = adventureDifficulties.chunked(2)
        assertEquals(1, rows[1].size)
        assertEquals(Difficulty.HARD, rows[1][0])
    }

    @Test
    fun `all 4 difficulties have non-empty displayName`() {
        for (d in Difficulty.entries) {
            assertTrue("${d.name} has empty displayName", d.displayName.isNotEmpty())
        }
    }

    @Test
    fun `all 4 difficulties have positive maxGuesses`() {
        for (d in Difficulty.entries) {
            assertTrue("${d.name} maxGuesses should be > 0", d.maxGuesses > 0)
        }
    }

    @Test
    fun `all 4 difficulties have positive wordLength`() {
        for (d in Difficulty.entries) {
            assertTrue("${d.name} wordLength should be > 0", d.wordLength > 0)
        }
    }

    @Test
    fun `all 4 difficulties have non-empty saveKey`() {
        for (d in Difficulty.entries) {
            assertTrue("${d.name} saveKey is empty", d.saveKey.isNotEmpty())
        }
    }

    @Test
    fun `saveKeys are unique`() {
        val keys = Difficulty.entries.map { it.saveKey }
        assertEquals("Duplicate saveKeys found", keys.size, keys.distinct().size)
    }

    @Test
    fun `word lengths align with game design — Easy 4 Regular 5 Hard 6`() {
        assertEquals(4, Difficulty.EASY.wordLength)
        assertEquals(5, Difficulty.REGULAR.wordLength)
        assertEquals(6, Difficulty.HARD.wordLength)
    }
}
