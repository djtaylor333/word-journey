package com.djtaylor.wordjourney.ui.dailychallenge

import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.db.DailyChallengeResultEntity
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
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
 * Tests for DailyChallengeViewModel.
 *
 * Covers:
 *  - Loading today's results
 *  - Streak display
 *  - Played/not-played state per word length
 *  - canPlay() logic
 *  - Lives display
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var dailyChallengeRepository: DailyChallengeRepository
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
        progress: PlayerProgress = PlayerProgress(),
        todayResults: List<DailyChallengeResultEntity> = emptyList(),
        totalWins: Int = 0,
        totalPlayed: Int = 0
    ): DailyChallengeViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } just Runs
        }
        dailyChallengeRepository = mockk {
            coEvery { getResultsForToday() } returns todayResults
            coEvery { totalWins() } returns totalWins
            coEvery { totalPlayed() } returns totalPlayed
        }
        audioManager = mockk(relaxed = true)

        return DailyChallengeViewModel(
            dailyChallengeRepository = dailyChallengeRepository,
            playerRepository = playerRepository,
            lifeRegenUseCase = LifeRegenUseCase(),
            audioManager = audioManager
        )
    }

    private fun testWithVm(
        progress: PlayerProgress = PlayerProgress(),
        todayResults: List<DailyChallengeResultEntity> = emptyList(),
        totalWins: Int = 0,
        totalPlayed: Int = 0,
        testBody: suspend TestScope.(DailyChallengeViewModel) -> Unit
    ) = runTest {
        val vm = createViewModel(progress, todayResults, totalWins, totalPlayed)
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
    fun `loads and sets isLoading false`() = testWithVm { vm ->
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun `displays correct streak from progress`() = testWithVm(
        progress = PlayerProgress(dailyChallengeStreak = 7, dailyChallengeBestStreak = 14)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(7, state.streak)
        assertEquals(14, state.bestStreak)
    }

    @Test
    fun `displays correct lives`() = testWithVm(
        progress = PlayerProgress(lives = 8)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(8, state.lives)
    }

    @Test
    fun `displays total wins and played`() = testWithVm(
        totalWins = 25,
        totalPlayed = 40
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(25, state.totalWins)
        assertEquals(40, state.totalPlayed)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. TODAY'S RESULTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `no results means all word lengths playable`() = testWithVm { vm ->
        val state = vm.uiState.first()
        assertFalse(state.played4)
        assertFalse(state.played5)
        assertFalse(state.played6)
    }

    @Test
    fun `played 4-letter shows as completed`() = testWithVm(
        todayResults = listOf(
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 4, word = "QUIZ", guessCount = 3, won = true, stars = 2)
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertTrue(state.played4)
        assertFalse(state.played5)
        assertFalse(state.played6)
    }

    @Test
    fun `all three played shows all completed`() = testWithVm(
        todayResults = listOf(
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 4, word = "QUIZ", guessCount = 3, won = true, stars = 2),
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 5, word = "CRANE", guessCount = 4, won = true, stars = 2),
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 6, word = "BRIDGE", guessCount = 5, won = true, stars = 1)
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertTrue(state.played4)
        assertTrue(state.played5)
        assertTrue(state.played6)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. CAN PLAY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `canPlay returns true when not played`() = testWithVm { vm ->
        assertTrue(vm.canPlay(4))
        assertTrue(vm.canPlay(5))
        assertTrue(vm.canPlay(6))
    }

    @Test
    fun `canPlay returns false when already played`() = testWithVm(
        todayResults = listOf(
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 5, word = "CRANE", guessCount = 3, won = true, stars = 3)
        )
    ) { vm ->
        assertTrue(vm.canPlay(4))
        assertFalse(vm.canPlay(5))
        assertTrue(vm.canPlay(6))
    }

    @Test
    fun `canPlay returns false for invalid word length`() = testWithVm { vm ->
        assertFalse(vm.canPlay(3))
        assertFalse(vm.canPlay(7))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. AUDIO
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `playButtonClick plays sound`() = testWithVm { vm ->
        vm.playButtonClick()
        verify { audioManager.playSfx(any()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. REACTIVE UPDATES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `UI updates when progress flow emits`() = testWithVm(
        progress = PlayerProgress(dailyChallengeStreak = 3)
    ) { vm ->
        assertEquals(3, vm.uiState.first().streak)

        progressFlow.value = PlayerProgress(dailyChallengeStreak = 5, dailyChallengeBestStreak = 10)
        testDispatcher.scheduler.runCurrent()

        assertEquals(5, vm.uiState.first().streak)
        assertEquals(10, vm.uiState.first().bestStreak)
    }

    @Test
    fun `loss result shows as played`() = testWithVm(
        todayResults = listOf(
            DailyChallengeResultEntity(date = "2026-02-21", wordLength = 6, word = "BRIDGE", guessCount = 6, won = false, stars = 0)
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertTrue(state.played6)
        assertFalse(vm.canPlay(6))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. PER-LENGTH STREAKS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `per-length streaks loaded from progress`() = testWithVm(
        progress = PlayerProgress(
            dailyStreak4 = 3, dailyStreak5 = 1, dailyStreak6 = 5,
            dailyBestStreak4 = 7, dailyBestStreak5 = 2, dailyBestStreak6 = 8
        )
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(3, state.streak4)
        assertEquals(1, state.streak5)
        assertEquals(5, state.streak6)
        assertEquals(7, state.bestStreak4)
        assertEquals(2, state.bestStreak5)
        assertEquals(8, state.bestStreak6)
    }

    @Test
    fun `per-length streaks default to zero`() = testWithVm { vm ->
        val state = vm.uiState.first()
        assertEquals(0, state.streak4)
        assertEquals(0, state.streak5)
        assertEquals(0, state.streak6)
    }

    @Test
    fun `wins per length loaded from progress`() = testWithVm(
        progress = PlayerProgress(dailyWins4 = 10, dailyWins5 = 5, dailyWins6 = 2)
    ) { vm ->
        val state = vm.uiState.first()
        assertEquals(10, state.wins4)
        assertEquals(5, state.wins5)
        assertEquals(2, state.wins6)
    }
}
