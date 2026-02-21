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
    ),
    VIP(
        displayName = "VIP",
        wordLength = 5, // default; actual length varies 3-7 by level
        maxGuesses = 6,
        bonusAttemptsPerLife = 2,
        levelBonusThreshold = 5, // bonus life every 5 levels
        saveKey = "vip"
    );

    companion object {
        /**
         * For VIP difficulty, word length varies by level (3-7 letters cycling).
         * Returns the effective word length for a given VIP level.
         */
        fun vipWordLengthForLevel(level: Int): Int {
            // Cycle through 3, 4, 5, 6, 7 letter words
            val lengths = intArrayOf(3, 4, 5, 6, 7)
            return lengths[(level - 1) % lengths.size]
        }
    }
}
