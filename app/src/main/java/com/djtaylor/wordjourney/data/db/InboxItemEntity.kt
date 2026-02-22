package com.djtaylor.wordjourney.data.db

import androidx.room.*

/**
 * Represents an item in the player's inbox.
 * Items accumulate here (VIP daily rewards, seasonal events, promos) until
 * the player explicitly claims them — even if they miss multiple days.
 */
@Entity(tableName = "inbox_items")
data class InboxItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Category: "vip_daily", "seasonal", "promo", "admin" */
    val type: String,
    val title: String,
    val message: String,
    val coinsGranted: Long = 0L,
    val diamondsGranted: Int = 0,
    val livesGranted: Int = 0,
    val addGuessItemsGranted: Int = 0,
    val removeLetterItemsGranted: Int = 0,
    val definitionItemsGranted: Int = 0,
    val showLetterItemsGranted: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val claimed: Boolean = false,
    val claimedAt: Long = 0L
)

@Dao
interface InboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InboxItemEntity): Long

    @Query("SELECT * FROM inbox_items WHERE claimed = 0 ORDER BY createdAt DESC")
    suspend fun getUnclaimed(): List<InboxItemEntity>

    @Query("SELECT * FROM inbox_items ORDER BY claimed ASC, createdAt DESC LIMIT 50")
    suspend fun getAll(): List<InboxItemEntity>

    @Query("SELECT COUNT(*) FROM inbox_items WHERE claimed = 0")
    suspend fun countUnclaimed(): Int

    @Query("UPDATE inbox_items SET claimed = 1, claimedAt = :claimedAt WHERE id = :id")
    suspend fun markClaimed(id: Int, claimedAt: Long)

    @Query("UPDATE inbox_items SET claimed = 1, claimedAt = :claimedAt WHERE claimed = 0")
    suspend fun claimAll(claimedAt: Long)

    @Query("SELECT * FROM inbox_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): InboxItemEntity?

    @Query("SELECT * FROM inbox_items WHERE type = :type AND DATE(createdAt / 1000, 'unixepoch') = :dateStr LIMIT 1")
    suspend fun getByTypeAndDate(type: String, dateStr: String): InboxItemEntity?

    @Query("DELETE FROM inbox_items WHERE claimed = 1 AND claimedAt < :beforeTs")
    suspend fun deleteClaimed(beforeTs: Long)
}
