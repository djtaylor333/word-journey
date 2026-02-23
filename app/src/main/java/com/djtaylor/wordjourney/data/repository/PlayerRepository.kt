package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.cloud.CloudSaveManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SavedGameState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val dataStore: PlayerDataStore,
    private val cloudSave: CloudSaveManager
) {
    val playerProgressFlow: Flow<PlayerProgress> = dataStore.playerProgressFlow
    val isFirstLaunch: Flow<Boolean> = dataStore.isFirstLaunch

    /** Load from cloud and merge (highest level wins), then persist locally. */
    suspend fun syncFromCloud(): PlayerProgress? {
        val cloud = cloudSave.loadSave() ?: return null
        val localFlow = dataStore.playerProgressFlow
        var local: PlayerProgress? = null
        localFlow.collect { local = it }
        val merged = mergeProgress(local ?: return cloud, cloud)
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
                timerTimePlayedMs = 0L
            )
        )
    }

    private fun mergeProgress(local: PlayerProgress, cloud: PlayerProgress): PlayerProgress {
        // Take the furthest level for each difficulty; take maximum currency
        return local.copy(
            coins         = maxOf(local.coins, cloud.coins),
            diamonds      = maxOf(local.diamonds, cloud.diamonds),
            lives         = maxOf(local.lives, cloud.lives),
            easyLevel     = maxOf(local.easyLevel, cloud.easyLevel),
            regularLevel  = maxOf(local.regularLevel, cloud.regularLevel),
            hardLevel     = maxOf(local.hardLevel, cloud.hardLevel)
        )
    }
}
