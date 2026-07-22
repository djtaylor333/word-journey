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
 * Production implementation of [IBillingManager] backed by Google Play Billing Library v8.
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

    init {
        // Pre-warm: connect and fetch product details at startup so the first purchase
        // attempt does not incur cold-start latency or find an empty cache.
        scope.launch {
            if (ensureConnected()) {
                fetchProductDetails()
                val loaded = productDetailsCache.keys
                val missing = (inAppProductIds + subsProductIds).filter { it !in loaded }
                Log.i(TAG, "Billing ready — loaded ${loaded.size}/${inAppProductIds.size + subsProductIds.size} products: $loaded")
                if (missing.isNotEmpty()) {
                    Log.w(TAG, "═══════════════════════════════════════════════════════")
                    Log.w(TAG, "BILLING SETUP WARNING: ${missing.size} product(s) not found in Play Console:")
                    missing.forEach { Log.w(TAG, "  ✗ $it") }
                    Log.w(TAG, "Fix checklist:")
                    Log.w(TAG, "  1) Products must be set to ACTIVE (not Draft/Inactive) in")
                    Log.w(TAG, "     Play Console → Monetize → in-app products / subscriptions")
                    Log.w(TAG, "  2) App must be published to Internal Testing (or higher) track")
                    Log.w(TAG, "  3) Test account must be added as a Licensed Tester in")
                    Log.w(TAG, "     Play Console → Setup → License testing")
                    Log.w(TAG, "  4) For subscriptions: each must have an active base plan")
                    Log.w(TAG, "═══════════════════════════════════════════════════════")
                }
                // Automatically restore any purchases that were completed but not
                // consumed/acknowledged before (app crash, network loss, etc.)
                restoreAndGrantPendingPurchases()
            } else {
                Log.e(TAG, "Pre-warm failed: could not connect to BillingClient. Is Google Play available?")
            }
        }
    }

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
                    productDetailsCache.clear() // Force re-fetch on reconnect
                    Log.w(TAG, "BillingClient disconnected — cache cleared, will reconnect on next purchase")
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
        } else {
            Log.e(TAG, "In-app query failed: code=${inAppResult.billingResult.responseCode} msg=${inAppResult.billingResult.debugMessage}")
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
        } else {
            Log.e(TAG, "Subs query failed: code=${subsResult.billingResult.responseCode} msg=${subsResult.billingResult.debugMessage}")
        }

        Log.d(TAG, "Product details fetched: ${productDetailsCache.keys}")
    }

    // ── Diagnostic helpers ────────────────────────────────────────────────────

    override suspend fun getLoadedProductIds(): Set<String> {
        if (productDetailsCache.isEmpty()) {
            ensureConnected()
            fetchProductDetails()
        }
        return productDetailsCache.keys.toSet()
    }

    override fun getAllProductIds(): Set<String> =
        (inAppProductIds + subsProductIds).toSet()

    /**
     * Queries Google Play for any purchases that were completed but not yet
     * consumed or acknowledged by this app (e.g., after a crash during a
     * previous purchase session). Processes and returns each recoverable
     * purchase.
     *
     * Should be called at app startup and when the user taps "Restore Purchases".
     */
    override suspend fun restoreAndGrantPendingPurchases(): List<PurchaseResult> {
        if (!ensureConnected()) {
            Log.e(TAG, "restoreAndGrantPendingPurchases: BillingClient not connected")
            return emptyList()
        }

        val results = mutableListOf<PurchaseResult>()

        // ── Query one-time products ──────────────────────────────────────────
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inAppPurchasesResult = billingClient.queryPurchasesAsync(inAppParams)
        if (inAppPurchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in inAppPurchasesResult.purchasesList) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val productId = purchase.products.firstOrNull() ?: continue
                    Log.i(TAG, "Restoring unprocessed INAPP purchase: $productId token=${purchase.purchaseToken.take(12)}...")
                    val result = consumeAndBuildResult(purchase, productId)
                    if (result.success) results.add(result)
                }
            }
        } else {
            Log.e(TAG, "restoreAndGrantPendingPurchases INAPP query failed: " +
                "code=${inAppPurchasesResult.billingResult.responseCode} " +
                "msg=${inAppPurchasesResult.billingResult.debugMessage}")
        }

        // ── Query subscriptions ──────────────────────────────────────────────
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsPurchasesResult = billingClient.queryPurchasesAsync(subsParams)
        if (subsPurchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in subsPurchasesResult.purchasesList) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    val productId = purchase.products.firstOrNull() ?: continue
                    Log.i(TAG, "Restoring unacknowledged SUBS purchase: $productId")
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val ackResult = billingClient.acknowledgePurchase(ackParams)
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        results.add(PurchaseResult(productId, success = true))
                    } else {
                        Log.e(TAG, "restoreAndGrantPendingPurchases ack failed for $productId: ${ackResult.debugMessage}")
                    }
                }
            }
        } else {
            Log.e(TAG, "restoreAndGrantPendingPurchases SUBS query failed: " +
                "code=${subsPurchasesResult.billingResult.responseCode} " +
                "msg=${subsPurchasesResult.billingResult.debugMessage}")
        }

        if (results.isNotEmpty()) {
            Log.i(TAG, "restoreAndGrantPendingPurchases: restored ${results.size} purchase(s)")
        } else {
            Log.d(TAG, "restoreAndGrantPendingPurchases: nothing to restore")
        }
        return results
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

        // Fetch product details if this specific product is not yet cached.
        // This also handles the case where a previous fetch partially succeeded,
        // or the cache was cleared after a billing client disconnect.
        if (!productDetailsCache.containsKey(productId)) {
            Log.d(TAG, "Product $productId not in cache — fetching product details")
            fetchProductDetails()
        }

        val productDetails = productDetailsCache[productId]
        if (productDetails == null) {
            Log.e(TAG, "ProductDetails not found for $productId after fetch.")
            Log.e(TAG, "  → Check Play Console: product '$productId' must be ACTIVE (not Draft)")
            Log.e(TAG, "  → Ensure app is published (even internally) and account is a Licensed Tester")
            Log.e(TAG, "  → Loaded products: ${productDetailsCache.keys}")
            onResult(PurchaseResult(productId, success = false,
                errorMessage = "Product not found: \"$productId\". Check Play Console setup — product must be ACTIVE and app published to a test track."))
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

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // The product was purchased previously but not yet consumed (e.g., app crash).
                // Re-query pending purchases and try to consume/acknowledge.
                Log.w(TAG, "ITEM_ALREADY_OWNED for $productId — attempting to restore pending purchase")
                scope.launch { handleItemAlreadyOwned(productId, callback) }
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Log.e(TAG, "BILLING_UNAVAILABLE for $productId: Google Play Billing service is unavailable")
                callback(PurchaseResult(productId, success = false,
                    errorMessage = "Google Play Billing is unavailable on this device. " +
                        "Ensure Google Play Services are up to date."))
            }

            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Log.e(TAG, "DEVELOPER_ERROR for $productId: ${result.debugMessage}")
                callback(PurchaseResult(productId, success = false,
                    errorMessage = "Purchase configuration error (DEVELOPER_ERROR). " +
                        "Product '$productId' may not be correctly set up in Play Console. " +
                        "Check that the product is ACTIVE and the package name matches."))
            }

            else -> {
                val msg = billingResponseMessage(result.responseCode, result.debugMessage, productId)
                Log.e(TAG, "Purchase failed: code=${result.responseCode} msg=${result.debugMessage}")
                callback(PurchaseResult(productId, success = false, errorMessage = msg))
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

    /**
     * Handles the ITEM_ALREADY_OWNED response: queries existing unconsumed purchases for
     * [productId] and re-processes the first matching one, so the player receives their items.
     */
    private suspend fun handleItemAlreadyOwned(productId: String, callback: (PurchaseResult) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val queryResult = billingClient.queryPurchasesAsync(params)
        if (queryResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val existing = queryResult.purchasesList.firstOrNull { it.products.contains(productId) }
            if (existing != null) {
                Log.i(TAG, "handleItemAlreadyOwned: found pending purchase for $productId — re-consuming")
                handlePurchase(existing, productId, callback)
                return
            }
        }
        Log.w(TAG, "handleItemAlreadyOwned: no pending purchase found for $productId")
        callback(PurchaseResult(productId, success = false,
            errorMessage = "\"$productId\" is marked as already owned but no pending purchase was found. " +
                "Try restoring purchases or contact support."))
    }

    /**
     * Consumes an INAPP purchase and returns the mapped [PurchaseResult].
     * Used by [restoreAndGrantPendingPurchases] to re-process orphaned purchases.
     */
    private suspend fun consumeAndBuildResult(purchase: Purchase, productId: String): PurchaseResult {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val (consumeResult, _) = billingClient.consumePurchase(consumeParams)
        return if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "consumeAndBuildResult: consumed $productId successfully")
            buildPurchaseResult(productId)
        } else {
            Log.e(TAG, "consumeAndBuildResult: consume failed for $productId: ${consumeResult.debugMessage}")
            PurchaseResult(productId, success = false)
        }
    }

    /**
     * Returns a human-readable error message for a given billing response code.
     */
    private fun billingResponseMessage(responseCode: Int, debugMessage: String, productId: String): String =
        when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                "Google Play Store is temporarily unavailable. Please check your internet connection and try again."
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                "Network error during purchase. Please check your connection and try again."
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "In-app purchases are not supported on this device."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "\"$productId\" is not available for purchase. " +
                    "Ensure the product is ACTIVE in Play Console and your account is a Licensed Tester."
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED ->
                "Subscription \"$productId\" is not owned by this account."
            else ->
                "Purchase failed (code $responseCode): $debugMessage. " +
                    "If this persists, ensure the product is ACTIVE in Play Console and " +
                    "your Google account is added as a Licensed Tester."
        }
}
