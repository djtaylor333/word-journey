package com.djtaylor.wordjourney.domain.model

enum class Difficulty(
    val displayName: String,
    val wordLength: Int,
    val maxGuesses: Int,
    val bonusAttemptsPerLife: Int,
    val levelBonusThreshold: Int, // levels completed before earning a bonus life
    val saveKey: String
) {
    EASY(
        displayName = "Easy",
        wordLength = 4,
        maxGuesses = 6,
        bonusAttemptsPerLife = 3,
        levelBonusThreshold = 10,
        saveKey = "easy"
    ),
    REGULAR(
        displayName = "Regular",
        wordLength = 5,
        maxGuesses = 6,
        bonusAttemptsPerLife = 2,
        levelBonusThreshold = 5,
        saveKey = "regular"
    ),
    HARD(
        displayName = "Hard",
        wordLength = 6,
        maxGuesses = 6,
        bonusAttemptsPerLife = 1,
        levelBonusThreshold = 3,
        saveKey = "hard"
    )
}
