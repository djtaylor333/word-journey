package com.djtaylor.wordjourney.ui.settings

import android.content.Context
import com.djtaylor.wordjourney.audio.AudioSettings
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
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
 * Tests for SettingsViewModel — including new accessibility features:
 * colorblind mode and text scale factor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var audioManager: WordJourneysAudioManager
    private lateinit var context: Context

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
    ): SettingsViewModel {
        progressFlow = MutableStateFlow(progress)
        playerRepository = mockk {
            every { playerProgressFlow } returns progressFlow
            coEvery { saveProgress(any()) } coAnswers {
                progressFlow.value = firstArg()
            }
        }
        audioManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        return SettingsViewModel(
            context = context,
            playerRepository = playerRepository,
            audioManager = audioManager
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `loads settings on init`() = runTest {
        val progress = PlayerProgress(
            musicEnabled = false,
            sfxEnabled = true,
            highContrast = true,
            darkMode = false,
            colorblindMode = "protanopia",
            textScaleFactor = 1.2f
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.musicEnabled)
        assertTrue(state.sfxEnabled)
        assertTrue(state.highContrast)
        assertFalse(state.darkMode)
        assertEquals("protanopia", state.colorblindMode)
        assertEquals(1.2f, state.textScaleFactor, 0.001f)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. COLORBLIND MODE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `setColorblindMode updates state and saves`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setColorblindMode("deuteranopia")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("deuteranopia", state.colorblindMode)
        coVerify { playerRepository.saveProgress(match { it.colorblindMode == "deuteranopia" }) }
    }

    @Test
    fun `setColorblindMode to none disables colorblind mode`() = runTest {
        val vm = createViewModel(PlayerProgress(colorblindMode = "protanopia"))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setColorblindMode("none")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("none", state.colorblindMode)
    }

    @Test
    fun `setColorblindMode tritanopia saves correctly`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setColorblindMode("tritanopia")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.colorblindMode == "tritanopia" }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. TEXT SCALE FACTOR
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `setTextScaleFactor updates state and saves`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setTextScaleFactor(1.3f)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(1.3f, state.textScaleFactor, 0.001f)
        coVerify { playerRepository.saveProgress(match { it.textScaleFactor == 1.3f }) }
    }

    @Test
    fun `setTextScaleFactor clamps at minimum 0_8`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setTextScaleFactor(0.5f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.textScaleFactor == 0.8f }) }
    }

    @Test
    fun `setTextScaleFactor clamps at maximum 1_5`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setTextScaleFactor(2.0f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.textScaleFactor == 1.5f }) }
    }

    @Test
    fun `setTextScaleFactor at 1_0 is default`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setTextScaleFactor(1.0f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { it.textScaleFactor == 1.0f }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. EXISTING SETTINGS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `setHighContrast enables high contrast`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setHighContrast(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.highContrast)
        coVerify { playerRepository.saveProgress(match { it.highContrast }) }
    }

    @Test
    fun `setDarkMode updates state`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setDarkMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.darkMode)
    }

    @Test
    fun `setMusicEnabled updates audio manager`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setMusicEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { audioManager.setMusicEnabled(false) }
    }

    @Test
    fun `setSfxVolume updates audio manager`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setSfxVolume(0.5f)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { audioManager.setSfxVolume(0.5f) }
    }

    @Test
    fun `appVersion is 2_1_0`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("2.1.0", state.appVersion)
    }
}
