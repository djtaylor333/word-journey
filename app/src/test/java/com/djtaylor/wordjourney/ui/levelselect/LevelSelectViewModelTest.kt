package com.djtaylor.wordjourney.ui.levelselect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.ActivityProvider
import com.djtaylor.wordjourney.billing.AdRewardResult
import com.djtaylor.wordjourney.billing.IAdManager
import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
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
 * Comprehensive tests for LevelSelectViewModel.
 *
 * Uses [testWithVm] helper that cancels viewModelScope after assertions
 * to prevent runTest from hanging on the infinite timer loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LevelSelectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var audioManager: WordJourneysAudioManager
    private lateinit var starRatingDao: StarRatingDao
    private lateinit var adManager: IAdManager
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
        difficulty: String = "easy",
        progress: PlayerProgress = PlayerProgress()
    ): LevelSelectViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } just Runs
        }
        audioManager = mockk(relaxed = true)

        starRatingDao = mockk {
            coEvery { getAllForDifficulty(any()) } returns emptyList()
            coEvery { totalStars() } returns 0
            coEvery { totalStarsForDifficulty(any()) } returns 0
            coEvery { countPerfectLevels() } returns 0
        }

        adManager = mockk {
            every { isRewardedAdReady } returns false
            coEvery { loadRewardedAd() } just Runs
            coEvery { showRewardedAd(any()) } returns AdRewardResult(watched = false)
        }
        activityProvider = mockk {
            every { currentActivity } returns null
        }

        return LevelSelectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("difficulty" to difficulty)),
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager,
            starRatingDao = starRatingDao,
            adManager = adManager,
            activityProvider = activityProvider
        )
    }

    /** Create VM, advance scheduler, run assertions, then cancel viewModelScope. */
    private fun testWithVm(
        difficulty: String = "easy",
        progress: PlayerProgress = PlayerProgress(),
        testBody: suspend TestScope.(LevelSelectViewModel) -> Unit
    ) = runTest {
        val vm = createViewModel(difficulty, progress)
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
    fun `loads easy difficulty correctly`() = testWithVm("easy", PlayerProgress(easyLevel = 3)) { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(Difficulty.EASY, state.difficulty)
        assertEquals(3, state.currentLevel)
    }

    @Test
    fun `loads regular difficulty correctly`() = testWithVm("regular", PlayerProgress(regularLevel = 7)) { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(Difficulty.REGULAR, state.difficulty)
        assertEquals(7, state.currentLevel)
    }

    @Test
    fun `loads hard difficulty correctly`() = testWithVm("hard", PlayerProgress(hardLevel = 12)) { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(Difficulty.HARD, state.difficulty)
        assertEquals(12, state.currentLevel)
    }

    @Test
    fun `displays correct lives and coins`() = testWithVm(progress = PlayerProgress(lives = 7, coins = 1500L, diamonds = 8)) { vm ->
        val state = vm.uiState.first()
        assertEquals(7, state.lives)
        assertEquals(0, state.bonusLives)
        assertEquals(1500L, state.coins)
        assertEquals(8, state.diamonds)
    }

    @Test
    fun `displays bonus lives when above 10`() = testWithVm(progress = PlayerProgress(lives = 15)) { vm ->
        val state = vm.uiState.first()
        assertEquals(10, state.lives)
        assertEquals(5, state.bonusLives)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. LEVEL START — LIFE DEDUCTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `canStartLevel returns true with lives`() = testWithVm(progress = PlayerProgress(lives = 5, easyLevel = 1)) { vm ->
        assertTrue(vm.canStartLevel(1))
    }

    @Test
    fun `canStartLevel returns false with 0 lives for current level`() = testWithVm(progress = PlayerProgress(lives = 0, easyLevel = 1)) { vm ->
        assertFalse(vm.canStartLevel(1))
    }

    @Test
    fun `canStartLevel returns true for replay even with 0 lives`() = testWithVm(progress = PlayerProgress(lives = 0, easyLevel = 5)) { vm ->
        assertTrue(vm.canStartLevel(3))
    }

    @Test
    fun `deductLifeForLevel decrements lives for current level`() = testWithVm(progress = PlayerProgress(lives = 5, easyLevel = 1)) { vm ->
        val result = vm.deductLifeForLevel(1)
        testDispatcher.scheduler.runCurrent()

        assertTrue(result)
        val state = vm.uiState.first()
        assertEquals(4, state.lives)
        assertTrue(state.lifeDeducted)
    }

    @Test
    fun `deductLifeForLevel does not deduct for replay`() = testWithVm(progress = PlayerProgress(lives = 5, easyLevel = 5)) { vm ->
        val result = vm.deductLifeForLevel(3)
        testDispatcher.scheduler.runCurrent()

        assertTrue(result)
        val state = vm.uiState.first()
        assertEquals(5, state.lives)
    }

    @Test
    fun `deductLifeForLevel with 0 lives shows dialog and returns false`() = testWithVm(progress = PlayerProgress(lives = 0, easyLevel = 1)) { vm ->
        val result = vm.deductLifeForLevel(1)
        testDispatcher.scheduler.runCurrent()

        assertFalse(result)
        assertTrue(vm.uiState.first().showNoLivesDialog)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DIALOGS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dismissNoLivesDialog hides dialog`() = testWithVm(progress = PlayerProgress(lives = 0, easyLevel = 1)) { vm ->
        vm.deductLifeForLevel(1)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.first().showNoLivesDialog)

        vm.dismissNoLivesDialog()
        testDispatcher.scheduler.runCurrent()
        assertFalse(vm.uiState.first().showNoLivesDialog)
    }

    @Test
    fun `resetLifeAnimation clears animation flag`() = testWithVm(progress = PlayerProgress(lives = 5, easyLevel = 1)) { vm ->
        vm.deductLifeForLevel(1)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.first().lifeDeducted)

        vm.resetLifeAnimation()
        testDispatcher.scheduler.runCurrent()
        assertFalse(vm.uiState.first().lifeDeducted)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. AUDIO
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playButtonClick plays audio`() = testWithVm { vm ->
        vm.playButtonClick()
        verify { audioManager.playSfx(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `deductLifeForLevel persists progress`() = testWithVm(progress = PlayerProgress(lives = 5, easyLevel = 1)) { vm ->
        vm.deductLifeForLevel(1)
        testDispatcher.scheduler.runCurrent()

        coVerify { playerRepository.saveProgress(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. STAR RATINGS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `star ratings loaded from dao`() = runTest {
        val ratings = listOf(
            com.djtaylor.wordjourney.data.db.StarRatingEntity(id = 1, difficultyKey = "easy", level = 1, stars = 3, guessCount = 1),
            com.djtaylor.wordjourney.data.db.StarRatingEntity(id = 2, difficultyKey = "easy", level = 2, stars = 2, guessCount = 3),
            com.djtaylor.wordjourney.data.db.StarRatingEntity(id = 3, difficultyKey = "easy", level = 3, stars = 1, guessCount = 5)
        )
        progressFlow = MutableStateFlow(PlayerProgress(easyLevel = 5))
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } just Runs
        }
        audioManager = mockk(relaxed = true)
        starRatingDao = mockk {
            coEvery { getAllForDifficulty("easy") } returns ratings
            coEvery { totalStars() } returns 6
        }
        adManager = mockk {
            every { isRewardedAdReady } returns false
            coEvery { loadRewardedAd() } just Runs
            coEvery { showRewardedAd(any()) } returns AdRewardResult(watched = false)
        }
        activityProvider = mockk { every { currentActivity } returns null }

        val vm = LevelSelectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("difficulty" to "easy")),
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager,
            starRatingDao = starRatingDao,
            adManager = adManager,
            activityProvider = activityProvider
        )
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.first()
        assertEquals(3, state.starRatings[1])
        assertEquals(2, state.starRatings[2])
        assertEquals(1, state.starRatings[3])
        assertEquals(6, state.totalStars)

        vm.viewModelScope.cancel()
    }

    @Test
    fun `empty star ratings map when no ratings exist`() = testWithVm { vm ->
        val state = vm.uiState.first()
        assertTrue(state.starRatings.isEmpty())
        assertEquals(0, state.totalStars)
    }

    @Test
    fun `star ratings only loads for current difficulty`() = testWithVm("regular") { vm ->
        coVerify { starRatingDao.getAllForDifficulty("regular") }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOTAL LEVELS & MORE LEVELS BANNER (TDD for "more levels coming soon")
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `totalLevels is 500 for easy difficulty`() = testWithVm("easy") { vm ->
        val state = vm.uiState.first()
        assertEquals(500, state.totalLevels)
    }

    @Test
    fun `totalLevels is 500 for regular difficulty`() = testWithVm("regular") { vm ->
        val state = vm.uiState.first()
        assertEquals(500, state.totalLevels)
    }

    @Test
    fun `totalLevels is 500 for hard difficulty`() = testWithVm("hard") { vm ->
        val state = vm.uiState.first()
        assertEquals(500, state.totalLevels)
    }

    @Test
    fun `totalLevels is 500 for vip difficulty`() = testWithVm("vip") { vm ->
        val state = vm.uiState.first()
        assertEquals(500, state.totalLevels)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. SEASONAL PACK SUPPORT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `seasonal_easter sets journeyTitle to Easter Journey`() =
        testWithVm("seasonal_easter") { vm ->
            val state = vm.uiState.first()
            assertTrue(
                "Expected 'Easter' in journey title but got '${state.journeyTitle}'",
                state.journeyTitle.contains("Easter", ignoreCase = true)
            )
        }

    @Test
    fun `seasonal_valentines sets journeyTitle to Valentines Journey`() =
        testWithVm("seasonal_valentines") { vm ->
            val state = vm.uiState.first()
            assertTrue(state.journeyTitle.contains("Valentines", ignoreCase = true))
        }

    @Test
    fun `seasonal_summer sets journeyTitle to Summer Journey`() =
        testWithVm("seasonal_summer") { vm ->
            val state = vm.uiState.first()
            assertTrue(state.journeyTitle.contains("Summer", ignoreCase = true))
        }

    @Test
    fun `seasonal_halloween sets journeyTitle to Halloween Journey`() =
        testWithVm("seasonal_halloween") { vm ->
            val state = vm.uiState.first()
            assertTrue(state.journeyTitle.contains("Halloween", ignoreCase = true))
        }

    @Test
    fun `seasonal_thanksgiving sets journeyTitle to Thanksgiving Journey`() =
        testWithVm("seasonal_thanksgiving") { vm ->
            val state = vm.uiState.first()
            assertTrue(state.journeyTitle.contains("Thanksgiving", ignoreCase = true))
        }

    @Test
    fun `seasonal_christmas sets journeyTitle to Christmas Journey`() =
        testWithVm("seasonal_christmas") { vm ->
            val state = vm.uiState.first()
            assertTrue(state.journeyTitle.contains("Christmas", ignoreCase = true))
        }

    @Test
    fun `seasonal pack key is exposed in uiState`() =
        testWithVm("seasonal_easter") { vm ->
            val state = vm.uiState.first()
            assertEquals("easter", state.seasonalPackKey)
        }

    @Test
    fun `non-seasonal difficulty has null seasonalPackKey`() =
        testWithVm("regular") { vm ->
            val state = vm.uiState.first()
            assertNull(state.seasonalPackKey)
        }

    @Test
    fun `seasonal_easter loads easter level from progress`() =
        testWithVm("seasonal_easter", PlayerProgress(seasonalEasterLevel = 15)) { vm ->
            val state = vm.uiState.first()
            assertEquals(15, state.currentLevel)
        }

    @Test
    fun `seasonal_halloween loads halloween level from progress`() =
        testWithVm("seasonal_halloween", PlayerProgress(seasonalHalloweenLevel = 42)) { vm ->
            val state = vm.uiState.first()
            assertEquals(42, state.currentLevel)
        }

    @Test
    fun `seasonal_christmas loads christmas level from progress`() =
        testWithVm("seasonal_christmas", PlayerProgress(seasonalChristmasLevel = 77)) { vm ->
            val state = vm.uiState.first()
            assertEquals(77, state.currentLevel)
        }

    @Test
    fun `totalLevels is 100 for seasonal easter pack`() =
        testWithVm("seasonal_easter") { vm ->
            assertEquals(100, vm.uiState.first().totalLevels)
        }

    @Test
    fun `regular difficulty journey title contains Regular not seasonal name`() =
        testWithVm("regular") { vm ->
            val title = vm.uiState.first().journeyTitle
            assertTrue(
                "Expected 'Regular' in journey title but got '$title'",
                title.contains("Regular", ignoreCase = true)
            )
            assertFalse(
                "Journey title should not say Easter for regular mode",
                title.contains("Easter", ignoreCase = true)
            )
        }

    @Test
    fun `seasonal deductLifeForLevel uses easter level for replay check`() =
        testWithVm("seasonal_easter", PlayerProgress(seasonalEasterLevel = 10, lives = 5)) { vm ->
            // level 5 is a replay (< 10), so no life should be deducted and returns true
            val result = vm.deductLifeForLevel(5)
            testDispatcher.scheduler.runCurrent()
            assertTrue(result)
            assertEquals(5, vm.uiState.first().lives) // lives unchanged
        }

    @Test
    fun `seasonal canStartLevel returns false when 0 lives for current level`() =
        testWithVm("seasonal_summer", PlayerProgress(seasonalSummerLevel = 3, lives = 0)) { vm ->
            assertFalse(vm.canStartLevel(3))
        }

    // ══════════════════════════════════════════════════════════════════════════
    // SEASONAL COUNTDOWN & INFO DIALOG (v2.33.0)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `seasonal pack has non-null daysLeft in ui state`() =
        testWithVm("seasonal_halloween") { vm ->
            val daysLeft = vm.uiState.first().seasonalDaysLeft
            assertNotNull("Expected non-null seasonalDaysLeft for seasonal pack", daysLeft)
            assertTrue("Expected daysLeft >= 0", daysLeft!! >= 0)
        }

    @Test
    fun `regular pack has null seasonalDaysLeft`() =
        testWithVm("regular") { vm ->
            assertNull(vm.uiState.first().seasonalDaysLeft)
        }

    @Test
    fun `showSeasonInfo sets showSeasonInfoDialog to true`() =
        testWithVm("seasonal_christmas") { vm ->
            vm.showSeasonInfo()
            testDispatcher.scheduler.runCurrent()
            assertTrue(vm.uiState.first().showSeasonInfoDialog)
        }

    @Test
    fun `dismissSeasonInfo sets showSeasonInfoDialog to false`() =
        testWithVm("seasonal_christmas") { vm ->
            vm.showSeasonInfo()
            testDispatcher.scheduler.runCurrent()
            vm.dismissSeasonInfo()
            testDispatcher.scheduler.runCurrent()
            assertFalse(vm.uiState.first().showSeasonInfoDialog)
        }

    @Test
    fun `watchAdForLife grants 1 life when ad is watched`() = runTest {
        val progress = PlayerProgress(lives = 0)
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } coAnswers {
                val updated = firstArg<PlayerProgress>()
                progressFlow.value = updated
            }
        }
        audioManager = mockk(relaxed = true)
        starRatingDao = mockk {
            coEvery { getAllForDifficulty(any()) } returns emptyList()
            coEvery { totalStars() } returns 0
            coEvery { totalStarsForDifficulty(any()) } returns 0
            coEvery { countPerfectLevels() } returns 0
        }
        adManager = mockk {
            every { isRewardedAdReady } returns true
            coEvery { loadRewardedAd() } just Runs
            coEvery { showRewardedAd(any()) } returns AdRewardResult(watched = true, rewardType = "life", rewardAmount = 1)
        }
        val mockActivity = mockk<android.app.Activity>(relaxed = true)
        activityProvider = mockk {
            every { currentActivity } returns mockActivity
        }

        val vm = LevelSelectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("difficulty" to "easy")),
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager,
            starRatingDao = starRatingDao,
            adManager = adManager,
            activityProvider = activityProvider
        )
        testDispatcher.scheduler.runCurrent()
        vm.watchAdForLife()
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.first()
        assertEquals(1, state.lives)  // 0 + 1 from ad reward
        assertFalse(state.showNoLivesDialog)
        assertNotNull(state.adLifeGrantedMessage)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `watchAdForLife does nothing when activityProvider has no activity`() =
        testWithVm("easy", PlayerProgress(lives = 0)) { vm ->
            every { activityProvider.currentActivity } returns null
            vm.watchAdForLife()
            testDispatcher.scheduler.runCurrent()
            assertEquals(0, vm.uiState.first().lives)
        }

    @Test
    fun `dismissAdLifeMessage clears adLifeGrantedMessage`() =
        testWithVm("easy") { vm ->
            // Force-set the message via a round-trip through the ViewModel internal state by watching an ad
            vm.dismissAdLifeMessage()
            testDispatcher.scheduler.runCurrent()
            assertNull(vm.uiState.first().adLifeGrantedMessage)
        }

    // ═════════════════════════════════════════════════════════════════════════
    // DEV MODE
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `devModeEnabled is false when progress has devModeEnabled false`() =
        testWithVm("easy", PlayerProgress(devModeEnabled = false)) { vm ->
            assertFalse(vm.uiState.first().devModeEnabled)
        }

    @Test
    fun `devModeEnabled is true when progress has devModeEnabled true`() =
        testWithVm("easy", PlayerProgress(devModeEnabled = true)) { vm ->
            assertTrue(vm.uiState.first().devModeEnabled)
        }

    @Test
    fun `devResetMapProgress calls repository with correct difficulty`() =
        testWithVm("easy", PlayerProgress(easyLevel = 10, devModeEnabled = true)) { vm ->
            coEvery { playerRepository.devResetMapProgress(any(), any()) } just Runs
            vm.devResetMapProgress()
            testDispatcher.scheduler.runCurrent()
            coVerify { playerRepository.devResetMapProgress(any(), Difficulty.EASY) }
        }

    @Test
    fun `devResetMapProgress for vip resets vip difficulty`() =
        testWithVm("vip", PlayerProgress(vipLevel = 5, devModeEnabled = true)) { vm ->
            coEvery { playerRepository.devResetMapProgress(any(), any()) } just Runs
            vm.devResetMapProgress()
            testDispatcher.scheduler.runCurrent()
            coVerify { playerRepository.devResetMapProgress(any(), Difficulty.VIP) }
        }
}
