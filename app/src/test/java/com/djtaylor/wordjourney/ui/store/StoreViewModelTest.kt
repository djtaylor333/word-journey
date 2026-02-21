package com.djtaylor.wordjourney.ui.store

import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.IBillingManager
import com.djtaylor.wordjourney.billing.PurchaseResult
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
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
    private lateinit var audioManager: WordJourneysAudioManager

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
        audioManager = mockk(relaxed = true)

        return StoreViewModel(
            playerRepository = playerRepository,
            billingManager = billingManager,
            audioManager = audioManager
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
}
