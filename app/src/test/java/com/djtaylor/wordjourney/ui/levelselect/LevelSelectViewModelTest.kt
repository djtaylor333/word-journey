package com.djtaylor.wordjourney.ui.levelselect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
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

        return LevelSelectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("difficulty" to difficulty)),
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager
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
}
