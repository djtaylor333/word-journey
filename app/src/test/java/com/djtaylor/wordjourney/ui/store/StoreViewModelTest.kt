package com.djtaylor.wordjourney.ui.store

import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.auth.AchievementManager
import com.djtaylor.wordjourney.billing.ActivityProvider
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
    private lateinit var achievementManager: AchievementManager
    private lateinit var activityProvider: ActivityProvider

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
            // New diagnostic interface methods — all products loaded by default
            coEvery { getLoadedProductIds() } returns setOf(
                ProductIds.COINS_500, ProductIds.COINS_1500, ProductIds.COINS_5000,
                ProductIds.DIAMONDS_10, ProductIds.DIAMONDS_50, ProductIds.DIAMONDS_200,
                ProductIds.LIVES_PACK_5,
                ProductIds.STARTER_BUNDLE, ProductIds.ADVENTURER_BUNDLE, ProductIds.CHAMPION_BUNDLE,
                ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY
            )
            every { getAllProductIds() } returns setOf(
                ProductIds.COINS_500, ProductIds.COINS_1500, ProductIds.COINS_5000,
                ProductIds.DIAMONDS_10, ProductIds.DIAMONDS_50, ProductIds.DIAMONDS_200,
                ProductIds.LIVES_PACK_5,
                ProductIds.STARTER_BUNDLE, ProductIds.ADVENTURER_BUNDLE, ProductIds.CHAMPION_BUNDLE,
                ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY
            )
            // Restore: no pending purchases by default
            coEvery { restoreAndGrantPendingPurchases() } returns emptyList()
        }
        adManager = mockk {
            every { isRewardedAdReady } returns true
            coEvery { loadRewardedAd() } just Runs
            coEvery { showRewardedAd(any()) } returns AdRewardResult(watched = true, rewardType = "coins", rewardAmount = 100)
        }
        audioManager = mockk(relaxed = true)
        inboxRepository = mockk(relaxed = true)
        vipDailyRewardUseCase = mockk {
            every { calculateRewards(any(), any()) } returns null   // no reward by default
        }
        achievementManager = mockk(relaxed = true)
        activityProvider = mockk {
            every { currentActivity } returns null
        }

        return StoreViewModel(
            playerRepository = playerRepository,
            billingManager = billingManager,
            adManager = adManager,
            audioManager = audioManager,
            inboxRepository = inboxRepository,
            vipDailyRewardUseCase = vipDailyRewardUseCase,
            achievementManager = achievementManager,
            activityProvider = activityProvider
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

        assertEquals("$0.99", vm.getPriceLabel(ProductIds.COINS_500))
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

        vm.watchAdForCoins(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 100L }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("100 coins"))
    }

    @Test
    fun `watchAdForCoins does nothing when ad not watched`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L))
        coEvery { adManager.showRewardedAd(any()) } returns AdRewardResult(watched = false, rewardType = "", rewardAmount = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForCoins(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { playerRepository.saveProgress(any()) }
    }

    @Test
    fun `watchAdForLife grants 1 life when ad watched`() = runTest {
        val vm = createViewModel(PlayerProgress(lives = 5))
        coEvery { adManager.showRewardedAd(any()) } returns AdRewardResult(watched = true, rewardType = "life", rewardAmount = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForLife(mockk())
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

    @Test
    fun `watchAdForItem grants coins when category is coins`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 1   // category = coins
        every { mockRandom.nextInt(451) } returns 100 // amount = 50 + 100 = 150
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 650L }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("150 coins"))
    }

    @Test
    fun `watchAdForItem grants diamonds when category is diamonds`() = runTest {
        val vm = createViewModel(PlayerProgress(diamonds = 5))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 2  // category = diamonds
        every { mockRandom.nextInt(10) } returns 5  // amount = 1 + 5 = 6
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.diamonds == 11 }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("6"))
        assertTrue(state.message!!.contains("💎"))
    }

    @Test
    fun `watchAdForItem grants item when category is item`() = runTest {
        val vm = createViewModel(PlayerProgress(removeLetterItems = 0))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returnsMany listOf(0, 2) // category=items (0), then count-1=2 → count=3
        every { mockRandom.nextInt(4) } returns 1                 // item type = removeLetterItems
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.removeLetterItems == 3 }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("3"))
        assertTrue(state.message!!.contains("Remove Letter"))
    }

    @Test
    fun `watchAdForItem grants 1 item when count roll is 0`() = runTest {
        val vm = createViewModel(PlayerProgress(definitionItems = 2))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returnsMany listOf(0, 0) // category=items, count-1=0 → count=1
        every { mockRandom.nextInt(4) } returns 2                 // item type = definitionItems
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.definitionItems == 3 }) }
        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertFalse(state.message!!.contains("items"))  // singular: "item", not "items"
        assertTrue(state.message!!.contains("Definition"))
    }

    @Test
    fun `watchAdForItem coins range maximum boundary`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 1    // category = coins
        every { mockRandom.nextInt(451) } returns 450 // 50 + 450 = 500
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 500L }) }
    }

    @Test
    fun `watchAdForItem coins range minimum boundary`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 1    // category = coins
        every { mockRandom.nextInt(451) } returns 0  // 50 + 0 = 50
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 50L }) }
    }

    @Test
    fun `watchAdForItem diamonds range maximum boundary`() = runTest {
        val vm = createViewModel(PlayerProgress(diamonds = 0))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 2    // category = diamonds
        every { mockRandom.nextInt(10) } returns 9   // 1 + 9 = 10
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.diamonds == 10 }) }
    }

    @Test
    fun `watchAdForItem diamonds range minimum boundary`() = runTest {
        val vm = createViewModel(PlayerProgress(diamonds = 0))
        val mockRandom = mockk<kotlin.random.Random>()
        every { mockRandom.nextInt(3) } returns 2    // category = diamonds
        every { mockRandom.nextInt(10) } returns 0   // 1 + 0 = 1
        vm.random = mockRandom
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.diamonds == 1 }) }
    }

    @Test
    fun `watchAdForItem does nothing when ad not watched`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 500L))
        coEvery { adManager.showRewardedAd(any()) } returns AdRewardResult(watched = false)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.watchAdForItem(mockk())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { playerRepository.saveProgress(any()) }
        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("not completed"))
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
            livesGranted = 2,
            coinsGranted = 167L,
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
        coVerify { inboxRepository.addVipDailyRewardIfNeeded(any(), any(), any(), any(), any(), any(), any()) }
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

    // ══════════════════════════════════════════════════════════════════════════
    // 13. PURCHASE FAILURES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `failed purchase shows error message`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false, coins = 0L))
        coEvery { billingManager.purchase(any(), any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(firstArg(), success = false))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(
            "Expected 'Purchase failed' message but got: '${state.message}'",
            state.message!!.contains("Purchase failed", ignoreCase = true)
        )
    }

    @Test
    fun `failed purchase does not change player progress`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false, coins = 500L))
        coEvery { billingManager.purchase(any(), any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(firstArg(), success = false))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { playerRepository.saveProgress(any()) }
    }

    @Test
    fun `failed purchase resets isPurchasing to false`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.purchase(any(), any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(firstArg(), success = false))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().isPurchasing)
    }

    @Test
    fun `successful purchase resets isPurchasing to false`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false, coins = 0L))
        coEvery { billingManager.purchase(ProductIds.COINS_1500, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.COINS_1500, success = true, coinsGranted = 1500L))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_1500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().isPurchasing)
    }

    @Test
    fun `purchase uses ProductIds_COINS_500 = coins_500`() = runTest {
        // Verifies the constant used when calling billingManager matches Play Console
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { billingManager.purchase(eq("coins_500"), any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §12 — Billing diagnostics / setup warning
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `billingSetupWarning is null when all products are loaded`() = runTest {
        // All products available — no warning should appear
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.first().billingSetupWarning)
    }

    @Test
    fun `billingSetupWarning is set when products are missing from Play Console`() = runTest {
        // Initialize all mocks via createViewModel first, then override billing mock
        createViewModel(PlayerProgress(devModeEnabled = false))

        // Set up a fresh progress flow with devMode = false
        progressFlow = MutableStateFlow(PlayerProgress(devModeEnabled = false))
        every { playerRepository.playerProgressFlow } returns progressFlow
        // Simulate Play Console only having loaded 1 of 3 products
        coEvery { billingManager.getLoadedProductIds() } returns setOf(ProductIds.COINS_500)
        every { billingManager.getAllProductIds() } returns setOf(
            ProductIds.COINS_500, ProductIds.DIAMONDS_10, ProductIds.VIP_MONTHLY
        )
        val vm = StoreViewModel(
            playerRepository = playerRepository,
            billingManager = billingManager,
            adManager = adManager,
            audioManager = audioManager,
            inboxRepository = inboxRepository,
            vipDailyRewardUseCase = vipDailyRewardUseCase,
            achievementManager = achievementManager,
            activityProvider = activityProvider
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = vm.uiState.first().billingSetupWarning
        assertNotNull(warning)
        assertTrue(warning!!.contains("unavailable"))
    }

    @Test
    fun `billingSetupWarning is null in dev mode even if products missing`() = runTest {
        // Initialize all mocks via createViewModel first, then override billing mock
        createViewModel(PlayerProgress(devModeEnabled = true))

        // Dev mode bypasses real billing — no warning should be shown
        progressFlow = MutableStateFlow(PlayerProgress(devModeEnabled = true))
        every { playerRepository.playerProgressFlow } returns progressFlow
        coEvery { billingManager.getLoadedProductIds() } returns emptySet()
        every { billingManager.getAllProductIds() } returns setOf(ProductIds.COINS_500)
        val vm = StoreViewModel(
            playerRepository = playerRepository,
            billingManager = billingManager,
            adManager = adManager,
            audioManager = audioManager,
            inboxRepository = inboxRepository,
            vipDailyRewardUseCase = vipDailyRewardUseCase,
            achievementManager = achievementManager,
            activityProvider = activityProvider
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.first().billingSetupWarning)
    }

    @Test
    fun `purchase failure shows errorMessage from PurchaseResult when provided`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        val specificError = "Product not found: \"coins_500\". Check Play Console setup."
        coEvery { billingManager.purchase(ProductIds.COINS_500, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.COINS_500, success = false, errorMessage = specificError))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        val message = vm.uiState.first().message
        assertNotNull(message)
        assertEquals(specificError, message)
    }

    @Test
    fun `purchase failure shows fallback message when no errorMessage`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.purchase(ProductIds.DIAMONDS_10, any()) } answers {
            val callback = secondArg<(PurchaseResult) -> Unit>()
            callback(PurchaseResult(ProductIds.DIAMONDS_10, success = false, errorMessage = null))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.DIAMONDS_10)
        testDispatcher.scheduler.advanceUntilIdle()

        val message = vm.uiState.first().message
        assertNotNull(message)
        assertTrue(message!!.contains("diamonds_10"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §13 — Restore Purchases
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `restorePurchases shows no purchases to restore when billing returns empty`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.restoreAndGrantPendingPurchases() } returns emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.restorePurchases()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("No purchases"))
        assertFalse(state.isRestoringPurchases)
    }

    @Test
    fun `restorePurchases grants coins when pending purchase is returned`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 0L, devModeEnabled = false))
        coEvery { billingManager.restoreAndGrantPendingPurchases() } returns listOf(
            PurchaseResult(ProductIds.COINS_500, success = true, coinsGranted = 500L)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.restorePurchases()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 500L }) }
        val state = vm.uiState.first()
        assertFalse(state.isRestoringPurchases)
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("Restored 1"))
    }

    @Test
    fun `restorePurchases grants multiple reward types from multiple pending purchases`() = runTest {
        val vm = createViewModel(PlayerProgress(coins = 100L, diamonds = 5, lives = 3, devModeEnabled = false))
        coEvery { billingManager.restoreAndGrantPendingPurchases() } returns listOf(
            PurchaseResult(ProductIds.COINS_500, success = true, coinsGranted = 500L),
            PurchaseResult(ProductIds.DIAMONDS_10, success = true, diamondsGranted = 10),
            PurchaseResult(ProductIds.LIVES_PACK_5, success = true, livesGranted = 5)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.restorePurchases()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.coins == 600L }) }
        coVerify { playerRepository.saveProgress(match { it.diamonds == 15 }) }
        coVerify { playerRepository.saveProgress(match { it.lives == 8 }) }
        val state = vm.uiState.first()
        assertTrue(state.message!!.contains("Restored 3"))
    }

    @Test
    fun `restorePurchases sets isRestoringPurchases true then false`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        // Use a suspending mock that we can observe state mid-flight
        val restoreStarted = mutableListOf<Boolean>()
        coEvery { billingManager.restoreAndGrantPendingPurchases() } coAnswers {
            restoreStarted.add(vm.uiState.value.isRestoringPurchases)
            emptyList()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.restorePurchases()
        testDispatcher.scheduler.advanceUntilIdle()

        // Was true during the async call, then reset to false
        assertTrue(restoreStarted.firstOrNull() == true)
        assertFalse(vm.uiState.first().isRestoringPurchases)
    }

    @Test
    fun `restorePurchases does not double-invoke when already in progress`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.restoreAndGrantPendingPurchases() } returns emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        // Force isRestoringPurchases = true by calling twice without advancing
        vm.restorePurchases()
        vm.restorePurchases() // second call should be ignored
        testDispatcher.scheduler.advanceUntilIdle()

        // restoreAndGrantPendingPurchases must only be called once
        coVerify(exactly = 1) { billingManager.restoreAndGrantPendingPurchases() }
    }

    @Test
    fun `restorePurchases shows product IDs in success message`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.restoreAndGrantPendingPurchases() } returns listOf(
            PurchaseResult(ProductIds.COINS_500, success = true, coinsGranted = 500L)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.restorePurchases()
        testDispatcher.scheduler.advanceUntilIdle()

        val message = vm.uiState.first().message
        assertNotNull(message)
        assertTrue(message!!.contains(ProductIds.COINS_500))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §14 — VIP Purchase flow
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchase VIP_MONTHLY in dev mode sets isVip = true`() = runTest {
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.isVip }) }
    }

    @Test
    fun `purchase VIP_YEARLY in dev mode sets isVip = true`() = runTest {
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.isVip }) }
    }

    @Test
    fun `purchase VIP_MONTHLY sets vipExpiryTimestamp approximately 30 days ahead`() = runTest {
        val before = System.currentTimeMillis()
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val expectedMin = before + 29L * 24 * 60 * 60 * 1000
        val expectedMax = before + 31L * 24 * 60 * 60 * 1000
        coVerify {
            playerRepository.saveProgress(match { progress ->
                progress.vipExpiryTimestamp in expectedMin..expectedMax
            })
        }
    }

    @Test
    fun `purchase VIP_YEARLY sets vipExpiryTimestamp approximately 365 days ahead`() = runTest {
        val before = System.currentTimeMillis()
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.VIP_YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val expectedMin = before + 364L * 24 * 60 * 60 * 1000
        val expectedMax = before + 366L * 24 * 60 * 60 * 1000
        coVerify {
            playerRepository.saveProgress(match { progress ->
                progress.vipExpiryTimestamp in expectedMin..expectedMax
            })
        }
    }

    @Test
    fun `VIP_MONTHLY in dev mode sets vipExpiryTimestamp far in the future`() = runTest {
        val before = System.currentTimeMillis()
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.purchase(ProductIds.VIP_MONTHLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should be at least 29 days from now
        coVerify { playerRepository.saveProgress(match { it.vipExpiryTimestamp > before + 29L * 24 * 60 * 60 * 1000 }) }
    }

    @Test
    fun `VIP_YEARLY in dev mode sets vipExpiryTimestamp approximately 365 days ahead`() = runTest {
        val before = System.currentTimeMillis()
        val vm = createViewModel(PlayerProgress(isVip = false, devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.purchase(ProductIds.VIP_YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should be at least 364 days from now
        coVerify { playerRepository.saveProgress(match { it.vipExpiryTimestamp > before + 364L * 24 * 60 * 60 * 1000 }) }
    }

    @Test
    fun `purchase STARTER_BUNDLE in dev mode grants bundle items`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, addGuessItems = 0))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.purchase(ProductIds.STARTER_BUNDLE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            playerRepository.saveProgress(match { progress ->
                progress.addGuessItems >= 5 &&
                progress.removeLetterItems >= 5 &&
                progress.definitionItems >= 5 &&
                progress.showLetterItems >= 5
            })
        }
    }

    @Test
    fun `purchase ADVENTURER_BUNDLE in dev mode grants 10 add-guess items`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, addGuessItems = 0))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.purchase(ProductIds.ADVENTURER_BUNDLE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.addGuessItems == 10 }) }
    }

    @Test
    fun `purchase CHAMPION_BUNDLE in dev mode grants 25 items of each type`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true, addGuessItems = 0))
        testDispatcher.scheduler.advanceUntilIdle()
        vm.purchase(ProductIds.CHAMPION_BUNDLE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            playerRepository.saveProgress(match { progress ->
                progress.addGuessItems == 25 &&
                progress.removeLetterItems == 25 &&
                progress.definitionItems == 25 &&
                progress.showLetterItems == 25
            })
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §15 — Billing warning message content
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `billingSetupWarning message contains count of missing products`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        // Only 1 product loaded out of all
        coEvery { billingManager.getLoadedProductIds() } returns setOf(ProductIds.COINS_500)
        every { billingManager.getAllProductIds() } returns setOf(
            ProductIds.COINS_500, ProductIds.COINS_1500, ProductIds.COINS_5000,
            ProductIds.DIAMONDS_10
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = vm.uiState.first().billingSetupWarning
        assertNotNull(warning)
        // Should contain "3" missing (4 total - 1 loaded)
        assertTrue("Warning should mention 3 missing products",
            warning!!.contains("3"))
    }

    @Test
    fun `billingSetupWarning message mentions ACTIVE in Play Console`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.getLoadedProductIds() } returns emptySet()
        every { billingManager.getAllProductIds() } returns setOf(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = vm.uiState.first().billingSetupWarning
        assertNotNull(warning)
        assertTrue("Warning should direct user to Play Console",
            warning!!.contains("ACTIVE", ignoreCase = true) ||
            warning.contains("Play Console", ignoreCase = true))
    }

    @Test
    fun `billingSetupWarning message mentions Licensed Tester`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.getLoadedProductIds() } returns emptySet()
        every { billingManager.getAllProductIds() } returns setOf(ProductIds.COINS_500)
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = vm.uiState.first().billingSetupWarning
        assertNotNull(warning)
        assertTrue("Warning should mention Licensed Tester",
            warning!!.contains("Licensed Tester", ignoreCase = true) ||
            warning.contains("tester", ignoreCase = true))
    }

    @Test
    fun `billingSetupWarning lists the specific missing product IDs`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = false))
        coEvery { billingManager.getLoadedProductIds() } returns emptySet()
        every { billingManager.getAllProductIds() } returns setOf(
            ProductIds.COINS_500, ProductIds.VIP_MONTHLY
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val warning = vm.uiState.first().billingSetupWarning
        assertNotNull(warning)
        assertTrue("Warning should list coins_500", warning!!.contains(ProductIds.COINS_500))
        assertTrue("Warning should list vip_monthly", warning.contains(ProductIds.VIP_MONTHLY))
    }
}
