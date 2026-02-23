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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val items: List<InboxItemEntity> = emptyList(),
    val unclaimedCount: Int = 0,
    val isLoading: Boolean = true,
    val claimAllDone: Boolean = false
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
