package com.djtaylor.wordjourney.ui.inbox

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.db.InboxItemEntity
import com.djtaylor.wordjourney.data.repository.InboxRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.notifications.LivesFullNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class InboxUiState(
    val items: List<InboxItemEntity> = emptyList(),
    val unclaimedCount: Int = 0,
    val isLoading: Boolean = true,
    val claimAllDone: Boolean = false,
    val isVip: Boolean = false,
    val nextVipRewardMs: Long = 0L   // ms until next VIP daily reward (0 = available now)
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inboxRepository: InboxRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadItems()
        observeVipStatus()
        startVipCountdownTick()
    }

    /** Observe player VIP state and last-reward date to keep countdown accurate. */
    private fun observeVipStatus() {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                val vipMs = if (progress.isVip && progress.lastVipRewardDate.isNotBlank()) {
                    val nextMidnight = LocalDate.now().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    (nextMidnight - System.currentTimeMillis()).coerceAtLeast(0L)
                } else 0L
                _uiState.update { it.copy(isVip = progress.isVip, nextVipRewardMs = vipMs) }
            }
        }
    }

    /** Ticks every second to update the VIP reward countdown. */
    private fun startVipCountdownTick() {
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val current = _uiState.value
                if (current.isVip && current.nextVipRewardMs > 0L) {
                    _uiState.update {
                        it.copy(nextVipRewardMs = (it.nextVipRewardMs - 1_000L).coerceAtLeast(0L))
                    }
                }
            }
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            val all      = inboxRepository.getAllItems()
            val unclaimed = all.count { !it.claimed }
            _uiState.update {
                it.copy(items = all, unclaimedCount = unclaimed, isLoading = false)
            }
        }
    }

    fun claimItem(itemId: Int) {
        viewModelScope.launch {
            val claimed = inboxRepository.claimItem(itemId) ?: return@launch
            // Apply reward to player progress
            playerRepository.playerProgressFlow.first().let { progress ->
                val updated = inboxRepository.applyRewardsToProgress(listOf(claimed), progress)
                playerRepository.saveProgress(updated)
                // Reschedule notification now that lives may have changed
                try {
                    LivesFullNotificationWorker.schedule(
                        context              = context,
                        currentLives         = updated.lives,
                        lastRegenTimestamp   = updated.lastLifeRegenTimestamp,
                        notificationsEnabled = updated.notifyLivesFull
                    )
                } catch (_: Exception) { /* not critical if WorkManager unavailable in tests */ }
            }
            loadItems()
        }
    }

    fun claimAll() {
        viewModelScope.launch {
            val claimed = inboxRepository.claimAllItems()
            if (claimed.isNotEmpty()) {
                playerRepository.playerProgressFlow.first().let { progress ->
                    val updated = inboxRepository.applyRewardsToProgress(claimed, progress)
                    playerRepository.saveProgress(updated)
                    // Reschedule notification now that lives may have changed
                    try {
                        LivesFullNotificationWorker.schedule(
                            context              = context,
                            currentLives         = updated.lives,
                            lastRegenTimestamp   = updated.lastLifeRegenTimestamp,
                            notificationsEnabled = updated.notifyLivesFull
                        )
                    } catch (_: Exception) { /* not critical if WorkManager unavailable in tests */ }
                }
            }
            _uiState.update { it.copy(claimAllDone = true) }
            loadItems()
        }
    }
}
