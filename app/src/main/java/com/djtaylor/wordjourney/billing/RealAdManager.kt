package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.djtaylor.wordjourney.BuildConfig
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production [IAdManager] backed by Yandex Mobile Ads SDK.
 *
 * ## Setup (all done)
 * ? 1. Yandex Advertising Network: partner.yandex.com  (account: Kiwi Land Racing)
 * ? 2. App registered: Word Journeys (com.djtaylor.wordjourney)
 * ? 3. Ad unit: R-M-19134646-1  (Rewarded ads — "Reward store add")
 * ? 4. SDK: com.yandex.android:mobileads:7.18.5  (build.gradle)
 * ? 5. MobileAds.initialize() called in Application.onCreate()
 *
 * ## Test mode
 * DEBUG builds use [TEST_AD_UNIT_ID] ("demo-rewarded-yandex") which always
 * returns a live test ad — no device registration or allowlisting needed.
 *
 * ## Verify integration
 *   adb logcat -v brief '*:S YandexAds'
 *   Expected: "[Integration] Ad type rewarded was integrated successfully"
 *
 * ## Meta (Facebook Audience Network) — on backburner
 * Credentials and implementation retained in git (tag v2.39.1).
 * To restore: swap build.gradle dependency back to audience-network-sdk
 * and checkout billing/RealAdManager.kt from that tag.
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    companion object {
        private const val TAG = "RealAdManager"

        /** Max time to wait for an ad to load before giving up. */
        private const val LOAD_TIMEOUT_MS = 15_000L

        /** Real ad unit ID — partner.yandex.com ? Word Journeys ? Rewards Based */
        const val PROD_AD_UNIT_ID = "R-M-19134646-1"

        /** Yandex demo unit — always returns a test ad, used in all DEBUG builds. */
        const val TEST_AD_UNIT_ID = "demo-rewarded-yandex"

        /** Runtime value: demo unit in DEBUG, real unit in release. */
        val AD_UNIT_ID get() = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID
    }

    // Strong reference required per Yandex SDK docs
    private var rewardedAdLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var adReadyInternal = false

    // Completed in load listener so loadRewardedAd() can await the result
    private var loadDeferred: CompletableDeferred<Boolean>? = null

    // Resolved once the ad is dismissed
    private var pendingCont: CancellableContinuation<AdRewardResult>? = null
    private var userRewarded = false

    override val isRewardedAdReady: Boolean
        get() = adReadyInternal && rewardedAd != null

    /**
     * Pre-fetches a rewarded ad and suspends until loaded (max [LOAD_TIMEOUT_MS]).
     *
     * Must be called on the main thread — satisfied because callers use
     * viewModelScope (Dispatchers.Main).
     */
    override suspend fun loadRewardedAd() {
        adReadyInternal = false
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadRewardedAd() — adUnitId=$AD_UNIT_ID")
        }

        val deferred = CompletableDeferred<Boolean>()
        loadDeferred = deferred

        // Reuse a single loader instance (Yandex recommendation for best performance)
        if (rewardedAdLoader == null) {
            rewardedAdLoader = RewardedAdLoader(context).apply {
                setAdLoadListener(loadListener)
            }
        }

        val config = AdRequestConfiguration.Builder(AD_UNIT_ID).build()
        rewardedAdLoader?.loadAd(config)

        val result = withTimeoutOrNull(LOAD_TIMEOUT_MS) { deferred.await() }
        if (result == null) {
            Log.w(TAG, "?? Ad load timed out after ${LOAD_TIMEOUT_MS}ms — adUnitId=$AD_UNIT_ID")
        }
    }

    /**
     * Shows the pre-loaded ad. Suspends until dismissed.
     * Returns [AdRewardResult.watched] = true only if [onRewarded] fired before dismissal.
     */
    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        val ad = rewardedAd
        if (!adReadyInternal || ad == null) {
            Log.w(TAG, "showRewardedAd called but no ad is ready — returning not-watched")
            return AdRewardResult(watched = false)
        }
        return suspendCancellableCoroutine { cont ->
            pendingCont = cont
            userRewarded = false
            adReadyInternal = false  // consumed; will reload after close
            ad.setAdEventListener(eventListener)
            ad.show(activity)
        }
    }

    // -- Listeners --------------------------------------------------------------

    private val loadListener = object : RewardedAdLoadListener {
        override fun onAdLoaded(ad: RewardedAd) {
            Log.d(TAG, "? Yandex rewarded ad loaded (adUnitId=$AD_UNIT_ID)")
            rewardedAd = ad
            adReadyInternal = true
            loadDeferred?.complete(true)
        }

        override fun onAdFailedToLoad(error: AdRequestError) {
            Log.e(TAG, "? Yandex ad failed to load\n" +
                "   ? code        : ${error.code}\n" +
                "   ? description : ${error.description}\n" +
                "   ? adUnitId    : $AD_UNIT_ID\n" +
                "   ? Verify unit is active: partner.yandex.com ? Word Journeys ? Android app")
            adReadyInternal = false
            rewardedAd = null
            loadDeferred?.complete(false)
        }
    }

    private val eventListener = object : RewardedAdEventListener {
        override fun onAdShown() {
            Log.d(TAG, "Yandex rewarded ad shown")
        }

        override fun onAdFailedToShow(error: AdError) {
            Log.e(TAG, "? Yandex rewarded ad failed to show: ${error.description}")
            cleanup()
            resolvePending(AdRewardResult(watched = false))
        }

        override fun onAdDismissed() {
            Log.d(TAG, "Yandex rewarded ad dismissed (rewarded=$userRewarded)")
            val result = AdRewardResult(
                watched = userRewarded,
                rewardType = "life",
                rewardAmount = 1
            )
            cleanup()
            resolvePending(result)
            // Pre-load next ad for a seamless future show
            rewardedAdLoader?.loadAd(AdRequestConfiguration.Builder(AD_UNIT_ID).build())
        }

        override fun onAdClicked() {
            Log.d(TAG, "Yandex rewarded ad clicked")
        }

        override fun onAdImpression(data: ImpressionData?) {
            Log.d(TAG, "Yandex rewarded ad impression logged")
        }

        override fun onRewarded(reward: Reward) {
            Log.d(TAG, "? Yandex reward granted — type=${reward.type} amount=${reward.amount}")
            userRewarded = true
        }
    }

    // -- Helpers ----------------------------------------------------------------

    private fun cleanup() {
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
    }

    private fun resolvePending(result: AdRewardResult) {
        pendingCont?.let { cont ->
            if (cont.isActive) cont.resume(result)
            pendingCont = null
        }
    }
}

