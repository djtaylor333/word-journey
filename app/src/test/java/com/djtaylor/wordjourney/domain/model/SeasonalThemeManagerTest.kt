package com.djtaylor.wordjourney.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.Month

/**
 * Tests for SeasonalThemeManager calendar-based seasonal detection.
 */
class SeasonalThemeManagerTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. ACTIVE SEASON DETECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `halloween is active on October 15`() {
        val date = LocalDate.of(2025, Month.OCTOBER, 15)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertNotNull(active)
        assertEquals(SeasonalThemeManager.Season.HALLOWEEN, active)
    }

    @Test
    fun `halloween is active on October 1`() {
        val date = LocalDate.of(2025, Month.OCTOBER, 1)
        assertTrue(SeasonalThemeManager.Season.HALLOWEEN.isActive(date))
    }

    @Test
    fun `halloween is active on October 31`() {
        val date = LocalDate.of(2025, Month.OCTOBER, 31)
        assertTrue(SeasonalThemeManager.Season.HALLOWEEN.isActive(date))
    }

    @Test
    fun `halloween is not active on November 1`() {
        val date = LocalDate.of(2025, Month.NOVEMBER, 1)
        assertFalse(SeasonalThemeManager.Season.HALLOWEEN.isActive(date))
    }

    @Test
    fun `christmas is active on December 25`() {
        val date = LocalDate.of(2025, Month.DECEMBER, 25)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertEquals(SeasonalThemeManager.Season.CHRISTMAS, active)
    }

    @Test
    fun `valentine is active on February 14`() {
        val date = LocalDate.of(2025, Month.FEBRUARY, 14)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertEquals(SeasonalThemeManager.Season.VALENTINES, active)
    }

    @Test
    fun `easter is active on April 1`() {
        val date = LocalDate.of(2025, Month.APRIL, 1)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertEquals(SeasonalThemeManager.Season.EASTER, active)
    }

    @Test
    fun `summer is active on July 4`() {
        val date = LocalDate.of(2025, Month.JULY, 4)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertEquals(SeasonalThemeManager.Season.SUMMER, active)
    }

    @Test
    fun `thanksgiving is active on November 15`() {
        val date = LocalDate.of(2025, Month.NOVEMBER, 15)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertEquals(SeasonalThemeManager.Season.THANKSGIVING, active)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. NO ACTIVE SEASON
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `no active season on January 15`() {
        val date = LocalDate.of(2025, Month.JANUARY, 15)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertNull(active)
    }

    @Test
    fun `no active season on May 15`() {
        val date = LocalDate.of(2025, Month.MAY, 15)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertNull(active)
    }

    @Test
    fun `no active season on September 15`() {
        val date = LocalDate.of(2025, Month.SEPTEMBER, 15)
        val active = SeasonalThemeManager.getActiveSeason(date)
        assertNull(active)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. NEXT SEASON DETECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `next season after January is Valentine`() {
        val date = LocalDate.of(2025, Month.JANUARY, 15)
        val next = SeasonalThemeManager.getNextSeason(date)
        assertEquals(SeasonalThemeManager.Season.VALENTINES, next.season)
        assertFalse(next.isActive)
        assertTrue(next.daysUntil > 0)
    }

    @Test
    fun `next season after February 15 is Easter`() {
        val date = LocalDate.of(2025, Month.FEBRUARY, 15)
        val next = SeasonalThemeManager.getNextSeason(date)
        assertEquals(SeasonalThemeManager.Season.EASTER, next.season)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. ALL SEASON STATUSES
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getAllSeasonStatuses returns all 6 seasons`() {
        val statuses = SeasonalThemeManager.getAllSeasonStatuses(LocalDate.of(2025, 1, 1))
        assertEquals(6, statuses.size)
    }

    @Test
    fun `active season appears first in sorted list`() {
        val date = LocalDate.of(2025, Month.OCTOBER, 15)
        val statuses = SeasonalThemeManager.getAllSeasonStatuses(date)
        assertTrue(statuses.first().isActive)
        assertEquals(SeasonalThemeManager.Season.HALLOWEEN, statuses.first().season)
    }

    @Test
    fun `inactive seasons sorted by days until start`() {
        val date = LocalDate.of(2025, Month.JANUARY, 1)
        val statuses = SeasonalThemeManager.getAllSeasonStatuses(date)
        val inactive = statuses.filter { !it.isActive }
        for (i in 0 until inactive.size - 1) {
            assertTrue(inactive[i].daysUntil <= inactive[i + 1].daysUntil)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. SEASON BOUNDARY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `summer starts June 1`() {
        assertTrue(SeasonalThemeManager.Season.SUMMER.isActive(LocalDate.of(2025, 6, 1)))
    }

    @Test
    fun `summer ends August 31`() {
        assertTrue(SeasonalThemeManager.Season.SUMMER.isActive(LocalDate.of(2025, 8, 31)))
    }

    @Test
    fun `summer is not active September 1`() {
        assertFalse(SeasonalThemeManager.Season.SUMMER.isActive(LocalDate.of(2025, 9, 1)))
    }

    @Test
    fun `valentine not active February 15`() {
        assertFalse(SeasonalThemeManager.Season.VALENTINES.isActive(LocalDate.of(2025, 2, 15)))
    }

    @Test
    fun `days until calculation is positive for upcoming season`() {
        val date = LocalDate.of(2025, 1, 1)
        val daysUntil = SeasonalThemeManager.Season.HALLOWEEN.daysUntilStart(date)
        assertTrue(daysUntil > 0)
        assertEquals(273, daysUntil) // Jan 1 to Oct 1
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. SEASONAL THEME EXPIRY / UPCOMING (TDD for lock status)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `isExpiredThisYear returns true when season has ended`() {
        val afterHalloween = LocalDate.of(2025, 11, 1)
        assertTrue(SeasonalThemeManager.Season.HALLOWEEN.isExpiredThisYear(afterHalloween))
    }

    @Test
    fun `isExpiredThisYear returns false during active season`() {
        val duringHalloween = LocalDate.of(2025, 10, 15)
        assertFalse(SeasonalThemeManager.Season.HALLOWEEN.isExpiredThisYear(duringHalloween))
    }

    @Test
    fun `isExpiredThisYear returns false before season starts`() {
        val beforeHalloween = LocalDate.of(2025, 9, 1)
        assertFalse(SeasonalThemeManager.Season.HALLOWEEN.isExpiredThisYear(beforeHalloween))
    }

    @Test
    fun `isUpcomingThisYear returns true before season starts`() {
        val beforeValentines = LocalDate.of(2026, 1, 15)
        assertTrue(SeasonalThemeManager.Season.VALENTINES.isUpcomingThisYear(beforeValentines))
    }

    @Test
    fun `isUpcomingThisYear returns false during active season`() {
        val duringValentines = LocalDate.of(2026, 2, 10)
        assertFalse(SeasonalThemeManager.Season.VALENTINES.isUpcomingThisYear(duringValentines))
    }

    @Test
    fun `isUpcomingThisYear returns false after season ends`() {
        val afterValentines = LocalDate.of(2026, 2, 20)
        assertFalse(SeasonalThemeManager.Season.VALENTINES.isUpcomingThisYear(afterValentines))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. THEME ID → SEASON MAPPING (TDD for getSeasonForThemeId)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getSeasonForThemeId returns HALLOWEEN for seasonal_halloween`() {
        assertEquals(SeasonalThemeManager.Season.HALLOWEEN, SeasonalThemeManager.getSeasonForThemeId("seasonal_halloween"))
    }

    @Test
    fun `getSeasonForThemeId returns CHRISTMAS for seasonal_christmas`() {
        assertEquals(SeasonalThemeManager.Season.CHRISTMAS, SeasonalThemeManager.getSeasonForThemeId("seasonal_christmas"))
    }

    @Test
    fun `getSeasonForThemeId returns VALENTINES for seasonal_valentines`() {
        assertEquals(SeasonalThemeManager.Season.VALENTINES, SeasonalThemeManager.getSeasonForThemeId("seasonal_valentines"))
    }

    @Test
    fun `getSeasonForThemeId returns EASTER for seasonal_easter`() {
        assertEquals(SeasonalThemeManager.Season.EASTER, SeasonalThemeManager.getSeasonForThemeId("seasonal_easter"))
    }

    @Test
    fun `getSeasonForThemeId returns SUMMER for seasonal_summer`() {
        assertEquals(SeasonalThemeManager.Season.SUMMER, SeasonalThemeManager.getSeasonForThemeId("seasonal_summer"))
    }

    @Test
    fun `getSeasonForThemeId returns THANKSGIVING for seasonal_thanksgiving`() {
        assertEquals(SeasonalThemeManager.Season.THANKSGIVING, SeasonalThemeManager.getSeasonForThemeId("seasonal_thanksgiving"))
    }

    @Test
    fun `getSeasonForThemeId returns null for non-seasonal theme`() {
        assertNull(SeasonalThemeManager.getSeasonForThemeId("classic"))
        assertNull(SeasonalThemeManager.getSeasonForThemeId("neon_nights"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. getThemeLockInfo (TDD for seasonal lock status)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getThemeLockInfo returns ACTIVE during halloween season`() {
        val duringHalloween = LocalDate.of(2025, 10, 15)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_halloween", duringHalloween)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.ACTIVE, info.status)
    }

    @Test
    fun `getThemeLockInfo returns PAST after halloween season`() {
        val afterHalloween = LocalDate.of(2025, 11, 5)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_halloween", afterHalloween)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.PAST, info.status)
        assertEquals(SeasonalThemeManager.Season.HALLOWEEN, info.season)
    }

    @Test
    fun `getThemeLockInfo returns FUTURE before halloween season`() {
        val beforeHalloween = LocalDate.of(2025, 9, 1)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_halloween", beforeHalloween)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.FUTURE, info.status)
        assertEquals(SeasonalThemeManager.Season.HALLOWEEN, info.season)
        assertTrue("daysUntil should be > 0", info.daysUntilStart > 0)
    }

    @Test
    fun `getThemeLockInfo returns ACTIVE for non-seasonal theme`() {
        // Non-seasonal themes (free/VIP) are always ACTIVE via lock info
        val anyDate = LocalDate.of(2025, 1, 15)
        val info = SeasonalThemeManager.getThemeLockInfo("classic", anyDate)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.ACTIVE, info.status)
    }

    @Test
    fun `getThemeLockInfo returns FUTURE for valentines in January 2026`() {
        val jan2026 = LocalDate.of(2026, 1, 15)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_valentines", jan2026)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.FUTURE, info.status)
    }

    @Test
    fun `getThemeLockInfo returns PAST for valentines in February 23 2026`() {
        // Today (Feb 23 2026) is after Valentine's Day (Feb 1-14)
        val today = LocalDate.of(2026, 2, 23)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_valentines", today)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.PAST, info.status)
    }

    @Test
    fun `getThemeLockInfo PAST status locks non-VIP but allows VIP`() {
        // The lock info itself just returns the status; VM enforces the rule
        val afterHalloween = LocalDate.of(2025, 11, 5)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_halloween", afterHalloween)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.PAST, info.status)
        // PAST → VIP can purchase, others cannot (enforced in SettingsViewModel)
    }

    @Test
    fun `future seasonal theme has positive daysUntilStart`() {
        val beforeChristmas = LocalDate.of(2025, 6, 1)
        val info = SeasonalThemeManager.getThemeLockInfo("seasonal_christmas", beforeChristmas)
        assertEquals(SeasonalThemeManager.SeasonalLockStatus.FUTURE, info.status)
        assertTrue(info.daysUntilStart > 0)
    }
}

