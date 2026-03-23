package com.djtaylor.wordjourney.ui.themedpacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.seasonalFirstOpenFor
import com.djtaylor.wordjourney.domain.model.withSeasonalFirstOpen
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemedPacksUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val isLoading: Boolean = true,
    /** Season key of the pack showing the intro dialog (null = no intro shown). */
    val introPackKey: String? = null
)

@HiltViewModel
class ThemedPacksViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val lifeRegenUseCase: LifeRegenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemedPacksUiState())
    val uiState: StateFlow<ThemedPacksUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                val regen = lifeRegenUseCase(progress.lives, progress.lastLifeRegenTimestamp)
                val updated = if (regen.livesAdded > 0) {
                    val p = progress.copy(
                        lives = regen.updatedLives,
                        lastLifeRegenTimestamp = regen.updatedTimestamp
                    )
                    playerRepository.saveProgress(p)
                    p
                } else progress
                _uiState.update { it.copy(progress = updated, isLoading = false) }
            }
        }
    }

    /**
     * Call when the player taps a pack card.
     * If the pack has never been opened, records the timestamp and triggers the intro dialog.
     * Returns true if the intro dialog should be shown (first time), false otherwise.
     */
    fun onPackOpened(seasonKey: String): Boolean {
        val progress = _uiState.value.progress
        val firstOpen = progress.seasonalFirstOpenFor(seasonKey)
        if (firstOpen == 0L) {
            // Record first-open timestamp
            viewModelScope.launch {
                val updated = progress.withSeasonalFirstOpen(seasonKey, System.currentTimeMillis())
                playerRepository.saveProgress(updated)
            }
            _uiState.update { it.copy(introPackKey = seasonKey) }
            return true
        }
        return false
    }

    fun dismissIntroDialog() {
        _uiState.update { it.copy(introPackKey = null) }
    }
}
