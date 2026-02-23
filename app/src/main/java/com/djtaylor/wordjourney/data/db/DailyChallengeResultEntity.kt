package com.djtaylor.wordjourney.data.db

import androidx.room.*

@Entity(
    tableName = "daily_challenge_results",
    indices = [Index(value = ["date", "wordLength"], unique = true)]
)
data class DailyChallengeResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,             // YYYY-MM-DD
    val wordLength: Int,
    val word: String,
    val guessCount: Int,
    val won: Boolean,
    val stars: Int
)

@Dao
interface DailyChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyChallengeResultEntity)

    @Query("SELECT * FROM daily_challenge_results WHERE date = :date AND wordLength = :wordLength")
    suspend fun getResult(date: String, wordLength: Int): DailyChallengeResultEntity?

    @Query("SELECT * FROM daily_challenge_results WHERE date = :date ORDER BY wordLength ASC")
    suspend fun getResultsForDate(date: String): List<DailyChallengeResultEntity>

    @Query("SELECT COUNT(*) FROM daily_challenge_results WHERE won = 1")
    suspend fun totalWins(): Int

    @Query("SELECT COUNT(*) FROM daily_challenge_results")
    suspend fun totalPlayed(): Int

    @Query("SELECT * FROM daily_challenge_results ORDER BY date DESC LIMIT :limit")
    suspend fun recentResults(limit: Int = 30): List<DailyChallengeResultEntity>

    @Query("DELETE FROM daily_challenge_results WHERE date = :date")
    suspend fun deleteAllForDate(date: String)
}
