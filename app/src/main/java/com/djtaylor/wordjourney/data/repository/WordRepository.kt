package com.djtaylor.wordjourney.data.repository

import android.content.Context
import com.djtaylor.wordjourney.data.db.WordDao
import com.djtaylor.wordjourney.data.db.WordEntity
import com.djtaylor.wordjourney.domain.model.Difficulty
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao,
    @ApplicationContext private val context: Context
) {
    // ── Comprehensive dictionary for guess validation (loaded from valid_words.json) ──
    private val validWordSets: Map<Int, Set<String>> by lazy {
        _overrideWordSets ?: loadValidWords()
    }

    /** For testing: inject word sets directly instead of loading from assets. */
    private var _overrideWordSets: Map<Int, Set<String>>? = null

    /** Visible for testing — override the valid-word dictionary with a known set. */
    fun setWordSetsForTesting(wordSets: Map<Int, Set<String>>) {
        _overrideWordSets = wordSets
        // Note: if validWordSets lazy was already initialised, this won't take effect.
        // Call this BEFORE first call to isValidWord.
    }

    /**
     * Fixed global seed used to shuffle word order.
     * Every player sees the same word for the same level number.
     * Changing this seed would reset the order for everyone.
     */
    companion object {
        const val GLOBAL_WORD_SEED = 2025_06_28L

        /**
         * For word lengths shared between standard and VIP difficulties (4, 5, 6),
         * the shuffled list is partitioned so VIP and standard levels never share words.
         * Words at indices 0..VIP_POOL_START[len]-1 → standard difficulties only.
         * Words at indices VIP_POOL_START[len]..end  → VIP only.
         * Lengths 3 and 7 are VIP-exclusive so no partition is needed.
         */
        val VIP_POOL_START = mapOf(4 to 76, 5 to 97, 6 to 193)
    }

    // ── Cached shuffled word lists per length ──
    private val shuffledWordsCache = mutableMapOf<Int, List<WordEntity>>()

    /** Allow overriding the seed in tests. Defaults to GLOBAL_WORD_SEED. */
    private var _testSeed: Long? = null

    private fun loadValidWords(): Map<Int, Set<String>> {
        return try {
            val json = context.assets.open("valid_words.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            mapOf(
                3 to parseWordSet(root, "3"),
                4 to parseWordSet(root, "4"),
                5 to parseWordSet(root, "5"),
                6 to parseWordSet(root, "6"),
                7 to parseWordSet(root, "7")
            )
        } catch (e: Exception) {
            android.util.Log.e("WordRepository", "Failed to load valid_words.json", e)
            emptyMap()
        }
    }

    private fun parseWordSet(root: JSONObject, key: String): Set<String> {
        val arr = root.getJSONArray(key)
        val set = HashSet<String>(arr.length())
        for (i in 0 until arr.length()) {
            set.add(arr.getString(i).uppercase())
        }
        return set
    }

    /**
     * Returns the partitioned word list for the given difficulty and length.
     * For lengths 4, 5, 6 that are shared between VIP and standard difficulties,
     * this ensures VIP and non-VIP levels use completely separate word pools.
     */
    private suspend fun getWordsForDifficulty(difficulty: Difficulty, length: Int): List<WordEntity> {
        val all = getShuffledWords(length)
        val splitPoint = VIP_POOL_START[length]
        return when {
            splitPoint == null -> all                                 // lengths 3/7 — VIP only
            difficulty == Difficulty.VIP -> all.drop(splitPoint)     // VIP partition
            else -> all.take(splitPoint)                             // standard partition
        }
    }

    /**
     * Returns the per-player random seed, generating one on first use.
     * Stored in SharedPreferences (separate from DataStore) for simplicity.
     */

    private fun getEffectiveSeed(): Long = _testSeed ?: GLOBAL_WORD_SEED

    /** Visible for testing — allows injecting a known seed. */
    fun setSeedForTesting(seed: Long) {
        _testSeed = seed
        shuffledWordsCache.clear()
    }

    /**
     * Returns the shuffled word list for a given word length.
     * Uses a fixed global seed so every player gets the SAME word order.
     */
    private suspend fun getShuffledWords(length: Int): List<WordEntity> {
        return shuffledWordsCache.getOrPut(length) {
            val allWords = wordDao.getAllByLength(length)
            allWords.toMutableList().also {
                java.util.Collections.shuffle(it, java.util.Random(getEffectiveSeed() + length))
            }
        }
    }

    /**
     * Returns the target word for a given [difficulty] and [level].
     * Uses a fixed global seed so all players get the same word for the same level.
     * Returns null only if the word list is unexpectedly empty.
     *
     * @param wordLengthOverride If provided, uses this word length instead of the difficulty's default.
     *                           Used for VIP difficulty which varies word length by level.
     */
    suspend fun getWordForLevel(difficulty: Difficulty, level: Int, wordLengthOverride: Int? = null): String? {
        val length = wordLengthOverride ?: difficulty.wordLength
        val words = getWordsForDifficulty(difficulty, length)
        if (words.isEmpty()) return null
        val index = (level - 1) % words.size
        return words[index].word
    }

    /**
     * Returns the definition for a given word (displayed on win screen).
     *
     * @param wordLengthOverride If provided, uses this word length instead of the difficulty's default.
     *                           Used for VIP difficulty which varies word length by level.
     */
    suspend fun getDefinition(difficulty: Difficulty, level: Int, wordLengthOverride: Int? = null): String {
        val length = wordLengthOverride ?: difficulty.wordLength
        val words = getWordsForDifficulty(difficulty, length)
        if (words.isEmpty()) return ""
        val index = (level - 1) % words.size
        return words[index].definition
    }

    /**
     * Returns true if the word at the given level has a non-blank definition.
     * Used to decide whether to enable the Definition item button.
     */
    suspend fun hasDefinition(difficulty: Difficulty, level: Int, wordLengthOverride: Int? = null): Boolean =
        getDefinition(difficulty, level, wordLengthOverride).isNotBlank()

    /**
     * Validates that the player's guess is a real English word.
     * Uses a comprehensive 32,000+ word dictionary loaded from valid_words.json.
     * Word should be passed UPPERCASE.
     */
    suspend fun isValidWord(word: String, length: Int): Boolean {
        return validWordSets[length]?.contains(word.uppercase()) ?: false
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
