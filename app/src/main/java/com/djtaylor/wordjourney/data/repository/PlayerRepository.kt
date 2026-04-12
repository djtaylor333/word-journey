package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.cloud.CloudSaveManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SavedGameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val dataStore: PlayerDataStore,
    private val cloudSave: CloudSaveManager
) {
    val playerProgressFlow: Flow<PlayerProgress> = dataStore.playerProgressFlow
    val isFirstLaunch: Flow<Boolean> = dataStore.isFirstLaunch

    /** Load from cloud and merge (highest wins), then persist locally.
     *
     * Fixed: uses `first()` instead of `collect {}` so the function actually returns.
     * Should be called once per session from HomeViewModel before the main progress loop.
     */
    suspend fun syncFromCloud(): PlayerProgress? {
        val cloud = cloudSave.loadSave() ?: return null
        val local = dataStore.playerProgressFlow.first()
        val merged = mergeProgress(local, cloud)
        dataStore.savePlayerProgress(merged)
        return merged
    }

    suspend fun saveProgress(progress: PlayerProgress) {
        dataStore.savePlayerProgress(progress)
        cloudSave.writeSave(progress)
    }

    suspend fun saveInProgressGame(state: SavedGameState) {
        dataStore.saveInProgressGame(state)
    }

    suspend fun clearInProgressGame(difficulty: Difficulty) {
        dataStore.clearInProgressGame(difficulty.saveKey)
    }

    suspend fun clearInProgressGame(difficultyKey: String) {
        dataStore.clearInProgressGame(difficultyKey)
    }

    suspend fun loadInProgressGame(difficulty: Difficulty): SavedGameState? {
        return dataStore.loadInProgressGame(difficulty.saveKey)
    }

    suspend fun loadInProgressGame(difficultyKey: String): SavedGameState? {
        return dataStore.loadInProgressGame(difficultyKey)
    }

    suspend fun markFirstLaunchDone() {
        dataStore.markFirstLaunchDone()
    }

    // ── Dev Mode helpers ──────────────────────────────────────────────────────

    /**
     * [DEV] Clears all daily challenge saves and resets streak / last-date fields
     * so daily challenges appear as if they haven't been done today.
     */
    suspend fun devResetDailyChallenges(current: PlayerProgress) {
        dataStore.clearInProgressGame("daily")
        dataStore.clearInProgressGame("daily_4")
        dataStore.clearInProgressGame("daily_5")
        dataStore.clearInProgressGame("daily_6")
        saveProgress(
            current.copy(
                dailyChallengeLastDate = "",
                dailyLastDate4 = "",
                dailyLastDate5 = "",
                dailyLastDate6 = ""
            )
        )
    }

    /**
     * [DEV] Resets all cumulative statistics to zero (levels, wins, guesses, etc.).
     * Does NOT reset currency, lives, or level progress.
     */
    suspend fun devResetStatistics(current: PlayerProgress) {
        saveProgress(
            current.copy(
                totalCoinsEarned = 0L,
                totalLevelsCompleted = 0,
                totalGuesses = 0,
                totalWins = 0,
                totalItemsUsed = 0,
                totalDailyChallengesCompleted = 0,
                totalDailyChallengesPlayed = 0,
                dailyChallengeStreak = 0,
                dailyChallengeBestStreak = 0,
                dailyStreak4 = 0,
                dailyStreak5 = 0,
                dailyStreak6 = 0,
                dailyBestStreak4 = 0,
                dailyBestStreak5 = 0,
                dailyBestStreak6 = 0,
                dailyWins4 = 0,
                dailyWins5 = 0,
                dailyWins6 = 0,
                timerBestLevelsEasy = 0,
                timerBestLevelsRegular = 0,
                timerBestLevelsHard = 0,
                timerBestTimeSecsEasy = 0,
                timerBestTimeSecsRegular = 0,
                timerBestTimeSecsHard = 0,
                totalTimePlayedMs = 0L,
                easyTimePlayedMs = 0L,
                regularTimePlayedMs = 0L,
                hardTimePlayedMs = 0L,
                vipTimePlayedMs = 0L,
                dailyTimePlayedMs = 0L,
                timerTimePlayedMs = 0L,
                totalStarsEarned = 0
            )
        )
    }

    /**
     * [DEV] Resets adventure mode level progress back to level 1 for all difficulties
     * and clears any in-progress adventure games.
     */
    suspend fun devResetLevelProgress(current: PlayerProgress) {
        dataStore.clearInProgressGame("easy")
        dataStore.clearInProgressGame("regular")
        dataStore.clearInProgressGame("hard")
        dataStore.clearInProgressGame("vip")
        saveProgress(
            current.copy(
                easyLevel = 1,
                regularLevel = 1,
                hardLevel = 1,
                vipLevel = 1,
                easyLevelsCompletedSinceBonusLife = 0,
                regularLevelsCompletedSinceBonusLife = 0,
                hardLevelsCompletedSinceBonusLife = 0,
                vipLevelsCompletedSinceBonusLife = 0
            )
        )
    }

    /**
     * Merge local and cloud progress, taking the best of each field.
     * Rule: currency/lives/items/levels all take the maximum.
     * VIP status is OR'd (either source having VIP grants it in merged).
     * Flags like hasReceivedNewPlayerBonus are OR'd (true wins).
     */
    private fun mergeProgress(local: PlayerProgress, cloud: PlayerProgress): PlayerProgress {
        return local.copy(
            coins                   = maxOf(local.coins, cloud.coins),
            diamonds                = maxOf(local.diamonds, cloud.diamonds),
            lives                   = maxOf(local.lives, cloud.lives),
            easyLevel               = maxOf(local.easyLevel, cloud.easyLevel),
            regularLevel            = maxOf(local.regularLevel, cloud.regularLevel),
            hardLevel               = maxOf(local.hardLevel, cloud.hardLevel),
            vipLevel                = maxOf(local.vipLevel, cloud.vipLevel),
            addGuessItems           = maxOf(local.addGuessItems, cloud.addGuessItems),
            removeLetterItems       = maxOf(local.removeLetterItems, cloud.removeLetterItems),
            definitionItems         = maxOf(local.definitionItems, cloud.definitionItems),
            showLetterItems         = maxOf(local.showLetterItems, cloud.showLetterItems),
            totalLevelsCompleted    = maxOf(local.totalLevelsCompleted, cloud.totalLevelsCompleted),
            totalCoinsEarned        = maxOf(local.totalCoinsEarned, cloud.totalCoinsEarned),
            loginBestStreak         = maxOf(local.loginBestStreak, cloud.loginBestStreak),
            dailyChallengeBestStreak = maxOf(local.dailyChallengeBestStreak, cloud.dailyChallengeBestStreak),
            // Seasonal level progress
            seasonalEasterLevel       = maxOf(local.seasonalEasterLevel, cloud.seasonalEasterLevel),
            seasonalValentinesLevel   = maxOf(local.seasonalValentinesLevel, cloud.seasonalValentinesLevel),
            seasonalSummerLevel       = maxOf(local.seasonalSummerLevel, cloud.seasonalSummerLevel),
            seasonalHalloweenLevel    = maxOf(local.seasonalHalloweenLevel, cloud.seasonalHalloweenLevel),
            seasonalThanksgivingLevel = maxOf(local.seasonalThanksgivingLevel, cloud.seasonalThanksgivingLevel),
            seasonalChristmasLevel    = maxOf(local.seasonalChristmasLevel, cloud.seasonalChristmasLevel),
            // Seasonal milestones
            seasonalMilestoneEaster       = maxOf(local.seasonalMilestoneEaster, cloud.seasonalMilestoneEaster),
            seasonalMilestoneValentines   = maxOf(local.seasonalMilestoneValentines, cloud.seasonalMilestoneValentines),
            seasonalMilestoneSummer       = maxOf(local.seasonalMilestoneSummer, cloud.seasonalMilestoneSummer),
            seasonalMilestoneHalloween    = maxOf(local.seasonalMilestoneHalloween, cloud.seasonalMilestoneHalloween),
            seasonalMilestoneThanksgiving = maxOf(local.seasonalMilestoneThanksgiving, cloud.seasonalMilestoneThanksgiving),
            seasonalMilestoneChristmas    = maxOf(local.seasonalMilestoneChristmas, cloud.seasonalMilestoneChristmas),
            isVip                   = local.isVip || cloud.isVip,
            hasReceivedNewPlayerBonus = local.hasReceivedNewPlayerBonus || cloud.hasReceivedNewPlayerBonus,
            hasReviewBeenRequested  = local.hasReviewBeenRequested || cloud.hasReviewBeenRequested,
            reviewRewarded          = local.reviewRewarded || cloud.reviewRewarded,
            // Star rewards — take higher spent value to avoid re-opening already-opened chests
            totalStarsEarned        = maxOf(local.totalStarsEarned, cloud.totalStarsEarned),
            starsSpentOnChests      = maxOf(local.starsSpentOnChests, cloud.starsSpentOnChests),
            // Keep whichever last-date strings are more recent (ISO format lexicographic sort)
            lastLoginDate           = maxOf(local.lastLoginDate, cloud.lastLoginDate),
            lastVipRewardDate       = maxOf(local.lastVipRewardDate, cloud.lastVipRewardDate)
        )
    }
}
