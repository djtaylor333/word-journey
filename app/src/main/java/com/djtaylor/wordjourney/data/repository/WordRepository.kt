package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.db.WordDao
import com.djtaylor.wordjourney.domain.model.Difficulty
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    /**
     * Returns the target word for a given [difficulty] and [level].
     * Mapping is deterministic: same level always yields the same word.
     * Returns null only if the word list is unexpectedly empty.
     */
    suspend fun getWordForLevel(difficulty: Difficulty, level: Int): String? {
        val count = wordDao.countByLength(difficulty.wordLength)
        if (count == 0) return null
        val offset = (level - 1) % count
        return wordDao.getWordAtOffset(difficulty.wordLength, offset)?.word
    }

    /**
     * Returns the definition for a given word (displayed on win screen).
     */
    suspend fun getDefinition(difficulty: Difficulty, level: Int): String {
        val count = wordDao.countByLength(difficulty.wordLength)
        if (count == 0) return ""
        val offset = (level - 1) % count
        return wordDao.getWordAtOffset(difficulty.wordLength, offset)?.definition ?: ""
    }

    /**
     * Validates that the player's guess exists in the word list for a given length.
     * Word should be passed UPPERCASE.
     */
    suspend fun isValidWord(word: String, length: Int): Boolean {
        return wordDao.isValidWord(word.uppercase(), length) > 0
    }

    /**
     * Returns a letter that is guaranteed NOT in [targetWord] among unused letters,
     * used by the "Remove a Letter" item.
     */
    suspend fun findAbsentLetter(
        targetWord: String,
        alreadyEliminated: Set<Char>,
        alreadyRevealed: Set<Char>
    ): Char? {
        val targetChars = targetWord.toSet()
        val alphabet = ('A'..'Z').toList()
        return alphabet
            .filter { it !in targetChars && it !in alreadyEliminated && it !in alreadyRevealed }
            .shuffled()
            .firstOrNull()
    }
}
