package com.djtaylor.wordjourney.auth

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.PlayGames
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper that wraps Google Play Games sign-in checks.
 *
 * Note: Play Games v2 APIs require an Activity context for most calls.
 * The singleton-scoped methods here act as best-effort stubs.
 * Call the Activity-scoped overloads from UI code when available.
 */
@Singleton
class PlayGamesHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Checks authentication using an Activity reference.
     * Returns false if the check fails for any reason.
     */
    fun isAuthenticated(activity: Activity, callback: (Boolean) -> Unit) {
        try {
            PlayGames.getGamesSignInClient(activity)
                .isAuthenticated
                .addOnCompleteListener { task ->
                    callback(task.isSuccessful && task.result?.isAuthenticated == true)
                }
        } catch (_: Exception) {
            callback(false)
        }
    }

    /**
     * Lightweight suspend-based check (best-effort, always returns false
     * when only application context is available).
     */
    suspend fun isAuthenticated(): Boolean = false

    /**
     * Retrieves the player's display name. Requires Activity context.
     */
    fun getPlayerDisplayName(activity: Activity, callback: (String?) -> Unit) {
        try {
            PlayGames.getPlayersClient(activity).currentPlayer
                .addOnSuccessListener { player -> callback(player.displayName) }
                .addOnFailureListener { callback(null) }
        } catch (_: Exception) {
            callback(null)
        }
    }

    suspend fun getPlayerDisplayName(): String? = null
}
