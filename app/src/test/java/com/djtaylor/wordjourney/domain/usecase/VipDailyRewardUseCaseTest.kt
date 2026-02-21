package com.djtaylor.wordjourney.domain.usecase

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for VipDailyRewardUseCase — VIP daily reward calculations.
 */
class VipDailyRewardUseCaseTest {

    private val useCase = VipDailyRewardUseCase()
    private val today = LocalDate.of(2025, 7, 1)

    // ══════════════════════════════════════════════════════════════════════════
    // 1. BASIC REWARD CALCULATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `first VIP reward grants 1 day of rewards`() {
        val reward = useCase.calculateRewards("", today)
        assertNotNull(reward)
        assertEquals(1, reward!!.daysAccumulated)
        assertEquals(5, reward.livesGranted)
        // 5 items / 4 types = 1 each + 1 remainder
        assertEquals(2, reward.addGuessItemsGranted)
        assertEquals(1, reward.removeLetterItemsGranted)
        assertEquals(1, reward.definitionItemsGranted)
        assertEquals(1, reward.showLetterItemsGranted)
        assertEquals("2025-07-01", reward.updatedLastRewardDate)
    }

    @Test
    fun `already collected today returns null`() {
        val reward = useCase.calculateRewards("2025-07-01", today)
        assertNull(reward)
    }

    @Test
    fun `collected yesterday grants 1 day of rewards`() {
        val reward = useCase.calculateRewards("2025-06-30", today)
        assertNotNull(reward)
        assertEquals(1, reward!!.daysAccumulated)
        assertEquals(5, reward.livesGranted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. ACCUMULATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `3 days offline accumulates 3 days of rewards`() {
        val reward = useCase.calculateRewards("2025-06-28", today)
        assertNotNull(reward)
        assertEquals(3, reward!!.daysAccumulated)
        assertEquals(15, reward.livesGranted)
        // 15 items / 4 = 3 each + 3 remainder => 4, 4, 4, 3
        assertEquals(4, reward.addGuessItemsGranted)
        assertEquals(4, reward.removeLetterItemsGranted)
        assertEquals(4, reward.definitionItemsGranted)
        assertEquals(3, reward.showLetterItemsGranted)
    }

    @Test
    fun `7 days offline accumulates 7 days of rewards`() {
        val reward = useCase.calculateRewards("2025-06-24", today)
        assertNotNull(reward)
        assertEquals(7, reward!!.daysAccumulated)
        assertEquals(35, reward.livesGranted)
    }

    @Test
    fun `capped at 30 days accumulation`() {
        // 60 days offline but capped at 30
        val reward = useCase.calculateRewards("2025-05-02", today)
        assertNotNull(reward)
        assertEquals(30, reward!!.daysAccumulated)
        assertEquals(150, reward.livesGranted) // 30 * 5
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. EDGE CASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `invalid date string treats as first reward`() {
        val reward = useCase.calculateRewards("invalid-date", today)
        assertNotNull(reward)
        assertEquals(1, reward!!.daysAccumulated)
    }

    @Test
    fun `item distribution for 1 day is correct`() {
        val reward = useCase.calculateRewards("", today)!!
        val totalItems = reward.addGuessItemsGranted +
                reward.removeLetterItemsGranted +
                reward.definitionItemsGranted +
                reward.showLetterItemsGranted
        assertEquals(5, totalItems)
    }

    @Test
    fun `item distribution for 4 days is evenly distributed`() {
        val reward = useCase.calculateRewards("2025-06-27", today)!! // 4 days
        assertEquals(4, reward.daysAccumulated)
        // 20 items / 4 = 5 each, 0 remainder
        assertEquals(5, reward.addGuessItemsGranted)
        assertEquals(5, reward.removeLetterItemsGranted)
        assertEquals(5, reward.definitionItemsGranted)
        assertEquals(5, reward.showLetterItemsGranted)
    }

    @Test
    fun `updated date is today`() {
        val reward = useCase.calculateRewards("2025-06-25", today)
        assertEquals("2025-07-01", reward!!.updatedLastRewardDate)
    }

    @Test
    fun `future last reward date still grants 1 day`() {
        // If somehow lastVipRewardDate is in the future (clock change), clamp to 1
        val reward = useCase.calculateRewards("2025-07-05", today)
        assertNotNull(reward)
        assertEquals(1, reward!!.daysAccumulated)
    }
}
