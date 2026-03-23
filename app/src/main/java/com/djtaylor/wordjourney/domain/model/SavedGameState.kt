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
    val revealedLetters: Map<String, String> = emptyMap(), // position index (String) → char (String)
    val savedDate: String = ""            // ISO date (yyyy-MM-dd) when this save was created; used to detect stale daily-challenge saves
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
    val totalDailyChallengesPlayed: Int = 0,   // includes losses (for win rate)
    // Timer Mode best records
    val timerBestLevelsEasy: Int = 0,
    val timerBestLevelsRegular: Int = 0,
    val timerBestLevelsHard: Int = 0,
    val timerBestTimeSecsEasy: Int = 0,        // total session seconds (base + bonus)
    val timerBestTimeSecsRegular: Int = 0,
    val timerBestTimeSecsHard: Int = 0,
    // Time played tracking (milliseconds — only counted when on a level screen, screen on)
    val totalTimePlayedMs: Long = 0L,
    val easyTimePlayedMs: Long = 0L,
    val regularTimePlayedMs: Long = 0L,
    val hardTimePlayedMs: Long = 0L,
    val vipTimePlayedMs: Long = 0L,
    val dailyTimePlayedMs: Long = 0L,
    val timerTimePlayedMs: Long = 0L,
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
    val notifyDailyChallenge: Boolean = true,    // noon reminder to play daily challenge
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
    val hasCompletedOnboarding: Boolean = false,
    // Dev Mode — enabled by tapping version string 10 times (disabled by 3 more taps)
    val devModeEnabled: Boolean = false,
    // Seasonal themed level pack progress (1-based, 100 levels per pack)
    val seasonalEasterLevel: Int = 1,
    val seasonalValentinesLevel: Int = 1,
    val seasonalSummerLevel: Int = 1,
    val seasonalHalloweenLevel: Int = 1,
    val seasonalThanksgivingLevel: Int = 1,
    val seasonalChristmasLevel: Int = 1,
    // Seasonal pack first-open timestamps (epoch ms, 0 = never opened)
    val seasonalFirstOpenEaster: Long = 0L,
    val seasonalFirstOpenValentines: Long = 0L,
    val seasonalFirstOpenSummer: Long = 0L,
    val seasonalFirstOpenHalloween: Long = 0L,
    val seasonalFirstOpenThanksgiving: Long = 0L,
    val seasonalFirstOpenChristmas: Long = 0L,
    // Seasonal pack milestone tiers claimed (0 = none, 1 = claimed 10-level reward, 2 = 20, …, 10 = 100)
    val seasonalMilestoneEaster: Int = 0,
    val seasonalMilestoneValentines: Int = 0,
    val seasonalMilestoneSummer: Int = 0,
    val seasonalMilestoneHalloween: Int = 0,
    val seasonalMilestoneThanksgiving: Int = 0,
    val seasonalMilestoneChristmas: Int = 0,
    // Stars earned on last completed regular level (used to grant free definition on next level)
    val lastLevelStars: Int = 0,
    // Date when all 3 daily challenge lengths (4/5/6) were won in the same day (YYYY-MM-DD)
    val dailyAllThreeCompletedDate: String = "",
    // In-app review prompt tracking
    // Counts levels completed (non-timer) since last review was requested; resets to 0 after prompt
    val levelsCompletedForReview: Int = 0,
    // True once the review prompt has been shown (so we only ask once)
    val hasReviewBeenRequested: Boolean = false,
    // True once the review reward (5 lives + 1000 coins + 10 diamonds) has been granted
    val reviewRewarded: Boolean = false
)

// ── Seasonal level helpers ────────────────────────────────────────────────────

/** Returns the player's current level for the given season pack key (e.g. "easter"). */
fun PlayerProgress.seasonalLevelFor(seasonKey: String): Int = when (seasonKey) {
    "easter"       -> seasonalEasterLevel
    "valentines"   -> seasonalValentinesLevel
    "summer"       -> seasonalSummerLevel
    "halloween"    -> seasonalHalloweenLevel
    "thanksgiving" -> seasonalThanksgivingLevel
    "christmas"    -> seasonalChristmasLevel
    else           -> 1
}

/** Returns a copy of this progress with the named season's level advanced to [newLevel]. */
fun PlayerProgress.withSeasonalLevelAdvanced(seasonKey: String, newLevel: Int): PlayerProgress = when (seasonKey) {
    "easter"       -> copy(seasonalEasterLevel       = newLevel)
    "valentines"   -> copy(seasonalValentinesLevel   = newLevel)
    "summer"       -> copy(seasonalSummerLevel       = newLevel)
    "halloween"    -> copy(seasonalHalloweenLevel    = newLevel)
    "thanksgiving" -> copy(seasonalThanksgivingLevel = newLevel)
    "christmas"    -> copy(seasonalChristmasLevel    = newLevel)
    else           -> this
}

/** Returns the last milestone tier claimed for the given season pack (0 = none). */
fun PlayerProgress.seasonalMilestoneFor(seasonKey: String): Int = when (seasonKey) {
    "easter"       -> seasonalMilestoneEaster
    "valentines"   -> seasonalMilestoneValentines
    "summer"       -> seasonalMilestoneSummer
    "halloween"    -> seasonalMilestoneHalloween
    "thanksgiving" -> seasonalMilestoneThanksgiving
    "christmas"    -> seasonalMilestoneChristmas
    else           -> 0
}

/** Returns a copy with the named season's milestone tier updated. */
fun PlayerProgress.withSeasonalMilestone(seasonKey: String, tier: Int): PlayerProgress = when (seasonKey) {
    "easter"       -> copy(seasonalMilestoneEaster       = tier)
    "valentines"   -> copy(seasonalMilestoneValentines   = tier)
    "summer"       -> copy(seasonalMilestoneSummer       = tier)
    "halloween"    -> copy(seasonalMilestoneHalloween    = tier)
    "thanksgiving" -> copy(seasonalMilestoneThanksgiving = tier)
    "christmas"    -> copy(seasonalMilestoneChristmas    = tier)
    else           -> this
}

/** Returns the epoch-ms timestamp when the pack was first opened (0 = never). */
fun PlayerProgress.seasonalFirstOpenFor(seasonKey: String): Long = when (seasonKey) {
    "easter"       -> seasonalFirstOpenEaster
    "valentines"   -> seasonalFirstOpenValentines
    "summer"       -> seasonalFirstOpenSummer
    "halloween"    -> seasonalFirstOpenHalloween
    "thanksgiving" -> seasonalFirstOpenThanksgiving
    "christmas"    -> seasonalFirstOpenChristmas
    else           -> 0L
}

/** Returns a copy with the named season's first-open timestamp recorded. */
fun PlayerProgress.withSeasonalFirstOpen(seasonKey: String, timestampMs: Long): PlayerProgress = when (seasonKey) {
    "easter"       -> copy(seasonalFirstOpenEaster       = timestampMs)
    "valentines"   -> copy(seasonalFirstOpenValentines   = timestampMs)
    "summer"       -> copy(seasonalFirstOpenSummer       = timestampMs)
    "halloween"    -> copy(seasonalFirstOpenHalloween    = timestampMs)
    "thanksgiving" -> copy(seasonalFirstOpenThanksgiving = timestampMs)
    "christmas"    -> copy(seasonalFirstOpenChristmas    = timestampMs)
    else           -> this
}
