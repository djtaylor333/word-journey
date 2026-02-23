package com.djtaylor.wordjourney.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.GameTheme
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SeasonalThemeManager
import com.djtaylor.wordjourney.domain.model.ThemeCategory
import com.djtaylor.wordjourney.domain.model.ThemeRegistry
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val notifyLivesFull: Boolean = true,
    val notifyDailyChallenge: Boolean = true,
    val highContrast: Boolean = false,
    val darkMode: Boolean = true,
    val colorblindMode: String = "none",
    val textScaleFactor: Float = 1.0f,
    val playGamesSignedIn: Boolean = false,
    val playerDisplayName: String? = null,
    val appVersion: String = "2.15.0",
    val selectedTheme: String = "classic",
    val ownedThemes: Set<String> = setOf("classic", "ocean_breeze", "forest_grove"),
    val diamonds: Int = 0,
    val isVip: Boolean = false,
    val devModeEnabled: Boolean = false   // unlocked via secret tap easter egg
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerRepository: PlayerRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var latestProgress: PlayerProgress = PlayerProgress()

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                latestProgress = progress
                _uiState.update { s ->
                    s.copy(
                        musicEnabled           = progress.musicEnabled,
                        musicVolume            = progress.musicVolume,
                        sfxEnabled             = progress.sfxEnabled,
                        sfxVolume              = progress.sfxVolume,
                        notifyLivesFull        = progress.notifyLivesFull,
                        notifyDailyChallenge   = progress.notifyDailyChallenge,
                        highContrast           = progress.highContrast,
                        darkMode               = progress.darkMode,
                        colorblindMode         = progress.colorblindMode,
                        textScaleFactor        = progress.textScaleFactor,
                        playGamesSignedIn      = progress.playGamesSignedIn,
                        selectedTheme          = progress.selectedTheme,
                        ownedThemes            = progress.ownedThemes.split(",").filter { it.isNotBlank() }.toSet(),
                        diamonds               = progress.diamonds,
                        isVip                  = progress.isVip,
                        devModeEnabled         = progress.devModeEnabled
                    )
                }
                // Sync audio manager with saved settings
                audioManager.updateSettings(
                    com.djtaylor.wordjourney.audio.AudioSettings(
                        musicEnabled = progress.musicEnabled,
                        musicVolume = progress.musicVolume,
                        sfxEnabled = progress.sfxEnabled,
                        sfxVolume = progress.sfxVolume
                    )
                )
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        audioManager.setMusicEnabled(enabled)
        saveField { it.copy(musicEnabled = enabled) }
    }

    fun setMusicVolume(volume: Float) {
        audioManager.setMusicVolume(volume)
        saveField { it.copy(musicVolume = volume) }
    }

    fun setSfxEnabled(enabled: Boolean) {
        audioManager.setSfxEnabled(enabled)
        saveField { it.copy(sfxEnabled = enabled) }
    }

    fun setSfxVolume(volume: Float) {
        audioManager.setSfxVolume(volume)
        saveField { it.copy(sfxVolume = volume) }
    }

    fun setNotifyLivesFull(enabled: Boolean) {
        saveField { it.copy(notifyLivesFull = enabled) }
        if (enabled) {
            LivesFullNotificationWorker.schedule(
                context,
                latestProgress.lives,
                latestProgress.lastLifeRegenTimestamp,
                notificationsEnabled = true
            )
        } else {
            androidx.work.WorkManager.getInstance(context)
                .cancelAllWorkByTag(LivesFullNotificationWorker.WORK_TAG)
        }
    }

    fun setNotifyDailyChallenge(enabled: Boolean) {
        saveField { it.copy(notifyDailyChallenge = enabled) }
        try {
            com.djtaylor.wordjourney.notifications.DailyChallengeReminderWorker.schedule(
                context, notificationsEnabled = enabled
            )
        } catch (_: IllegalStateException) {
            // WorkManager not yet initialized (e.g. during unit tests) — safe to ignore
        }
    }

    fun setHighContrast(enabled: Boolean) = saveField { it.copy(highContrast = enabled) }
    fun setDarkMode(enabled: Boolean) = saveField { it.copy(darkMode = enabled) }
    fun setColorblindMode(mode: String) = saveField { it.copy(colorblindMode = mode) }
    fun setTextScaleFactor(factor: Float) = saveField { it.copy(textScaleFactor = factor.coerceIn(0.8f, 1.5f)) }

    fun selectTheme(themeId: String) {
        val owned = _uiState.value.ownedThemes
        if (themeId in owned) {
            saveField { it.copy(selectedTheme = themeId) }
        }
    }

    fun purchaseTheme(themeId: String, date: LocalDate = LocalDate.now()): Boolean {
        val theme = ThemeRegistry.getThemeById(themeId) ?: return false
        // Enforce seasonal theme lock rules
        if (theme.category == ThemeCategory.SEASONAL) {
            val lockInfo = SeasonalThemeManager.getThemeLockInfo(themeId, date)
            when (lockInfo.status) {
                SeasonalThemeManager.SeasonalLockStatus.FUTURE -> return false // not yet available
                SeasonalThemeManager.SeasonalLockStatus.PAST   ->
                    if (!latestProgress.isVip) return false // VIP-only for past events
                SeasonalThemeManager.SeasonalLockStatus.ACTIVE -> { /* anyone can buy */ }
            }
        }
        val cost = theme.diamondCost
        if (latestProgress.diamonds < cost) return false
        val newOwned = _uiState.value.ownedThemes + themeId
        saveField {
            it.copy(
                diamonds = it.diamonds - cost,
                ownedThemes = newOwned.joinToString(","),
                selectedTheme = themeId
            )
        }
        return true
    }

    /**
     * Cancel VIP subscription. Sets isVip = false and switches selected theme to Classic
     * if the currently selected theme requires VIP.
     */
    fun cancelVip() {
        saveField { progress ->
            val currentThemeId = progress.selectedTheme
            val theme = ThemeRegistry.getThemeById(currentThemeId)
            val newTheme = if (theme?.category == ThemeCategory.VIP) {
                "classic"
            } else {
                currentThemeId
            }
            progress.copy(isVip = false, selectedTheme = newTheme)
        }
    }

    // ── Dev Mode ─────────────────────────────────────────────────────────────

    fun setDevModeEnabled(enabled: Boolean) {
        saveField { it.copy(devModeEnabled = enabled) }
    }

    /** [DEV] Clears all daily challenge progress so they appear undone today. */
    fun devResetDailyChallenges() {
        viewModelScope.launch {
            playerRepository.devResetDailyChallenges(latestProgress)
            dailyChallengeRepository.devClearTodayResults() // also wipe Room DB entries
        }
    }

    /** [DEV] Zeroes out all cumulative statistics. */
    fun devResetStatistics() {
        viewModelScope.launch {
            playerRepository.devResetStatistics(latestProgress)
        }
    }

    /** [DEV] Resets adventure mode level progress back to level 1 and clears in-progress saves. */
    fun devResetLevelProgress() {
        viewModelScope.launch {
            playerRepository.devResetLevelProgress(latestProgress)
        }
    }

    private fun saveField(block: (PlayerProgress) -> PlayerProgress) {
        viewModelScope.launch {
            val updated = block(latestProgress)
            latestProgress = updated
            playerRepository.saveProgress(updated)
            _uiState.update { s ->
                s.copy(
                    musicEnabled           = updated.musicEnabled,
                    musicVolume            = updated.musicVolume,
                    sfxEnabled             = updated.sfxEnabled,
                    sfxVolume              = updated.sfxVolume,
                    notifyLivesFull        = updated.notifyLivesFull,
                    notifyDailyChallenge   = updated.notifyDailyChallenge,
                    highContrast           = updated.highContrast,
                    darkMode               = updated.darkMode,
                    colorblindMode         = updated.colorblindMode,
                    textScaleFactor        = updated.textScaleFactor,
                    selectedTheme          = updated.selectedTheme,
                    ownedThemes            = updated.ownedThemes.split(",").filter { it.isNotBlank() }.toSet(),
                    diamonds               = updated.diamonds,
                    isVip                  = updated.isVip,
                    devModeEnabled         = updated.devModeEnabled
                )
            }
        }
    }
}
