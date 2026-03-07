package com.djtaylor.wordjourney.domain.usecase

import com.djtaylor.wordjourney.domain.model.TileState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Additional tests for [EvaluateGuessUseCase] covering non-standard word lengths
 * (4-letter Easy mode and 6-letter Hard mode) and VIP scenarios.
 *
 * The core duplicate-letter algorithm must work identically regardless of word length.
 */
class EvaluateGuessUseCaseWordLengthTest {

    private lateinit var evaluate: EvaluateGuessUseCase

    @Before
    fun setup() {
        evaluate = EvaluateGuessUseCase()
    }

    // ── 4-letter words (Easy mode) ───────────────────────────────────────────

    @Test
    fun `4-letter exact match — all CORRECT`() {
        val result = evaluate("BAKE", "BAKE")
        assertTrue("All 4 tiles should be CORRECT",
            result.all { it.second == TileState.CORRECT })
        assertEquals(4, result.size)
    }

    @Test
    fun `4-letter no match — all ABSENT`() {
        val result = evaluate("BAKE", "CORN")
        assertTrue("All 4 tiles should be ABSENT",
            result.all { it.second == TileState.ABSENT })
    }

    @Test
    fun `4-letter partial match with present letter`() {
        // target=BAKE, guess=BIKE
        // B: pos 0 in both => CORRECT
        // I: not in BAKE => ABSENT
        // K: pos 2 guess, pos 2 target => CORRECT
        // E: pos 3 in both => CORRECT
        val result = evaluate("BIKE", "BAKE")
        assertEquals(TileState.CORRECT, result[0].second)  // B
        assertEquals(TileState.ABSENT, result[1].second)   // I
        assertEquals(TileState.CORRECT, result[2].second)  // K
        assertEquals(TileState.CORRECT, result[3].second)  // E
    }

    @Test
    fun `4-letter duplicate in target handled correctly`() {
        // target=NOON, guess=NOON => all CORRECT
        val result = evaluate("NOON", "NOON")
        assertTrue("NOON vs NOON all CORRECT",
            result.all { it.second == TileState.CORRECT })
    }

    @Test
    fun `4-letter one letter present not correct`() {
        // target=BAKE, guess=EBAY
        // E: in BAKE at pos 3, guess pos is 0 => PRESENT
        // B: in BAKE at pos 0, guess pos is 1 => PRESENT
        // A: in BAKE at pos 1, guess pos is 2 => PRESENT
        // Y: not in BAKE => ABSENT
        val result = evaluate("EBAY", "BAKE")
        assertEquals(TileState.PRESENT, result[0].second)  // E
        assertEquals(TileState.PRESENT, result[1].second)  // B
        assertEquals(TileState.PRESENT, result[2].second)  // A
        assertEquals(TileState.ABSENT, result[3].second)   // Y
    }

    // ── 6-letter words (Hard mode) ───────────────────────────────────────────

    @Test
    fun `6-letter exact match — all CORRECT`() {
        val result = evaluate("BRIDGE", "BRIDGE")
        assertTrue("All 6 tiles should be CORRECT",
            result.all { it.second == TileState.CORRECT })
        assertEquals(6, result.size)
    }

    @Test
    fun `6-letter no match — all ABSENT`() {
        // BRIDGE: B,R,I,D,G,E — CANOPY: C,A,N,O,P,Y — no letter in common
        val result = evaluate("BRIDGE", "CANOPY")
        assertTrue("All tiles should be ABSENT",
            result.all { it.second == TileState.ABSENT })
    }

    @Test
    fun `6-letter mixed positions`() {
        // target=BRIDGE, guess=GRIMED
        // G: pos 0 guess, in BRIDGE at pos 2 => PRESENT
        // R: pos 1 in both => CORRECT
        // I: pos 2 in both => CORRECT
        // M: not in BRIDGE => ABSENT
        // E: pos 4 guess, pos 5 in BRIDGE => PRESENT
        // D: pos 5 guess, pos 4 in BRIDGE => PRESENT
        val result = evaluate("GRIMED", "BRIDGE")
        assertEquals(TileState.PRESENT, result[0].second)  // G
        assertEquals(TileState.CORRECT, result[1].second)  // R
        assertEquals(TileState.CORRECT, result[2].second)  // I
        assertEquals(TileState.ABSENT,  result[3].second)  // M
        assertEquals(TileState.PRESENT, result[4].second)  // E
        assertEquals(TileState.PRESENT, result[5].second)  // D
    }

    @Test
    fun `6-letter duplicate in guess — only one in target`() {
        // target=BRIDGE, guess=BRRRR? => only first R is CORRECT, rest ABSENT
        // target=BRIDGE, guess=BRIICE
        // B: CORRECT, R: CORRECT, I: CORRECT, I: ABSENT(only 1 I in BRIDGE), C: ABSENT, E: CORRECT
        val result = evaluate("BRIICE", "BRIDGE")
        assertEquals(TileState.CORRECT, result[0].second)  // B
        assertEquals(TileState.CORRECT, result[1].second)  // R
        assertEquals(TileState.CORRECT, result[2].second)  // I (first, correct position)
        assertEquals(TileState.ABSENT,  result[3].second)  // I (second, no more I left)
        assertEquals(TileState.ABSENT,  result[4].second)  // C
        assertEquals(TileState.CORRECT, result[5].second)  // E
    }

    // ── 3-letter words (VIP mode min) ────────────────────────────────────────

    @Test
    fun `3-letter exact match`() {
        val result = evaluate("CAT", "CAT")
        assertEquals(3, result.size)
        assertTrue("All CORRECT", result.all { it.second == TileState.CORRECT })
    }

    @Test
    fun `3-letter fully wrong`() {
        val result = evaluate("CAT", "DOG")
        assertEquals(3, result.size)
        assertTrue("All ABSENT", result.all { it.second == TileState.ABSENT })
    }

    @Test
    fun `3-letter present but wrong position`() {
        // target=CAT, guess=TAC
        // T: in CAT at pos 2, guess pos 0 => PRESENT
        // A: in CAT at pos 1, guess pos 1 => CORRECT
        // C: in CAT at pos 0, guess pos 2 => PRESENT
        val result = evaluate("TAC", "CAT")
        assertEquals(TileState.PRESENT, result[0].second)  // T
        assertEquals(TileState.CORRECT, result[1].second)  // A
        assertEquals(TileState.PRESENT, result[2].second)  // C
    }

    // ── 7-letter words (VIP mode max) ────────────────────────────────────────

    @Test
    fun `7-letter exact match`() {
        val result = evaluate("JOURNEY", "JOURNEY")
        assertEquals(7, result.size)
        assertTrue("All CORRECT", result.all { it.second == TileState.CORRECT })
    }

    @Test
    fun `7-letter all absent`() {
        val result = evaluate("JOURNEY", "BATHBXX")
        assertEquals(7, result.size)
        // J, O, U, R, N, E, Y not in BATHBXX
        assertTrue("All absent", result.all { it.second == TileState.ABSENT })
    }

    @Test
    fun `result size always equals input length`() {
        listOf("CAT", "BAKE", "CRANE", "BRIDGE", "JOURNEY").forEach { word ->
            val result = evaluate(word, word)
            assertEquals("Size mismatch for $word", word.length, result.size)
        }
    }

    @Test
    fun `result pairs contain original letters`() {
        val guess = "CRANE"
        val result = evaluate(guess, "STONE")
        for (i in guess.indices) {
            assertEquals("Letter at $i", guess[i], result[i].first)
        }
    }
}
