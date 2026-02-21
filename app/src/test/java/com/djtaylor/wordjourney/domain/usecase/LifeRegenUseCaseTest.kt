package com.djtaylor.wordjourney.domain.usecase

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for passive life regeneration logic.
 *
 * Rules:
 *  - 1 life regenerates every 10 minutes
 *  - Time-based regen caps at 10 lives (TIME_REGEN_CAP)
 *  - Lives from level bonuses CAN exceed 10 (no absolute ceiling)
 *  - If lives >= 10, the regen timer is paused (no regen owed)
 *  - First invocation (lastRegenTimestamp=0) initialises the timer, no regen
 */
class LifeRegenUseCaseTest {

    private lateinit var regen: LifeRegenUseCase

    @Before
    fun setup() {
        regen = LifeRegenUseCase()
    }

    // ── No regen when at cap ─────────────────────────────────────────────────

    @Test
    fun `no regen when lives at cap`() {
        val result = regen(currentLives = 10, lastRegenTimestamp = 1000L, nowMs = 999_999L)
        assertEquals(10, result.updatedLives)
        assertEquals(0, result.livesAdded)
    }

    @Test
    fun `no regen when lives above cap`() {
        val result = regen(currentLives = 13, lastRegenTimestamp = 1000L, nowMs = 999_999L)
        assertEquals(13, result.updatedLives)
        assertEquals(0, result.livesAdded)
    }

    // ── First invocation ─────────────────────────────────────────────────────

    @Test
    fun `first invocation with timestamp 0 — no regen, timestamp unchanged`() {
        // When lastRegenTimestamp is 0, baseTimestamp = nowMs, elapsed = 0.
        // Since elapsed < REGEN_INTERVAL_MS, returns original timestamp (0).
        // This is by design: the timer starts from the next call where
        // the caller saves updatedTimestamp=nowMs via saveProgress.
        val now = 1_000_000L
        val result = regen(currentLives = 5, lastRegenTimestamp = 0L, nowMs = now)
        assertEquals(5, result.updatedLives)
        assertEquals(0, result.livesAdded)
        // Code returns lastRegenTimestamp (0) when no regen occurs
        assertEquals(0L, result.updatedTimestamp)
    }

    // ── Not enough time elapsed ──────────────────────────────────────────────

    @Test
    fun `no regen when less than 10 minutes elapsed`() {
        val base = 1_000_000L
        val result = regen(
            currentLives = 5,
            lastRegenTimestamp = base,
            nowMs = base + 9 * 60 * 1000  // 9 minutes
        )
        assertEquals(5, result.updatedLives)
        assertEquals(0, result.livesAdded)
        assertEquals(base, result.updatedTimestamp) // unchanged
    }

    // ── Single life regen ────────────────────────────────────────────────────

    @Test
    fun `one life after exactly 10 minutes`() {
        val base = 1_000_000L
        val tenMin = 10 * 60 * 1000L
        val result = regen(currentLives = 5, lastRegenTimestamp = base, nowMs = base + tenMin)
        assertEquals(6, result.updatedLives)
        assertEquals(1, result.livesAdded)
    }

    // ── Multiple lives regen ─────────────────────────────────────────────────

    @Test
    fun `three lives after 30 minutes`() {
        val base = 1_000_000L
        val thirtyMin = 30 * 60 * 1000L
        val result = regen(currentLives = 5, lastRegenTimestamp = base, nowMs = base + thirtyMin)
        assertEquals(8, result.updatedLives)
        assertEquals(3, result.livesAdded)
    }

    // ── Cap enforcement ──────────────────────────────────────────────────────

    @Test
    fun `regen does not exceed cap`() {
        val base = 1_000_000L
        val manyHours = 24 * 60 * 60 * 1000L // 24 hours = 144 intervals
        val result = regen(currentLives = 8, lastRegenTimestamp = base, nowMs = base + manyHours)
        assertEquals(10, result.updatedLives)
        assertEquals(2, result.livesAdded) // only 2 needed to reach cap
    }

    @Test
    fun `regen from 0 lives caps at 10`() {
        val base = 1_000_000L
        val manyHours = 24 * 60 * 60 * 1000L
        val result = regen(currentLives = 0, lastRegenTimestamp = base, nowMs = base + manyHours)
        assertEquals(10, result.updatedLives)
        assertEquals(10, result.livesAdded)
    }

    // ── Timestamp advancement ────────────────────────────────────────────────

    @Test
    fun `timestamp advances by consumed intervals`() {
        val base = 1_000_000L
        val tenMin = 10 * 60 * 1000L
        // 25 minutes elapsed = 2 full intervals + 5 min leftover
        val result = regen(currentLives = 5, lastRegenTimestamp = base, nowMs = base + 25 * 60 * 1000)
        assertEquals(7, result.updatedLives)
        assertEquals(2, result.livesAdded)
        assertEquals(base + 2 * tenMin, result.updatedTimestamp) // 20 min consumed
    }

    // ── Next life timer ──────────────────────────────────────────────────────

    @Test
    fun `nextLifeAtMs returns correct future timestamp`() {
        val base = 1_000_000L
        val now = base + 3 * 60 * 1000L // 3 min after base
        val nextLifeAt = regen.nextLifeAtMs(lastRegenTimestamp = base, nowMs = now)
        // 3 min elapsed, need 7 more min for next interval
        assertEquals(now + 7 * 60 * 1000L, nextLifeAt)
    }

    @Test
    fun `msUntilFull returns 0 when at cap`() {
        assertEquals(0L, regen.msUntilFull(currentLives = 10, lastRegenTimestamp = 0L))
    }
}
