package com.djtaylor.wordjourney.billing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [IAdManager].
 * Simulates a successful rewarded ad viewing after a short delay.
 *
 * ## To integrate real ads (AdMob):
 *
 * 1. Add the AdMob SDK to app/build.gradle:
 *    ```
 *    implementation("com.google.android.gms:play-services-ads:23.6.0")
 *    ```
 *
 * 2. Add your AdMob App ID to AndroidManifest.xml inside <application>:
 *    ```xml
 *    <meta-data
 *        android:name="com.google.android.gms.ads.APPLICATION_ID"
 *        android:value="ca-app-pub-XXXX~YYYY" />
 *    ```
 *
 * 3. Create `RealAdManager : IAdManager` that:
 *    - Calls `MobileAds.initialize(context)` once
 *    - Loads rewarded ads with `RewardedAd.load(context, adUnitId, ...)`
 *    - Implements `showRewardedAd()` by calling `rewardedAd?.show(activity, ...)`
 *    - Returns `AdRewardResult(watched = true, rewardType, amount)` on reward
 *
 * 4. Swap the Hilt binding in BillingModule:
 *    ```kotlin
 *    @Binds @Singleton
 *    abstract fun bindAdManager(impl: RealAdManager): IAdManager
 *    ```
 *
 * 5. See INTEGRATION_GUIDE.md for full details.
 */
@Singleton
class StubAdManager @Inject constructor() : IAdManager {

    override val isRewardedAdReady: Boolean = true

    override suspend fun loadRewardedAd() {
        // No-op in stub — real implementation would pre-fetch an ad
    }

    override suspend fun showRewardedAd(): AdRewardResult {
        // Simulate ad viewing
        kotlinx.coroutines.delay(500)
        return AdRewardResult(watched = true, rewardType = "coins", rewardAmount = 100)
    }
}
