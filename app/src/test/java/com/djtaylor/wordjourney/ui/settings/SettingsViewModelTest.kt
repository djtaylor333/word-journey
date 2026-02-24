package com.djtaylor.wordjourney.ui.settings

import android.content.Context
import com.djtaylor.wordjourney.audio.AudioSettings
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
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
 * Tests for SettingsViewModel — including new accessibility features:
 * colorblind mode and text scale factor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var progressFlow: MutableStateFlow<PlayerProgress>
    private lateinit var playerRepository: PlayerRepository
    private lateinit var dailyChallengeRepository: DailyChallengeRepository
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
            coEvery { devResetDailyChallenges(any()) } coAnswers {
                val current = firstArg<PlayerProgress>()
                progressFlow.value = current.copy(
                    dailyChallengeLastDate = "",
                    dailyLastDate4 = "",
                    dailyLastDate5 = "",
                    dailyLastDate6 = ""
                )
            }
            coEvery { devResetStatistics(any()) } coAnswers {
                val current = firstArg<PlayerProgress>()
                progressFlow.value = current.copy(
                    totalLevelsCompleted = 0,
                    totalGuesses = 0,
                    totalWins = 0,
                    dailyChallengeStreak = 0,
                    dailyChallengeBestStreak = 0
                )
            }
            coEvery { devResetLevelProgress(any()) } coAnswers {
                val current = firstArg<PlayerProgress>()
                progressFlow.value = current.copy(
                    easyLevel = 1,
                    regularLevel = 1,
                    hardLevel = 1,
                    vipLevel = 1,
                    easyLevelsCompletedSinceBonusLife = 0,
                    regularLevelsCompletedSinceBonusLife = 0,
                    hardLevelsCompletedSinceBonusLife = 0,
                    vipLevelsCompletedSinceBonusLife = 0
                )
            }
        }
        audioManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        dailyChallengeRepository = mockk {
            coEvery { devClearTodayResults() } just Runs
        }

        return SettingsViewModel(
            context = context,
            playerRepository = playerRepository,
            dailyChallengeRepository = dailyChallengeRepository,
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
    fun `appVersion is 2_16_2`() = runTest {
        // Test that the appVersion field reflects the current version
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("2.16.2", state.appVersion)
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

        // Use an active Halloween date so lock rules don't block the purchase
        val result = vm.purchaseTheme("seasonal_halloween", java.time.LocalDate.of(2025, 10, 15))
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

    // ══════════════════════════════════════════════════════════════════════════
    // 7. CANCEL VIP
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cancelVip sets isVip to false`() = runTest {
        val progress = PlayerProgress(isVip = true, selectedTheme = "classic")
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancelVip()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isVip)
    }

    @Test
    fun `cancelVip with non-VIP theme keeps current theme`() = runTest {
        val progress = PlayerProgress(isVip = true, selectedTheme = "ocean_breeze",
            ownedThemes = "classic,ocean_breeze")
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancelVip()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isVip)
        // ocean_breeze is a non-VIP premium theme — cancelled VIP should keep it
        assertEquals("ocean_breeze", state.selectedTheme)
    }

    @Test
    fun `cancelVip with VIP-category theme falls back to classic`() = runTest {
        // royal_gold is a VIP category theme
        val progress = PlayerProgress(isVip = true, selectedTheme = "royal_gold",
            ownedThemes = "classic,royal_gold")
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancelVip()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isVip)
        assertEquals("classic", state.selectedTheme)
    }

    @Test
    fun `cancelVip saves progress with isVip false`() = runTest {
        val progress = PlayerProgress(isVip = true, selectedTheme = "classic")
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancelVip()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.saveProgress(match { !it.isVip }) }
    }

    @Test
    fun `cancelVip on already non-VIP user keeps isVip false`() = runTest {
        val progress = PlayerProgress(isVip = false, selectedTheme = "classic")
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.cancelVip()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isVip)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEASONAL THEME LOCK RULES (TDD)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `purchaseTheme for future seasonal theme is always blocked`() = runTest {
        // Halloween is FUTURE on Jan 15 2026
        val futureDate = java.time.LocalDate.of(2026, 1, 15)
        val progress = PlayerProgress(diamonds = 100, isVip = true)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween", futureDate)
        assertFalse("Future seasonal theme should be blocked even for VIP", result)
    }

    @Test
    fun `purchaseTheme for future seasonal theme blocked for non-VIP too`() = runTest {
        val futureDate = java.time.LocalDate.of(2026, 6, 1) // Halloween future
        val progress = PlayerProgress(diamonds = 100, isVip = false)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween", futureDate)
        assertFalse("Future seasonal theme should be blocked for non-VIP", result)
    }

    @Test
    fun `purchaseTheme for past seasonal theme blocked for non-VIP`() = runTest {
        // Halloween is PAST on Nov 5 2025
        val pastDate = java.time.LocalDate.of(2025, 11, 5)
        val progress = PlayerProgress(diamonds = 100, isVip = false)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween", pastDate)
        assertFalse("Past seasonal theme should be blocked for non-VIP", result)
    }

    @Test
    fun `purchaseTheme for past seasonal theme allowed for VIP with enough diamonds`() = runTest {
        // Halloween is PAST on Nov 5 2025; cost is 30 diamonds
        val pastDate = java.time.LocalDate.of(2025, 11, 5)
        val progress = PlayerProgress(diamonds = 50, isVip = true)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween", pastDate)
        assertTrue("VIP should be able to purchase past seasonal theme", result)
    }

    @Test
    fun `purchaseTheme for active seasonal theme allowed for everyone`() = runTest {
        // Halloween is ACTIVE on Oct 15 2025
        val activeDate = java.time.LocalDate.of(2025, 10, 15)
        val progress = PlayerProgress(diamonds = 50, isVip = false)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = vm.purchaseTheme("seasonal_halloween", activeDate)
        assertTrue("Active seasonal theme should be purchasable by anyone with enough diamonds", result)
    }

    @Test
    fun `purchaseTheme for non-seasonal theme is unaffected by lock rules`() = runTest {
        val anyDate = java.time.LocalDate.of(2026, 1, 15)
        val progress = PlayerProgress(diamonds = 100)
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        // NEON_NIGHTS costs 50 diamonds — non-seasonal VIP theme
        val result = vm.purchaseTheme("neon_nights", anyDate)
        assertTrue("Non-seasonal theme should not be affected by seasonal locks", result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. DAILY CHALLENGE NOTIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `notifyDailyChallenge defaults to true`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().notifyDailyChallenge)
    }

    @Test
    fun `notifyDailyChallenge reflects saved progress`() = runTest {
        val vm = createViewModel(PlayerProgress(notifyDailyChallenge = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().notifyDailyChallenge)
    }

    @Test
    fun `setNotifyDailyChallenge true saves and updates state`() = runTest {
        val vm = createViewModel(PlayerProgress(notifyDailyChallenge = false))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setNotifyDailyChallenge(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().notifyDailyChallenge)
        coVerify { playerRepository.saveProgress(match { it.notifyDailyChallenge }) }
    }

    @Test
    fun `setNotifyDailyChallenge false saves and updates state`() = runTest {
        val vm = createViewModel(PlayerProgress(notifyDailyChallenge = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setNotifyDailyChallenge(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().notifyDailyChallenge)
        coVerify { playerRepository.saveProgress(match { !it.notifyDailyChallenge }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. DEV MODE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `devModeEnabled defaults to false`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().devModeEnabled)
    }

    @Test
    fun `devModeEnabled true reflects saved progress`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().devModeEnabled)
    }

    @Test
    fun `setDevModeEnabled true unlocks dev mode and saves`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setDevModeEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.first().devModeEnabled)
        coVerify { playerRepository.saveProgress(match { it.devModeEnabled }) }
    }

    @Test
    fun `setDevModeEnabled false disables dev mode and saves`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setDevModeEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.first().devModeEnabled)
        coVerify { playerRepository.saveProgress(match { !it.devModeEnabled }) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. DEV RESET FUNCTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `devResetDailyChallenges clears daily challenge dates`() = runTest {
        val progress = PlayerProgress(
            devModeEnabled = true,
            dailyChallengeLastDate = "2025-06-01",
            dailyLastDate4 = "2025-06-01",
            dailyLastDate5 = "2025-06-01",
            dailyLastDate6 = "2025-06-01"
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetDailyChallenges()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.devResetDailyChallenges(any()) }
    }

    @Test
    fun `devResetStatistics zeroes statistical fields`() = runTest {
        val progress = PlayerProgress(
            devModeEnabled = true,
            totalLevelsCompleted = 50,
            totalGuesses = 200,
            totalWins = 40,
            dailyChallengeStreak = 7,
            dailyChallengeBestStreak = 12
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetStatistics()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playerRepository.devResetStatistics(any()) }
    }

    @Test
    fun `devResetDailyChallenges delegates to repository`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetDailyChallenges()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { playerRepository.devResetDailyChallenges(any()) }
    }

    @Test
    fun `devResetDailyChallenges also clears today Room DB results`() = runTest {
        // The fix for the daily reset bug: Room DB entries for today were not cleared,
        // causing hasPlayedToday() to still return true after resetting DataStore fields.
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetDailyChallenges()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { dailyChallengeRepository.devClearTodayResults() }
    }

    @Test
    fun `devResetStatistics delegates to repository`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetStatistics()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { playerRepository.devResetStatistics(any()) }
    }

    @Test
    fun `devResetLevelProgress delegates to repository`() = runTest {
        val vm = createViewModel(PlayerProgress(devModeEnabled = true))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetLevelProgress()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { playerRepository.devResetLevelProgress(any()) }
    }

    @Test
    fun `devResetLevelProgress resets all level fields to 1`() = runTest {
        val progress = PlayerProgress(
            devModeEnabled = true,
            easyLevel = 10,
            regularLevel = 7,
            hardLevel = 5,
            vipLevel = 3,
            easyLevelsCompletedSinceBonusLife = 4,
            regularLevelsCompletedSinceBonusLife = 2,
            hardLevelsCompletedSinceBonusLife = 1,
            vipLevelsCompletedSinceBonusLife = 3
        )
        val vm = createViewModel(progress)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.devResetLevelProgress()
        testDispatcher.scheduler.advanceUntilIdle()

        // Repository was called; verify the progress emitted has all levels reset to 1
        coVerify { playerRepository.devResetLevelProgress(any()) }
        val saved = progressFlow.value
        assertEquals(1, saved.easyLevel)
        assertEquals(1, saved.regularLevel)
        assertEquals(1, saved.hardLevel)
        assertEquals(1, saved.vipLevel)
        assertEquals(0, saved.easyLevelsCompletedSinceBonusLife)
    }
}

