package com.djtaylor.wordjourney.billing

import org.junit.Assert.*
import org.junit.Test

/**
 * Validates that all ProductIds constants match the Google Play Console product IDs
 * configured via scripts/setup_play_products.py.
 *
 * If any test here fails, update BOTH the constant AND the Play Console setup script
 * to keep them in sync, then re-run setup_play_products.py.
 */
class ProductIdsTest {

    // ── One-time consumable products ──────────────────────────────────────────

    @Test
    fun `COINS_500 matches Play Console product ID`() {
        assertEquals("coins_500", ProductIds.COINS_500)
    }

    @Test
    fun `COINS_1500 matches Play Console product ID`() {
        assertEquals("coins_1500", ProductIds.COINS_1500)
    }

    @Test
    fun `COINS_5000 matches Play Console product ID`() {
        assertEquals("coins_5000", ProductIds.COINS_5000)
    }

    @Test
    fun `DIAMONDS_10 matches Play Console product ID`() {
        assertEquals("diamonds_10", ProductIds.DIAMONDS_10)
    }

    @Test
    fun `DIAMONDS_50 matches Play Console product ID`() {
        assertEquals("diamonds_50", ProductIds.DIAMONDS_50)
    }

    @Test
    fun `DIAMONDS_200 matches Play Console product ID`() {
        assertEquals("diamonds_200", ProductIds.DIAMONDS_200)
    }

    @Test
    fun `LIVES_PACK_5 matches Play Console product ID`() {
        assertEquals("lives_pack_5", ProductIds.LIVES_PACK_5)
    }

    // ── Bundle products ───────────────────────────────────────────────────────

    @Test
    fun `STARTER_BUNDLE matches Play Console product ID`() {
        assertEquals("bundle_starter", ProductIds.STARTER_BUNDLE)
    }

    @Test
    fun `ADVENTURER_BUNDLE matches Play Console product ID`() {
        assertEquals("bundle_adventurer", ProductIds.ADVENTURER_BUNDLE)
    }

    @Test
    fun `CHAMPION_BUNDLE matches Play Console product ID`() {
        assertEquals("bundle_champion", ProductIds.CHAMPION_BUNDLE)
    }

    // ── Subscription products ─────────────────────────────────────────────────

    @Test
    fun `VIP_MONTHLY matches Play Console subscription ID`() {
        assertEquals("vip_monthly", ProductIds.VIP_MONTHLY)
    }

    @Test
    fun `VIP_YEARLY matches Play Console subscription ID`() {
        assertEquals("vip_yearly", ProductIds.VIP_YEARLY)
    }

    // ── No product ID starts with "coin_pack" (old legacy prefix removed) ────

    @Test
    fun `no product ID uses legacy coin_pack prefix`() {
        val allProductIds = listOf(
            ProductIds.COINS_500,
            ProductIds.COINS_1500,
            ProductIds.COINS_5000,
            ProductIds.DIAMONDS_10,
            ProductIds.DIAMONDS_50,
            ProductIds.DIAMONDS_200,
            ProductIds.LIVES_PACK_5,
            ProductIds.STARTER_BUNDLE,
            ProductIds.ADVENTURER_BUNDLE,
            ProductIds.CHAMPION_BUNDLE,
            ProductIds.VIP_MONTHLY,
            ProductIds.VIP_YEARLY,
        )
        for (id in allProductIds) {
            assertFalse(
                "Product ID '$id' uses old 'coin_pack_' prefix — update to match Play Console",
                id.startsWith("coin_pack_")
            )
        }
    }

    // ── Ad reward IDs are NOT billing product IDs ─────────────────────────────

    @Test
    fun `ad reward IDs are not valid play billing product IDs`() {
        val adIds = listOf(
            ProductIds.AD_REWARD_COINS_100,
            ProductIds.AD_REWARD_LIFE,
            ProductIds.AD_REWARD_ITEM
        )
        for (id in adIds) {
            assertTrue(
                "Ad reward ID '$id' should start with 'ad_reward_'",
                id.startsWith("ad_reward_")
            )
        }
    }

    // ── StubBillingManager returns a price label for every known product ──────

    @Test
    fun `StubBillingManager returns non-dash price for every consumable`() {
        val stub = StubBillingManager()
        val consumables = listOf(
            ProductIds.COINS_500,
            ProductIds.COINS_1500,
            ProductIds.COINS_5000,
            ProductIds.DIAMONDS_10,
            ProductIds.DIAMONDS_50,
            ProductIds.DIAMONDS_200,
            ProductIds.LIVES_PACK_5,
            ProductIds.STARTER_BUNDLE,
            ProductIds.ADVENTURER_BUNDLE,
            ProductIds.CHAMPION_BUNDLE,
        )
        for (id in consumables) {
            val label = stub.getPriceLabel(id)
            assertNotEquals("StubBillingManager returned '—' for $id", "—", label)
        }
    }

    @Test
    fun `StubBillingManager returns non-dash price for subscriptions`() {
        val stub = StubBillingManager()
        assertNotEquals("—", stub.getPriceLabel(ProductIds.VIP_MONTHLY))
        assertNotEquals("—", stub.getPriceLabel(ProductIds.VIP_YEARLY))
    }
}
