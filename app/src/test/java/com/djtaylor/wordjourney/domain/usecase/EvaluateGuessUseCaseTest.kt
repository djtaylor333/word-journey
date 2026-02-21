package com.djtaylor.wordjourney.domain.usecase

import com.djtaylor.wordjourney.domain.model.TileState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the Wordle guess evaluation algorithm.
 *
 * Rules:
 *  - CORRECT (green): letter is in the correct position
 *  - PRESENT (yellow): letter is in the word but wrong position
 *  - ABSENT  (grey):   letter is not in the word (or all occurrences accounted for)
 *  - Duplicate handling: a letter cannot be marked PRESENT more times than
 *    it appears in the target minus the number of CORRECT matches for that letter
 */
class EvaluateGuessUseCaseTest {

    private lateinit var evaluate: EvaluateGuessUseCase

    @Before
    fun setup() {
        evaluate = EvaluateGuessUseCase()
    }

    // ── Basic cases ──────────────────────────────────────────────────────────

    @Test
    fun `exact match — all CORRECT`() {
        val result = evaluate("CRANE", "CRANE")
        val states = result.map { it.second }
        assertTrue("All tiles should be CORRECT", states.all { it == TileState.CORRECT })
    }

    @Test
    fun `no letters in common — all ABSENT`() {
        val result = evaluate("BRICK", "SOUND")
        val states = result.map { it.second }
        // B, R, I, C, K not in SOUND
        assertTrue("All tiles should be ABSENT", states.all { it == TileState.ABSENT })
    }

    @Test
    fun `mixed correct and present positions`() {
        // target=CRANE, guess=TRICK:
        // T: not in CRANE => ABSENT
        // R: pos 1 in both => CORRECT
        // I: not in CRANE => ABSENT
        // C: pos 3 in guess, pos 0 in target => PRESENT
        // K: not in CRANE => ABSENT
        val result = evaluate("TRICK", "CRANE")
        assertEquals(TileState.ABSENT, result[0].second)   // T
        assertEquals(TileState.CORRECT, result[1].second)  // R — same index in both
        assertEquals(TileState.ABSENT, result[2].second)   // I
        assertEquals(TileState.PRESENT, result[3].second)  // C — in target at index 0
        assertEquals(TileState.ABSENT, result[4].second)   // K
    }

    @Test
    fun `correct and present in same guess`() {
        // target=CRANE, guess=CRACK
        // C: pos 0 in both => CORRECT
        // R: pos 1 in both => CORRECT
        // A: pos 2 in both => CORRECT
        // C: pos 3, C already matched at 0 => only 1 C in CRANE => ABSENT
        // K: not in CRANE => ABSENT
        val result = evaluate("CRACK", "CRANE")
        assertEquals(TileState.CORRECT, result[0].second) // C
        assertEquals(TileState.CORRECT, result[1].second) // R
        assertEquals(TileState.CORRECT, result[2].second) // A
        assertEquals(TileState.ABSENT, result[3].second)  // C (duplicate)
        assertEquals(TileState.ABSENT, result[4].second)  // K
    }

    // ── Duplicate letter handling ────────────────────────────────────────────

    @Test
    fun `duplicate letter in guess — only one in target`() {
        // target=APPLE, guess=PAPAL
        // P at 0: target A => not correct. P in target? Yes (pos 1,2). PRESENT
        // A at 1: target P => not correct. A in target? Yes (pos 0). PRESENT
        // P at 2: target P => CORRECT
        // A at 3: target L => not correct. A in target? Was, but only 1 A and it's consumed by prior PRESENT. ABSENT
        // L at 4: target E => not correct. L in target? Yes (pos 4). Wait APPLE=A,P,P,L,E. L at pos 3.
        //   L at guess pos 4, target pos 3 => PRESENT
        val result = evaluate("PAPAL", "APPLE")
        assertEquals('P', result[0].first)
        assertEquals(TileState.PRESENT, result[0].second) // P
        assertEquals('A', result[1].first)
        assertEquals(TileState.PRESENT, result[1].second) // A
        assertEquals('P', result[2].first)
        assertEquals(TileState.CORRECT, result[2].second) // P at pos 2
        assertEquals('A', result[3].first)
        assertEquals(TileState.ABSENT, result[3].second)  // A — only 1 A in target, already used
        assertEquals('L', result[4].first)
        assertEquals(TileState.PRESENT, result[4].second) // L in target at pos 3
    }

