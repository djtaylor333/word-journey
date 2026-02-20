package com.djtaylor.wordjourney.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    /** Total count of words for a given letter length. */
    @Query("SELECT COUNT(*) FROM words WHERE length = :length")
    suspend fun countByLength(length: Int): Int

    /**
     * Deterministic level-to-word mapping:
     * level index wraps around the word list so every level always resolves
     * to the same word, even after reinstall.
     * idx = (level - 1) % count is computed by the caller.
     */
    @Query("SELECT * FROM words WHERE length = :length ORDER BY id ASC LIMIT 1 OFFSET :offset")
    suspend fun getWordAtOffset(length: Int, offset: Int): WordEntity?

    /** Validate a player-entered guess against the full word list. */
    @Query("SELECT COUNT(*) FROM words WHERE word = :word AND length = :length")
    suspend fun isValidWord(word: String, length: Int): Int

    /** Used to populate the removed-letter item — fetch a random absent letter. */
    @Query("SELECT * FROM words WHERE length = :length ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWord(length: Int): WordEntity?

    @Query("SELECT * FROM words WHERE length = :length ORDER BY id ASC")
    suspend fun getAllByLength(length: Int): List<WordEntity>
}
