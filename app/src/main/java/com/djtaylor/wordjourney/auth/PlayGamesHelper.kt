package com.djtaylor.wordjourney.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages Google Play Games sign-in state.
 *
 * Play Games Services v2 supports automatic silent sign-in — on first launch
 * the SDK silently authenticates the user if the Play Games app is installed.
 * Explicit [signIn] prompts the account chooser only if needed.
 *
 * Lifecycle:
 *  1. Call [trySilentSignIn] from MainActivity.onCreate.
 *  2. Observe [signInState] to react to sign-in changes in UI.
 *  3. Call [signIn] when the user taps "Sign in with Google Play".
 */
@Singleton
class PlayGamesHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityProvider: ActivityProvider
) {
    companion object {
        private const val TAG = "PlayGamesHelper"
    }

    // ── Sign-in state ─────────────────────────────────────────────────────────

    data class SignInState(
        val isSignedIn: Boolean = false,
        val playerName: String? = null,
        val playerId: String? = null
    )

    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    val isSignedIn: Boolean get() = _signInState.value.isSignedIn

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Attempt silent sign-in. Call once from MainActivity.onCreate.
     */
    fun trySilentSignIn(activity: Activity) {
        try {
            PlayGames.getGamesSignInClient(activity)
                .isAuthenticated
                .addOnCompleteListener { task ->
                    val authenticated = task.isSuccessful && task.result?.isAuthenticated == true
                    if (authenticated) {
                        Log.d(TAG, "Silent sign-in successful")
                        fetchPlayerInfo(activity)
                    } else {
                        Log.d(TAG, "Not signed in — silent sign-in unavailable")
                        _signInState.value = SignInState(isSignedIn = false)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "trySilentSignIn failed: ${e.message}")
        }
    }

    /**
     * Explicitly sign in. Shows the Play Games account chooser if not already signed in.
     */
    fun signIn(activity: Activity) {
        try {
            val client: GamesSignInClient = PlayGames.getGamesSignInClient(activity)
            client.isAuthenticated.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.isAuthenticated == true) {
                    fetchPlayerInfo(activity)
                } else {
                    client.signIn().addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful && signInTask.result?.isAuthenticated == true) {
                            Log.d(TAG, "Explicit sign-in successful")
                            fetchPlayerInfo(activity)
                        } else {
                            Log.w(TAG, "Explicit sign-in failed: ${signInTask.exception?.message}")
                            _signInState.value = SignInState(isSignedIn = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "signIn failed: ${e.message}")
        }
    }

    /**
     * Clear local sign-in state. Note: Play Games v2 has no explicit sign-out API —
     * users manage their account via the Play Games app.
     */
    fun signOut() {
        _signInState.value = SignInState(isSignedIn = false)
        Log.d(TAG, "Signed out (local state cleared)")
    }

    // ── Legacy callback API (preserved for existing call sites) ──────────────

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

    suspend fun isAuthenticated(): Boolean =
        activityProvider.currentActivity?.let { activity ->
            suspendCancellableCoroutine { cont ->
                try {
                    PlayGames.getGamesSignInClient(activity)
                        .isAuthenticated
                        .addOnCompleteListener { task ->
                            cont.resume(task.isSuccessful && task.result?.isAuthenticated == true)
                        }
                } catch (_: Exception) { cont.resume(false) }
            }
        } ?: false

    fun getPlayerDisplayName(activity: Activity, callback: (String?) -> Unit) {
        try {
            PlayGames.getPlayersClient(activity).currentPlayer
                .addOnSuccessListener { player -> callback(player.displayName) }
                .addOnFailureListener { callback(null) }
        } catch (_: Exception) {
            callback(null)
        }
    }

    suspend fun getPlayerDisplayName(): String? = _signInState.value.playerName

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun fetchPlayerInfo(activity: Activity) {
        try {
            PlayGames.getPlayersClient(activity).currentPlayer
                .addOnSuccessListener { player ->
                    _signInState.value = SignInState(
                        isSignedIn = true,
                        playerName = player.displayName,
                        playerId   = player.playerId
                    )
                    Log.d(TAG, "Player info: ${player.displayName} (${player.playerId})")
                }
                .addOnFailureListener { e ->
                    _signInState.value = SignInState(isSignedIn = true)
                    Log.w(TAG, "Could not fetch player name: ${e.message}")
                }
        } catch (e: Exception) {
            _signInState.value = SignInState(isSignedIn = true)
        }
    }
}
