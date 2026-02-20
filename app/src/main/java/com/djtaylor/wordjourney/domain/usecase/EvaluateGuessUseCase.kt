package com.djtaylor.wordjourney.domain.usecase

import com.djtaylor.wordjourney.domain.model.TileState
import javax.inject.Inject

/**
 * Evaluates a player's guess against the target word using the canonical
 * Wordle algorithm with correct duplicate-letter handling:
 *
 * Pass 1 – mark all exact-position matches as CORRECT (green).
 *           Decrement each matched letter's remaining count in the target.
 * Pass 2 – for non-green positions, mark PRESENT (yellow) only if that
 *           letter still has remaining count in the target, then decrement;
 *           otherwise mark ABSENT (grey).
 *
 * This ensures a letter is never shown as yellow more times than it
 * actually appears in the target word.
 */
class EvaluateGuessUseCase @Inject constructor() {

    /**
     * @param guess      the player's submitted word, already uppercased
     * @param target     the secret target word, already uppercased
     * @return a list of [Pair<Char, TileState>] the same length as [guess]
     */
    operator fun invoke(
        guess: String,
        target: String
    ): List<Pair<Char, TileState>> {
        require(guess.length == target.length) {
            "Guess length ${guess.length} must equal target length ${target.length}"
        }

        val length = guess.length
        val result = Array(length) { Pair(guess[it], TileState.ABSENT) }

        // Frequency map of remaining unmatched letters in the target
        val targetFreq = IntArray(26)
        for (ch in target) targetFreq[ch - 'A']++

        // Pass 1: mark CORRECT, remove from frequency pool
        for (i in 0 until length) {
            if (guess[i] == target[i]) {
                result[i] = Pair(guess[i], TileState.CORRECT)
                targetFreq[guess[i] - 'A']--
            }
        }

        // Pass 2: mark PRESENT for remaining unmatched positions
        for (i in 0 until length) {
            if (result[i].second != TileState.CORRECT) {
                val idx = guess[i] - 'A'
                if (idx in 0..25 && targetFreq[idx] > 0) {
                    result[i] = Pair(guess[i], TileState.PRESENT)
                    targetFreq[idx]--
                }
                // else remains ABSENT
            }
        }

        return result.toList()
    }
}
