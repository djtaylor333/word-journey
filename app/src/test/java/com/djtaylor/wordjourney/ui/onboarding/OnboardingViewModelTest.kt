package com.djtaylor.wordjourney.ui.onboarding

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
 * Tests for OnboardingViewModel — the tutorial/level-0 experience.
 * TDD: Tests written first, then ViewModel implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository

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
    ): OnboardingViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } coAnswers {
                progressFlow.value = firstArg()
            }
        }
        return OnboardingViewModel(playerRepository = playerRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `initial state shows first tutorial step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(0, state.currentStep)
        assertFalse(state.isCompleted)
        assertNotNull(state.dialogText)
    }

    @Test
    fun `initial onboarding has multiple steps`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().totalSteps > 1)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. STEP NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `nextStep advances to next tutorial step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.first().currentStep)
    }

    @Test
    fun `previousStep goes back one step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.previousStep()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.first().currentStep)
    }

    @Test
    fun `previousStep does nothing on first step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.previousStep()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.first().currentStep)
    }

    @Test
    fun `each step has different dialog text`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val text0 = vm.uiState.first().dialogText
        vm.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()
        val text1 = vm.uiState.first().dialogText

        assertNotEquals(text0, text1)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. COMPLETION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `completing final step marks onboarding as done`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val totalSteps = vm.uiState.first().totalSteps
        // Advance to last step
        repeat(totalSteps - 1) {
            vm.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        vm.completeOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().isCompleted)
    }

    @Test
    fun `completing onboarding saves hasCompletedOnboarding flag`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.completeOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.hasCompletedOnboarding }) }
    }

    @Test
    fun `skip onboarding immediately completes`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.skipOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().isCompleted)
        coVerify { playerRepository.saveProgress(match { it.hasCompletedOnboarding }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. TUTORIAL CONTENT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `tutorial includes step about guessing words`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val allTexts = mutableListOf<String>()
        val total = vm.uiState.first().totalSteps
        allTexts.add(vm.uiState.first().dialogText ?: "")
        repeat(total - 1) {
            vm.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()
            allTexts.add(vm.uiState.first().dialogText ?: "")
        }

        assertTrue(allTexts.any { it.contains("guess", ignoreCase = true) || it.contains("word", ignoreCase = true) })
    }

    @Test
    fun `tutorial includes step about tile colors`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val allTexts = mutableListOf<String>()
        val total = vm.uiState.first().totalSteps
        allTexts.add(vm.uiState.first().dialogText ?: "")
        repeat(total - 1) {
            vm.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()
            allTexts.add(vm.uiState.first().dialogText ?: "")
        }

        assertTrue(allTexts.any {
            it.contains("green", ignoreCase = true) || it.contains("correct", ignoreCase = true)
        })
    }

    @Test
    fun `last step reveals congratulations or completion message`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val total = vm.uiState.first().totalSteps
        repeat(total - 1) {
            vm.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val lastText = vm.uiState.first().dialogText ?: ""
        assertTrue(lastText.contains("ready", ignoreCase = true) || lastText.contains("adventure", ignoreCase = true))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. STEP INDICATORS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `canGoBack is false on first step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().canGoBack)
    }

    @Test
    fun `canGoBack is true after advancing`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.nextStep()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().canGoBack)
    }

    @Test
    fun `isLastStep true on final step`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val total = vm.uiState.first().totalSteps
        repeat(total - 1) {
            vm.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertTrue(vm.uiState.first().isLastStep)
    }
}
