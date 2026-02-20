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
