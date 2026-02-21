package com.djtaylor.wordjourney.data.db

import androidx.room.*

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["key"], unique = true)]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,              // unique identifier e.g. "first_win"
    val unlockedAt: Long = 0L,    // epoch ms, 0 = not unlocked
    val progress: Int = 0,        // current progress (for progressive achievements)
    val target: Int = 1           // target to unlock
)

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE `key` = :key")
    suspend fun get(key: String): AchievementEntity?

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements WHERE unlockedAt > 0")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun countTotal(): Int
}
