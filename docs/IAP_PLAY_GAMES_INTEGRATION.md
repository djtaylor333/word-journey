# In-App Purchases & Google Play Games Integration Guide

## Word Journey — Monetization & Social Integration

---

## 1. Google Play Billing Library (In-App Purchases)

### Overview
Google Play Billing Library v7+ enables selling digital content (coins, diamonds, item packs,
ad-free upgrades) within the app. It supports one-time purchases and subscriptions.

### Dependencies

Add to `app/build.gradle`:
```gradle
dependencies {
    implementation 'com.android.billingclient:billing-ktx:7.1.1'
}
```

### Manifest Permissions
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

### Products to Create in Google Play Console

#### One-Time Products (Consumable)
| Product ID               | Name            | Price  | Description                              |
|--------------------------|-----------------|--------|------------------------------------------|
| `coins_500`              | 500 Coins       | $0.99  | 500 coins for the in-game store          |
| `coins_2000`             | 2,000 Coins     | $2.99  | 2,000 coins (20% bonus)                 |
| `coins_5000`             | 5,000 Coins     | $4.99  | 5,000 coins (40% bonus)                 |
| `diamonds_10`            | 10 Diamonds     | $1.99  | 10 premium diamonds                     |
| `diamonds_50`            | 50 Diamonds     | $7.99  | 50 diamonds (20% bonus)                 |
| `lives_refill`           | Full Lives       | $0.99  | Instantly refill all 10 lives            |
| `starter_pack`           | Starter Pack    | $2.99  | 1000 coins + 10 diamonds + 5 bonus lives|

#### One-Time Products (Non-Consumable)
| Product ID               | Name            | Price  | Description                              |
|--------------------------|-----------------|--------|------------------------------------------|
| `remove_ads`             | Remove Ads      | $3.99  | Permanently remove all advertisements    |
| `premium_themes`         | Premium Themes  | $2.99  | Unlock exclusive visual themes           |

#### Subscriptions
| Product ID               | Name            | Price    | Description                            |
|--------------------------|-----------------|----------|----------------------------------------|
| `word_journey_pass`      | Journey Pass    | $1.99/mo | 500 coins/week, no ads, exclusive themes, 2x coin earnings |

### Implementation Architecture

```
com.djtaylor.wordjourney.billing/
├── BillingManager.kt          // Singleton managing BillingClient lifecycle
├── BillingRepository.kt       // Repository exposing purchase state via Flow
├── PurchaseVerifier.kt        // Server-side verification (optional)
└── model/
    ├── GameProduct.kt         // App-level product definitions
    └── PurchaseState.kt       // Purchase status enum
```

### Key Implementation Steps

#### 1. BillingManager.kt
```kotlin
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _purchaseFlow = MutableSharedFlow<Purchase>()
    val purchaseFlow: SharedFlow<Purchase> = _purchaseFlow

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }

    suspend fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("coins_500")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                    // ... more products
                )
            ).build()

        val (result, productDetails) = billingClient.queryProductDetails(params)
        // Cache product details for display
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            ).build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    // Verify and acknowledge purchase
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* user cancelled */ }
            else -> { /* handle error */ }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                // Acknowledge consumable
                val params = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.consumeAsync(params) { _, _ -> }
            }
            // Grant items to player
            scope.launch { _purchaseFlow.emit(purchase) }
        }
    }
}
```

#### 2. Integration with StoreScreen
- Display products with real prices from Google Play
- Replace current coin/diamond buttons with IAP-backed purchases
- Add "Restore Purchases" button for non-consumables

#### 3. Testing
- Use Google Play's test tracks (internal testing)
- License testing accounts in Google Play Console
- Test with `android.test.purchased` product ID during development

### Revenue Considerations
- Google takes 15% (first $1M/year) or 30% commission
- Set prices in all currencies via Play Console
- Consider regional pricing for global appeal

---

## 2. Google Play Games Services

