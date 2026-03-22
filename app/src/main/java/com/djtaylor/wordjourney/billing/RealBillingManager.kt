package com.djtaylor.wordjourney.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "RealBillingManager"

/**
 * Production implementation of [IBillingManager] backed by Google Play Billing Library v7.
 *
 * ## How it works
 * 1. On first call to [purchase], a [BillingClient] connection is established.
 * 2. Product details (prices) are fetched from Google Play and cached.
 * 3. [launchBillingFlow] shows the Play Store purchase sheet.
 * 4. Purchases are acknowledged/consumed in [onPurchasesUpdated].
 * 5. The [onResult] callback is invoked on the main thread.
 *
 * ## Google Play Console setup required
 * For each product ID in [ProductIds], create the matching product in the Play Console:
 *
 * ### One-time purchases (In-app products → Managed products)
 *   coins_500, coins_1500, coins_5000
 *   diamonds_10, diamonds_50, diamonds_200
 *   lives_pack_5
 *   bundle_starter, bundle_adventurer, bundle_champion
 *
 * ### Subscriptions
 *   vip_monthly  (base plan ID: "monthly")
 *   vip_yearly   (base plan ID: "yearly")
 *
 * Each subscription must have a matching offer/base plan configured in the console.
 */
@Singleton
class RealBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityProvider: ActivityProvider
) : IBillingManager, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Pending callback for the in-flight purchase — one purchase at a time. */
    private var pendingCallback: ((PurchaseResult) -> Unit)? = null
    private var pendingProductId: String? = null

    /** Cached product details (price labels). Keyed by productId. */
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    // ── BillingClient ─────────────────────────────────────────────────────────

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var isConnected = false

    /** All one-time product IDs */
    private val inAppProductIds = listOf(
        ProductIds.COINS_500, ProductIds.COINS_1500, ProductIds.COINS_5000,
        ProductIds.DIAMONDS_10, ProductIds.DIAMONDS_50, ProductIds.DIAMONDS_200,
        ProductIds.LIVES_PACK_5,
        ProductIds.STARTER_BUNDLE, ProductIds.ADVENTURER_BUNDLE, ProductIds.CHAMPION_BUNDLE
    )

    /** All subscription product IDs */
    private val subsProductIds = listOf(
        ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY
    )

    // ── Connection ────────────────────────────────────────────────────────────

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true

        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        Log.d(TAG, "BillingClient connected")
                        if (cont.isActive) cont.resume(true)
                    } else {
                        Log.e(TAG, "BillingClient connection failed: ${result.debugMessage}")
                        if (cont.isActive) cont.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                    Log.w(TAG, "BillingClient disconnected — will reconnect on next purchase")
                }
            })
        }
    }

    // ── Product detail fetch ──────────────────────────────────────────────────

    private suspend fun fetchProductDetails() {
        // One-time products
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProductIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            })
            .build()

        val inAppResult = billingClient.queryProductDetails(inAppParams)
        if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            inAppResult.productDetailsList?.forEach { productDetailsCache[it.productId] = it }
        }

        // Subscriptions
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsProductIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()

        val subsResult = billingClient.queryProductDetails(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            subsResult.productDetailsList?.forEach { productDetailsCache[it.productId] = it }
        }

        Log.d(TAG, "Product details fetched: ${productDetailsCache.keys}")
    }

    // ── IBillingManager ───────────────────────────────────────────────────────

    /**
     * Launches the Play Store purchase sheet for [productId].
     * Requires an [Activity] context to show the overlay — call this from a ViewModel
     * that holds an ActivityResultLauncher, or pass the current Activity.
     *
     * NOTE: [activity] must be the currently resumed Activity. Pass it from the UI layer:
     *   val activity = LocalContext.current as Activity
     */
    override suspend fun purchase(productId: String, onResult: (PurchaseResult) -> Unit) {
        if (!ensureConnected()) {
            Log.e(TAG, "Cannot purchase $productId: BillingClient not connected")
            onResult(PurchaseResult(productId, success = false))
            return
        }

        if (productDetailsCache.isEmpty()) {
            fetchProductDetails()
        }

        val productDetails = productDetailsCache[productId]
        if (productDetails == null) {
            Log.e(TAG, "ProductDetails not found for $productId — is it set up in Play Console?")
            onResult(PurchaseResult(productId, success = false))
            return
        }

        // Store callback for onPurchasesUpdated
        pendingCallback = onResult
        pendingProductId = productId

        // Build purchase params
        val productDetailsParams = if (subsProductIds.contains(productId)) {
            // Subscription: use the first available offer
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                Log.e(TAG, "No offer token for subscription $productId")
                pendingCallback = null
                pendingProductId = null
                onResult(PurchaseResult(productId, success = false))
                return
            }
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        // launchBillingFlow MUST be called on the main thread with a live Activity.
        val activity = activityProvider.currentActivity
        if (activity == null) {
            Log.e(TAG, "No current Activity — cannot launch billing flow. Is the app in foreground?")
            pendingCallback = null
            pendingProductId = null
            onResult(PurchaseResult(productId, success = false))
            return
        }

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: ${result.debugMessage}")
            pendingCallback = null
            pendingProductId = null
            onResult(PurchaseResult(productId, success = false))
        }
        // Otherwise, wait for onPurchasesUpdated callback
    }

    override fun getPriceLabel(productId: String): String {
        val details = productDetailsCache[productId] ?: return StubBillingManager().getPriceLabel(productId)
        return when {
            subsProductIds.contains(productId) -> {
                val offer = details.subscriptionOfferDetails?.firstOrNull()
                val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                pricingPhase?.formattedPrice
                    ?: if (productId == ProductIds.VIP_MONTHLY) "$4.99/mo" else "$39.99/yr"
            }
            else -> details.oneTimePurchaseOfferDetails?.formattedPrice ?: "—"
        }
    }

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val callback = pendingCallback ?: return
        val productId = pendingProductId ?: return
        pendingCallback = null
        pendingProductId = null

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull { it.products.contains(productId) }
                if (purchase != null) {
                    handlePurchase(purchase, productId, callback)
                } else {
                    Log.w(TAG, "Purchase response OK but no matching purchase found")
                    callback(PurchaseResult(productId, success = false))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled purchase of $productId")
                callback(PurchaseResult(productId, success = false))
            }

            else -> {
                Log.e(TAG, "Purchase failed: code=${result.responseCode} msg=${result.debugMessage}")
                callback(PurchaseResult(productId, success = false))
            }
        }
    }

    // ── Purchase handling (acknowledge / consume) ─────────────────────────────

    private fun handlePurchase(purchase: Purchase, productId: String, callback: (PurchaseResult) -> Unit) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.w(TAG, "Purchase state is not PURCHASED for $productId: ${purchase.purchaseState}")
            callback(PurchaseResult(productId, success = false))
            return
        }

        scope.launch {
            // Subscriptions must be acknowledged (not consumed)
            if (subsProductIds.contains(productId)) {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val ackResult = billingClient.acknowledgePurchase(ackParams)
                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "Acknowledge failed for $productId: ${ackResult.debugMessage}")
                        callback(PurchaseResult(productId, success = false))
                        return@launch
                    }
                }
                Log.i(TAG, "Subscription acknowledged: $productId")
                callback(PurchaseResult(productId, success = true))

            } else {
                // One-time products are consumed so they can be re-purchased
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val (consumeResult, _) = billingClient.consumePurchase(consumeParams)
                if (consumeResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Consume failed for $productId: ${consumeResult.debugMessage}")
                    callback(PurchaseResult(productId, success = false))
                    return@launch
                }
                Log.i(TAG, "Purchase consumed and granted: $productId")
                callback(buildPurchaseResult(productId))
            }
        }
    }

    /** Maps a product ID to the reward it grants. */
    private fun buildPurchaseResult(productId: String): PurchaseResult = when (productId) {
        ProductIds.COINS_500    -> PurchaseResult(productId, true, coinsGranted = 500L)
        ProductIds.COINS_1500   -> PurchaseResult(productId, true, coinsGranted = 1500L)
        ProductIds.COINS_5000   -> PurchaseResult(productId, true, coinsGranted = 5000L)
        ProductIds.DIAMONDS_10  -> PurchaseResult(productId, true, diamondsGranted = 10)
        ProductIds.DIAMONDS_50  -> PurchaseResult(productId, true, diamondsGranted = 50)
        ProductIds.DIAMONDS_200 -> PurchaseResult(productId, true, diamondsGranted = 200)
        ProductIds.LIVES_PACK_5 -> PurchaseResult(productId, true, livesGranted = 5)
        ProductIds.STARTER_BUNDLE ->
            PurchaseResult(productId, true, coinsGranted = 1000L, diamondsGranted = 5)
        ProductIds.ADVENTURER_BUNDLE ->
            PurchaseResult(productId, true, coinsGranted = 3000L, diamondsGranted = 20, livesGranted = 10)
        ProductIds.CHAMPION_BUNDLE ->
            PurchaseResult(productId, true, coinsGranted = 10000L, diamondsGranted = 100, livesGranted = 25)
        else -> PurchaseResult(productId, success = false)
    }
}
