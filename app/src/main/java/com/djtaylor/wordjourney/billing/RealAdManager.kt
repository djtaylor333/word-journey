package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.BuildConfig
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production [IAdManager] backed by IronSource LevelPlay (Unity) SDK.
 *
 * ## Setup (all done)
 *  App Key       : 261bf8a5d
 *  Ad Unit ID    : pln4wccgklbgalc6  (ad unit name: "Rewarded Android")
 *  Unity Game ID : 6097761
 *  SDK           : com.ironsource.sdk:mediationsdk:8.3.0
 *
 * ## Test mode
 * DEBUG builds call IronSource.setMetaData("is_test_suite", "enable") before
 * init, which routes all ad requests to the LevelPlay test suite.
 * No device registration or dashboard changes needed.
 *
 * ## Verify integration
 *   adb logcat -s IronSource:V
 *   Expected: "IronSource SDK version X.Y.Z initialized successfully"
 *   Then:     ad available callback fires -> isRewardedAdReady = true
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    companion object {
        private const val TAG = "RealAdManager"

        /** LevelPlay App Key - Unity dashboard -> LevelPlay -> App Settings */
        const val APP_KEY = "261bf8a5d"

        /** LevelPlay rewarded ad unit ID (ad unit name: "Rewarded Android") */
        const val AD_UNIT_ID = "pln4wccgklbgalc6"

        /** Unity Ads Game ID (used by Unity Ads mediation adapter) */
        const val UNITY_GAME_ID = "6097761"
    }

    @Volatile private var adReady = false

    // Resolved once the rewarded ad sequence completes (dismissed after possible reward)
    private var pendingCont: CancellableContinuation<AdRewardResult>? = null
    private var userRewarded = false

    override val isRewardedAdReady: Boolean
        get() = adReady

    // -- LevelPlay rewarded video listener -----------------------------------
    // Declared before init{} so it is initialized when init{} references it.

    private val rewardedListener = object : LevelPlayRewardedVideoListener {

        override fun onAdAvailable(adInfo: AdInfo) {
            Log.d(TAG, "LevelPlay rewarded ad available -- network=${adInfo.adNetwork}")
            adReady = true
        }

        override fun onAdUnavailable() {
            Log.w(TAG, "LevelPlay rewarded ad unavailable (no fill)")
            adReady = false
        }

        override fun onAdOpened(adInfo: AdInfo) {
            Log.d(TAG, "LevelPlay rewarded ad opened")
        }

        override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
            Log.e(TAG, "LevelPlay ad show failed: ${error.errorMessage} (code=${error.errorCode})")
            resolvePending(AdRewardResult(watched = false))
        }

        override fun onAdClosed(adInfo: AdInfo) {
            Log.d(TAG, "LevelPlay rewarded ad closed (rewarded=$userRewarded)")
            resolvePending(
                AdRewardResult(watched = userRewarded, rewardType = "life", rewardAmount = 1)
            )
            // Pre-fetch the next ad immediately
            IronSource.loadRewardedVideo()
        }

        override fun onAdRewarded(placement: Placement, adInfo: AdInfo) {
            Log.d(TAG, "Reward granted -- placement=${placement.placementName}")
            userRewarded = true
        }

        override fun onAdClicked(placement: Placement, adInfo: AdInfo) {
            Log.d(TAG, "LevelPlay rewarded ad clicked")
        }
    }

    init {
        // Register the listener; IronSource routes events to it after init()
        IronSource.setLevelPlayRewardedVideoListener(rewardedListener)
    }

    /**
     * Requests an ad load. In the IronSource SDK, loadRewardedVideo() triggers
     * a fresh fill attempt; availability is reported via [LevelPlayRewardedVideoListener.onAdAvailable].
     */
    override suspend fun loadRewardedAd() {
        if (BuildConfig.DEBUG) Log.d(TAG, "loadRewardedAd() -- requesting IronSource fill")
        IronSource.loadRewardedVideo()
        // onAdAvailable / onAdUnavailable will update adReady asynchronously
    }

    /**
     * Shows the rewarded ad and suspends until it is dismissed.
     * Returns [AdRewardResult.watched] = true only when the reward callback fired
     * before dismissal.
     */
    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        if (!adReady) {
            Log.w(TAG, "showRewardedAd called but no ad is ready -- returning not-watched")
            return AdRewardResult(watched = false)
        }
        return suspendCancellableCoroutine { cont ->
            pendingCont = cont
            userRewarded = false
            adReady = false  // consumed; will be reset when next ad becomes available
            IronSource.showRewardedVideo()
        }
    }

    // -- Helpers --------------------------------------------------------------

    private fun resolvePending(result: AdRewardResult) {
        pendingCont?.let { cont ->
            if (cont.isActive) cont.resume(result)
            pendingCont = null
        }
    }
}
