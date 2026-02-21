package com.djtaylor.wordjourney.domain.model

import java.time.LocalDate
import java.time.Month

/**
 * Manages seasonal theme detection using the device calendar.
 *
 * Each season has a defined date range. The manager checks the current date
 * against these ranges to determine which seasonal theme packs are active,
 * upcoming, or past.
 *
 * ## Seasonal Themes
 * - **Valentine's Day**: Feb 1–14
 * - **Easter / Spring**: Mar 15–Apr 20
 * - **Summer**: Jun 1–Aug 31
 * - **Halloween**: Oct 1–31
 * - **Thanksgiving**: Nov 1–28
 * - **Christmas / Winter**: Dec 1–31
 *
 * ## Integration Notes
 * When real word packs are added, each [Season] should map to a list of
 * themed words loaded from a resource file (e.g., `seasonal_halloween.json`).
 */
object SeasonalThemeManager {

    enum class Season(
        val displayName: String,
        val emoji: String,
        val description: String,
        val startMonth: Month,
        val startDay: Int,
        val endMonth: Month,
        val endDay: Int
    ) {
        VALENTINES(
            displayName = "Valentine's Day",
            emoji = "💕",
            description = "Love-themed words",
            startMonth = Month.FEBRUARY, startDay = 1,
            endMonth = Month.FEBRUARY, endDay = 14
        ),
        EASTER(
            displayName = "Easter & Spring",
            emoji = "🐣",
            description = "Spring renewal words",
            startMonth = Month.MARCH, startDay = 15,
            endMonth = Month.APRIL, endDay = 20
        ),
        SUMMER(
            displayName = "Summer",
            emoji = "☀️",
            description = "Sun & vacation words",
            startMonth = Month.JUNE, startDay = 1,
            endMonth = Month.AUGUST, endDay = 31
        ),
        HALLOWEEN(
            displayName = "Halloween",
            emoji = "🎃",
            description = "Spooky themed words",
            startMonth = Month.OCTOBER, startDay = 1,
            endMonth = Month.OCTOBER, endDay = 31
        ),
        THANKSGIVING(
            displayName = "Thanksgiving",
            emoji = "🦃",
            description = "Gratitude & harvest words",
            startMonth = Month.NOVEMBER, startDay = 1,
            endMonth = Month.NOVEMBER, endDay = 28
        ),
        CHRISTMAS(
            displayName = "Christmas & Winter",
            emoji = "🎄",
            description = "Holiday & winter words",
            startMonth = Month.DECEMBER, startDay = 1,
            endMonth = Month.DECEMBER, endDay = 31
        );

        /** Check if a date falls within this season's active window. */
        fun isActive(date: LocalDate): Boolean {
            val start = LocalDate.of(date.year, startMonth, startDay)
            val end = LocalDate.of(date.year, endMonth, endDay)
            return !date.isBefore(start) && !date.isAfter(end)
        }

        /** Days remaining until this season starts (0 if active/past for this year). */
        fun daysUntilStart(date: LocalDate): Long {
            val start = LocalDate.of(date.year, startMonth, startDay)
            return if (date.isBefore(start)) {
                java.time.temporal.ChronoUnit.DAYS.between(date, start)
            } else {
                // Either active or past — check next year
                val nextStart = LocalDate.of(date.year + 1, startMonth, startDay)
                java.time.temporal.ChronoUnit.DAYS.between(date, nextStart)
            }
        }
    }

    data class SeasonStatus(
        val season: Season,
        val isActive: Boolean,
        val daysUntil: Long
    )

    /**
     * Returns all seasons with their current status, sorted so active seasons
     * come first, then upcoming seasons sorted by days until start.
     */
    fun getAllSeasonStatuses(date: LocalDate = LocalDate.now()): List<SeasonStatus> {
        return Season.entries.map { season ->
            SeasonStatus(
                season = season,
                isActive = season.isActive(date),
                daysUntil = season.daysUntilStart(date)
            )
        }.sortedWith(compareBy({ !it.isActive }, { it.daysUntil }))
    }

    /** Returns the currently active season, if any. */
    fun getActiveSeason(date: LocalDate = LocalDate.now()): Season? {
        return Season.entries.firstOrNull { it.isActive(date) }
    }

    /** Returns the next upcoming season. */
    fun getNextSeason(date: LocalDate = LocalDate.now()): SeasonStatus {
        return Season.entries
            .map { SeasonStatus(it, it.isActive(date), it.daysUntilStart(date)) }
            .filter { !it.isActive }
            .minByOrNull { it.daysUntil }
            ?: SeasonStatus(Season.CHRISTMAS, false, 365)
    }
}
