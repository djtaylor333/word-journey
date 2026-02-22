package com.djtaylor.wordjourney.domain.model

import kotlinx.serialization.Serializable

/**
 * Serializable version of in-progress game state, persisted to DataStore
 * so the player can resume exactly where they left off after closing the app.
 */
@Serializable
data class SavedGameState(
    val difficultyKey: String,             // "easy" | "regular" | "hard"
    val level: Int,
    val targetWord: String,                // stored locally only, never in cloud UI
    val completedGuesses: List<List<Pair<String, String>>> = emptyList(), // Char/TileState as strings
    val currentInput: List<String> = emptyList(),
    val maxGuesses: Int,
    val revealedLetters: Map<String, String> = emptyMap() // position index (String) → char (String)
)

@Serializable
data class PlayerProgress(
    val coins: Long = 0L,
    val diamonds: Int = 5,
    val lives: Int = 10,
    val lastLifeRegenTimestamp: Long = 0L,  // epoch ms when regen was last calculated
    val easyLevel: Int = 1,
    val regularLevel: Int = 1,
    val hardLevel: Int = 1,
    val vipLevel: Int = 1,                             // VIP level pack progress
    val easyLevelsCompletedSinceBonusLife: Int = 0,
    val regularLevelsCompletedSinceBonusLife: Int = 0,
    val hardLevelsCompletedSinceBonusLife: Int = 0,
    val vipLevelsCompletedSinceBonusLife: Int = 0,
    // Item inventory
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,
    val definitionItems: Int = 0,
    val showLetterItems: Int = 0,
    // Streak tracking
    val dailyChallengeStreak: Int = 0,
    val dailyChallengeBestStreak: Int = 0,
    val dailyChallengeLastDate: String = "",   // YYYY-MM-DD
    val loginStreak: Int = 0,
    val loginBestStreak: Int = 0,
    val lastLoginDate: String = "",            // YYYY-MM-DD
    // Per-length daily challenge streaks (consecutive days winning each length)
    val dailyStreak4: Int = 0,
    val dailyStreak5: Int = 0,
    val dailyStreak6: Int = 0,
    val dailyBestStreak4: Int = 0,
    val dailyBestStreak5: Int = 0,
    val dailyBestStreak6: Int = 0,
    val dailyLastDate4: String = "",          // last date 4-letter challenge was WON
    val dailyLastDate5: String = "",
    val dailyLastDate6: String = "",
    val dailyWins4: Int = 0,                  // total 4-letter wins
    val dailyWins5: Int = 0,
    val dailyWins6: Int = 0,
    // Cumulative statistics
    val totalCoinsEarned: Long = 0L,
    val totalLevelsCompleted: Int = 0,
    val totalGuesses: Int = 0,
    val totalWins: Int = 0,
    val totalItemsUsed: Int = 0,
    val totalDailyChallengesCompleted: Int = 0,
    // VIP
    val isVip: Boolean = false,
    val vipExpiryTimestamp: Long = 0L,
    val lastVipRewardDate: String = "",        // YYYY-MM-DD — last day VIP daily rewards were collected
    // New player bonus
    val hasReceivedNewPlayerBonus: Boolean = false,
    // Settings flags
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val notifyLivesFull: Boolean = true,
    val highContrast: Boolean = false,
    val darkMode: Boolean = true,
    val colorblindMode: String = "none",
    val textScaleFactor: Float = 1.0f,
    val playGamesSignedIn: Boolean = false,
    // Cosmetics / Themes
    val selectedTheme: String = "classic",             // active theme id
    val ownedThemes: String = "classic,ocean_breeze,forest_grove",  // comma-separated owned theme ids
    val selectedTileTheme: String = "default",
    val selectedKeyboardTheme: String = "default",
    val ownedTileThemes: String = "default",          // comma-separated
    val ownedKeyboardThemes: String = "default",      // comma-separated
    // Onboarding
    val hasCompletedOnboarding: Boolean = false
)