### Overview
Play Games Services adds achievements, leaderboards, and cloud saves — increasing
user engagement and retention.

### Dependencies

Add to `app/build.gradle`:
```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-games-v2:20.1.2'
}
```

### Manifest Configuration
```xml
<application>
    <meta-data
        android:name="com.google.android.gms.games.APP_ID"
        android:value="@string/play_games_app_id" />
</application>
```

### Setup in Google Play Console
1. Go to **Play Games Services** → **Setup and management**
2. Link your app
3. Create achievements and leaderboards
4. Get the APP_ID

### Achievements to Implement

| Achievement               | ID                    | Description                         | Type         |
|---------------------------|-----------------------|-------------------------------------|-------------|
| First Steps               | `ach_first_word`      | Complete your first level           | Standard     |
| Wordsmith                 | `ach_10_levels`       | Complete 10 levels                  | Standard     |
| Lexicon Explorer          | `ach_50_levels`       | Complete 50 levels in any difficulty| Standard     |
| Century Mark              | `ach_100_levels`      | Complete 100 levels total           | Standard     |
| Enchanted Meadow Master   | `ach_zone_1`          | Complete all 10 Enchanted Meadow levels | Standard |
| Crystal Cavern Conqueror  | `ach_zone_2`          | Complete all Crystal Cavern levels  | Standard     |
| Flawless Victory          | `ach_first_guess`     | Guess a word on the first try       | Standard     |
| Coin Collector            | `ach_1000_coins`      | Earn 1000 coins total               | Incremental  |
| Diamond Hoarder           | `ach_50_diamonds`     | Collect 50 diamonds                 | Incremental  |
| Easy Peasy                | `ach_easy_complete`   | Complete all Easy levels            | Standard     |
| Regular Champion          | `ach_regular_complete`| Complete all Regular levels         | Standard     |
| Hard Mode Hero            | `ach_hard_complete`   | Complete all Hard levels            | Standard     |
| Streak Star               | `ach_10_streak`       | Win 10 levels in a row              | Standard     |

### Leaderboards

| Leaderboard       | ID                        | Description                     |
|--------------------|---------------------------|---------------------------------|
| Total Levels       | `lb_total_levels`         | Total levels completed          |
| Easy Progress      | `lb_easy_progress`        | Highest Easy level reached      |
| Regular Progress   | `lb_regular_progress`     | Highest Regular level reached   |
| Hard Progress      | `lb_hard_progress`        | Highest Hard level reached      |
| Total Coins Earned | `lb_total_coins`          | Lifetime coins earned           |

### Implementation Architecture

```
com.djtaylor.wordjourney.playgames/
├── PlayGamesManager.kt        // Sign-in, achievement unlock, leaderboard submit
├── PlayGamesRepository.kt     // Flow-based state for sign-in status
└── CloudSaveManager.kt        // Saved games API for cross-device sync
```

### Key Implementation

```kotlin
@Singleton
class PlayGamesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gamesSignInClient = PlayGames.getGamesSignInClient(context as Activity)
    private val achievementsClient = PlayGames.getAchievementsClient(context as Activity)
    private val leaderboardsClient = PlayGames.getLeaderboardsClient(context as Activity)

    fun signIn() {
        gamesSignInClient.isAuthenticated.addOnCompleteListener { task ->
            val isAuthenticated = task.result.isAuthenticated
            if (!isAuthenticated) {
                gamesSignInClient.signIn()
            }
        }
    }

    fun unlockAchievement(achievementId: String) {
        achievementsClient.unlock(achievementId)
    }

    fun incrementAchievement(achievementId: String, steps: Int) {
        achievementsClient.increment(achievementId, steps)
    }

    fun submitScore(leaderboardId: String, score: Long) {
        leaderboardsClient.submitScore(leaderboardId, score)
    }

    fun showAchievements(activity: Activity) {
        achievementsClient.achievementsIntent.addOnSuccessListener { intent ->
            activity.startActivityForResult(intent, RC_ACHIEVEMENTS)
        }
    }

    fun showLeaderboard(activity: Activity, leaderboardId: String) {
        leaderboardsClient.getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent ->
                activity.startActivityForResult(intent, RC_LEADERBOARD)
            }
    }

    companion object {
        const val RC_ACHIEVEMENTS = 9001
        const val RC_LEADERBOARD = 9002
    }
}
```

