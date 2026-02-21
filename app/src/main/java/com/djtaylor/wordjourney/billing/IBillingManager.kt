package com.djtaylor.wordjourney.billing

/**
 * Product IDs that match what will be configured in Google Play Console.
 * Currently backed by [StubBillingManager] — swap in real BillingClient when
 * the app is published to an internal test track.
 */
object ProductIds {
    const val COINS_500    = "coins_500"
    const val COINS_1500   = "coins_1500"
    const val COINS_5000   = "coins_5000"
    const val DIAMONDS_10  = "diamonds_10"
    const val DIAMONDS_50  = "diamonds_50"
    const val DIAMONDS_200 = "diamonds_200"
    const val LIVES_PACK_5 = "lives_pack_5"

    // ── Bundles ────────────────────────────────────────────────────────────
    const val STARTER_BUNDLE   = "bundle_starter"     // 1000 coins + 5 diamonds + 5 of each item
    const val ADVENTURER_BUNDLE = "bundle_adventurer"  // 3000 coins + 20 diamonds + 10 lives + 10 of each item
    const val CHAMPION_BUNDLE  = "bundle_champion"     // 10000 coins + 100 diamonds + 25 lives + 25 of each item

    // ── VIP Subscription ──────────────────────────────────────────────────
    const val VIP_MONTHLY  = "vip_monthly"             // Monthly VIP subscription
    const val VIP_YEARLY   = "vip_yearly"              // Yearly VIP subscription

    // ── Ad Reward Placeholders ────────────────────────────────────────────
    // These are handled via IAdManager, not Google Play Billing.
    // Listed here for reference only.
    const val AD_REWARD_COINS_100  = "ad_reward_coins_100"   // Watch ad → 100 coins
    const val AD_REWARD_LIFE       = "ad_reward_life"        // Watch ad → 1 life
    const val AD_REWARD_ITEM       = "ad_reward_item"        // Watch ad → 1 random item
}

data class PurchaseResult(
    val productId: String,
    val success: Boolean,
    val coinsGranted: Long = 0L,
    val diamondsGranted: Int = 0,
    val livesGranted: Int = 0
)

interface IBillingManager {
    /**
     * Initiates a purchase flow for [productId].
     * The result is delivered via [onResult] on the main thread.
     */
    suspend fun purchase(productId: String, onResult: (PurchaseResult) -> Unit)

    /** Returns a human-readable price label for [productId] (mock or real). */
    fun getPriceLabel(productId: String): String
}

/**
 * Interface for rewarded ad display.
 *
 * ## Integration Guide
 * Replace [StubAdManager] with a real implementation backed by AdMob or similar:
 *
 * 1. Add the AdMob SDK dependency:
 *    ```
 *    implementation("com.google.android.gms:play-services-ads:23.6.0")
 *    ```
 *
 * 2. Add your AdMob App ID to AndroidManifest.xml:
 *    ```xml
 *    <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID"
 *               android:value="ca-app-pub-XXXX~YYYY" />
 *    ```
 *
 * 3. Create `RealAdManager` implementing this interface:
 *    - Initialize the Mobile Ads SDK in Application.onCreate()
 *    - Load rewarded ads via `RewardedAd.load()`
 *    - Show the ad and grant rewards in [showRewardedAd] callback
 *
 * 4. Swap the Hilt binding in BillingModule from StubAdManager to RealAdManager.
 *
 * See INTEGRATION_GUIDE.md for full step-by-step instructions.
 */
interface IAdManager {
    /** Whether a rewarded ad is loaded and ready to show. */
    val isRewardedAdReady: Boolean

    /** Load/pre-fetch the next rewarded ad. Call after showing one. */
    suspend fun loadRewardedAd()

    /**
     * Shows a rewarded ad. Returns the [AdRewardResult] when the user
     * finishes watching (or cancels).
     */
    suspend fun showRewardedAd(): AdRewardResult
}

data class AdRewardResult(
    val watched: Boolean,
    val rewardType: String = "",   // "coins", "life", "item"
    val rewardAmount: Int = 0
)
