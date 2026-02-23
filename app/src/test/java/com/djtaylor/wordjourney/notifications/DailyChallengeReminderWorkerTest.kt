package com.djtaylor.wordjourney.notifications

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [DailyChallengeReminderWorker] logic.
 *
 * The worker itself requires Android instrumentation to run end-to-end, but the
 * [DailyChallengeReminderWorker.Companion.msUntilNextNoon] helper is pure JVM
 * logic that can be exercised here.
 */
class DailyChallengeReminderWorkerTest {

    // ══════════════════════════════════════════════════════════════════════════
    // msUntilNextNoon invariants
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `msUntilNextNoon returns positive value`() {
        val ms = DailyChallengeReminderWorker.msUntilNextNoon()
        assertTrue("Delay must be positive", ms > 0L)
    }

    @Test
    fun `msUntilNextNoon is at least 1 second`() {
        val ms = DailyChallengeReminderWorker.msUntilNextNoon()
        assertTrue("Delay must be at least 1 second", ms >= 1_000L)
    }

    @Test
    fun `msUntilNextNoon does not exceed 25 hours`() {
        val maxMs = TimeUnit.HOURS.toMillis(25)
        val ms = DailyChallengeReminderWorker.msUntilNextNoon()
        assertTrue("Delay must not exceed 25 hours (ms=$ms)", ms <= maxMs)
    }

    @Test
    fun `msUntilNextNoon returns value within one day`() {
        // The function always schedules for the NEXT noon occurrence.
        // That is at most 24 hours away (just after noon) + a little margin.
        val oneDayMs = TimeUnit.HOURS.toMillis(24) + TimeUnit.MINUTES.toMillis(2)
        val ms = DailyChallengeReminderWorker.msUntilNextNoon()
        assertTrue(
            "Expected delay <= 24h 2min but got ${ms}ms (${TimeUnit.MILLISECONDS.toMinutes(ms)} min)",
            ms <= oneDayMs
        )
    }

    @Test
    fun `msUntilNextNoon is stable over two consecutive calls`() {
        // Two back-to-back calls should differ by at most a few hundred ms
        val ms1 = DailyChallengeReminderWorker.msUntilNextNoon()
        val ms2 = DailyChallengeReminderWorker.msUntilNextNoon()
        val diff = Math.abs(ms1 - ms2)
        assertTrue("Two consecutive calls should agree within 500ms (diff=$diff)", diff < 500L)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Companion constants
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `WORK_TAG is non-empty`() {
        assertTrue(DailyChallengeReminderWorker.WORK_TAG.isNotEmpty())
    }

    @Test
    fun `NOTIFICATION_ID is positive and distinct from lives-full id`() {
        // Lives-full uses 1001; daily challenge should use a different id
        assertTrue(DailyChallengeReminderWorker.NOTIFICATION_ID > 0)
        assertNotEquals(1001, DailyChallengeReminderWorker.NOTIFICATION_ID)
    }
}
