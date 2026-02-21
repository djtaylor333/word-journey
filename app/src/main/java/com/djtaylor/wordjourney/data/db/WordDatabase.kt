package com.djtaylor.wordjourney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Database(entities = [WordEntity::class], version = 1, exportSchema = false)
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
                        // Populate synchronously via raw SQL so words are
                        // available immediately when the database is first opened.
                        populateWithRawSql(context.applicationContext, db)
                    }
                })
                .build()
        }

        /**
         * Inserts all words from assets/words.json using raw SQL.
         * Runs synchronously inside the onCreate callback so the data
         * is guaranteed to exist before anything queries the database.
         */
        private fun populateWithRawSql(context: Context, db: SupportSQLiteDatabase) {
            try {
                val json = context.assets.open("words.json")
                    .bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(json).jsonObject

                db.beginTransaction()
                try {
                    for ((lengthKey, arrayElement) in root) {
                        val length = lengthKey.toIntOrNull() ?: continue
                        for (item in arrayElement.jsonArray) {
                            val obj = item.jsonObject
                            val word = obj["word"]?.jsonPrimitive?.content?.uppercase() ?: continue
                            val definition = obj["definition"]?.jsonPrimitive?.content ?: ""
                            db.execSQL(
                                "INSERT OR IGNORE INTO words (word, length, definition) VALUES (?, ?, ?)",
                                arrayOf(word, length, definition)
                            )
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
