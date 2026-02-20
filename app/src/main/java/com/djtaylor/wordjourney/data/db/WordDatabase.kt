package com.djtaylor.wordjourney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Database(entities = [WordEntity::class], version = 1, exportSchema = true)
abstract class WordDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        const val DATABASE_NAME = "word_journeys.db"

        fun buildDatabase(context: Context): WordDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WordDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate from assets/words.json on first install
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = buildDatabase(context)
                            populateFromAssets(context, database.wordDao())
                        }
                    }
                })
                .build()
        }

        private suspend fun populateFromAssets(context: Context, dao: WordDao) {
            try {
                val json = context.assets.open("words.json")
                    .bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(json).jsonObject
                val words = mutableListOf<WordEntity>()

                for ((lengthKey, arrayElement) in root) {
                    val length = lengthKey.toIntOrNull() ?: continue
                    for (item in arrayElement.jsonArray) {
                        val obj = item.jsonObject
                        val word = obj["word"]?.jsonPrimitive?.content?.uppercase() ?: continue
                        val definition = obj["definition"]?.jsonPrimitive?.content ?: ""
                        words.add(WordEntity(word = word, length = length, definition = definition))
                    }
                }
                dao.insertAll(words)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
