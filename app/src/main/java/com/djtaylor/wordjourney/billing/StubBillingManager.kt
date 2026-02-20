package com.djtaylor.wordjourney.billing

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [IBillingManager].
 * Simulates a successful purchase after a short delay.
 * Replace this class with a real BillingClient v7 implementation once
 * the app is published to Google Play's internal test track.
 */
@Singleton
class StubBillingManager @Inject constructor() : IBillingManager {

    override suspend fun purchase(productId: String, onResult: (PurchaseResult) -> Unit) {
        // Simulate network round-trip
        delay(800)
        val result = when (productId) {
            ProductIds.COINS_500    -> PurchaseResult(productId, true, coinsGranted = 500)
            ProductIds.COINS_1500   -> PurchaseResult(productId, true, coinsGranted = 1500)
            ProductIds.COINS_5000   -> PurchaseResult(productId, true, coinsGranted = 5000)
            ProductIds.DIAMONDS_10  -> PurchaseResult(productId, true, diamondsGranted = 10)
            ProductIds.DIAMONDS_50  -> PurchaseResult(productId, true, diamondsGranted = 50)
            ProductIds.DIAMONDS_200 -> PurchaseResult(productId, true, diamondsGranted = 200)
            ProductIds.LIVES_PACK_5 -> PurchaseResult(productId, true, livesGranted = 5)
            else                    -> PurchaseResult(productId, false)
        }
        onResult(result)
    }

    override fun getPriceLabel(productId: String): String = when (productId) {
        ProductIds.COINS_500    -> "$0.99"
        ProductIds.COINS_1500   -> "$2.49"
        ProductIds.COINS_5000   -> "$6.99"
        ProductIds.DIAMONDS_10  -> "$0.99"
        ProductIds.DIAMONDS_50  -> "$3.99"
        ProductIds.DIAMONDS_200 -> "$12.99"
        ProductIds.LIVES_PACK_5 -> "$0.99"
        else                    -> "—"
    }
}
