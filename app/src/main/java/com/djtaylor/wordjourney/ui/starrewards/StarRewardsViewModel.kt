package com.djtaylor.wordjourney.ui.starrewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.ChestReward
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.StarChest
import com.djtaylor.wordjourney.domain.model.StarChestDefinitions
import com.djtaylor.wordjourney.domain.model.availableStars
import com.djtaylor.wordjourney.domain.model.openedChestsThisMonth
import com.djtaylor.wordjourney.domain.model.toReward
import com.djtaylor.wordjourney.domain.model.withChestOpened
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StarRewardsUiState(
    val isLoading: Boolean = true,
    val availableStars: Int = 0,
    val totalStarsEarned: Int = 0,
    val isVip: Boolean = false,
    val currentMonthKey: String = "",            // YYYY-MM
    val openedChestIds: Set<String> = emptySet(),
    val regularChests: List<StarChest> = emptyList(),
    val vipChests: List<StarChest> = emptyList(),
    val openingResult: ChestOpenResult? = null,  // non-null while reward dialog is visible
    val errorMessage: String? = null
)

data class ChestOpenResult(
    val chest: StarChest,
    val reward: ChestReward
)

@HiltViewModel
class StarRewardsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarRewardsUiState())
    val uiState: StateFlow<StarRewardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { p ->
                val monthKey = currentMonthKey()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableStars = p.availableStars(),
                        totalStarsEarned = p.totalStarsEarned,
                        isVip = p.isVip,
                        currentMonthKey = monthKey,
                        openedChestIds = p.openedChestsThisMonth(monthKey),
                        regularChests = StarChestDefinitions.regularChests,
                        vipChests = StarChestDefinitions.vipChests
                    )
                }
            }
        }
    }

    fun openChest(chest: StarChest) {
        viewModelScope.launch {
            val progress = playerRepository.playerProgressFlow.first()
            val monthKey = currentMonthKey()
            val openedIds = progress.openedChestsThisMonth(monthKey)

            // Guards
            if (chest.id in openedIds) {
                _uiState.update { it.copy(errorMessage = "You've already opened this chest this month!") }
                return@launch
            }
            if (chest.isVip && !progress.isVip) {
                _uiState.update { it.copy(errorMessage = "VIP membership required.") }
                return@launch
            }
            if (progress.availableStars() < chest.starCost) {
                _uiState.update { it.copy(errorMessage = "Not enough stars! Need ${chest.starCost} ⭐") }
                return@launch
            }

            val reward = chest.toReward()
            val updated = progress
                .withChestOpened(chest.id, chest.starCost, monthKey)
                .copy(
                    coins = progress.coins + reward.coins,
                    lives = progress.lives + reward.lives,
                    addGuessItems = progress.addGuessItems + reward.addGuessItems,
                    removeLetterItems = progress.removeLetterItems + reward.removeLetterItems,
                    diamonds = progress.diamonds + reward.gems
                )
            playerRepository.saveProgress(updated)
            audioManager.playSfx(SfxSound.LEVEL_COMPLETE)

            _uiState.update {
                it.copy(openingResult = ChestOpenResult(chest = chest, reward = reward))
            }
        }
    }

    fun dismissReward() {
        _uiState.update { it.copy(openingResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun currentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    private fun currentMonthName(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun currentMonthDisplayName(): String = currentMonthName()
}
