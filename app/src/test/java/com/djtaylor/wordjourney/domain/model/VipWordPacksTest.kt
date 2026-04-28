package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for VipWordPacks — verifies the integrity of all 50 hardcoded VIP levels:
 * pack coverage, word lengths, non-empty definitions, and spot-checks for
 * the historically broken levels 7, 8, and 9.
 */
class VipWordPacksTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. PACK COVERAGE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all levels 1 to 50 are covered`() {
        for (level in 1..50) {
            assertTrue("Level $level should be in VipWordPacks", VipWordPacks.hasLevel(level))
        }
    }

    @Test
    fun `levels beyond 50 are not covered`() {
        for (level in listOf(51, 52, 100, 500)) {
            assertFalse("Level $level should NOT be in VipWordPacks", VipWordPacks.hasLevel(level))
        }
    }

    @Test
    fun `getWord returns null for level 0 and levels beyond 50`() {
        assertNull(VipWordPacks.getWord(0))
        assertNull(VipWordPacks.getWord(51))
        assertNull(VipWordPacks.getWord(100))
    }

    @Test
    fun `getDefinition returns null for levels beyond 50`() {
        assertNull(VipWordPacks.getDefinition(51))
        assertNull(VipWordPacks.getDefinition(100))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. WORD LENGTH VALIDATION
    // Every VIP level word must match the expected length for that level's cycle
    // position: (level - 1) % 5 → index into [3, 4, 5, 6, 7].
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all 50 VipWordPacks words have the correct length for their level`() {
        for (level in 1..50) {
            val expectedLength = Difficulty.vipWordLengthForLevel(level)
            val word = VipWordPacks.getWord(level)
            assertNotNull("Level $level word must not be null", word)
            assertEquals(
                "Level $level word '${word}' should be $expectedLength letters",
                expectedLength,
                word!!.length
            )
        }
    }

    @Test
    fun `3-letter levels have 3-letter words`() {
        val threeLetter = listOf(1, 6, 11, 16, 21, 26, 31, 36, 41, 46)
        for (level in threeLetter) {
            assertEquals("Level $level should have a 3-letter word", 3, VipWordPacks.getWord(level)!!.length)
        }
    }

    @Test
    fun `4-letter levels have 4-letter words`() {
        val fourLetter = listOf(2, 7, 12, 17, 22, 27, 32, 37, 42, 47)
        for (level in fourLetter) {
            assertEquals("Level $level should have a 4-letter word", 4, VipWordPacks.getWord(level)!!.length)
        }
    }

    @Test
    fun `5-letter levels have 5-letter words`() {
        val fiveLetter = listOf(3, 8, 13, 18, 23, 28, 33, 38, 43, 48)
        for (level in fiveLetter) {
            assertEquals("Level $level should have a 5-letter word", 5, VipWordPacks.getWord(level)!!.length)
        }
    }

    @Test
    fun `6-letter levels have 6-letter words`() {
        val sixLetter = listOf(4, 9, 14, 19, 24, 29, 34, 39, 44, 49)
        for (level in sixLetter) {
            assertEquals("Level $level should have a 6-letter word", 6, VipWordPacks.getWord(level)!!.length)
        }
    }

    @Test
    fun `7-letter levels have 7-letter words`() {
        val sevenLetter = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50)
        for (level in sevenLetter) {
            assertEquals("Level $level should have a 7-letter word", 7, VipWordPacks.getWord(level)!!.length)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. CONTENT INTEGRITY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all 50 words are uppercase and non-blank`() {
        for (level in 1..50) {
            val word = VipWordPacks.getWord(level)!!
            assertTrue("Level $level word must be non-blank", word.isNotBlank())
            assertEquals("Level $level word '$word' must be UPPERCASE", word.uppercase(), word)
        }
    }

    @Test
    fun `all 50 definitions are non-blank`() {
        for (level in 1..50) {
            val definition = VipWordPacks.getDefinition(level)
            assertNotNull("Level $level definition must not be null", definition)
            assertTrue("Level $level definition must be non-blank", definition!!.isNotBlank())
        }
    }

    @Test
    fun `no duplicate words within the pack`() {
        val words = (1..50).map { VipWordPacks.getWord(it)!! }
        assertEquals("All 50 VipWordPacks words must be unique", 50, words.toSet().size)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. SPOT-CHECKS FOR HISTORICALLY BROKEN LEVELS 7, 8, 9
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `VIP level 7 is BOLD — a 4-letter word with a definition`() {
        assertEquals("BOLD", VipWordPacks.getWord(7))
        assertEquals(4, VipWordPacks.getWord(7)!!.length)
        assertTrue(VipWordPacks.getDefinition(7)!!.isNotBlank())
    }

    @Test
    fun `VIP level 8 is BRAVE — a 5-letter word with a definition`() {
        assertEquals("BRAVE", VipWordPacks.getWord(8))
        assertEquals(5, VipWordPacks.getWord(8)!!.length)
        assertTrue(VipWordPacks.getDefinition(8)!!.isNotBlank())
    }

    @Test
    fun `VIP level 9 is CASTLE — a 6-letter word with a definition`() {
        assertEquals("CASTLE", VipWordPacks.getWord(9))
        assertEquals(6, VipWordPacks.getWord(9)!!.length)
        assertTrue(VipWordPacks.getDefinition(9)!!.isNotBlank())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. CYCLE BOUNDARY CHECK
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cycle 1 words match expected content`() {
        assertEquals("CAT",     VipWordPacks.getWord(1))
        assertEquals("ABLE",    VipWordPacks.getWord(2))
        assertEquals("CRANE",   VipWordPacks.getWord(3))
        assertEquals("BRIDGE",  VipWordPacks.getWord(4))
        assertEquals("KITCHEN", VipWordPacks.getWord(5))
    }

    @Test
    fun `cycle 2 words match expected content`() {
        assertEquals("SUN",     VipWordPacks.getWord(6))
        assertEquals("BOLD",    VipWordPacks.getWord(7))
        assertEquals("BRAVE",   VipWordPacks.getWord(8))
        assertEquals("CASTLE",  VipWordPacks.getWord(9))
        assertEquals("CAPTAIN", VipWordPacks.getWord(10))
    }

    @Test
    fun `final cycle (levels 46-50) words match expected content`() {
        assertEquals("PIE",     VipWordPacks.getWord(46))
        assertEquals("NEST",    VipWordPacks.getWord(47))
        assertEquals("PRIZE",   VipWordPacks.getWord(48))
        assertEquals("MARKET",  VipWordPacks.getWord(49))
        assertEquals("DIAMOND", VipWordPacks.getWord(50))
    }
}
