package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.RewardedVideoAd
import com.facebook.ads.RewardedVideoAdListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production [IAdManager] backed by Meta Audience Network (Facebook Audience Network).
 *
 * ## Setup
 * 1. Go to https://developers.facebook.com/apps → create/open your app.
 * 2. Add the "Audience Network" product, create a Property (Android, package = com.djtaylor.wordjourney).
 * 3. Inside the property, create an Ad Unit of type "Rewarded Video" → copy the Placement ID.
 * 4. Set [PLACEMENT_ID] below.
 * 5. In AppModule.kt, change @Binds from StubAdManager → RealAdManager.
 * 6. Call AudienceNetworkAds.initialize(this) in Application.onCreate() — already done.
 *
 * ## Test mode
 * During development, add your device's hashed ID as a test device in the Meta dashboard OR
 * use the test placement ID "YOUR_PLACEMENT_ID#YOUR_APP_ID" format from the dashboard.
 *
 * ## GDPR / Privacy
 * For EU users, call AudienceNetworkAds.setDataProcessingOptions([]) before showing ads.
 * See https://developers.facebook.com/docs/audience-network/optimization/best-practices/gdpr
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    companion object {
        private const val TAG = "RealAdManager"

        // Meta Audience Network — Rewarded Video placement
        // Property: https://business.facebook.com/pub/property/adspace?business_id=303388240893368&property_id=4583483115306548
        const val PLACEMENT_ID = "1685702049238776_1685706569238324"
    }

    private var rewardedVideoAd: RewardedVideoAd? = null
    private var adReadyInternal = false

    // Set in showRewardedAd(), resolved in listener callbacks
    private var pendingCont: CancellableContinuation<AdRewardResult>? = null
    private var userCompletedWatch = false

    override val isRewardedAdReady: Boolean
        get() = adReadyInternal && rewardedVideoAd != null

    /**
     * Pre-fetch the next rewarded ad. Called automatically after a show and by ViewModels on init.
     * Safe to call multiple times.
     */
    override suspend fun loadRewardedAd() {
        adReadyInternal = false
        prefetchAd()
    }

    /**
     * Show the pre-loaded rewarded ad. The coroutine suspends until the ad is dismissed.
     * Returns [AdRewardResult.watched] = true only if the user watched to completion.
     */
    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        val ad = rewardedVideoAd
        if (!adReadyInternal || ad == null) {
            Log.w(TAG, "showRewardedAd called but no ad is loaded — returning not-watched")
            return AdRewardResult(watched = false)
        }
        return suspendCancellableCoroutine { cont ->
            pendingCont = cont
            userCompletedWatch = false
            adReadyInternal = false   // consumed; will reload after close
            ad.show(ad.buildShowAdConfig().build())
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    internal fun prefetchAd() {
        rewardedVideoAd?.destroy()
        val ad = RewardedVideoAd(context, PLACEMENT_ID)
        rewardedVideoAd = ad
        ad.loadAd(
            ad.buildLoadAdConfig()
                .withAdListener(adListener)
                .build()
        )
        Log.d(TAG, "Requesting Meta rewarded ad for placement: $PLACEMENT_ID")
    }

    private val adListener = object : RewardedVideoAdListener {

        override fun onAdLoaded(ad: Ad) {
            Log.d(TAG, "Meta rewarded ad loaded and ready")
            adReadyInternal = true
        }

        override fun onError(ad: Ad?, error: AdError) {
            Log.e(TAG, "Meta ad error ${error.errorCode}: ${error.errorMessage}")
            adReadyInternal = false
            resolvePending(AdRewardResult(watched = false))
        }

        override fun onLoggingImpression(ad: Ad) {
            Log.d(TAG, "Meta ad impression logged")
        }

        override fun onAdClicked(ad: Ad) {
            Log.d(TAG, "Meta ad clicked")
        }

        override fun onRewardedVideoCompleted() {
            // Called when user finishes watching the full video (before the close button appears)
            Log.d(TAG, "Meta rewarded video completed — reward earned")
            userCompletedWatch = true
        }

        override fun onRewardedVideoClosed() {
            // Always the last callback; resume the coroutine here
            Log.d(TAG, "Meta rewarded video closed (watched=$userCompletedWatch)")
            resolvePending(
                AdRewardResult(
                    watched = userCompletedWatch,
                    rewardType = "life",
                    rewardAmount = 1
                )
            )
            // Pre-load the next ad for a seamless future show
            prefetchAd()
        }
    }

    private fun resolvePending(result: AdRewardResult) {
        pendingCont?.let { cont ->
            if (cont.isActive) cont.resume(result)
            pendingCont = null
        }
    }
}