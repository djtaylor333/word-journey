package com.djtaylor.wordjourney.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.json.JSONObject

@Database(entities = [WordEntity::class], version = 1, exportSchema = false)
abstract class WordDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    /**
     * Checks if the word database is empty and populates it from assets/words.json.
     * Uses org.json (Android built-in, never stripped by R8) instead of kotlinx.serialization.
     * Called once from DatabaseModule on app startup.
     */
    suspend fun ensurePopulated(context: Context) {
        val dao = wordDao()
        val total = dao.countByLength(4) + dao.countByLength(5) + dao.countByLength(6)
        if (total > 0) {
            Log.d(TAG, "Database already populated with $total words")
            return
        }
        Log.d(TAG, "Database is empty — populating from words.json")
        try {
            val jsonStr = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val words = mutableListOf<WordEntity>()

            for (lengthKey in root.keys()) {
                val length = lengthKey.toIntOrNull() ?: continue
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
            Log.d(TAG, "Successfully populated ${words.size} words")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate word database", e)
        }
    }

    companion object {
        private const val TAG = "WordDatabase"
        const val DATABASE_NAME = "word_journeys.db"

        fun buildDatabase(context: Context): WordDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WordDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
