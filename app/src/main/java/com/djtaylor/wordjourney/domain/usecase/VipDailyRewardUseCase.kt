package com.djtaylor.wordjourney.domain.usecase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates VIP daily rewards.
 *
 * VIP members receive +5 lives and +5 random items per day.
 * If the player hasn't logged in for multiple days, rewards accumulate
 * (capped at 30 days to prevent abuse).
 */
@Singleton
class VipDailyRewardUseCase @Inject constructor() {

    companion object {
        const val LIVES_PER_DAY = 5
        const val ITEMS_PER_DAY = 5      // total items, distributed randomly
        const val MAX_ACCUMULATION_DAYS = 30
    }

    data class VipReward(
        val livesGranted: Int,
        val addGuessItemsGranted: Int,
        val removeLetterItemsGranted: Int,
        val definitionItemsGranted: Int,
        val showLetterItemsGranted: Int,
        val daysAccumulated: Int,
        val updatedLastRewardDate: String   // YYYY-MM-DD
    )

    /**
     * Calculate VIP daily rewards based on the last reward date.
     *
     * @param lastVipRewardDate the last date rewards were collected (YYYY-MM-DD), or empty string
     * @param today the current date (injectable for testing)
     * @return VipReward with items to grant, or null if no rewards earned today
     */
    fun calculateRewards(
        lastVipRewardDate: String,
        today: LocalDate = LocalDate.now()
    ): VipReward? {
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Already collected today
        if (lastVipRewardDate == todayStr) return null

        val daysAccumulated = if (lastVipRewardDate.isBlank()) {
            1 // First time VIP reward
        } else {
            try {
                val lastDate = LocalDate.parse(lastVipRewardDate, DateTimeFormatter.ISO_LOCAL_DATE)
                ChronoUnit.DAYS.between(lastDate, today).toInt().coerceIn(1, MAX_ACCUMULATION_DAYS)
            } catch (e: Exception) {
                1 // If parsing fails, grant 1 day
            }
        }

        val totalLives = daysAccumulated * LIVES_PER_DAY
        val totalItems = daysAccumulated * ITEMS_PER_DAY

        // Distribute items evenly across 4 types, with remainder going round-robin
        val basePerType = totalItems / 4
        val remainder = totalItems % 4

        return VipReward(
            livesGranted = totalLives,
            addGuessItemsGranted = basePerType + if (remainder > 0) 1 else 0,
            removeLetterItemsGranted = basePerType + if (remainder > 1) 1 else 0,
            definitionItemsGranted = basePerType + if (remainder > 2) 1 else 0,
            showLetterItemsGranted = basePerType,
            daysAccumulated = daysAccumulated,
            updatedLastRewardDate = todayStr
        )
    }
}
