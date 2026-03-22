package com.djtaylor.wordjourney.ui.themedpacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.usecase.LifeRegenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemedPacksUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val isLoading: Boolean = true
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
}
