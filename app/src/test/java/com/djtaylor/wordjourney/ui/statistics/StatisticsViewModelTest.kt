package com.djtaylor.wordjourney.ui.statistics

import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
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
 * Tests for StatisticsViewModel.
 *
 * Covers:
 *  - Loading aggregated statistics
 *  - Star totals (overall, per-difficulty)
 *  - Perfect levels count
 *  - Daily challenge stats
 *  - Player progress stats
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var starRatingDao: StarRatingDao
    private lateinit var dailyChallengeRepository: DailyChallengeRepository

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
        totalStars: Int = 0,
        perfectLevels: Int = 0,
        easyStars: Int = 0,
        regularStars: Int = 0,
        hardStars: Int = 0,
        dailyWins: Int = 0,
        dailyPlayed: Int = 0
    ): StatisticsViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
        }
        starRatingDao = mockk {
            coEvery { totalStars() } returns totalStars
            coEvery { countPerfectLevels() } returns perfectLevels
            coEvery { totalStarsForDifficulty("easy") } returns easyStars
            coEvery { totalStarsForDifficulty("regular") } returns regularStars
            coEvery { totalStarsForDifficulty("hard") } returns hardStars
        }
        dailyChallengeRepository = mockk {
            coEvery { totalWins() } returns dailyWins
            coEvery { totalPlayed() } returns dailyPlayed
        }

        return StatisticsViewModel(
            playerRepository = playerRepository,
            starRatingDao = starRatingDao,
            dailyChallengeRepository = dailyChallengeRepository
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `loads and sets isLoading false`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. STAR RATINGS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `displays total stars`() = runTest {
        val vm = createViewModel(totalStars = 42)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(42, vm.uiState.first().totalStars)
    }

    @Test
    fun `displays perfect levels count`() = runTest {
        val vm = createViewModel(perfectLevels = 15)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(15, vm.uiState.first().perfectLevels)
    }

    @Test
    fun `displays per-difficulty stars`() = runTest {
        val vm = createViewModel(easyStars = 20, regularStars = 15, hardStars = 10)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(20, state.easyStars)
        assertEquals(15, state.regularStars)
        assertEquals(10, state.hardStars)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. DAILY CHALLENGE STATS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `displays daily challenge wins and played`() = runTest {
        val vm = createViewModel(dailyWins = 30, dailyPlayed = 45)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(30, state.dailyWins)
        assertEquals(45, state.dailyPlayed)
    }

    @Test
    fun `zero daily stats when no challenges played`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(0, state.dailyWins)
        assertEquals(0, state.dailyPlayed)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. PLAYER PROGRESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `displays player progress stats`() = runTest {
        val progress = PlayerProgress(
            totalCoinsEarned = 5000L,
            totalLevelsCompleted = 25,
            totalWins = 30,
            totalGuesses = 150,
            totalItemsUsed = 10,
            totalDailyChallengesCompleted = 15,
            loginStreak = 5,
            loginBestStreak = 12
        )
        val vm = createViewModel(progress = progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(5000L, state.progress.totalCoinsEarned)
        assertEquals(25, state.progress.totalLevelsCompleted)
        assertEquals(30, state.progress.totalWins)
        assertEquals(150, state.progress.totalGuesses)
        assertEquals(10, state.progress.totalItemsUsed)
        assertEquals(15, state.progress.totalDailyChallengesCompleted)
        assertEquals(5, state.progress.loginStreak)
        assertEquals(12, state.progress.loginBestStreak)
    }

    @Test
    fun `displays streak information`() = runTest {
        val progress = PlayerProgress(
            dailyChallengeStreak = 7,
            dailyChallengeBestStreak = 14,
            loginStreak = 3,
            loginBestStreak = 10
        )
        val vm = createViewModel(progress = progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(7, state.progress.dailyChallengeStreak)
        assertEquals(14, state.progress.dailyChallengeBestStreak)
        assertEquals(3, state.progress.loginStreak)
        assertEquals(10, state.progress.loginBestStreak)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. REACTIVE UPDATES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `UI updates when progress changes`() = runTest {
        val vm = createViewModel(progress = PlayerProgress(totalWins = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, vm.uiState.first().progress.totalWins)

        progressFlow.value = PlayerProgress(totalWins = 10)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(10, vm.uiState.first().progress.totalWins)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. COMBINED STATISTICS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all statistics load together correctly`() = runTest {
        val progress = PlayerProgress(
            easyLevel = 15,
            regularLevel = 8,
            hardLevel = 3,
            totalCoinsEarned = 10000L,
            totalWins = 50,
            totalGuesses = 300
        )
        val vm = createViewModel(
            progress = progress,
            totalStars = 65,
            perfectLevels = 20,
            easyStars = 35,
            regularStars = 20,
            hardStars = 10,
            dailyWins = 40,
            dailyPlayed = 50
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(65, state.totalStars)
        assertEquals(20, state.perfectLevels)
        assertEquals(35, state.easyStars)
        assertEquals(20, state.regularStars)
        assertEquals(10, state.hardStars)
        assertEquals(40, state.dailyWins)
        assertEquals(50, state.dailyPlayed)
        assertEquals(15, state.progress.easyLevel)
        assertEquals(8, state.progress.regularLevel)
        assertEquals(3, state.progress.hardLevel)
    }
}
