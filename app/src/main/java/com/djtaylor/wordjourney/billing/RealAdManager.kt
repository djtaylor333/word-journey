package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ad-partner placeholder — AdMob removed while a new ad partner is evaluated.
 *
 * TO RE-ENABLE ADS WITH A NEW PARTNER:
 * 1. Add the partner SDK to app/build.gradle.
 * 2. Restore the ad-unit ID constants below (see comment at bottom).
 * 3. Implement [IAdManager] here (or create a new class) using the partner SDK.
 * 4. In [com.djtaylor.wordjourney.di.AppModule], switch the Hilt @Binds from
 *    StubAdManager back to RealAdManager (or your new implementation).
 * 5. Restore the app-ID meta-data in AndroidManifest.xml.
 *
 * The [StubAdManager] is currently bound via AppModule and simulates a
 * successful ad view (100 coins reward) with a short delay so all existing
 * UI ad flows continue to work without a live ad SDK.
 *
 * Previous AdMob ad-unit IDs (for reference / future partner migration):
 *   Production: ca-app-pub-8973997605504935/9065149682
 *   Test:       ca-app-pub-3940256099942544/5224354917
 *   App ID:     ca-app-pub-8973997605504935~5070064358
 */
@Singleton
class RealAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    override val isRewardedAdReady: Boolean = false

    override suspend fun loadRewardedAd() {
        // TODO: implement with chosen ad partner SDK
        Log.d("RealAdManager", "Ad partner not yet configured — using StubAdManager instead")
    }

    override suspend fun showRewardedAd(activity: Activity): AdRewardResult {
        // TODO: implement with chosen ad partner SDK
        return AdRewardResult(watched = false, rewardType = "", rewardAmount = 0)
    }
}
