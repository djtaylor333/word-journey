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
}
