package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "RealAdManager"

/** AdMob rewarded ad unit ID for Word Journey. */
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-8973997605504935/9065149682"

/**
 * Production [IAdManager] backed by AdMob rewarded ads.
 *
 * The ad is pre-loaded by [loadRewardedAd] and cached until [showRewardedAd] is called.
 * After a successful show, the cached ad is consumed and a new one pre-fetched automatically
 * by [StoreViewModel].
 *
 * AdMob SDK is initialised once in [WordJourneysApplication.onCreate].
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    override val isRewardedAdReady: Boolean
        get() = rewardedAd != null

    /**
     * Pre-fetches the next rewarded ad from AdMob.
     * No-op if an ad is already loaded or a load is in progress.
     */
    override suspend fun loadRewardedAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true

        suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                REWARDED_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded ad loaded successfully")
                        rewardedAd = ad
                        isLoading = false
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Rewarded ad failed to load: ${error.message} (code=${error.code})")
                        rewardedAd = null
                        isLoading = false
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            )
            cont.invokeOnCancellation { isLoading = false }
        }
    }

    /**
     * Shows the pre-loaded rewarded ad on top of [activity].
     * Returns [AdRewardResult.watched] = true only if the user watched to completion
     * and AdMob delivered the reward callback.
     *
     * The cached ad is consumed (set to null) before showing to prevent double-show.
     */
    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        val ad = rewardedAd ?: run {
            Log.w(TAG, "showRewardedAd called but no ad is loaded")
            return AdRewardResult(watched = false)
        }
        rewardedAd = null // consume before showing

        return suspendCancellableCoroutine { cont ->
            var rewarded = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed (rewarded=$rewarded)")
                    if (cont.isActive) {
                        cont.resume(
                            AdRewardResult(
                                watched = rewarded,
                                rewardType = "coins",
                                rewardAmount = 100
                            )
                        )
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                    if (cont.isActive) cont.resume(AdRewardResult(watched = false))
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "AdMob reward granted: type=${rewardItem.type} amount=${rewardItem.amount}")
                rewarded = true
            }
        }
    }
}
