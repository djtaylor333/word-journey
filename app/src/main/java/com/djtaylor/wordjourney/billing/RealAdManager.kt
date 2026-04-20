package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.RewardedInterstitialAd
import com.facebook.ads.RewardedInterstitialAdListener
import com.djtaylor.wordjourney.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production [IAdManager] backed by Meta Audience Network (Facebook Audience Network).
 *
 * ## Setup checklist — tick each before testing
 * ☐ 1. Go to https://developers.facebook.com/apps → open your app.
 * ☐ 2. Add the "Audience Network" product; create a Property for Android (package = com.djtaylor.wordjourney).
 * ☐ 3. Create an Ad Unit of type "Rewarded Interstitial" → copy the Placement ID into [PLACEMENT_ID].
 * ☐ 4. Set [PLACEMENT_ID] below.
 * ☐ 5. In strings.xml set facebook_app_id = numeric App ID from Meta (Settings → Basic).
 * ☐ 6. In strings.xml set facebook_client_token from Meta (Settings → Advanced).
 * ☐ 7. Both values must also appear in AndroidManifest.xml via the string refs (already done).
 *
 * ## Debug / test mode
 * In DEBUG builds [loadRewardedAd] calls [logAdDiagnostics] which prints:
 *  • SDK init status
 *  • Hashed device ID (add this in Meta dashboard → Test Devices if needed)
 *  • Current placement ID
 *  • Error codes with plain-English explanations
 *
 * Run the instrumented test to verify the whole pipeline end-to-end:
 *   ./gradlew :app:connectedDebugAndroidTest --tests "*.RealAdManagerInstrumentedTest"
 *
 * Filter Logcat to see just ad-related output:
 *   adb logcat -s RealAdManager:V AudienceNetworkAds:V FBAudienceNetwork:V
 *
 * ## GDPR / Privacy
 * For EU users, call AudienceNetworkAds.setDataProcessingOptions([]) before showing ads.
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    companion object {
        private const val TAG = "RealAdManager"
        /** Timeout for ad load requests — prevents the UI showing "Loading" forever. */
        private const val LOAD_TIMEOUT_MS = 15_000L

        /**
         * The Placement ID from your Meta Audience Network property.
         * Format: "<numeric_app_id>_<placement_id>"
         * Find it at: developers.facebook.com → Audience Network → your property → Ad Units
         */
        const val PLACEMENT_ID = "1685702049238776_1685706569238324"

        /**
         * Known Meta error codes with plain-English explanations.
         * Printed in debug builds to diagnose "retry" loops.
         */
        private val ERROR_EXPLANATIONS = mapOf(
            1000 to "Network error — device has no connectivity",
            1001 to "No fill — no ads available for this placement right now (normal on new/unreviewed apps; enable test mode to bypass)",
            1002 to "Load too frequently — too many ad requests in short succession",
            2000 to "Internal error — usually means SDK failed to initialize (check App ID + Client Token in strings.xml and Manifest)",
            2001 to "Server error — Meta backend returned an unexpected response",
            6 to "Ad load failed — placement ID may be wrong or the ad unit is paused",
        )
    }

    private var rewardedVideoAd: RewardedInterstitialAd? = null
    private var adReadyInternal = false

    // Completed in onAdLoaded/onError so loadRewardedAd() can await the result
    private var loadDeferred: CompletableDeferred<Boolean>? = null

    // Set in showRewardedAd(), resolved in listener callbacks
    private var pendingCont: CancellableContinuation<AdRewardResult>? = null
    private var userCompletedWatch = false

    override val isRewardedAdReady: Boolean
        get() = adReadyInternal && rewardedVideoAd != null

    /**
     * Pre-fetch the next rewarded ad and WAIT until it is loaded or fails (max 15 s).
     *
     * Waits for the Meta SDK to finish initializing before firing the request — this fixes
     * a race condition where the ViewModel is created (and triggers the first load) fractions
     * of a second after Application.onCreate(), before AudienceNetworkAds.initialize() has
     * completed its async internal setup.
     */
    override suspend fun loadRewardedAd() {
        adReadyInternal = false

        if (BuildConfig.DEBUG) logAdDiagnostics("loadRewardedAd called")

        // Wait for SDK initialization (up to 3 s, checked every 300 ms)
        var initTries = 0
        while (!AudienceNetworkAds.isInitialized(context) && initTries < 10) {
            Log.d(TAG, "Waiting for Meta SDK init (attempt ${initTries + 1}/10)…")
            delay(300)
            initTries++
        }
        if (!AudienceNetworkAds.isInitialized(context)) {
            Log.w(TAG, "⚠️  Meta SDK still not initialized after ${initTries * 300}ms. " +
                "Check that facebook_app_id and facebook_client_token strings are set, " +
                "and the <meta-data> entries exist in AndroidManifest.xml — attempting ad load anyway")
        }

        val deferred = prefetchAd()
        val result = withTimeoutOrNull(LOAD_TIMEOUT_MS) { deferred.await() }
        if (result == null) {
            Log.w(TAG, "⚠️  Ad load timed out after ${LOAD_TIMEOUT_MS}ms — " +
                "possible causes: no network, wrong placement ID, SDK not initialized. " +
                "Run: adb logcat -s RealAdManager:V AudienceNetworkAds:V FBAudienceNetwork:V")
        }
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

    internal fun prefetchAd(): CompletableDeferred<Boolean> {
        rewardedVideoAd?.destroy()
        val deferred = CompletableDeferred<Boolean>()
        loadDeferred = deferred
        val ad = RewardedInterstitialAd(context, PLACEMENT_ID)
        rewardedVideoAd = ad
        ad.loadAd(
            ad.buildLoadAdConfig()
                .withAdListener(adListener)
                .build()
        )
        Log.d(TAG, "Requesting Meta rewarded ad | placement=$PLACEMENT_ID | debug=${BuildConfig.DEBUG} | sdkInit=${AudienceNetworkAds.isInitialized(context)}")
        return deferred
    }

    private val adListener = object : RewardedInterstitialAdListener {

        override fun onAdLoaded(ad: Ad) {
            Log.d(TAG, "✅ Meta rewarded ad loaded and ready")
            adReadyInternal = true
            loadDeferred?.complete(true)
        }

        override fun onError(ad: Ad?, error: AdError) {
            val explanation = ERROR_EXPLANATIONS[error.errorCode]
                ?: "Unknown error — see https://developers.facebook.com/docs/audience-network/reference/error-codes"
            Log.e(TAG, "❌ Meta ad error ${error.errorCode}: ${error.errorMessage}\n" +
                "   ↳ Meaning: $explanation\n" +
                "   ↳ Placement: $PLACEMENT_ID\n" +
                "   ↳ SDK initialized: ${AudienceNetworkAds.isInitialized(context)}\n" +
                "   ↳ Test mode: ${BuildConfig.DEBUG}\n" +
                "   ↳ To debug: adb logcat -s RealAdManager:V AudienceNetworkAds:V FBAudienceNetwork:V")
            adReadyInternal = false
            loadDeferred?.complete(false)
            resolvePending(AdRewardResult(watched = false))
        }

        override fun onLoggingImpression(ad: Ad) {
            Log.d(TAG, "Meta ad impression logged")
        }

        override fun onAdClicked(ad: Ad) {
            Log.d(TAG, "Meta ad clicked")
        }

        override fun onRewardedInterstitialCompleted() {
            Log.d(TAG, "✅ Meta rewarded interstitial completed — reward earned")
            userCompletedWatch = true
        }

        override fun onRewardedInterstitialClosed() {
            Log.d(TAG, "Meta rewarded interstitial closed (watched=$userCompletedWatch)")
            resolvePending(
                AdRewardResult(
                    watched = userCompletedWatch,
                    rewardType = "life",
                    rewardAmount = 1
                )
            )
            prefetchAd()
        }
    }

    private fun resolvePending(result: AdRewardResult) {
        pendingCont?.let { cont ->
            if (cont.isActive) cont.resume(result)
            pendingCont = null
        }
    }

    /**
     * Prints a full diagnostic block to Logcat in DEBUG builds.
     * Filter with: adb logcat -s RealAdManager:V
     */
    private fun logAdDiagnostics(trigger: String) {
        Log.d(TAG, """
            ┌─ Ad Diagnostics [$trigger] ─
            │  SDK initialized : ${AudienceNetworkAds.isInitialized(context)}
            │  Test mode       : ${BuildConfig.DEBUG}
            │  Placement ID    : $PLACEMENT_ID
            │  Ad ready        : $adReadyInternal
            │  Hashed device ID: see Logcat tag=FBAudienceNetwork, text="Test Device Hash"
            │    → OR ensure AdSettings.setTestMode(true) is called BEFORE
            │      AudienceNetworkAds.initialize() in Application.onCreate()
            └──────────────────────────────────────────────────────────
        """.trimIndent())
    }
}
