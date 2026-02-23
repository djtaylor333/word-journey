package com.djtaylor.wordjourney.ui.store

import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.AdRewardResult
import com.djtaylor.wordjourney.billing.IAdManager
import com.djtaylor.wordjourney.billing.IBillingManager
import com.djtaylor.wordjourney.billing.ProductIds
import com.djtaylor.wordjourney.billing.PurchaseResult
import com.djtaylor.wordjourney.data.repository.InboxRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.VipDailyRewardUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for StoreViewModel.
 *
 * Covers:
 *  - Coin/diamond/life trading
 *  - Item purchasing (add guess, remove letter, definition)
 *  - Insufficient currency handling
 *  - Billing manager integration
 *  - Progress persistence
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var billingManager: IBillingManager
    private lateinit var adManager: IAdManager
    private lateinit var audioManager: WordJourneysAudioManager
    private lateinit var inboxRepository: InboxRepository
    private lateinit var vipDailyRewardUseCase: VipDailyRewardUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        progress: PlayerProgress = PlayerProgress(coins = 5000L, diamonds = 20, lives = 10)
    ): StoreViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } just Runs
        }
        billingManager = mockk {
            every { getPriceLabel(any()) } returns "$0.99"
            coEvery { purchase(any(), any()) } answers {
                val callback = secondArg<(PurchaseResult) -> Unit>()
                callback(PurchaseResult(firstArg(), success = true, coinsGranted = 500L))
            }
        }
        adManager = mockk {
            every { isRewardedAdReady } returns true
            coEvery { loadRewardedAd() } just Runs
            coEvery { showRewardedAd() } returns AdRewardResult(watched = true, rewardType = "coins", rewardAmount = 100)
        }
        audioManager = mockk(relaxed = true)
        inboxRepository = mockk(relaxed = true)
        vipDailyRewardUseCase = mockk {
            every { calculateRewards(any(), any()) } returns null   // no reward by default
        }

        return StoreViewModel(
            playerRepository = playerRepository,
            billingManager = billingManager,
            adManager = adManager,
            audioManager = audioManager,
            inboxRepository = inboxRepository,
            vipDailyRewardUseCase = vipDailyRewardUseCase
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `loads player progress on init`() = runTest {
        val progress = PlayerProgress(coins = 2000L, diamonds = 15, lives = 8)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(2000L, state.progress.coins)
        assertEquals(15, state.progress.diamonds)
        assertEquals(8, state.progress.lives)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. TRADE COINS FOR LIFE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `tradeCoinsForLife deducts 1000 coins and adds 1 life`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 2000L, lives = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.tradeCoinsForLife()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("1000 coins"))
        coVerify { playerRepository.saveProgress(match { it.coins == 1000L && it.lives == 6 }) }
    }

    @Test
    fun `tradeCoinsForLife fails with insufficient coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L, lives = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.tradeCoinsForLife()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("1000 coins"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. TRADE DIAMONDS FOR LIFE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `tradeDiamondsForLife deducts 3 diamonds and adds 1 life`() = runTest {
        val vm = createViewModel(PlayerProgress(diamonds = 10, lives = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.tradeDiamondsForLife()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.diamonds == 7 && it.lives == 6 }) }
    }

    @Test
    fun `tradeDiamondsForLife fails with insufficient diamonds`() = runTest {
        val vm = createViewModel(PlayerProgress(diamonds = 2, lives = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.tradeDiamondsForLife()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("3 diamonds"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. BUY ITEMS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `buyAddGuessItem deducts 200 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyAddGuessItem()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 300L && it.addGuessItems == 1 }) }
    }

    @Test
    fun `buyAddGuessItem fails with insufficient coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 100L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyAddGuessItem()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("200 coins"))
    }

    @Test
    fun `buyRemoveLetterItem deducts 150 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 300L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyRemoveLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 150L && it.removeLetterItems == 1 }) }
    }

    @Test
    fun `buyRemoveLetterItem fails with insufficient coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 50L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyRemoveLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("150 coins"))
    }

    @Test
    fun `buyDefinitionItem deducts 300 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyDefinitionItem()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 200L && it.definitionItems == 1 }) }
    }

    @Test
    fun `buyDefinitionItem fails with insufficient coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 200L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyDefinitionItem()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("300 coins"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. MESSAGE DISMISSAL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dismissMessage clears the message`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 50L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyAddGuessItem() // triggers error message
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.first().message)

        vm.dismissMessage()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.first().message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. PRICE LABELS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getPriceLabel returns billing manager label`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("$0.99", vm.getPriceLabel("coins_500"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. SHOW LETTER ITEM
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `buyShowLetterItem deducts 250 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyShowLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 250L && it.showLetterItems == 1 }) }
    }

    @Test
    fun `buyShowLetterItem fails with insufficient coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 100L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyShowLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("250 coins"))
    }

    @Test
    fun `buyShowLetterItem increments count`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L, showLetterItems = 2))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyShowLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.showLetterItems == 3 }) }
    }

    @Test
    fun `buyShowLetterItem shows success message`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyShowLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("Show Letter"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. MULTIPLE ITEM PURCHASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `buying all item types tracks inventory correctly`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 2000L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.buyAddGuessItem()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.buyRemoveLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.buyDefinitionItem()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.buyShowLetterItem()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all four purchases were saved
        coVerify(atLeast = 4) { playerRepository.saveProgress(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. AD REWARDS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `watchAdForCoins grants 100 coins when ad watched`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForCoins()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 100L }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("100 coins"))
    }

    @Test
    fun `watchAdForCoins does nothing when ad not watched`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L))
        coEvery { adManager.showRewardedAd() } returns AdRewardResult(watched = false, rewardType = "", rewardAmount = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForCoins()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { playerRepository.saveProgress(any()) }
    }

    @Test
    fun `watchAdForLife grants 1 life when ad watched`() = runTest {
        val vm = createViewModel(PlayerProgress(lives = 5))
        coEvery { adManager.showRewardedAd() } returns AdRewardResult(watched = true, rewardType = "life", rewardAmount = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForLife()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.lives == 6 }) }
    }

    @Test
    fun `isAdReady reflects ad manager state`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.isAdReady)
    }

    @Test
    fun `isAdReady is false when ad not loaded`() = runTest {
        val vm = createViewModel()
        every { adManager.isRewardedAdReady } returns false
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger a re-emission by updating progress
        progressFlow.value = progressFlow.value.copy()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isAdReady)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. BUNDLE PURCHASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase starter bundle grants coins, diamonds, lives, and items`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L, diamonds = 0, lives = 0))
        coEvery { billingManager.purchase(ProductIds.STARTER_BUNDLE, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.STARTER_BUNDLE, success = true, coinsGranted = 1000L, diamondsGranted = 5, livesGranted = 5))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.STARTER_BUNDLE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match {
            it.coins == 1000L && it.diamonds == 5 && it.lives == 5 &&
            it.addGuessItems == 5 && it.removeLetterItems == 5 && it.definitionItems == 5 && it.showLetterItems == 5
        }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. VIP SUBSCRIPTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase VIP monthly sets isVip`() = runTest {
        val vm = createViewModel(PlayerProgress(isVip = false))
        coEvery { billingManager.purchase(ProductIds.VIP_MONTHLY, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.VIP_MONTHLY, success = true))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.isVip }) }
    }

    @Test
    fun `purchase VIP yearly sets isVip`() = runTest {
        val vm = createViewModel(PlayerProgress(isVip = false))
        coEvery { billingManager.purchase(ProductIds.VIP_YEARLY, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.VIP_YEARLY, success = true))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.isVip }) }
    }

    @Test
    fun `VIP monthly purchase immediately triggers inbox reward`() = runTest {
        val reward = VipDailyRewardUseCase.VipReward(
            livesGranted = 5,
            addGuessItemsGranted = 2,
            removeLetterItemsGranted = 1,
            definitionItemsGranted = 1,
            showLetterItemsGranted = 1,
            daysAccumulated = 1,
            updatedLastRewardDate = "2026-02-21"
        )
        val vm = createViewModel(PlayerProgress(isVip = false))
        every { vipDailyRewardUseCase.calculateRewards(any(), any()) } returns reward
        coEvery { billingManager.purchase(ProductIds.VIP_MONTHLY, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.VIP_MONTHLY, success = true))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Inbox reward added immediately on purchase
        coVerify { inboxRepository.addVipDailyRewardIfNeeded(any(), any(), any(), any(), any(), any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. DEV MODE PURCHASES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase in dev mode does not call billingManager`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, coins = 0L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        // billingManager.purchase() must NOT be invoked when dev mode is on
        coVerify(exactly = 0) { billingManager.purchase(any(), any()) }
    }

    @Test
    fun `purchase coins 500 in dev mode grants 500 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, coins = 0L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 500L }) }
    }

    @Test
    fun `purchase coins 5000 in dev mode grants 5000 coins`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, coins = 100L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_5000)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 5100L }) }
    }

    @Test
    fun `purchase VIP monthly in dev mode sets isVip without billingManager`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, isVip = false))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { billingManager.purchase(any(), any()) }
        coVerify { playerRepository.saveProgress(match { it.isVip }) }
    }

    @Test
    fun `purchase diamonds 50 in dev mode grants 50 diamonds`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, diamonds = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.DIAMONDS_50)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.diamonds == 55 }) }
    }

    @Test
    fun `purchase normal mode still calls billingManager`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false, coins = 0L))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { billingManager.purchase(ProductIds.COINS_500, any()) }
    }

    @Test
    fun `devBuildFreeResult returns expected result for each productId`() {
        val vm = createViewModel()
        assertEquals(500L,  vm.devBuildFreeResult(ProductIds.COINS_500).coinsGranted)
        assertEquals(1500L, vm.devBuildFreeResult(ProductIds.COINS_1500).coinsGranted)
        assertEquals(5000L, vm.devBuildFreeResult(ProductIds.COINS_5000).coinsGranted)
        assertEquals(10,    vm.devBuildFreeResult(ProductIds.DIAMONDS_10).diamondsGranted)
        assertEquals(50,    vm.devBuildFreeResult(ProductIds.DIAMONDS_50).diamondsGranted)
        assertEquals(200,   vm.devBuildFreeResult(ProductIds.DIAMONDS_200).diamondsGranted)
        assertEquals(5,     vm.devBuildFreeResult(ProductIds.LIVES_PACK_5).livesGranted)
        assertTrue(vm.devBuildFreeResult(ProductIds.VIP_MONTHLY).success)
        assertTrue(vm.devBuildFreeResult(ProductIds.VIP_YEARLY).success)
        assertFalse(vm.devBuildFreeResult("unknown_product").success)
    }
}
