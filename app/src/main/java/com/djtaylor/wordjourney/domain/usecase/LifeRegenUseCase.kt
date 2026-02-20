package com.djtaylor.wordjourney.domain.usecase

import javax.inject.Inject

/**
 * Calculates passive life regeneration elapsed since [lastRegenTimestamp].
 *
 * Rules:
 *  - 1 life regenerates every [REGEN_INTERVAL_MS] minutes.
 *  - Time-based regen caps lives at [TIME_REGEN_CAP] (10).
 *  - Lives earned via level completion can exceed the cap (no absolute ceiling).
 *  - If [currentLives] >= [TIME_REGEN_CAP] the timer is paused (no regen owed).
 */
class LifeRegenUseCase @Inject constructor() {

    companion object {
        const val REGEN_INTERVAL_MS = 10 * 60 * 1000L  // 10 minutes in ms
        const val TIME_REGEN_CAP = 10
    }

    data class RegenResult(
        val updatedLives: Int,
        val updatedTimestamp: Long,   // new "last calculated" epoch ms
        val livesAdded: Int
    )

    /**
     * @param currentLives       current life count (may be > 10 from level bonuses)
     * @param lastRegenTimestamp epoch ms of the last regen calculation (0 = never)
     * @param nowMs              current time in epoch ms (injectable for testing)
     */
    operator fun invoke(
        currentLives: Int,
        lastRegenTimestamp: Long,
        nowMs: Long = System.currentTimeMillis()
    ): RegenResult {
        // If already at or above the time regen cap, no passive regen applies
        if (currentLives >= TIME_REGEN_CAP) {
            return RegenResult(currentLives, nowMs, 0)
        }

        val baseTimestamp = if (lastRegenTimestamp == 0L) nowMs else lastRegenTimestamp
        val elapsed = nowMs - baseTimestamp
        if (elapsed < REGEN_INTERVAL_MS) {
            return RegenResult(currentLives, lastRegenTimestamp, 0)
        }

        val intervalsElapsed = (elapsed / REGEN_INTERVAL_MS).toInt()
        val livesEarned = intervalsElapsed.coerceAtMost(TIME_REGEN_CAP - currentLives)
        val updatedLives = (currentLives + livesEarned).coerceAtMost(TIME_REGEN_CAP)

        // Advance timestamp by exactly the intervals consumed
        val consumedMs = intervalsElapsed * REGEN_INTERVAL_MS
        val updatedTimestamp = baseTimestamp + consumedMs

        return RegenResult(updatedLives, updatedTimestamp, livesEarned)
    }

    /**
     * Returns the epoch-ms timestamp when the next life will be ready,
     * given current lives < [TIME_REGEN_CAP].
     */
    fun nextLifeAtMs(lastRegenTimestamp: Long, nowMs: Long = System.currentTimeMillis()): Long {
        val base = if (lastRegenTimestamp == 0L) nowMs else lastRegenTimestamp
        val elapsed = nowMs - base
        val nextIntervalMs = REGEN_INTERVAL_MS - (elapsed % REGEN_INTERVAL_MS)
        return nowMs + nextIntervalMs
    }

    /**
     * Returns ms until lives will reach [TIME_REGEN_CAP] from [currentLives].
     */
    fun msUntilFull(
        currentLives: Int,
        lastRegenTimestamp: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        if (currentLives >= TIME_REGEN_CAP) return 0L
        val livesNeeded = TIME_REGEN_CAP - currentLives
        val base = if (lastRegenTimestamp == 0L) nowMs else lastRegenTimestamp
        val elapsed = nowMs - base
        val remainderInCurrentInterval = REGEN_INTERVAL_MS - (elapsed % REGEN_INTERVAL_MS)
        return remainderInCurrentInterval + (livesNeeded - 1) * REGEN_INTERVAL_MS
    }
}