### Cloud Save (Cross-Device Sync)
```kotlin
// Use SnapshotsClient to save/restore player progress
// This allows players to continue on a new device
PlayGames.getSnapshotsClient(activity)
```

### Integration Points in Word Journey
1. **GameViewModel** — unlock achievements on level complete, submit leaderboard scores
2. **HomeScreen** — show sign-in button, achievement/leaderboard buttons
3. **SettingsScreen** — Play Games sign-in status (already has `playGamesSignedIn` field)
4. **StoreScreen** — show "Restore Purchases" when signed in

---

## 3. Ad Integration (Optional Revenue)

### AdMob Integration

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-ads:23.6.0'
}
```

### Ad Placements
| Placement          | Type              | Trigger                           |
|--------------------|-------------------|-----------------------------------|
| Between levels     | Interstitial      | After every 3rd level completion  |
| Extra lives        | Rewarded Video    | Watch ad to earn 1 free life      |
| Hint boost         | Rewarded Video    | Watch ad to get free hint item    |
| Bottom banner      | Banner (optional) | Main menu only (non-intrusive)    |

### Best Practices
- Never show ads during gameplay (ruins flow)
- Rewarded ads are user-initiated (better UX)
- Remove all ads with `remove_ads` IAP purchase
- Limit interstitials to avoid negative reviews

---

## 4. Implementation Checklist

### Phase 1: Google Play Games (Free — Increases Engagement)
- [ ] Add play-services-games-v2 dependency
- [ ] Create Play Games project in Google Play Console
- [ ] Implement auto sign-in on app launch
- [ ] Add achievements (13 defined above)
- [ ] Add leaderboards (5 defined above)
- [ ] Add achievements/leaderboards buttons to HomeScreen
- [ ] Trigger achievement unlocks in GameViewModel
- [ ] Implement cloud saves

### Phase 2: In-App Purchases (Revenue)
- [ ] Add billing-ktx dependency
- [ ] Create products in Google Play Console
- [ ] Implement BillingManager singleton
- [ ] Update StoreScreen with real IAP products
- [ ] Handle purchase verification and acknowledgment
- [ ] Add "Restore Purchases" flow
- [ ] Test with license testers

### Phase 3: Ads (Additional Revenue)
- [ ] Register app with AdMob
- [ ] Add play-services-ads dependency
- [ ] Implement rewarded video for extra lives
- [ ] Add interstitial between levels (every 3rd)
- [ ] Respect `remove_ads` purchase flag

### Phase 4: Launch Preparation
- [ ] Privacy policy (required for ads/IAP)
- [ ] Terms of service
- [ ] Age rating questionnaire
- [ ] Store listing optimization (screenshots, description)
- [ ] Internal testing track deployment
- [ ] Closed beta with 20+ testers
- [ ] Production release

---

## 5. Revenue Projections

### Realistic Estimates (first year)
- **Ad Revenue**: $0.50-2.00 per 1000 daily active users (DAU)
- **IAP Conversion**: 2-5% of users make a purchase
- **Average Revenue Per Paying User (ARPPU)**: $5-15
- **Subscription**: Higher LTV but lower conversion (1-3%)

### Pricing Strategy
- Keep core game free and fully playable
- Cosmetic/convenience purchases only (not pay-to-win)
- Competitive pricing vs similar word games
- Seasonal sales and promotions

---

*Document created for Word Journey v1.9.0*
*Last updated: June 2025*
