package com.djtaylor.wordjourney.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val sfxVolume: Float = 0.8f,
    val notifyLivesFull: Boolean = true,
    val highContrast: Boolean = false,
    val darkMode: Boolean = true,
    val playGamesSignedIn: Boolean = false,
    val playerDisplayName: String? = null,
    val appVersion: String = "1.8.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerRepository: PlayerRepository,
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
                        musicEnabled      = progress.musicEnabled,
                        musicVolume       = progress.musicVolume,
                        sfxEnabled        = progress.sfxEnabled,
                        sfxVolume         = progress.sfxVolume,
                        notifyLivesFull   = progress.notifyLivesFull,
                        highContrast      = progress.highContrast,
                        darkMode          = progress.darkMode,
                        playGamesSignedIn = progress.playGamesSignedIn
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

    fun setHighContrast(enabled: Boolean) = saveField { it.copy(highContrast = enabled) }
    fun setDarkMode(enabled: Boolean) = saveField { it.copy(darkMode = enabled) }

    private fun saveField(block: (PlayerProgress) -> PlayerProgress) {
        viewModelScope.launch {
            val updated = block(latestProgress)
            latestProgress = updated
            playerRepository.saveProgress(updated)
            _uiState.update { s ->
                s.copy(
                    musicEnabled    = updated.musicEnabled,
                    musicVolume     = updated.musicVolume,
                    sfxEnabled      = updated.sfxEnabled,
                    sfxVolume       = updated.sfxVolume,
                    notifyLivesFull = updated.notifyLivesFull,
                    highContrast    = updated.highContrast,
                    darkMode        = updated.darkMode
                )
            }
        }
    }
}
