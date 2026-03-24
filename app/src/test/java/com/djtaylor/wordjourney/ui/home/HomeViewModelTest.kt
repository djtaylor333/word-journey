package com.djtaylor.wordjourney.ui.home

import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.auth.AchievementManager
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.data.repository.InboxRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import com.djtaylor.wordjourney.domain.usecase.VipDailyRewardUseCase
import com.djtaylor.wordjourney.review.InAppReviewManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for HomeViewModel.
 *
 * Uses [testWithVm] helper that cancels viewModelScope after assertions
 * to prevent runTest from hanging on the infinite timer loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var inboxRepository: InboxRepository
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
        progress: PlayerProgress = PlayerProgress()
    ): HomeViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } just Runs
        }
        inboxRepository = mockk {
            coEvery { getUnclaimedCount() } returns 0
            coEvery { addVipDailyRewardIfNeeded(any(), any(), any(), any(), any(), any(), any()) } returns -1L
        }
        audioManager = mockk(relaxed = true)

        return HomeViewModel(
            context = mockk(relaxed = true),
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            vipDailyRewardUseCase = VipDailyRewardUseCase(),
            inboxRepository = inboxRepository,
            audioManager = audioManager,
            achievementManager = mockk(relaxed = true),
            activityProvider = mockk(relaxed = true),
            inAppReviewManager = mockk {
                coEvery { requestReview(any()) } returns true
            }
        )
    }

    /** Create VM, advance scheduler, run assertions, then cancel viewModelScope. */
    private fun testWithVm(
        progress: PlayerProgress = PlayerProgress(),
        testBody: suspend TestScope.(HomeViewModel) -> Unit
    ) = runTest {
        val vm = createViewModel(progress)
        testDispatcher.scheduler.runCurrent()
        try {
            testBody(vm)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `loads progress and sets isLoading false`() = testWithVm(PlayerProgress(hasReceivedNewPlayerBonus = true)) { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun `displays correct player progress`() = testWithVm(
        PlayerProgress(coins = 3000L, diamonds = 25, lives = 8, easyLevel = 5, regularLevel = 3, hardLevel = 2, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(3000L, state.progress.coins)
        assertEquals(25, state.progress.diamonds)
        assertEquals(8, state.progress.lives)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. LEVEL TRACKING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `levelForDifficulty returns correct easy level`() = testWithVm(
        PlayerProgress(easyLevel = 10, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(10, vm.levelForDifficulty(Difficulty.EASY))
    }

    @Test
    fun `levelForDifficulty returns correct regular level`() = testWithVm(
        PlayerProgress(regularLevel = 7, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(7, vm.levelForDifficulty(Difficulty.REGULAR))
    }

    @Test
    fun `levelForDifficulty returns correct hard level`() = testWithVm(
        PlayerProgress(hardLevel = 3, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(3, vm.levelForDifficulty(Difficulty.HARD))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. LIFE REGEN
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `does not regen lives when at cap`() = testWithVm(
        PlayerProgress(lives = 10, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(10, state.progress.lives)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. AUDIO
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playButtonClick plays sound effect`() = testWithVm(PlayerProgress(hasReceivedNewPlayerBonus = true)) { vm ->
        vm.playButtonClick()
        verify { audioManager.playSfx(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. REACTIVE UPDATES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `UI updates when progress flow emits new value`() = testWithVm(
        PlayerProgress(coins = 100L, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(100L, vm.uiState.first().progress.coins)

        progressFlow.value = PlayerProgress(coins = 500L, hasReceivedNewPlayerBonus = true)
        testDispatcher.scheduler.runCurrent()

        assertEquals(500L, vm.uiState.first().progress.coins)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. LOGIN STREAK
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `first login sets streak to 1`() = testWithVm(
        PlayerProgress(lastLoginDate = "", loginStreak = 0, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(1, state.progress.loginStreak)
        coVerify { playerRepository.saveProgress(match { it.loginStreak == 1 }) }
    }

    @Test
    fun `same day login does not change streak`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true,
            lastLoginDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            loginStreak = 5
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(5, state.progress.loginStreak)
    }

    @Test
    fun `consecutive day login increments streak`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true,
            lastLoginDate = java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            loginStreak = 3
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(4, state.progress.loginStreak)
        coVerify { playerRepository.saveProgress(match { it.loginStreak == 4 }) }
    }

    @Test
    fun `non-consecutive day resets streak to 1`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true,
            lastLoginDate = java.time.LocalDate.now().minusDays(3).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            loginStreak = 10
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(1, state.progress.loginStreak)
        coVerify { playerRepository.saveProgress(match { it.loginStreak == 1 }) }
    }

    @Test
    fun `login streak updates loginBestStreak`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true,
            lastLoginDate = java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            loginStreak = 9,
            loginBestStreak = 9
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(10, state.progress.loginStreak)
        assertEquals(10, state.progress.loginBestStreak)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. DAILY CHALLENGE STREAK DISPLAY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dailyChallengeStreak displayed from progress`() = testWithVm(
        PlayerProgress(dailyChallengeStreak = 7, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(7, vm.uiState.first().dailyChallengeStreak)
    }

    @Test
    fun `zero daily streak when new player`() = testWithVm(PlayerProgress(hasReceivedNewPlayerBonus = true)) { vm ->
        assertEquals(0, vm.uiState.first().dailyChallengeStreak)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. SHOW LETTER ITEMS IN PROGRESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `showLetterItems tracked in progress`() = testWithVm(
        PlayerProgress(showLetterItems = 3, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertEquals(3, vm.uiState.first().progress.showLetterItems)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. NEW PLAYER BONUS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `new player receives 500 coins 5 diamonds and 3 of each item`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = false, coins = 0, diamonds = 5)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(500L, state.progress.coins)
        assertEquals(10, state.progress.diamonds)
        assertEquals(3, state.progress.addGuessItems)
        assertEquals(3, state.progress.removeLetterItems)
        assertEquals(3, state.progress.definitionItems)
        assertEquals(3, state.progress.showLetterItems)
        assertTrue(state.progress.hasReceivedNewPlayerBonus)
        assertNotNull(state.newPlayerBonusMessage)
    }

    @Test
    fun `existing player does not get bonus again`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true, coins = 100, diamonds = 2)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(100L, state.progress.coins)
        assertEquals(2, state.progress.diamonds)
        assertNull(state.newPlayerBonusMessage)
    }

    @Test
    fun `new player bonus adds to existing items`() = testWithVm(
        PlayerProgress(
            hasReceivedNewPlayerBonus = false,
            coins = 200,
            diamonds = 10,
            addGuessItems = 2,
            removeLetterItems = 1,
            definitionItems = 0,
            showLetterItems = 5
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(700L, state.progress.coins) // 200 + 500
        assertEquals(15, state.progress.diamonds) // 10 + 5
        assertEquals(5, state.progress.addGuessItems) // 2 + 3
        assertEquals(4, state.progress.removeLetterItems) // 1 + 3
        assertEquals(3, state.progress.definitionItems) // 0 + 3
        assertEquals(8, state.progress.showLetterItems) // 5 + 3
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. VIP DAILY REWARDS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `VIP player receives daily rewards`() = runTest {
        // VIP rewards now go to inbox rather than being applied directly.
        // The showVipClaimDialog should be set to true when a reward is deposited.
        val vm = createViewModel(
            PlayerProgress(
                isVip = true,
                lastVipRewardDate = "",
                lives = 5,
                hasReceivedNewPlayerBonus = true
            )
        )
        // Override inbox mock so addVipDailyRewardIfNeeded returns a valid id (reward added)
        coEvery { inboxRepository.addVipDailyRewardIfNeeded(any(), any(), any(), any(), any(), any(), any()) } returns 1L
        coEvery { inboxRepository.getUnclaimedCount() } returns 1
        testDispatcher.scheduler.runCurrent()
        try {
            val state = vm.uiState.first()
            // Lives are no longer added directly — they stay at 5
            assertEquals(5, state.progress.lives)
            assertTrue("showVipClaimDialog should be true when reward is added", state.showVipClaimDialog)
            assertNotNull(state.pendingVipDaysMessage)
            assertTrue(state.progress.lastVipRewardDate.isNotEmpty())
            assertEquals(1, state.inboxCount)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `dismissVipClaimDialog clears showVipClaimDialog`() = runTest {
        val vm = createViewModel(
            PlayerProgress(
                isVip = true,
                lastVipRewardDate = "",
                lives = 5,
                hasReceivedNewPlayerBonus = true
            )
        )
        coEvery { inboxRepository.addVipDailyRewardIfNeeded(any(), any(), any(), any(), any(), any(), any()) } returns 1L
        coEvery { inboxRepository.getUnclaimedCount() } returns 1
        testDispatcher.scheduler.runCurrent()
        try {
            assertTrue(vm.uiState.first().showVipClaimDialog)
            vm.dismissVipClaimDialog()
            testDispatcher.scheduler.runCurrent()
            assertFalse(vm.uiState.first().showVipClaimDialog)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `non-VIP player gets no VIP rewards`() = testWithVm(
        PlayerProgress(isVip = false, lives = 5, hasReceivedNewPlayerBonus = true)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(5, state.progress.lives)
        assertFalse(state.showVipClaimDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. DEV MODE STATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `devModeEnabled is false by default`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true)
    ) { vm ->
        assertFalse(vm.uiState.first().devModeEnabled)
    }

    @Test
    fun `devModeEnabled true when progress has dev mode enabled`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true, devModeEnabled = true)
    ) { vm ->
        assertTrue(vm.uiState.first().devModeEnabled)
    }

    @Test
    fun `devModeEnabled updates reactively when progress changes`() = testWithVm(
        PlayerProgress(hasReceivedNewPlayerBonus = true, devModeEnabled = false)
    ) { vm ->
        assertFalse(vm.uiState.first().devModeEnabled)

        progressFlow.value = progressFlow.value.copy(devModeEnabled = true)
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.first().devModeEnabled)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. IN-APP REVIEW PROMPT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `showReviewPrompt is false when levelsCompletedForReview is below threshold`() = testWithVm(
        PlayerProgress(
            levelsCompletedForReview = 9,
            hasReviewBeenRequested = false,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        assertFalse(vm.uiState.first().showReviewPrompt)
    }

    @Test
    fun `showReviewPrompt is true when levelsCompletedForReview reaches 10`() = testWithVm(
        PlayerProgress(
            levelsCompletedForReview = 10,
            hasReviewBeenRequested = false,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        assertTrue(vm.uiState.first().showReviewPrompt)
    }

    @Test
    fun `showReviewPrompt is false when hasReviewBeenRequested is true even with 10 levels`() = testWithVm(
        PlayerProgress(
            levelsCompletedForReview = 10,
            hasReviewBeenRequested = true,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        assertFalse(vm.uiState.first().showReviewPrompt)
    }

    @Test
    fun `dismissReviewPrompt hides dialog and saves hasReviewBeenRequested`() = testWithVm(
        PlayerProgress(
            levelsCompletedForReview = 10,
            hasReviewBeenRequested = false,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        // Dialog should be shown initially
        assertTrue(vm.uiState.first().showReviewPrompt)

        vm.dismissReviewPrompt()
        testDispatcher.scheduler.runCurrent()

        // Dialog should be hidden
        assertFalse(vm.uiState.first().showReviewPrompt)

        // Progress should be saved with hasReviewBeenRequested = true and levelsCompletedForReview reset
        coVerify { playerRepository.saveProgress(match { it.hasReviewBeenRequested && it.levelsCompletedForReview == 0 }) }
    }

    @Test
    fun `completeReviewWithReward grants 5 lives 1000 coins 10 diamonds`() = testWithVm(
        PlayerProgress(
            lives = 3, coins = 500L, diamonds = 2,
            levelsCompletedForReview = 10,
            hasReviewBeenRequested = false,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        val activity = mockk<android.app.Activity>(relaxed = true)
        vm.completeReviewWithReward(activity)
        testDispatcher.scheduler.runCurrent()

        // Dialog should be dismissed
        assertFalse(vm.uiState.first().showReviewPrompt)

        // Reward should be saved: +5 lives, +1000 coins, +10 diamonds
        coVerify {
            playerRepository.saveProgress(match {
                it.lives == 8 &&
                it.coins == 1500L &&
                it.diamonds == 12 &&
                it.hasReviewBeenRequested &&
                it.reviewRewarded &&
                it.levelsCompletedForReview == 0
            })
        }
    }

    @Test
    fun `review prompt is not shown again once session flag is set`() = testWithVm(
        PlayerProgress(
            levelsCompletedForReview = 10,
            hasReviewBeenRequested = false,
            hasReceivedNewPlayerBonus = true
        )
    ) { vm ->
        // First emission — should trigger prompt
        assertTrue(vm.uiState.first().showReviewPrompt)

        // Simulate flow re-emission (still 10 levels, not requested yet in DB)
        progressFlow.value = progressFlow.value.copy(coins = 9999L)
        testDispatcher.scheduler.runCurrent()

        // showReviewPrompt should still be true (not toggled off by re-emission)
        assertTrue(vm.uiState.first().showReviewPrompt)
    }
}
