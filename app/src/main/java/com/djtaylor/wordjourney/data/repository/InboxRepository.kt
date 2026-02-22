package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.data.db.InboxDao
import com.djtaylor.wordjourney.data.db.InboxItemEntity
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the player's inbox — items earned but not yet claimed.
 *
 * Items accumulate here from:
 *  - VIP daily rewards (instead of being applied directly)
 *  - Seasonal events
 *  - Promotions / admin grants
 *
 * Players claim items explicitly, even if they haven't opened the app in days.
 */
@Singleton
class InboxRepository @Inject constructor(
    private val inboxDao: InboxDao
) {

    suspend fun getUnclaimedItems(): List<InboxItemEntity> = inboxDao.getUnclaimed()

    suspend fun getUnclaimedCount(): Int = inboxDao.countUnclaimed()

    suspend fun getAllItems(): List<InboxItemEntity> = inboxDao.getAll()

    suspend fun addItem(item: InboxItemEntity): Long = inboxDao.insert(item)

    /**
     * Mark a single item as claimed.
     * Returns the claimed item, or null if not found.
     */
    suspend fun claimItem(itemId: Int): InboxItemEntity? {
        val item = inboxDao.getById(itemId) ?: return null
        if (item.claimed) return item
        inboxDao.markClaimed(item.id, System.currentTimeMillis())
        return inboxDao.getById(item.id)
    }

    /**
     * Mark ALL unclaimed items as claimed in one operation.
     * Returns the items that were claimed (to apply rewards).
     */
    suspend fun claimAllItems(): List<InboxItemEntity> {
        val unclaimed = inboxDao.getUnclaimed()
        if (unclaimed.isNotEmpty()) {
            inboxDao.claimAll(System.currentTimeMillis())
        }
        return unclaimed
    }

    /**
     * Clean up old claimed items to keep the DB tidy.
     * Removes items claimed more than 30 days ago.
     */
    suspend fun pruneOldClaimed() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        inboxDao.deleteClaimed(thirtyDaysAgo)
    }

    /**
     * Check if a VIP daily reward has already been added today.
     */
    suspend fun hasVipRewardToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return inboxDao.getByTypeAndDate("vip_daily", today) != null
    }

    /**
     * Add a VIP daily reward to the inbox (if not already added today).
     * Returns the created InboxItem id, or -1 if already added today.
     */
    suspend fun addVipDailyRewardIfNeeded(
        livesGranted: Int,
        addGuessItems: Int,
        removeLetterItems: Int,
        definitionItems: Int,
        showLetterItems: Int,
        daysAccumulated: Int
    ): Long {
        if (hasVipRewardToday()) return -1L
        val totalItems = addGuessItems + removeLetterItems + definitionItems + showLetterItems
        val title = if (daysAccumulated > 1) {
            "👑 VIP ${daysAccumulated}-Day Reward"
        } else {
            "👑 VIP Daily Reward"
        }
        val message = buildString {
            append("+$livesGranted lives")
            if (totalItems > 0) append(", +$totalItems items")
            if (daysAccumulated > 1) append(" ($daysAccumulated days accumulated)")
        }
        val item = InboxItemEntity(
            type = "vip_daily",
            title = title,
            message = message,
            livesGranted = livesGranted,
            addGuessItemsGranted = addGuessItems,
            removeLetterItemsGranted = removeLetterItems,
            definitionItemsGranted = definitionItems,
            showLetterItemsGranted = showLetterItems
        )
        return inboxDao.insert(item)
    }

    /**
     * Apply all claimed inbox items to PlayerProgress.
     * Returns an updated PlayerProgress with all rewards applied.
     */
    fun applyRewardsToProgress(items: List<InboxItemEntity>, progress: PlayerProgress): PlayerProgress {
        var p = progress
        for (item in items) {
            p = p.copy(
                coins = p.coins + item.coinsGranted,
                totalCoinsEarned = p.totalCoinsEarned + item.coinsGranted,
                diamonds = p.diamonds + item.diamondsGranted,
                lives = p.lives + item.livesGranted,
                addGuessItems = p.addGuessItems + item.addGuessItemsGranted,
                removeLetterItems = p.removeLetterItems + item.removeLetterItemsGranted,
                definitionItems = p.definitionItems + item.definitionItemsGranted,
                showLetterItems = p.showLetterItems + item.showLetterItemsGranted
            )
        }
        return p
    }
}
