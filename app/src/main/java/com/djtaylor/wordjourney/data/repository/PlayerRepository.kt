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
