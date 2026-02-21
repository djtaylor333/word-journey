package com.djtaylor.wordjourney.data.db

import androidx.room.*

@Entity(
    tableName = "star_ratings",
    indices = [Index(value = ["difficultyKey", "level"], unique = true)]
)
data class StarRatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val difficultyKey: String,
    val level: Int,
    val stars: Int,           // 1-3
    val guessCount: Int
)

@Dao
interface StarRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: StarRatingEntity)

    @Query("SELECT * FROM star_ratings WHERE difficultyKey = :difficultyKey AND level = :level")
    suspend fun get(difficultyKey: String, level: Int): StarRatingEntity?

    @Query("SELECT * FROM star_ratings WHERE difficultyKey = :difficultyKey ORDER BY level ASC")
    suspend fun getAllForDifficulty(difficultyKey: String): List<StarRatingEntity>

    @Query("SELECT COALESCE(SUM(stars), 0) FROM star_ratings WHERE difficultyKey = :difficultyKey")
    suspend fun totalStarsForDifficulty(difficultyKey: String): Int

    @Query("SELECT COALESCE(SUM(stars), 0) FROM star_ratings")
    suspend fun totalStars(): Int

    @Query("SELECT COUNT(*) FROM star_ratings WHERE stars = 3")
    suspend fun countPerfectLevels(): Int
}
