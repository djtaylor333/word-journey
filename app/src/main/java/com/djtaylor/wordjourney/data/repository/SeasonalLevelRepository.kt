package com.djtaylor.wordjourney.data.repository

import com.djtaylor.wordjourney.domain.model.SeasonalWordPacks
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository providing access to seasonal themed level word data.
 *
 * Wraps [SeasonalWordPacks] to make it injectable and easily mockable in tests.
 * Each of the six seasonal packs contains exactly 100 five-letter words.
 *
 * Season pack keys: "easter", "valentines", "summer", "halloween", "thanksgiving", "christmas"
 */
@Singleton
class SeasonalLevelRepository @Inject constructor() {

    /**
     * Returns the target word for the given season pack and level (1-based).
     * Wraps around if [level] exceeds the pack size.
     */
    fun getWordForLevel(seasonKey: String, level: Int): String =
        SeasonalWordPacks.getWord(seasonKey, level)

    /**
     * Returns the total number of levels in the given season pack (always 100 if valid).
     */
    fun packSize(seasonKey: String): Int =
        SeasonalWordPacks.packSize(seasonKey)

    /**
     * Returns true if a pack exists for the given season key.
     */
    fun hasPack(seasonKey: String): Boolean =
        SeasonalWordPacks.availableSeasonKeys.contains(seasonKey)

    /**
     * Returns all available season pack keys.
     */
    val availableSeasonKeys: Set<String>
        get() = SeasonalWordPacks.availableSeasonKeys
}
