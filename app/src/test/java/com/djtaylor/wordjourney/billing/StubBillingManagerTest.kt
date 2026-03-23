package com.djtaylor.wordjourney.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StubBillingManager].
 *
 * Verifies that the stub:
 *  1) Returns the correct reward amounts for every purchasable product
 *  2) Correctly reports all product IDs as "loaded" (since it's a stub, not Play)
 *  3) restoreAndGrantPendingPurchases returns an empty list (no real purchases in stub)
 *  4) getPriceLabel returns a non-empty, non-dash label for every known product
 *  5) Unknown product IDs produce a failed PurchaseResult without crashing
 *
 * These tests act as a contract: if any product ID or reward amount changes, the
 * identical change must be reflected in both StubBillingManager AND the Play Console.
 */
class StubBillingManagerTest {

    private lateinit var stub: StubBillingManager

    @Before
    fun setUp() {
        stub = StubBillingManager()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §1 — getLoadedProductIds / getAllProductIds
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getLoadedProductIds returns all known product IDs in stub`() = runTest {
        val loaded = stub.getLoadedProductIds()
        val all = stub.getAllProductIds()
        assertEquals("Stub should report ALL products as loaded", all, loaded)
    }

    @Test
    fun `getAllProductIds contains all 12 purchasable products`() {
        val all = stub.getAllProductIds()
        assertEquals("Expected 12 products", 12, all.size)
    }

    @Test
    fun `getAllProductIds contains every expected ProductIds constant`() {
        val all = stub.getAllProductIds()
        assertTrue(ProductIds.COINS_500     in all)
        assertTrue(ProductIds.COINS_1500    in all)
        assertTrue(ProductIds.COINS_5000    in all)
        assertTrue(ProductIds.DIAMONDS_10   in all)
        assertTrue(ProductIds.DIAMONDS_50   in all)
        assertTrue(ProductIds.DIAMONDS_200  in all)
        assertTrue(ProductIds.LIVES_PACK_5  in all)
        assertTrue(ProductIds.STARTER_BUNDLE    in all)
        assertTrue(ProductIds.ADVENTURER_BUNDLE in all)
        assertTrue(ProductIds.CHAMPION_BUNDLE   in all)
        assertTrue(ProductIds.VIP_MONTHLY   in all)
        assertTrue(ProductIds.VIP_YEARLY    in all)
    }

    @Test
    fun `getAllProductIds does NOT include ad reward IDs`() {
        val all = stub.getAllProductIds()
        assertFalse(ProductIds.AD_REWARD_COINS_100 in all)
        assertFalse(ProductIds.AD_REWARD_LIFE      in all)
        assertFalse(ProductIds.AD_REWARD_ITEM      in all)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §2 — purchase: coin packs
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase COINS_500 grants 500 coins`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.COINS_500) { result = it }
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(500L, result!!.coinsGranted)
        assertEquals(0, result!!.diamondsGranted)
        assertEquals(0, result!!.livesGranted)
    }

    @Test
    fun `purchase COINS_1500 grants 1500 coins`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.COINS_1500) { result = it }
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(1500L, result!!.coinsGranted)
    }

    @Test
    fun `purchase COINS_5000 grants 5000 coins`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.COINS_5000) { result = it }
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(5000L, result!!.coinsGranted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §3 — purchase: diamond packs
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase DIAMONDS_10 grants 10 diamonds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.DIAMONDS_10) { result = it }
        assertTrue(result!!.success)
        assertEquals(10, result!!.diamondsGranted)
        assertEquals(0L, result!!.coinsGranted)
    }

    @Test
    fun `purchase DIAMONDS_50 grants 50 diamonds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.DIAMONDS_50) { result = it }
        assertTrue(result!!.success)
        assertEquals(50, result!!.diamondsGranted)
    }

    @Test
    fun `purchase DIAMONDS_200 grants 200 diamonds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.DIAMONDS_200) { result = it }
        assertTrue(result!!.success)
        assertEquals(200, result!!.diamondsGranted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §4 — purchase: lives pack
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase LIVES_PACK_5 grants 5 lives`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.LIVES_PACK_5) { result = it }
        assertTrue(result!!.success)
        assertEquals(5, result!!.livesGranted)
        assertEquals(0L, result!!.coinsGranted)
        assertEquals(0, result!!.diamondsGranted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §5 — purchase: bundles
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase STARTER_BUNDLE grants 1000 coins and 5 diamonds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.STARTER_BUNDLE) { result = it }
        assertTrue(result!!.success)
        assertEquals(1000L, result!!.coinsGranted)
        assertEquals(5, result!!.diamondsGranted)
    }

    @Test
    fun `purchase ADVENTURER_BUNDLE grants 3000 coins, 20 diamonds, 10 lives`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.ADVENTURER_BUNDLE) { result = it }
        assertTrue(result!!.success)
        assertEquals(3000L, result!!.coinsGranted)
        assertEquals(20, result!!.diamondsGranted)
        assertEquals(10, result!!.livesGranted)
    }

    @Test
    fun `purchase CHAMPION_BUNDLE grants 10000 coins, 100 diamonds, 25 lives`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.CHAMPION_BUNDLE) { result = it }
        assertTrue(result!!.success)
        assertEquals(10000L, result!!.coinsGranted)
        assertEquals(100, result!!.diamondsGranted)
        assertEquals(25, result!!.livesGranted)
    }

    @Test
    fun `bundle rewards are ordered correctly starter less than adventurer less than champion`() = runTest {
        var starter: PurchaseResult? = null
        var adventurer: PurchaseResult? = null
        var champion: PurchaseResult? = null
        stub.purchase(ProductIds.STARTER_BUNDLE)    { starter    = it }
        stub.purchase(ProductIds.ADVENTURER_BUNDLE) { adventurer = it }
        stub.purchase(ProductIds.CHAMPION_BUNDLE)   { champion   = it }
        assertTrue(adventurer!!.coinsGranted > starter!!.coinsGranted)
        assertTrue(champion!!.coinsGranted > adventurer!!.coinsGranted)
        assertTrue(adventurer!!.diamondsGranted > starter!!.diamondsGranted)
        assertTrue(champion!!.diamondsGranted > adventurer!!.diamondsGranted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §6 — purchase: VIP subscriptions
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase VIP_MONTHLY succeeds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.VIP_MONTHLY) { result = it }
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(ProductIds.VIP_MONTHLY, result!!.productId)
    }

    @Test
    fun `purchase VIP_YEARLY succeeds`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase(ProductIds.VIP_YEARLY) { result = it }
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(ProductIds.VIP_YEARLY, result!!.productId)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §7 — purchase: unknown product ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase unknown product ID returns failure without crashing`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase("unknown_product_xyz") { result = it }
        assertNotNull(result)
        assertFalse("Unknown product should return success=false", result!!.success)
    }

    @Test
    fun `purchase empty product ID returns failure without crashing`() = runTest {
        var result: PurchaseResult? = null
        stub.purchase("") { result = it }
        assertNotNull(result)
        assertFalse(result!!.success)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §8 — getPriceLabel
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getPriceLabel returns non-empty non-dash for every consumable`() {
        val consumables = listOf(
            ProductIds.COINS_500,
            ProductIds.COINS_1500,
            ProductIds.COINS_5000,
            ProductIds.DIAMONDS_10,
            ProductIds.DIAMONDS_50,
            ProductIds.DIAMONDS_200,
            ProductIds.LIVES_PACK_5,
        )
        for (id in consumables) {
            val label = stub.getPriceLabel(id)
            assertNotEquals("getPriceLabel returned '—' for $id", "—", label)
            assertTrue("getPriceLabel returned empty string for $id", label.isNotEmpty())
        }
    }

    @Test
    fun `getPriceLabel returns monthly price for VIP_MONTHLY`() {
        val label = stub.getPriceLabel(ProductIds.VIP_MONTHLY)
        assertNotEquals("—", label)
        assertTrue("Monthly label should contain 'mo' or '/m'",
            label.contains("mo", ignoreCase = true) || label.contains("/m", ignoreCase = true))
    }

    @Test
    fun `getPriceLabel returns yearly price for VIP_YEARLY`() {
        val label = stub.getPriceLabel(ProductIds.VIP_YEARLY)
        assertNotEquals("—", label)
        assertTrue("Yearly label should contain 'yr' or '/y'",
            label.contains("yr", ignoreCase = true) || label.contains("/y", ignoreCase = true))
    }

    @Test
    fun `getPriceLabel yearly subscription is more expensive than monthly by raw price`() {
        val monthly = stub.getPriceLabel(ProductIds.VIP_MONTHLY)
        val yearly  = stub.getPriceLabel(ProductIds.VIP_YEARLY)
        // Extract numeric values; yearly price should be numerically higher
        val monthlyNum = monthly.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val yearlyNum  = yearly.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        assertTrue("Yearly price ($yearlyNum) should be > monthly ($monthlyNum)",
            yearlyNum > monthlyNum)
    }

    @Test
    fun `getPriceLabel coin pack prices increase with quantity`() {
        val p500  = stub.getPriceLabel(ProductIds.COINS_500).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val p1500 = stub.getPriceLabel(ProductIds.COINS_1500).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val p5000 = stub.getPriceLabel(ProductIds.COINS_5000).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        assertTrue("1500-coin pack ($p1500) should cost more than 500-coin pack ($p500)", p1500 > p500)
        assertTrue("5000-coin pack ($p5000) should cost more than 1500-coin pack ($p1500)", p5000 > p1500)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §9 — restoreAndGrantPendingPurchases (stub always returns empty)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restoreAndGrantPendingPurchases returns empty list in stub`() = runTest {
        val results = stub.restoreAndGrantPendingPurchases()
        assertTrue("Stub should return empty list (no real Play Store)", results.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §10 — PurchaseResult data class contract
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `PurchaseResult default values are all zero or null`() {
        val result = PurchaseResult(productId = "test", success = true)
        assertEquals(0L,   result.coinsGranted)
        assertEquals(0,    result.diamondsGranted)
        assertEquals(0,    result.livesGranted)
        assertNull(result.errorMessage)
    }

    @Test
    fun `PurchaseResult with errorMessage stores message correctly`() {
        val msg = "Product not found in Play Console"
        val result = PurchaseResult(productId = "test", success = false, errorMessage = msg)
        assertFalse(result.success)
        assertEquals(msg, result.errorMessage)
    }

    @Test
    fun `PurchaseResult with all reward fields set preserves values`() {
        val result = PurchaseResult(
            productId       = ProductIds.CHAMPION_BUNDLE,
            success         = true,
            coinsGranted    = 10000L,
            diamondsGranted = 100,
            livesGranted    = 25
        )
        assertEquals(10000L, result.coinsGranted)
        assertEquals(100,    result.diamondsGranted)
        assertEquals(25,     result.livesGranted)
        assertNull(result.errorMessage)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §11 — purchase product ID round-trip
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase result always echoes back the correct productId`() = runTest {
        val products = stub.getAllProductIds()
        for (productId in products) {
            var result: PurchaseResult? = null
            stub.purchase(productId) { result = it }
            assertEquals("productId in result should match input for '$productId'",
                productId, result!!.productId)
        }
    }
}
