package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.db.InboxDao
import com.djtaylor.wordjourney.data.db.InboxItemEntity
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InboxRepository.
 *
 * Tests cover:
 *  - applyRewardsToProgress pure logic
 *  - addVipDailyRewardIfNeeded idempotency
 *  - claimItem / claimAllItems
 *  - getUnclaimedCount
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxRepositoryTest {

    private lateinit var inboxDao: InboxDao
    private lateinit var repo: InboxRepository

    @Before
    fun setUp() {
        inboxDao = mockk()
        repo = InboxRepository(inboxDao)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. applyRewardsToProgress
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `applyRewardsToProgress adds coins`() {
        val progress = PlayerProgress(coins = 100L)
        val items = listOf(inbox(coinsGranted = 200L))
        val result = repo.applyRewardsToProgress(items, progress)
        assertEquals(300L, result.coins)
        assertEquals(200L, result.totalCoinsEarned)
    }

    @Test
    fun `applyRewardsToProgress adds lives`() {
        val progress = PlayerProgress(lives = 5)
        val items = listOf(inbox(livesGranted = 3))
        val result = repo.applyRewardsToProgress(items, progress)
        assertEquals(8, result.lives)
    }

    @Test
    fun `applyRewardsToProgress adds all item types`() {
        val progress = PlayerProgress(
            addGuessItems = 1,
            removeLetterItems = 2,
            definitionItems = 0,
            showLetterItems = 3
        )
        val items = listOf(
            inbox(
                addGuessItemsGranted = 2,
                removeLetterItemsGranted = 1,
                definitionItemsGranted = 3,
                showLetterItemsGranted = 1
            )
        )
        val result = repo.applyRewardsToProgress(items, progress)
        assertEquals(3, result.addGuessItems)
        assertEquals(3, result.removeLetterItems)
        assertEquals(3, result.definitionItems)
        assertEquals(4, result.showLetterItems)
    }

    @Test
    fun `applyRewardsToProgress with empty list returns same progress`() {
        val progress = PlayerProgress(coins = 500L)
        val result = repo.applyRewardsToProgress(emptyList(), progress)
        assertEquals(500L, result.coins)
    }

    @Test
    fun `applyRewardsToProgress accumulates multiple items`() {
        val progress = PlayerProgress(coins = 0L)
        val items = listOf(
            inbox(coinsGranted = 100L),
            inbox(coinsGranted = 200L),
            inbox(coinsGranted = 300L)
        )
        val result = repo.applyRewardsToProgress(items, progress)
        assertEquals(600L, result.coins)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. getUnclaimedCount
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getUnclaimedCount returns dao count`() = runTest {
        coEvery { inboxDao.countUnclaimed() } returns 5
        assertEquals(5, repo.getUnclaimedCount())
    }

    @Test
    fun `getUnclaimedCount returns 0 when empty`() = runTest {
        coEvery { inboxDao.countUnclaimed() } returns 0
        assertEquals(0, repo.getUnclaimedCount())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. addVipDailyRewardIfNeeded
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `addVipDailyRewardIfNeeded inserts when no reward today`() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        coEvery { inboxDao.getByTypeAndDate("vip_daily", today) } returns null
        coEvery { inboxDao.insert(any()) } returns 42L

        val id = repo.addVipDailyRewardIfNeeded(
            livesGranted = 2,
            addGuessItems = 1,
            removeLetterItems = 1,
            definitionItems = 1,
            showLetterItems = 1,
            daysAccumulated = 1
        )
        assertEquals(42L, id)
        coVerify { inboxDao.insert(any()) }
    }

    @Test
    fun `addVipDailyRewardIfNeeded returns -1 when reward already added today`() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        coEvery { inboxDao.getByTypeAndDate("vip_daily", today) } returns inbox()

        val id = repo.addVipDailyRewardIfNeeded(
            livesGranted = 2,
            addGuessItems = 1,
            removeLetterItems = 1,
            definitionItems = 1,
            showLetterItems = 1,
            daysAccumulated = 1
        )
        assertEquals(-1L, id)
        coVerify(exactly = 0) { inboxDao.insert(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. claimItem
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `claimItem returns null for unknown id`() = runTest {
        coEvery { inboxDao.getById(99) } returns null
        assertNull(repo.claimItem(99))
    }

    @Test
    fun `claimItem marks item claimed`() = runTest {
        val item = inbox(id = 1)
        val claimedItem = item.copy(claimed = true)
        coEvery { inboxDao.getById(1) } returnsMany listOf(item, claimedItem)
        coEvery { inboxDao.markClaimed(1, any()) } just Runs

        val result = repo.claimItem(1)
        assertNotNull(result)
        coVerify { inboxDao.markClaimed(1, any()) }
    }

    @Test
    fun `claimItem returns already-claimed item without re-marking`() = runTest {
        val item = inbox(id = 1, claimed = true)
        coEvery { inboxDao.getById(1) } returns item

        val result = repo.claimItem(1)
        assertEquals(item, result)
        coVerify(exactly = 0) { inboxDao.markClaimed(any(), any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. claimAllItems
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `claimAllItems claims all unclaimed and returns them`() = runTest {
        val unclaimed = listOf(inbox(id = 1), inbox(id = 2))
        coEvery { inboxDao.getUnclaimed() } returns unclaimed
        coEvery { inboxDao.claimAll(any()) } just Runs

        val result = repo.claimAllItems()
        assertEquals(2, result.size)
        coVerify { inboxDao.claimAll(any()) }
    }

    @Test
    fun `claimAllItems returns empty list when nothing unclaimed`() = runTest {
        coEvery { inboxDao.getUnclaimed() } returns emptyList()

        val result = repo.claimAllItems()
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { inboxDao.claimAll(any()) }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun inbox(
        id: Int = 0,
        type: String = "vip_daily",
        title: String = "Test",
        message: String = "Test message",
        coinsGranted: Long = 0L,
        diamondsGranted: Int = 0,
        livesGranted: Int = 0,
        addGuessItemsGranted: Int = 0,
        removeLetterItemsGranted: Int = 0,
        definitionItemsGranted: Int = 0,
        showLetterItemsGranted: Int = 0,
        claimed: Boolean = false
    ) = InboxItemEntity(
        id = id,
        type = type,
        title = title,
        message = message,
        coinsGranted = coinsGranted,
        diamondsGranted = diamondsGranted,
        livesGranted = livesGranted,
        addGuessItemsGranted = addGuessItemsGranted,
        removeLetterItemsGranted = removeLetterItemsGranted,
        definitionItemsGranted = definitionItemsGranted,
        showLetterItemsGranted = showLetterItemsGranted,
        claimed = claimed
    )
}
