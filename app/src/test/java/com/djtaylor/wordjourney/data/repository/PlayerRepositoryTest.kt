package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.cloud.CloudSaveManager
import com.djtaylor.wordjourney.data.datastore.PlayerDataStore
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PlayerRepository].
 *
 * Tests focus on [mergeProgress] logic (accessed via [syncFromCloud]) and the
 * [syncFromCloud] fix (using first() instead of collect{} so it terminates).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerRepositoryTest {

    private lateinit var dataStore: PlayerDataStore
    private lateinit var cloudSave: CloudSaveManager
    private lateinit var repository: PlayerRepository
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>

    @Before
    fun setUp() {
        progressFlow = MutableStateFlow(PlayerProgress())
        dataStore = mockk {
            every { playerProgressFlow } returns progressFlow
            every { isFirstLaunch } returns MutableStateFlow(false)
            coEvery { savePlayerProgress(any()) } just Runs
        }
        cloudSave = mockk {
            coEvery { writeSave(any()) } just Runs
        }
        repository = PlayerRepository(dataStore, cloudSave)
    }

    // ── syncFromCloud ────────────────────────────────────────────────────────

    @Test
    fun `syncFromCloud returns null when cloud returns null`() = runTest {
        coEvery { cloudSave.loadSave() } returns null
        val result = repository.syncFromCloud()
        assertNull(result)
        coVerify(exactly = 0) { dataStore.savePlayerProgress(any()) }
    }

    @Test
    fun `syncFromCloud merges and saves when cloud has data`() = runTest {
        val cloud = PlayerProgress(coins = 500L, easyLevel = 5)
        val local = PlayerProgress(coins = 200L, easyLevel = 3, regularLevel = 7)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()

        assertNotNull(result)
        assertEquals(500L, result!!.coins)           // max(200, 500)
        assertEquals(5, result.easyLevel)             // max(3, 5)
        assertEquals(7, result.regularLevel)          // max(7, 0) — from local
        coVerify { dataStore.savePlayerProgress(result) }
    }

    // ── mergeProgress coverage ────────────────────────────────────────────────

    @Test
    fun `merge takes max vipLevel from cloud`() = runTest {
        val local = PlayerProgress(vipLevel = 3)
        val cloud = PlayerProgress(vipLevel = 8)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertEquals(8, result.vipLevel)
    }

    @Test
    fun `merge takes max items from both sources`() = runTest {
        val local = PlayerProgress(addGuessItems = 2, removeLetterItems = 5)
        val cloud = PlayerProgress(addGuessItems = 4, removeLetterItems = 1)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertEquals(4, result.addGuessItems)
        assertEquals(5, result.removeLetterItems)
    }

    @Test
    fun `merge grants VIP if either source is VIP`() = runTest {
        val local = PlayerProgress(isVip = false)
        val cloud = PlayerProgress(isVip = true)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertTrue(result.isVip)
    }

    @Test
    fun `merge hasReceivedNewPlayerBonus true if either source is true`() = runTest {
        val local = PlayerProgress(hasReceivedNewPlayerBonus = false)
        val cloud = PlayerProgress(hasReceivedNewPlayerBonus = true)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertTrue(result.hasReceivedNewPlayerBonus)
    }

    @Test
    fun `merge takes max seasonal levels from both sources`() = runTest {
        val local = PlayerProgress(seasonalEasterLevel = 10, seasonalChristmasLevel = 50)
        val cloud = PlayerProgress(seasonalEasterLevel = 25, seasonalChristmasLevel = 30)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertEquals(25, result.seasonalEasterLevel)
        assertEquals(50, result.seasonalChristmasLevel)
    }

    @Test
    fun `merge takes max lastVipRewardDate (lexicographic, favours more recent YYYY-MM-DD)`() = runTest {
        val local = PlayerProgress(lastVipRewardDate = "2026-03-01")
        val cloud = PlayerProgress(lastVipRewardDate = "2026-03-15")
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertEquals("2026-03-15", result.lastVipRewardDate)
    }

    @Test
    fun `merge takes max totalStarsEarned`() = runTest {
        val local = PlayerProgress(totalStarsEarned = 100)
        val cloud = PlayerProgress(totalStarsEarned = 250)
        progressFlow.value = local
        coEvery { cloudSave.loadSave() } returns cloud

        val result = repository.syncFromCloud()!!
        assertEquals(250, result.totalStarsEarned)
    }

    // ── saveProgress ─────────────────────────────────────────────────────────

    @Test
    fun `saveProgress writes to both dataStore and cloud`() = runTest {
        val progress = PlayerProgress(coins = 999L)
        repository.saveProgress(progress)
        coVerify { dataStore.savePlayerProgress(progress) }
        coVerify { cloudSave.writeSave(progress) }
    }
}