    @Test
    fun `double letter in target — guess has one at wrong position`() {
        // target=LLAMA, guess=ALARM
        // A at 0: target L => not match. A in LLAMA at 2,4 => PRESENT
        // L at 1: target L => CORRECT
        // A at 2: target A => CORRECT
        // R at 3: target M => not match. R not in LLAMA => ABSENT
        // M at 4: target A => not match. M in LLAMA at 3 => PRESENT
        val result = evaluate("ALARM", "LLAMA")
        assertEquals(TileState.PRESENT, result[0].second) // A
        assertEquals(TileState.CORRECT, result[1].second)  // L
        assertEquals(TileState.CORRECT, result[2].second)  // A
        assertEquals(TileState.ABSENT, result[3].second)    // R
        assertEquals(TileState.PRESENT, result[4].second)  // M
    }

    @Test
    fun `three of same letter — one correct two absent`() {
        // target=STEAL, guess=STEEL
        // S at 0: CORRECT
        // T at 1: CORRECT
        // E at 2: CORRECT (target E at pos 2)
        // E at 3: target A. E in target? Only 1 E (pos 2) already consumed => ABSENT
        // L at 4: CORRECT
        val result = evaluate("STEEL", "STEAL")
        assertEquals(TileState.CORRECT, result[0].second) // S
        assertEquals(TileState.CORRECT, result[1].second) // T
        assertEquals(TileState.CORRECT, result[2].second) // E
        assertEquals(TileState.ABSENT, result[3].second)  // E (no more E's)
        assertEquals(TileState.CORRECT, result[4].second) // L
    }

    @Test
    fun `guess with duplicate where first is wrong and second is correct`() {
        // target=ABIDE, guess=BRAID
        // B at 0: target A => not match. B in ABIDE at 1 => PRESENT
        // R at 1: target B => not match. R not in ABIDE => ABSENT
        // A at 2: target I => not match. A in ABIDE at 0 => PRESENT
        // I at 3: target D => not match. I in ABIDE at 2 => PRESENT
        // D at 4: target E => not match. D in ABIDE at 3 => PRESENT
        val result = evaluate("BRAID", "ABIDE")
        assertEquals(TileState.PRESENT, result[0].second) // B
        assertEquals(TileState.ABSENT, result[1].second)  // R
        assertEquals(TileState.PRESENT, result[2].second) // A
        assertEquals(TileState.PRESENT, result[3].second) // I
        assertEquals(TileState.PRESENT, result[4].second) // D
    }

    // ── 4-letter words (Easy difficulty) ─────────────────────────────────────

    @Test
    fun `4-letter word — exact match`() {
        val result = evaluate("BAKE", "BAKE")
        assertTrue(result.all { it.second == TileState.CORRECT })
        assertEquals(4, result.size)
    }

    @Test
    fun `4-letter word — mixed states`() {
        // target=FISH, guess=FIST
        // F: CORRECT, I: CORRECT, S: CORRECT, T: ABSENT (H)
        val result = evaluate("FIST", "FISH")
        assertEquals(TileState.CORRECT, result[0].second) // F
        assertEquals(TileState.CORRECT, result[1].second) // I
        assertEquals(TileState.CORRECT, result[2].second) // S
        assertEquals(TileState.ABSENT, result[3].second)  // T
    }

    // ── 6-letter words (Hard difficulty) ─────────────────────────────────────

    @Test
    fun `6-letter word — exact match`() {
        val result = evaluate("BRIDGE", "BRIDGE")
        assertTrue(result.all { it.second == TileState.CORRECT })
        assertEquals(6, result.size)
    }

    @Test
    fun `6-letter word — all absent`() {
        val result = evaluate("STRUCK", "ONLINE")
        assertTrue(result.all { it.second == TileState.ABSENT })
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `returned chars match input guess`() {
        val result = evaluate("CRANE", "HOUSE")
        result.forEachIndexed { i, (ch, _) ->
            assertEquals("Char at index $i should match guess", "CRANE"[i], ch)
        }
    }

    @Test
    fun `result length matches word length`() {
        assertEquals(5, evaluate("CRANE", "HOUSE").size)
        assertEquals(4, evaluate("BAKE", "FISH").size)
        assertEquals(6, evaluate("BRIDGE", "CASTLE").size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mismatched lengths should throw`() {
        evaluate("CAT", "HOUSE")
    }

    @Test
    fun `all same letter guess against normal word`() {
        // target=APPLE, guess=AAAAA
        // A at 0: CORRECT (A at pos 0)
        // A at 1: not P => A in target only at pos 0, already consumed => ABSENT
        // A at 2: not P => ABSENT (same reason)
        // A at 3: not L => ABSENT
        // A at 4: not E => ABSENT
        val result = evaluate("AAAAA", "APPLE")
        assertEquals(TileState.CORRECT, result[0].second)
        assertEquals(TileState.ABSENT, result[1].second)
        assertEquals(TileState.ABSENT, result[2].second)
        assertEquals(TileState.ABSENT, result[3].second)
        assertEquals(TileState.ABSENT, result[4].second)
    }
}
