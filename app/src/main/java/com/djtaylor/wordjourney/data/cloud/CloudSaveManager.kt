package com.djtaylor.wordjourney.data.cloud

import android.content.Context
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages player progress cloud saves via Google Play Games Snapshots API.
 *
 * Note: The full Snapshots API requires an Activity context and authenticated
 * player. This singleton-scoped class provides stub implementations that
 * silently no-op. Real cloud save integration should be triggered from an
 * Activity-scoped component that can pass an Activity reference.
 *
 * All operations are best-effort — failures fall back silently to local DataStore.
 */
@Singleton
class CloudSaveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Attempts to write [progress] to the Play Games cloud snapshot.
     * Currently a no-op stub — requires Activity context for full implementation.
     */
    suspend fun writeSave(progress: PlayerProgress) {
        // No-op: real implementation requires Activity + authenticated PlayGames client
        // The serialized payload would be:
        // Json.encodeToString(progress).toByteArray(Charsets.UTF_8)
    }

    /**
     * Attempts to load a [PlayerProgress] snapshot from Play Games.
     * Returns null — requires Activity context for full implementation.
     */
    suspend fun loadSave(): PlayerProgress? {
        // No-op: real implementation requires Activity + authenticated PlayGames client
        return null
    }
}
