package com.djtaylor.wordjourney.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["word"], unique = true), Index(value = ["length"])]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,          // UPPERCASE stored for fast comparison
    val length: Int,           // 3, 4, 5, 6, or 7
    val definition: String     // short definition shown on win
)
