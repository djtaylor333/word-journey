package com.djtaylor.wordjourney.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject

@Database(
    entities = [
        WordEntity::class,
        StarRatingEntity::class,
        DailyChallengeResultEntity::class,
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WordDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun starRatingDao(): StarRatingDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun achievementDao(): AchievementDao

    /**
     * Checks if the word database is empty and populates it from assets/words.json.
     * Uses org.json (Android built-in, never stripped by R8) instead of kotlinx.serialization.
     * Called once from DatabaseModule on app startup.
     */
    suspend fun ensurePopulated(context: Context) {
        val dao = wordDao()
        val count3 = dao.countByLength(3)
        val count4 = dao.countByLength(4)
        val count5 = dao.countByLength(5)
        val count6 = dao.countByLength(6)
        val count7 = dao.countByLength(7)
        val total = count3 + count4 + count5 + count6 + count7

        // Determine which word lengths still need to be populated
        val existingLengths = mutableSetOf<Int>()
        if (count3 > 0) existingLengths.add(3)
        if (count4 > 0) existingLengths.add(4)
        if (count5 > 0) existingLengths.add(5)
        if (count6 > 0) existingLengths.add(6)
        if (count7 > 0) existingLengths.add(7)

        val allLengths = setOf(3, 4, 5, 6, 7)
        if (existingLengths == allLengths) {
            Log.d(TAG, "Database already populated with $total words across all lengths")
            return
        }

        val needsLengths = allLengths - existingLengths
        if (existingLengths.isEmpty()) {
            Log.d(TAG, "Database is empty — populating from words.json")
        } else {
            Log.d(TAG, "Database missing word lengths $needsLengths — adding from words.json")
        }
        try {
            val jsonStr = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val words = mutableListOf<WordEntity>()

            for (lengthKey in root.keys()) {
                val length = lengthKey.toIntOrNull() ?: continue
                if (length !in needsLengths) continue // skip already populated lengths
                val arr = root.getJSONArray(lengthKey)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val word = obj.getString("word").uppercase()
                    val definition = obj.optString("definition", "")
                    words.add(WordEntity(word = word, length = length, definition = definition))
                }
            }

            // Insert in chunks to avoid transaction size limits
            words.chunked(200).forEach { chunk ->
                dao.insertAll(chunk)
            }
            Log.d(TAG, "Successfully populated ${words.size} words for lengths $needsLengths")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate word database", e)
        }
    }

    companion object {
        private const val TAG = "WordDatabase"
        const val DATABASE_NAME = "word_journeys.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `star_ratings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `difficultyKey` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `stars` INTEGER NOT NULL,
                        `guessCount` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_star_ratings_difficultyKey_level` ON `star_ratings` (`difficultyKey`, `level`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_challenge_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `wordLength` INTEGER NOT NULL,
                        `word` TEXT NOT NULL,
                        `guessCount` INTEGER NOT NULL,
                        `won` INTEGER NOT NULL DEFAULT 0,
                        `stars` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_challenge_results_date_wordLength` ON `daily_challenge_results` (`date`, `wordLength`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `unlockedAt` INTEGER NOT NULL DEFAULT 0,
                        `progress` INTEGER NOT NULL DEFAULT 0,
                        `target` INTEGER NOT NULL DEFAULT 1
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievements_key` ON `achievements` (`key`)")
            }
        }

        fun buildDatabase(context: Context): WordDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WordDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2)
            .build()
        }
    }
}
