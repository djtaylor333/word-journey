package com.djtaylor.wordjourney.data.repository

import android.content.Context
import com.djtaylor.wordjourney.data.db.DailyChallengeDao
import com.djtaylor.wordjourney.data.db.DailyChallengeResultEntity
import com.djtaylor.wordjourney.data.db.WordDao
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Daily Challenge mode:
 * - Picks a deterministic daily word based on the date
 * - Words come from valid_words.json (NOT the level words in words.json)
 * - One challenge per word length (4, 5, 6) per day
 */
@Singleton
class DailyChallengeRepository @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao,
    private val wordDao: WordDao,
    @ApplicationContext private val context: Context
) {
    private val dailyWordPool: Map<Int, List<String>> by lazy { loadDailyWordPool() }

    private fun loadDailyWordPool(): Map<Int, List<String>> {
        return try {
            // Load all valid words
            val json = context.assets.open("valid_words.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)

            // Load level words (to exclude from daily pool)
            val levelWords = mutableSetOf<String>()
            try {
                val levelJson = context.assets.open("words.json").bufferedReader().use { it.readText() }
                val levelRoot = JSONObject(levelJson)
                for (key in levelRoot.keys()) {
                    val arr = levelRoot.getJSONArray(key)
                    for (i in 0 until arr.length()) {
                        levelWords.add(arr.getJSONObject(i).getString("word").uppercase())
                    }
                }
            } catch (_: Exception) { }

            mapOf(
                4 to buildWordList(root, "4", levelWords),
                5 to buildWordList(root, "5", levelWords),
                6 to buildWordList(root, "6", levelWords)
            )
        } catch (e: Exception) {
            android.util.Log.e("DailyChallengeRepo", "Failed to load daily word pool", e)
            emptyMap()
        }
    }

    private fun buildWordList(root: JSONObject, key: String, exclude: Set<String>): List<String> {
        val arr = root.getJSONArray(key)
        val words = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val w = arr.getString(i).uppercase()
            if (w !in exclude) words.add(w)
        }
        // Sort for determinism before seeding
        words.sort()
        return words
    }

    fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Returns today's daily challenge word for a given word length.
     * Uses the date as a seed (DDMMYYYY numeric) so every player gets the same
     * word on the same day, but words change each day.
     */
    fun getDailyWord(wordLength: Int, dateStr: String = todayDateString()): String? {
        val pool = dailyWordPool[wordLength] ?: return null
        if (pool.isEmpty()) return null
        val seed = computeDateSeed(dateStr, wordLength)
        return pickWordByDateSeed(pool, seed)
    }

    companion object {
        /**
         * Computes a deterministic numeric seed from a date string (yyyy-MM-dd)
         * and word length using the DDMMYYYY format, e.g. 23022026 for Feb 23 2026.
         * This ensures every player gets the same word on the same day.
         */
        internal fun computeDateSeed(dateStr: String, wordLength: Int): Long {
            val parts = dateStr.split("-")
            val year  = parts.getOrNull(0)?.toLongOrNull() ?: 2024L
            val month = parts.getOrNull(1)?.toLongOrNull() ?: 1L
            val day   = parts.getOrNull(2)?.toLongOrNull() ?: 1L
            // Encode as DDMMYYYYe.g. 23022026 = 23 * 1_000_000 + 02 * 10_000 + 2026
            val datePart = day * 1_000_000L + month * 10_000L + year
            return datePart + wordLength.toLong() * 31L
        }

        /**
         * Picks a word from the pool using the given seed deterministically.
         * Same seed always returns the same word.
         */
        internal fun pickWordByDateSeed(pool: List<String>, seed: Long): String {
            val rng = Random(seed)
            val index = ((rng.nextInt(pool.size)) + pool.size) % pool.size
            return pool[index]
        }
    }

    suspend fun hasPlayedToday(wordLength: Int): Boolean {
        return dailyChallengeDao.getResult(todayDateString(), wordLength) != null
    }

    /** [DEV] Deletes all Room DB entries for today so daily challenges appear unplayed. */
    suspend fun devClearTodayResults() {
        dailyChallengeDao.deleteAllForDate(todayDateString())
    }

    suspend fun saveResult(
        wordLength: Int,
        word: String,
        guessCount: Int,
        won: Boolean,
        stars: Int,
        dateStr: String = todayDateString()
    ) {
        dailyChallengeDao.upsert(
            DailyChallengeResultEntity(
                date = dateStr,
                wordLength = wordLength,
                word = word,
                guessCount = guessCount,
                won = won,
                stars = stars
            )
        )
    }

    suspend fun getResultsForToday(): List<DailyChallengeResultEntity> {
        return dailyChallengeDao.getResultsForDate(todayDateString())
    }

    suspend fun totalWins(): Int = dailyChallengeDao.totalWins()
    suspend fun totalPlayed(): Int = dailyChallengeDao.totalPlayed()

    /**
     * Returns words of the given length from the Room database, shuffled randomly.
     * Used by Timer Mode to get a fresh word set with definitions available.
     * Words are sourced from the level word database (which includes definitions).
     */
    suspend fun getTimerWords(wordLength: Int): List<String> {
        return wordDao.getAllByLength(wordLength).map { it.word.uppercase() }.shuffled()
    }
}
