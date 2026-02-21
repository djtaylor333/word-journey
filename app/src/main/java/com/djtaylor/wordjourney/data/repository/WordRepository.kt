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
    private val validWordSets: Map<Int, Set<String>> by lazy { loadValidWords() }

    // ── Per-player seed for deterministic but randomized word ordering ──
    private var _playerSeed: Long? = null

    // ── Cached shuffled word lists per length ──
    private val shuffledWordsCache = mutableMapOf<Int, List<WordEntity>>()

    private fun loadValidWords(): Map<Int, Set<String>> {
        return try {
            val json = context.assets.open("valid_words.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            mapOf(
                4 to parseWordSet(root, "4"),
                5 to parseWordSet(root, "5"),
                6 to parseWordSet(root, "6")
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
            set.add(arr.getString(i))
        }
        return set
    }

    /**
     * Returns the per-player random seed, generating one on first use.
     * Stored in SharedPreferences (separate from DataStore) for simplicity.
     */
    fun getPlayerSeed(): Long {
        _playerSeed?.let { return it }
        val prefs = context.getSharedPreferences("word_journey_seed", Context.MODE_PRIVATE)
        var seed = prefs.getLong("player_seed", 0L)
        if (seed == 0L) {
            seed = System.nanoTime() xor (Math.random() * Long.MAX_VALUE).toLong()
            if (seed == 0L) seed = 1L // avoid 0
            prefs.edit().putLong("player_seed", seed).apply()
        }
        _playerSeed = seed
        return seed
    }

    /** Visible for testing — allows injecting a known seed. */
    fun setPlayerSeedForTesting(seed: Long) {
        _playerSeed = seed
        shuffledWordsCache.clear()
    }

    /**
     * Returns the shuffled word list for a given word length.
     * Uses the player's unique seed so each player gets a different order,
     * but the same player always sees the same sequence.
     */
    private suspend fun getShuffledWords(length: Int): List<WordEntity> {
        return shuffledWordsCache.getOrPut(length) {
            val allWords = wordDao.getAllByLength(length)
            allWords.toMutableList().also {
                java.util.Collections.shuffle(it, java.util.Random(getPlayerSeed() + length))
            }
        }
    }

    /**
     * Returns the target word for a given [difficulty] and [level].
     * Uses per-player seeded shuffle so different players get different word orders.
     * Returns null only if the word list is unexpectedly empty.
     */
    suspend fun getWordForLevel(difficulty: Difficulty, level: Int): String? {
        val words = getShuffledWords(difficulty.wordLength)
        if (words.isEmpty()) return null
        val index = (level - 1) % words.size
        return words[index].word
    }

    /**
     * Returns the definition for a given word (displayed on win screen).
     */
    suspend fun getDefinition(difficulty: Difficulty, level: Int): String {
        val words = getShuffledWords(difficulty.wordLength)
        if (words.isEmpty()) return ""
        val index = (level - 1) % words.size
        return words[index].definition
    }

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
