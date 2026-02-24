package com.djtaylor.wordjourney.data.cloud

import android.app.Activity
import android.content.Context
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages player progress cloud saves via Google Play Games Snapshots API.
 *
 * Call [setActivity] from MainActivity.onResume / onPause to give this
 * class an Activity reference. All operations are best-effort — failures
 * fall back silently to local DataStore.
 */
@Singleton
class CloudSaveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val SNAPSHOT_NAME = "word_journeys_progress"
    }

    private var currentActivity: WeakReference<Activity>? = null

    /** Update the Activity reference from MainActivity lifecycle callbacks. */
    fun setActivity(activity: Activity?) {
        currentActivity = if (activity != null) WeakReference(activity) else null
    }

    /**
     * Writes [progress] to the Play Games cloud snapshot.
     * No-ops silently if no Activity is available or the user is not signed in.
     */
    suspend fun writeSave(progress: PlayerProgress) {
        val activity = currentActivity?.get() ?: return
        val client = try { PlayGames.getSnapshotsClient(activity) } catch (_: Exception) { return }
        val bytes = Json.encodeToString(progress).toByteArray(Charsets.UTF_8)
        try {
            val dataOrConflict = suspendCancellableCoroutine { cont ->
                client.open(SNAPSHOT_NAME, true,
                    SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            if (!dataOrConflict.isConflict) {
                val snapshot = dataOrConflict.data ?: return
                snapshot.snapshotContents.writeBytes(bytes)
                val metadata = SnapshotMetadataChange.Builder().build()
                suspendCancellableCoroutine { cont ->
                    client.commitAndClose(snapshot, metadata)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
            }
        } catch (_: Exception) {
            // Best-effort — silently ignore cloud save failures
        }
    }

    /**
     * Loads a [PlayerProgress] snapshot from Play Games.
     * Returns null if unavailable, not signed in, or on any error.
     */
    suspend fun loadSave(): PlayerProgress? {
        val activity = currentActivity?.get() ?: return null
        val client = try { PlayGames.getSnapshotsClient(activity) } catch (_: Exception) { return null }
        return try {
            val dataOrConflict = suspendCancellableCoroutine { cont ->
                client.open(SNAPSHOT_NAME, true,
                    SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            if (dataOrConflict.isConflict) return null
            val snapshot = dataOrConflict.data ?: return null
            val bytes = snapshot.snapshotContents.readFully()
            // Close without writing changes
            suspendCancellableCoroutine { cont ->
                client.discardAndClose(snapshot)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resume(Unit) } // ignore close error
            }
            if (bytes.isEmpty()) null
            else Json.decodeFromString<PlayerProgress>(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
