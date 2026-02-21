package com.djtaylor.wordjourney.ui.home

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
        audioManager = mockk(relaxed = true)

        return HomeViewModel(
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager
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
    fun `loads progress and sets isLoading false`() = testWithVm { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun `displays correct player progress`() = testWithVm(
        PlayerProgress(coins = 3000L, diamonds = 25, lives = 8, easyLevel = 5, regularLevel = 3, hardLevel = 2)
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
        PlayerProgress(easyLevel = 10)
    ) { vm ->
        assertEquals(10, vm.levelForDifficulty(Difficulty.EASY))
    }

    @Test
    fun `levelForDifficulty returns correct regular level`() = testWithVm(
        PlayerProgress(regularLevel = 7)
    ) { vm ->
        assertEquals(7, vm.levelForDifficulty(Difficulty.REGULAR))
    }

    @Test
    fun `levelForDifficulty returns correct hard level`() = testWithVm(
        PlayerProgress(hardLevel = 3)
    ) { vm ->
        assertEquals(3, vm.levelForDifficulty(Difficulty.HARD))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. LIFE REGEN
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `does not regen lives when at cap`() = testWithVm(
        PlayerProgress(lives = 10)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(10, state.progress.lives)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. AUDIO
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playButtonClick plays sound effect`() = testWithVm { vm ->
        vm.playButtonClick()
        verify { audioManager.playSfx(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. REACTIVE UPDATES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `UI updates when progress flow emits new value`() = testWithVm(
        PlayerProgress(coins = 100L)
    ) { vm ->
        assertEquals(100L, vm.uiState.first().progress.coins)

        progressFlow.value = PlayerProgress(coins = 500L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(500L, vm.uiState.first().progress.coins)
    }
}
