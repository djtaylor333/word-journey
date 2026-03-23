package com.djtaylor.wordjourney.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "InAppReviewManager"
private const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=com.djtaylor.wordjourney"

/**
 * Manages the Google Play In-App Review flow.
 *
 * The Play In-App Review API shows a native overlay that lets the user rate and
 * review the app without leaving it. Quota limits apply — Google may suppress the
 * dialog silently if it has been shown recently.
 *
 * Usage:
 * 1. Call [requestReview] from a resumed Activity.
 * 2. If the native flow fails (e.g. quota exceeded or not a Play build), the user is
 *    redirected to the Play Store via [openPlayStoreListing].
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Launches the Play In-App Review overlay for [activity].
     *
     * Always resolves to `true` from the caller's perspective (the API gives no
     * guarantee about whether the dialog was actually shown).  Falls back to
     * opening the Play Store listing if the API call fails.
     */
    suspend fun requestReview(activity: Activity): Boolean {
        return try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = suspendCancellableCoroutine<com.google.android.play.core.review.ReviewInfo?> { cont ->
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(task.result)
                    } else {
                        Log.w(TAG, "requestReviewFlow failed: ${task.exception?.message}")
                        cont.resume(null)
                    }
                }
            }

            if (reviewInfo != null) {
                val launched = suspendCancellableCoroutine<Boolean> { cont ->
                    manager.launchReviewFlow(activity, reviewInfo)
                        .addOnCompleteListener { cont.resume(it.isSuccessful) }
                }
                Log.d(TAG, "In-app review flow completed (launched=$launched)")
                true // treat as success regardless — API quota may silently skip
            } else {
                Log.w(TAG, "ReviewInfo null — opening Play Store listing as fallback")
                openPlayStoreListing(activity)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "In-app review exception: ${e.message}")
            openPlayStoreListing(activity)
            true
        }
    }

    /**
     * Opens the app's Play Store listing page so the user can leave a rating/review.
     * Used as fallback when the In-App Review API is unavailable.
     */
    fun openPlayStoreListing(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Play Store: ${e.message}")
        }
    }
}
