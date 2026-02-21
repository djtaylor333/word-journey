package com.djtaylor.wordjourney.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.audio.SfxSound
import com.djtaylor.wordjourney.audio.WordJourneysAudioManager
import com.djtaylor.wordjourney.billing.IAdManager
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
    val message: String? = null,
    val isAdReady: Boolean = false,
    val isWatchingAd: Boolean = false
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val billingManager: IBillingManager,
    private val adManager: IAdManager,
    private val audioManager: WordJourneysAudioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                _uiState.update { it.copy(progress = progress, isAdReady = adManager.isRewardedAdReady) }
            }
        }
        // Pre-fetch a rewarded ad
        viewModelScope.launch { adManager.loadRewardedAd() }
    }

    fun purchase(productId: String) {
        _uiState.update { it.copy(isPurchasing = true) }
        viewModelScope.launch {
            billingManager.purchase(productId) { result ->
                if (result.success) {
                    viewModelScope.launch {
                        val current = _uiState.value.progress
                        var updated = current.copy(
                            coins    = current.coins + result.coinsGranted,
                            diamonds = current.diamonds + result.diamondsGranted,
                            lives    = current.lives + result.livesGranted
                        )
                        // Handle bundle item grants
                        updated = when (productId) {
                            ProductIds.STARTER_BUNDLE -> updated.copy(
                                addGuessItems = updated.addGuessItems + 5,
                                removeLetterItems = updated.removeLetterItems + 5,
                                definitionItems = updated.definitionItems + 5,
                                showLetterItems = updated.showLetterItems + 5
                            )
                            ProductIds.ADVENTURER_BUNDLE -> updated.copy(
                                addGuessItems = updated.addGuessItems + 10,
                                removeLetterItems = updated.removeLetterItems + 10,
                                definitionItems = updated.definitionItems + 10,
                                showLetterItems = updated.showLetterItems + 10
                            )
                            ProductIds.CHAMPION_BUNDLE -> updated.copy(
                                addGuessItems = updated.addGuessItems + 25,
                                removeLetterItems = updated.removeLetterItems + 25,
                                definitionItems = updated.definitionItems + 25,
                                showLetterItems = updated.showLetterItems + 25
                            )
                            ProductIds.VIP_MONTHLY, ProductIds.VIP_YEARLY -> updated.copy(
                                isVip = true,
                                vipExpiryTimestamp = System.currentTimeMillis() +
                                    if (productId == ProductIds.VIP_YEARLY) 365L * 24 * 60 * 60 * 1000
                                    else 30L * 24 * 60 * 60 * 1000
                            )
                            else -> updated
                        }
                        playerRepository.saveProgress(updated)
                        audioManager.playSfx(SfxSound.COIN_EARN)
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

    // ── Ad Rewards ────────────────────────────────────────────────────────────

    fun watchAdForCoins() {
        _uiState.update { it.copy(isWatchingAd = true) }
        viewModelScope.launch {
            val result = adManager.showRewardedAd()
            if (result.watched) {
                val current = _uiState.value.progress
                val updated = current.copy(coins = current.coins + 100)
                playerRepository.saveProgress(updated)
                audioManager.playSfx(SfxSound.COIN_EARN)
                _uiState.update { it.copy(isWatchingAd = false, message = "✅ +100 coins from ad!") }
            } else {
                _uiState.update { it.copy(isWatchingAd = false, message = "Ad not completed.") }
            }
            adManager.loadRewardedAd()
            _uiState.update { it.copy(isAdReady = adManager.isRewardedAdReady) }
        }
    }

    fun watchAdForLife() {
        _uiState.update { it.copy(isWatchingAd = true) }
        viewModelScope.launch {
            val result = adManager.showRewardedAd()
            if (result.watched) {
                val current = _uiState.value.progress
                val updated = current.copy(lives = current.lives + 1)
                playerRepository.saveProgress(updated)
                audioManager.playSfx(SfxSound.LIFE_GAINED)
                _uiState.update { it.copy(isWatchingAd = false, message = "✅ +1 life from ad!") }
            } else {
                _uiState.update { it.copy(isWatchingAd = false, message = "Ad not completed.") }
            }
            adManager.loadRewardedAd()
            _uiState.update { it.copy(isAdReady = adManager.isRewardedAdReady) }
        }
    }

    fun watchAdForItem() {
        _uiState.update { it.copy(isWatchingAd = true) }
        viewModelScope.launch {
            val result = adManager.showRewardedAd()
            if (result.watched) {
                val current = _uiState.value.progress
                // Grant a random item
                val itemIndex = (System.currentTimeMillis() % 4).toInt()
                val updated = when (itemIndex) {
                    0 -> current.copy(addGuessItems = current.addGuessItems + 1)
                    1 -> current.copy(removeLetterItems = current.removeLetterItems + 1)
                    2 -> current.copy(definitionItems = current.definitionItems + 1)
                    else -> current.copy(showLetterItems = current.showLetterItems + 1)
                }
                val itemName = when (itemIndex) {
                    0 -> "Add Guess"; 1 -> "Remove Letter"; 2 -> "Definition"; else -> "Show Letter"
                }
                playerRepository.saveProgress(updated)
                audioManager.playSfx(SfxSound.COIN_EARN)
                _uiState.update { it.copy(isWatchingAd = false, message = "✅ +1 $itemName item from ad!") }
            } else {
                _uiState.update { it.copy(isWatchingAd = false, message = "Ad not completed.") }
            }
            adManager.loadRewardedAd()
            _uiState.update { it.copy(isAdReady = adManager.isRewardedAdReady) }
        }
    }

    // ── Currency Trading ──────────────────────────────────────────────────────

    fun tradeCoinsForLife() {
        val progress = _uiState.value.progress
        if (progress.coins < 1000) {
            _uiState.update { it.copy(message = "You need 1000 coins.") }
            return
        }
        audioManager.playSfx(SfxSound.LIFE_GAINED)
        viewModelScope.launch {
            val updated = progress.copy(coins = progress.coins - 1000, lives = progress.lives + 1)
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ Traded 1000 coins for 1 life!") }
        }
    }

    fun tradeDiamondsForLife() {
        val progress = _uiState.value.progress
        if (progress.diamonds < 3) {
            _uiState.update { it.copy(message = "You need 3 diamonds.") }
            return
        }
        audioManager.playSfx(SfxSound.LIFE_GAINED)
        viewModelScope.launch {
            val updated = progress.copy(diamonds = progress.diamonds - 3, lives = progress.lives + 1)
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ Traded 3 diamonds for 1 life!") }
        }
    }

    // ── Item Purchasing ───────────────────────────────────────────────────────
    fun buyAddGuessItem() {
        val progress = _uiState.value.progress
        if (progress.coins < 200) {
            _uiState.update { it.copy(message = "Need 200 coins.") }
            return
        }
        audioManager.playSfx(SfxSound.COIN_EARN)
        viewModelScope.launch {
            val updated = progress.copy(
                coins = progress.coins - 200,
                addGuessItems = progress.addGuessItems + 1
            )
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ +1 Add a Guess item! (${updated.addGuessItems} owned)") }
        }
    }

    fun buyRemoveLetterItem() {
        val progress = _uiState.value.progress
        if (progress.coins < 150) {
            _uiState.update { it.copy(message = "Need 150 coins.") }
            return
        }
        audioManager.playSfx(SfxSound.COIN_EARN)
        viewModelScope.launch {
            val updated = progress.copy(
                coins = progress.coins - 150,
                removeLetterItems = progress.removeLetterItems + 1
            )
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ +1 Remove Letter item! (${updated.removeLetterItems} owned)") }
        }
    }

    fun buyDefinitionItem() {
        val progress = _uiState.value.progress
        if (progress.coins < 300) {
            _uiState.update { it.copy(message = "Need 300 coins.") }
            return
        }
        audioManager.playSfx(SfxSound.COIN_EARN)
        viewModelScope.launch {
            val updated = progress.copy(
                coins = progress.coins - 300,
                definitionItems = progress.definitionItems + 1
            )
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ +1 Definition item! (${updated.definitionItems} owned)") }
        }
    }

    fun buyShowLetterItem() {
        val progress = _uiState.value.progress
        if (progress.coins < 250) {
            _uiState.update { it.copy(message = "Need 250 coins.") }
            return
        }
        audioManager.playSfx(SfxSound.COIN_EARN)
        viewModelScope.launch {
            val updated = progress.copy(
                coins = progress.coins - 250,
                showLetterItems = progress.showLetterItems + 1
            )
            playerRepository.saveProgress(updated)
            _uiState.update { it.copy(message = "✅ +1 Show Letter item! (${updated.showLetterItems} owned)") }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    /**
     * Immediately activates VIP without billing — for use until Google Play billing is live.
     * Sets VIP for 30 days.
     */
    fun activateVipDirect() {
        viewModelScope.launch {
            val current = _uiState.value.progress
            if (current.isVip) {
                _uiState.update { it.copy(message = "You're already a VIP member! 👑") }
                return@launch
            }
            val updated = current.copy(
                isVip = true,
                vipExpiryTimestamp = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            )
            playerRepository.saveProgress(updated)
            audioManager.playSfx(SfxSound.COIN_EARN)
            _uiState.update { it.copy(message = "👑 VIP activated for 30 days! Enjoy +5 lives & items daily!") }
        }
    }

    fun getPriceLabel(productId: String) = billingManager.getPriceLabel(productId)

    private fun buildGrantMessage(coins: Long, diamonds: Int, lives: Int): String {
        val parts = mutableListOf<String>()
        if (coins > 0) parts.add("+$coins coins")
        if (diamonds > 0) parts.add("+$diamonds diamonds")
        if (lives > 0) parts.add("+$lives lives")
        return "✅ ${parts.joinToString(", ")} added!"
    }
}
