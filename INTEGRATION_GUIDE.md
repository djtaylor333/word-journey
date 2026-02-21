# Word Journeys — Integration Guide

> Step-by-step instructions for adding **AdMob ads**, **Google Play Billing**, **Google Play Games achievements**, **subscriptions**, and **in-app purchases** to Word Journeys.

---

## Table of Contents

1. [AdMob Rewarded Ads](#1-admob-rewarded-ads)
2. [Google Play Billing (In-App Purchases)](#2-google-play-billing-in-app-purchases)
3. [VIP Subscriptions](#3-vip-subscriptions)
4. [Google Play Games (Achievements & Leaderboards)](#4-google-play-games-achievements--leaderboards)
5. [Testing Checklist](#5-testing-checklist)

---

## 1. AdMob Rewarded Ads

The app already has a complete `IAdManager` interface and a `StubAdManager` for development. You only need to create a real implementation and swap the DI binding.

### Step 1: Add the AdMob dependency

In `app/build.gradle`:

```groovy
dependencies {
    implementation 'com.google.android.gms:play-services-ads:23.6.0'
}
```

### Step 2: Add your AdMob App ID

In `AndroidManifest.xml`, inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
```

### Step 3: Create `AdMobAdManager.kt`

Create `app/src/main/java/com/djtaylor/wordjourney/billing/AdMobAdManager.kt`:

```kotlin
package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AdMobAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IAdManager {

    // Replace with your real ad unit ID from AdMob console
    private val adUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"

    private var rewardedAd: RewardedAd? = null

    override val isRewardedAdReady: Boolean
        get() = rewardedAd != null

    override suspend fun loadRewardedAd() {
        suspendCancellableCoroutine { cont ->
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, adUnitId, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        cont.resume(Unit)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        cont.resume(Unit)
                    }
                }
            )
        }
    }

    override suspend fun showRewardedAd(): AdRewardResult {
        val ad = rewardedAd ?: return AdRewardResult(false, "", 0)
        val activity = context as? Activity
            ?: return AdRewardResult(false, "", 0)

        return suspendCancellableCoroutine { cont ->
            ad.show(activity) { rewardItem ->
                rewardedAd = null // consumed
                cont.resume(
                    AdRewardResult(
                        watched = true,
                        rewardType = rewardItem.type,    // "coins", "life", etc. (set in AdMob console)
                        rewardAmount = rewardItem.amount
                    )
                )
            }
        }
    }
}
```

### Step 4: Swap the DI binding

In `AppModule.kt`, replace the stub binding:

```kotlin
// BEFORE (development):
@Binds @Singleton
abstract fun bindAdManager(stub: StubAdManager): IAdManager

// AFTER (production):
@Binds @Singleton
abstract fun bindAdManager(impl: AdMobAdManager): IAdManager
```

### Step 5: Configure Ad Units in AdMob Console

1. Go to [AdMob Console](https://admob.google.com)
2. Create a new app or link your existing app
3. Create a **Rewarded** ad unit
4. Set reward types: `coins` (amount: 100), `life` (amount: 1), `item` (amount: 1)
5. Copy the ad unit ID into `AdMobAdManager.kt`

### Step 6: Test with test ad IDs

During development, use the Google-provided test ad unit ID:
```
ca-app-pub-3940256099942544/5224354917  (rewarded test ad)
```

---

## 2. Google Play Billing (In-App Purchases)

The app uses `IBillingManager` with a `StubBillingManager` for development. The product IDs are already defined in `ProductIds`.

### Step 1: Add the Billing Library

In `app/build.gradle`:

```groovy
dependencies {
    implementation 'com.android.billingclient:billing-ktx:7.1.1'
}
```

### Step 2: Create `GoogleBillingManager.kt`

Create `app/src/main/java/com/djtaylor/wordjourney/billing/GoogleBillingManager.kt`:

```kotlin
package com.djtaylor.wordjourney.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GoogleBillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IBillingManager, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var purchaseContinuation: ((Boolean) -> Unit)? = null

    // Cache product details for price display
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                }
            }
            override fun onBillingServiceDisconnected() { /* retry logic */ }
        })
    }

    private fun queryProducts() {
        // Query in-app products (coins, diamonds, lives, items, bundles)
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    ProductIds.COINS_500, ProductIds.COINS_2000, ProductIds.COINS_5000,
                    ProductIds.DIAMONDS_10, ProductIds.DIAMONDS_50, ProductIds.DIAMONDS_200,
                    ProductIds.LIVES_5, ProductIds.LIVES_15,
                    ProductIds.ITEM_ADD_GUESS, ProductIds.ITEM_REMOVE_LETTER,
                    ProductIds.ITEM_DEFINITION, ProductIds.ITEM_SHOW_LETTER,
                    ProductIds.STARTER_BUNDLE, ProductIds.ADVENTURER_BUNDLE, ProductIds.CHAMPION_BUNDLE
                ).map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient?.queryProductDetailsAsync(inAppParams) { _, detailsList ->
            detailsList.forEach { productDetailsCache[it.productId] = it }
        }

        // Query subscription products (VIP)
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY).map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient?.queryProductDetailsAsync(subParams) { _, detailsList ->
            detailsList.forEach { productDetailsCache[it.productId] = it }
        }
    }

    override suspend fun purchase(productId: String): Boolean {
        val productDetails = productDetailsCache[productId]
            ?: return false

        val activity = context as? Activity ?: return false

        val productType = if (productId in listOf(ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY))
            BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP

        val offerToken = if (productType == BillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false
        } else null

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply { offerToken?.let { setOfferToken(it) } }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        return suspendCancellableCoroutine { cont ->
            purchaseContinuation = { success -> cont.resume(success) }
            billingClient?.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val success = result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()
        if (success) {
            // Acknowledge/consume purchases
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.consumeAsync(consumeParams) { _, _ -> }
                }
            }
        }
        purchaseContinuation?.invoke(success)
        purchaseContinuation = null
    }

    override fun getPriceLabel(productId: String): String {
        val details = productDetailsCache[productId]
        return details?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: details?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            ?: ProductIds.DEFAULT_PRICES[productId]
            ?: "—"
    }
}
```

### Step 3: Swap the DI binding

In `AppModule.kt`:

```kotlin
// BEFORE (development):
@Binds @Singleton
abstract fun bindBillingManager(stub: StubBillingManager): IBillingManager

