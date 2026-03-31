package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production [IAdManager] backed by Unity Ads 4.x.
 *
 * ## Setup
 * 1. Go to https://dashboard.unity3d.com/, create a project and a Rewarded Video placement.
 * 2. Set [GAME_ID] to your Unity Game ID (e.g. "1234567").
 * 3. Set [REWARDED_PLACEMENT] to your placement ID (default is "Rewarded_Android").
 * 4. In [AppModule], change the @Binds annotation from [StubAdManager] to [RealAdManager].
 * 5. Call [initialize] once from Application.onCreate().
 *
 * ## Reward amounts
 * Unity Ads does NOT pass a reward amount through its SDK; amounts are configured
 * server-side in the Unity dashboard OR handled in [showRewardedAd] based
 * on the [AdRewardResult.rewardType] the caller requested.
 *
 * ## Test Mode
 * Set [TEST_MODE] = BuildConfig.DEBUG to show test ads during development.
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    companion object {
        private const val TAG = "RealAdManager"

        // ── CONFIGURE THESE ────────────────────────────────────────────────
        // Replace with your Unity Ads Game ID from https://dashboard.unity3d.com/
        const val GAME_ID = "YOUR_UNITY_GAME_ID"

        // Replace with your rewarded placement ID (default is "Rewarded_Android")
        const val REWARDED_PLACEMENT = "Rewarded_Android"

        // Use test ads in debug builds; set false for production
        val TEST_MODE get() = false  // Change to BuildConfig.DEBUG for test ads
        // ──────────────────────────────────────────────────────────────────
    }

    private var adReadyInternal = false

    override val isRewardedAdReady: Boolean get() = adReadyInternal

    /**
     * Initialize Unity Ads. Call once from Application.onCreate().
     * Safe to call multiple times — UnityAds ignores subsequent calls once initialized.
     */
    fun initialize() {
        if (GAME_ID == "YOUR_UNITY_GAME_ID") {
            Log.w(TAG, "Unity Ads GAME_ID not set — using StubAdManager should be preferred until configured.")
            return
        }
        UnityAds.initialize(context, GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(TAG, "Unity Ads initialized")
                // Pre-load the first rewarded ad
                prefetchAd()
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                Log.e(TAG, "Unity Ads init failed: $error — $message")
            }
        })
    }

    private fun prefetchAd() {
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "Unity Ads ad loaded: $placementId")
                adReadyInternal = true
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                Log.w(TAG, "Unity Ads failed to load: $placementId — $error: $message")
                adReadyInternal = false
            }
        })
    }

    /** Pre-fetch the next ad. Call this after a successful show. */
    override suspend fun loadRewardedAd() {
        adReadyInternal = false
        prefetchAd()
    }

    /**
     * Show the rewarded ad on top of [activity].
     * Returns [AdRewardResult.watched = true] if the player watched to completion.
     *
     * Note: Unity Ads does not pass reward amounts via SDK — the reward type and
     * amount in [AdRewardResult] are hard-coded here; configure them via the Unity
     * Dashboard's "Rewards" setting or adjust the defaults below.
     */
    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        if (!adReadyInternal) {
            Log.w(TAG, "showRewardedAd called but no ad loaded — returning not-watched")
            return AdRewardResult(watched = false)
        }
        return suspendCancellableCoroutine { cont ->
            adReadyInternal = false  // Mark as consumed
            UnityAds.show(
                activity,
                REWARDED_PLACEMENT,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                        Log.w(TAG, "Unity Ads show failed: $error — $message")
                        if (cont.isActive) cont.resume(AdRewardResult(watched = false))
                    }
                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d(TAG, "Unity Ads show started: $placementId")
                    }
                    override fun onUnityAdsShowClick(placementId: String) {}
                    override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                        val watched = state == UnityAds.UnityAdsShowCompletionState.COMPLETED
                        Log.d(TAG, "Unity Ads show complete: $placementId — state=$state watched=$watched")
                        if (cont.isActive) cont.resume(
                            AdRewardResult(
                                watched = watched,
                                rewardType = "life",
                                rewardAmount = 1
                            )
                        )
                        // Pre-load next ad for seamless future shows
                        prefetchAd()
                    }
                }
            )
        }
    }
}
