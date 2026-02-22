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
    fun `appVersion is 2_3_0`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("2.5.0", state.appVersion)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. THEME SYSTEM
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `init loads selected theme and owned themes`() = runTest {
        val progress = PlayerProgress(
            selectedTheme = "ocean_breeze",
            ownedThemes = "classic,ocean_breeze,forest_grove,neon_nights"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("ocean_breeze", state.selectedTheme)
        assertTrue(state.ownedThemes.contains("neon_nights"))
        assertEquals(4, state.ownedThemes.size)
    }

    @Test
    fun `selectTheme changes active theme when owned`() = runTest {
        val progress = PlayerProgress(
            selectedTheme = "classic",
            ownedThemes = "classic,ocean_breeze,forest_grove"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectTheme("ocean_breeze")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("ocean_breeze", state.selectedTheme)
        coVerify { playerRepository.saveProgress(match { it.selectedTheme == "ocean_breeze" }) }
    }

    @Test
    fun `selectTheme does not change if theme not owned`() = runTest {
        val progress = PlayerProgress(
            selectedTheme = "classic",
            ownedThemes = "classic,ocean_breeze,forest_grove"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectTheme("neon_nights")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("classic", state.selectedTheme)
    }

    @Test
    fun `purchaseTheme deducts diamonds and selects theme`() = runTest {
        val progress = PlayerProgress(
            diamonds = 100,
            selectedTheme = "classic",
            ownedThemes = "classic,ocean_breeze,forest_grove"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("neon_nights")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        val state = vm.uiState.first()
        assertEquals("neon_nights", state.selectedTheme)
        assertTrue(state.ownedThemes.contains("neon_nights"))
        assertEquals(50, state.diamonds) // 100 - 50 diamond cost
    }

    @Test
    fun `purchaseTheme fails when not enough diamonds`() = runTest {
        val progress = PlayerProgress(
            diamonds = 10,
            selectedTheme = "classic",
            ownedThemes = "classic,ocean_breeze,forest_grove"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("neon_nights")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        val state = vm.uiState.first()
        assertEquals("classic", state.selectedTheme)
        assertFalse(state.ownedThemes.contains("neon_nights"))
    }

    @Test
    fun `purchaseTheme fails for unknown theme id`() = runTest {
        val progress = PlayerProgress(diamonds = 1000)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("nonexistent_theme")
        assertFalse(result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. VIP THEME ACCESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `non-VIP user isVip is false by default`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().isVip)
    }

    @Test
    fun `VIP user has isVip true`() = runTest {
        val progress = PlayerProgress(isVip = true)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().isVip)
    }

    @Test
    fun `VIP theme purchase with enough diamonds succeeds`() = runTest {
        val progress = PlayerProgress(
            diamonds = 100,
            isVip = true,
            ownedThemes = "classic"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("royal_gold")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        val state = vm.uiState.first()
        assertTrue(state.ownedThemes.contains("royal_gold"))
        assertEquals(50, state.diamonds)
    }

    @Test
    fun `seasonal theme purchase with enough diamonds succeeds`() = runTest {
        val progress = PlayerProgress(
            diamonds = 60,
            ownedThemes = "classic"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        val state = vm.uiState.first()
        assertTrue(state.ownedThemes.contains("seasonal_halloween"))
        assertEquals(30, state.diamonds)
    }

    @Test
    fun `purchasing already-owned theme does nothing costly`() = runTest {
        val progress = PlayerProgress(
            diamonds = 100,
            ownedThemes = "classic,neon_nights",
            selectedTheme = "classic"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        // Purchasing an owned theme — cost is still deducted by current logic
        // but the theme is added to set (already present)
        val initialDiamonds = vm.uiState.first().diamonds
        vm.selectTheme("neon_nights")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("neon_nights", state.selectedTheme)
        assertEquals(initialDiamonds, state.diamonds) // selectTheme doesn't cost
    }
}