// AFTER (production):
@Binds @Singleton
abstract fun bindBillingManager(impl: GoogleBillingManager): IBillingManager
```

### Step 4: Set up products in Google Play Console

1. Go to [Google Play Console](https://play.google.com/console) → Your App → Monetize
2. Create **In-app products** with these IDs (matching `ProductIds`):

| Product ID | Type | Price |
|---|---|---|
| `coins_500` | Consumable | $0.99 |
| `coins_2000` | Consumable | $2.99 |
| `coins_5000` | Consumable | $4.99 |
| `diamonds_10` | Consumable | $0.99 |
| `diamonds_50` | Consumable | $3.99 |
| `diamonds_200` | Consumable | $9.99 |
| `lives_5` | Consumable | $0.99 |
| `lives_15` | Consumable | $1.99 |
| `item_add_guess` | Consumable | $0.49 |
| `item_remove_letter` | Consumable | $0.49 |
| `item_definition` | Consumable | $0.49 |
| `item_show_letter` | Consumable | $0.49 |
| `starter_bundle` | Consumable | $2.99 |
| `adventurer_bundle` | Consumable | $7.99 |
| `champion_bundle` | Consumable | $19.99 |

3. Create **Subscriptions**:

| Product ID | Type | Price |
|---|---|---|
| `vip_monthly` | Monthly subscription | $4.99/mo |
| `vip_yearly` | Yearly subscription | $39.99/yr |

---

## 3. VIP Subscriptions

VIP is already integrated into the store UI and view model. The `StoreViewModel.purchase()` method handles VIP by setting `isVip = true` and `vipExpiry`. When you switch to real billing:

### Subscription Verification

Add server-side receipt verification or use Google Play's Real-Time Developer Notifications:

1. **RTDN setup**: Configure a Cloud Pub/Sub topic in Play Console → Monetization setup
2. **Backend verification**: Verify subscription status via `Purchases.subscriptions` API
3. **Grace period**: Handle grace periods for lapsed subscriptions

### Subscription Status Checks

Add periodic subscription validation in `StoreViewModel`:

```kotlin
private fun checkSubscriptionStatus() {
    viewModelScope.launch {
        // Query active subscriptions from BillingClient
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val hasActiveVip = result.purchasesList.any { purchase ->
            purchase.products.any { it in listOf(ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY) } &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        // Update player progress accordingly
        if (!hasActiveVip && latestProgress.isVip) {
            saveField { it.copy(isVip = false) }
        }
    }
}
```

---

## 4. Google Play Games (Achievements & Leaderboards)

### Step 1: Add Play Games dependency

In `app/build.gradle`:

```groovy
dependencies {
    implementation 'com.google.android.gms:play-services-games-v2:20.1.2'
}
```

### Step 2: Configure in Google Play Console

1. Go to Play Console → Grow → Play Games Services → Setup and management
2. Create achievements with IDs matching your game milestones

### Step 3: Define achievement IDs

Create `app/src/main/java/com/djtaylor/wordjourney/achievements/PlayGamesAchievements.kt`:

```kotlin
object PlayGamesAchievements {
    // Replace with actual achievement IDs from Play Console
    const val FIRST_WIN = "CgkI..."
    const val TEN_WINS = "CgkI..."
    const val HUNDRED_WINS = "CgkI..."
    const val FIRST_THREE_STARS = "CgkI..."
    const val COMPLETE_EASY = "CgkI..."
    const val COMPLETE_REGULAR = "CgkI..."
    const val COMPLETE_HARD = "CgkI..."
    const val DAILY_STREAK_7 = "CgkI..."
    const val DAILY_STREAK_30 = "CgkI..."
    const val COLLECTOR = "CgkI..."  // Own 50+ items
}
```

### Step 4: Sign in automatically

In `MainActivity.onCreate()`:

```kotlin
import com.google.android.gms.games.PlayGamesSdk

// Inside onCreate, before setContent:
PlayGamesSdk.initialize(this)
```

### Step 5: Unlock achievements

In your `GameViewModel` (or wherever wins are tracked):

```kotlin
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.AchievementsClient

private fun unlockAchievement(achievementId: String) {
    val activity = /* get activity reference */
    PlayGames.getAchievementsClient(activity).unlock(achievementId)
}

// Call after a win:
unlockAchievement(PlayGamesAchievements.FIRST_WIN)
```

### Step 6: Show achievements UI

Wire the "Achievements" button on HomeScreen:

```kotlin
QuickNavCard(
    emoji = "🏆",
    title = "Achievements",
    modifier = Modifier.weight(1f),
    enabled = true,  // Change from false
    onClick = {
        val activity = context as? Activity ?: return@QuickNavCard
        PlayGames.getAchievementsClient(activity)
            .achievementsIntent
            .addOnSuccessListener { intent ->
                activity.startActivityForResult(intent, RC_ACHIEVEMENT_UI)
            }
    }
)
```

---

## 5. Testing Checklist

### AdMob
- [ ] Test ads load with test ad unit ID
- [ ] Rewarded ad grants correct coins (100)
- [ ] Rewarded ad grants 1 life
- [ ] Ad not ready state shows disabled button
- [ ] No crashes when ad fails to load

### In-App Purchases
- [ ] All product prices display correctly from Play Store
- [ ] Coins purchase grants correct amount
- [ ] Diamond purchase grants correct amount
- [ ] Lives purchase grants correct amount
- [ ] Item purchase grants correct item
- [ ] Bundle purchase grants all included items
- [ ] Purchase failure handled gracefully
- [ ] Consumable purchases can be re-purchased

### VIP Subscriptions
- [ ] Monthly subscription sets VIP flag
- [ ] Yearly subscription sets VIP flag
- [ ] VIP tab shows "You're a VIP!" when active
- [ ] Subscription cancellation removes VIP status
- [ ] Grace period handling works

### Google Play Games
- [ ] Auto sign-in on launch
- [ ] Achievement unlocks trigger correctly
- [ ] Achievements UI opens from HomeScreen
- [ ] Sign-in status shows in Settings

### General
- [ ] Stub implementations still work in debug builds
- [ ] No billing logic in unit tests (all mocked via interfaces)
- [ ] Production build uses real implementations
- [ ] Offline mode handles billing unavailability gracefully

---

## Architecture Notes

The app uses a **clean interface-based approach** for all monetization:

```
IAdManager (interface)
├── StubAdManager      (development — always succeeds)
└── AdMobAdManager     (production — real ads)

IBillingManager (interface)
├── StubBillingManager (development — simulates purchases)
└── GoogleBillingManager (production — real billing)
```

All implementations are swapped via Hilt DI in `AppModule.kt`. This means:
- **Unit tests**: Mock the interfaces with MockK
- **Debug builds**: Use Stub implementations (no real money)
- **Release builds**: Swap to real implementations

To switch between stub and real implementations, only change the `@Binds` annotation target in `AppModule.kt`. No other code changes needed.
