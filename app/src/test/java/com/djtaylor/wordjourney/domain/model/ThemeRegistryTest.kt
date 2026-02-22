package com.djtaylor.wordjourney.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for GameTheme, ThemeRegistry, and BackgroundPattern.
 * Covers theme lookup, category grouping, color uniqueness,
 * background patterns, and font assignments.
 */
class ThemeRegistryTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. THEME REGISTRY — LOOKUP
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getThemeById returns classic for id classic`() {
        val theme = ThemeRegistry.getThemeById("classic")
        assertNotNull(theme)
        assertEquals("classic", theme!!.id)
        assertEquals("Classic", theme.displayName)
    }

    @Test
    fun `getThemeById returns null for unknown id`() {
        val theme = ThemeRegistry.getThemeById("nonexistent_theme")
        assertNull(theme)
    }

    @Test
    fun `getThemeById returns neon_nights for vip theme`() {
        val theme = ThemeRegistry.getThemeById("neon_nights")
        assertNotNull(theme)
        assertEquals(ThemeCategory.VIP, theme!!.category)
    }

    @Test
    fun `getThemeById returns seasonal theme by id`() {
        val theme = ThemeRegistry.getThemeById("seasonal_halloween")
        assertNotNull(theme)
        assertEquals(ThemeCategory.SEASONAL, theme!!.category)
        assertEquals("Halloween", theme.displayName)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. THEME CATEGORY GROUPING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `FREE_THEMES has exactly 3 themes`() {
        assertEquals(3, ThemeRegistry.FREE_THEMES.size)
        assertTrue(ThemeRegistry.FREE_THEMES.all { it.category == ThemeCategory.FREE })
    }

    @Test
    fun `VIP_THEMES has exactly 5 themes`() {
        assertEquals(5, ThemeRegistry.VIP_THEMES.size)
        assertTrue(ThemeRegistry.VIP_THEMES.all { it.category == ThemeCategory.VIP })
    }

    @Test
    fun `SEASONAL_THEMES has exactly 6 themes`() {
        assertEquals(6, ThemeRegistry.SEASONAL_THEMES.size)
        assertTrue(ThemeRegistry.SEASONAL_THEMES.all { it.category == ThemeCategory.SEASONAL })
    }

    @Test
    fun `ALL_THEMES has 14 total themes`() {
        assertEquals(14, ThemeRegistry.ALL_THEMES.size)
    }

    @Test
    fun `all theme IDs are unique`() {
        val ids = ThemeRegistry.ALL_THEMES.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. THEME PRICING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `free themes have zero diamond cost`() {
        ThemeRegistry.FREE_THEMES.forEach { theme ->
            assertEquals("Free theme ${theme.id} should cost 0", 0, theme.diamondCost)
        }
    }

    @Test
    fun `VIP themes cost 50 diamonds each`() {
        ThemeRegistry.VIP_THEMES.forEach { theme ->
            assertEquals("VIP theme ${theme.id} should cost 50", 50, theme.diamondCost)
        }
    }

    @Test
    fun `seasonal themes cost 30 diamonds each`() {
        ThemeRegistry.SEASONAL_THEMES.forEach { theme ->
            assertEquals("Seasonal theme ${theme.id} should cost 30", 30, theme.diamondCost)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. THEME STRUCTURE — REQUIRED FIELDS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `all themes have non-blank display names`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertTrue("Theme ${theme.id} has blank displayName", theme.displayName.isNotBlank())
        }
    }

    @Test
    fun `all themes have non-blank descriptions`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertTrue("Theme ${theme.id} has blank description", theme.description.isNotBlank())
        }
    }

    @Test
    fun `all themes have non-blank emojis`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertTrue("Theme ${theme.id} has blank emoji", theme.emoji.isNotBlank())
        }
    }

    @Test
    fun `all themes have valid tile colors set`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotEquals("${theme.id} tileCorrect", Color.Unspecified, theme.tileCorrect)
            assertNotEquals("${theme.id} tilePresent", Color.Unspecified, theme.tilePresent)
            assertNotEquals("${theme.id} tileAbsent", Color.Unspecified, theme.tileAbsent)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. BACKGROUND PATTERNS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `BackgroundPattern enum has all expected values`() {
        val patterns = BackgroundPattern.values()
        assertTrue(patterns.contains(BackgroundPattern.NONE))
        assertTrue(patterns.contains(BackgroundPattern.DOTS))
        assertTrue(patterns.contains(BackgroundPattern.WAVES))
        assertTrue(patterns.contains(BackgroundPattern.STARS))
        assertTrue(patterns.contains(BackgroundPattern.SNOWFLAKES))
        assertTrue(patterns.contains(BackgroundPattern.HEARTS))
        assertTrue(patterns.contains(BackgroundPattern.LEAVES))
        assertTrue(patterns.contains(BackgroundPattern.DIAMONDS))
        assertTrue(patterns.contains(BackgroundPattern.GRID))
        assertEquals(9, patterns.size)
    }

    @Test
    fun `classic theme has DOTS pattern`() {
        assertEquals(BackgroundPattern.DOTS, ThemeRegistry.CLASSIC.backgroundPattern)
    }

    @Test
    fun `ocean breeze has WAVES pattern`() {
        assertEquals(BackgroundPattern.WAVES, ThemeRegistry.OCEAN_BREEZE.backgroundPattern)
    }

    @Test
    fun `neon nights has GRID pattern`() {
        assertEquals(BackgroundPattern.GRID, ThemeRegistry.NEON_NIGHTS.backgroundPattern)
    }

    @Test
    fun `arctic frost has SNOWFLAKES pattern`() {
        assertEquals(BackgroundPattern.SNOWFLAKES, ThemeRegistry.ARCTIC_FROST.backgroundPattern)
    }

    @Test
    fun `halloween has STARS pattern`() {
        assertEquals(BackgroundPattern.STARS, ThemeRegistry.HALLOWEEN.backgroundPattern)
    }

    @Test
    fun `christmas has SNOWFLAKES pattern`() {
        assertEquals(BackgroundPattern.SNOWFLAKES, ThemeRegistry.CHRISTMAS.backgroundPattern)
    }

    @Test
    fun `all themes have a background pattern assigned`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotNull("Theme ${theme.id} has null pattern", theme.backgroundPattern)
        }
    }

    @Test
    fun `all themes with non-NONE pattern have a patternEmoji`() {
        ThemeRegistry.ALL_THEMES
            .filter { it.backgroundPattern != BackgroundPattern.NONE }
            .forEach { theme ->
                assertTrue(
                    "Theme ${theme.id} has pattern but no emoji",
                    theme.patternEmoji.isNotBlank()
                )
            }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. FONT ASSIGNMENTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `classic theme uses default font`() {
        assertEquals(FontFamily.Default, ThemeRegistry.CLASSIC.fontFamily)
    }

    @Test
    fun `forest grove uses serif font`() {
        assertEquals(FontFamily.Serif, ThemeRegistry.FOREST_GROVE.fontFamily)
    }

    @Test
    fun `neon nights uses monospace font`() {
        assertEquals(FontFamily.Monospace, ThemeRegistry.NEON_NIGHTS.fontFamily)
    }

    @Test
    fun `cherry blossom uses cursive font`() {
        assertEquals(FontFamily.Cursive, ThemeRegistry.CHERRY_BLOSSOM.fontFamily)
    }

    @Test
    fun `royal gold uses serif font`() {
        assertEquals(FontFamily.Serif, ThemeRegistry.ROYAL_GOLD.fontFamily)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. THEME COLOR DISTINCTNESS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `tileCorrect differs from tilePresent in all themes`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotEquals(
                "Theme ${theme.id}: correct/present tiles should differ",
                theme.tileCorrect,
                theme.tilePresent
            )
        }
    }

    @Test
    fun `tileCorrect differs from tileAbsent in all themes`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotEquals(
                "Theme ${theme.id}: correct/absent tiles should differ",
                theme.tileCorrect,
                theme.tileAbsent
            )
        }
    }

    @Test
    fun `tilePresent differs from tileAbsent in all themes`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotEquals(
                "Theme ${theme.id}: present/absent tiles should differ",
                theme.tilePresent,
                theme.tileAbsent
            )
        }
    }

    @Test
    fun `backgroundDark differs from backgroundLight in all themes`() {
        ThemeRegistry.ALL_THEMES.forEach { theme ->
            assertNotEquals(
                "Theme ${theme.id}: dark/light backgrounds should differ",
                theme.backgroundDark,
                theme.backgroundLight
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. THEME CATEGORY ENUM
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ThemeCategory has 3 values`() {
        assertEquals(3, ThemeCategory.values().size)
    }

    @Test
    fun `ThemeCategory contains FREE VIP SEASONAL`() {
        val values = ThemeCategory.values().map { it.name }.toSet()
        assertTrue(values.contains("FREE"))
        assertTrue(values.contains("VIP"))
        assertTrue(values.contains("SEASONAL"))
    }
}
