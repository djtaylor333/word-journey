package com.djtaylor.wordjourney.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.billing.IBillingManager
import com.djtaylor.wordjourney.billing.ProductIds
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val isPurchasing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val billingManager: IBillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
    }

    fun purchase(productId: String) {
        _uiState.update { it.copy(isPurchasing = true) }
        viewModelScope.launch {
            billingManager.purchase(productId) { result ->
                if (result.success) {
                    viewModelScope.launch {
                        val current = _uiState.value.progress
                        val updated = current.copy(
                            coins    = current.coins + result.coinsGranted,
                            diamonds = current.diamonds + result.diamondsGranted,
                            lives    = current.lives + result.livesGranted
                        )
                        playerRepository.saveProgress(updated)
                        _uiState.update { it.copy(
                            isPurchasing = false,
                            message = buildGrantMessage(result.coinsGranted, result.diamondsGranted, result.livesGranted)
                        )}
                    }
                } else {
                    _uiState.update { it.copy(isPurchasing = false, message = "Purchase failed. Please try again.") }
                }
            }
        }
    }

    fun tradeCoinsForLife() {
        val progress = _uiState.value.progress
        if (progress.coins < 1000) {
            _uiState.update { it.copy(message = "You need 1000 coins.") }
            return
        }
        viewModelScope.launch {
            val updated = progress.copy(coins = progress.coins - 1000, lives = progress.lives + 1)
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ Traded 1000 coins for 1 life!") }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun getPriceLabel(productId: String) = billingManager.getPriceLabel(productId)

    private fun buildGrantMessage(coins: Long, diamonds: Int, lives: Int): String {
        val parts = mutableListOf<String>()
        if (coins > 0) parts.add("+$coins coins")
        if (diamonds > 0) parts.add("+$diamonds diamonds")
        if (lives > 0) parts.add("+$lives lives")
        return "✅ ${parts.joinToString(", ")} added!"
    }
}
