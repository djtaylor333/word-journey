package com.djtaylor.wordjourney.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Background decoration pattern drawn subtly behind game content.
 */
enum class BackgroundPattern {
    NONE, DOTS, WAVES, STARS, SNOWFLAKES, HEARTS, LEAVES, DIAMONDS, GRID
}

/**
 * Defines a complete visual theme for the game.
 * Each theme changes background, tile, keyboard, and accent colors,
 * plus optional font family and background decorations.
 */
data class GameTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String,
    val category: ThemeCategory,

    // Background
    val backgroundDark: Color,
    val backgroundLight: Color,
    val surfaceDark: Color,
    val surfaceLight: Color,

    // Tiles
    val tileCorrect: Color,
    val tilePresent: Color,
    val tileAbsent: Color,
    val tileEmpty: Color,
    val tileFilled: Color,

    // Keyboard
    val keyDefault: Color,
    val keyText: Color,

    // Accents
    val primaryAccent: Color,
    val coinColor: Color = Color(0xFFF59E0B),
    val diamondColor: Color = Color(0xFF67E8F9),

    // Audio identifiers (for future use)
    val musicTrack: String = "music_theme",
    val sfxPack: String = "default",

    // Cost (0 = free, -1 = VIP only, >0 = diamond cost)
    val diamondCost: Int = 0,

    // Background decoration
    val backgroundPattern: BackgroundPattern = BackgroundPattern.NONE,
    val patternEmoji: String = "",

    // Gradient overlay colours (top, mid, bottom) for rich background visuals
    val gradientTop: Color = Color.Transparent,
    val gradientMid: Color = Color.Transparent,
    val gradientBottom: Color = Color.Transparent,

    // Font family
    val fontFamily: FontFamily = FontFamily.Default
)

enum class ThemeCategory {
    FREE,       // 3 free themes
    VIP,        // 5 VIP-exclusive themes
    SEASONAL    // Seasonal event themes
}

/**
 * Registry of all available themes.
 */
object ThemeRegistry {

    // ═══════════════════════════════════════════════════════════════════════════
    // FREE THEMES (3) — available to everyone
    // ═══════════════════════════════════════════════════════════════════════════

    val CLASSIC = GameTheme(
        id = "classic",
        displayName = "Classic",
        description = "The original adventure-map theme",
        emoji = "🗺️",
        category = ThemeCategory.FREE,
        backgroundDark = Color(0xFF1C1610),
        backgroundLight = Color(0xFFFAF0E0),
        surfaceDark = Color(0xFF2A2117),
        surfaceLight = Color(0xFFFFF8EC),
        tileCorrect = Color(0xFF538D4E),
        tilePresent = Color(0xFFC9A84C),
        tileAbsent = Color(0xFF555759),
        tileEmpty = Color(0xFF121213),
        tileFilled = Color(0xFF1C1B1F),
        keyDefault = Color(0xFF818384),
        keyText = Color(0xFFFFFFFF),
        primaryAccent = Color(0xFFC9A84C),
        backgroundPattern = BackgroundPattern.DOTS,
        patternEmoji = "🗺️",
        gradientTop = Color(0x0AC9A84C),
        gradientMid = Color(0x05D4A843),
        gradientBottom = Color(0x0ABFA240)
    )

    val OCEAN_BREEZE = GameTheme(
        id = "ocean_breeze",
        displayName = "Ocean Breeze",
        description = "Cool blues and seafoam greens — ride the wave!",
        emoji = "🌊",
        category = ThemeCategory.FREE,
        backgroundDark = Color(0xFF0A1628),
        backgroundLight = Color(0xFFE8F4F8),
        surfaceDark = Color(0xFF122240),
        surfaceLight = Color(0xFFF0FAFF),
        tileCorrect = Color(0xFF00B894),
        tilePresent = Color(0xFF0984E3),
        tileAbsent = Color(0xFF2D3436),
        tileEmpty = Color(0xFF0D1B2A),
        tileFilled = Color(0xFF1B2838),
        keyDefault = Color(0xFF4A6FA5),
        keyText = Color(0xFFE0F7FA),
        primaryAccent = Color(0xFF00CEC9),
        musicTrack = "music_ocean",
        backgroundPattern = BackgroundPattern.WAVES,
        patternEmoji = "🌊",
        gradientTop = Color(0x1A006994),
        gradientMid = Color(0x0800B4D8),
        gradientBottom = Color(0x1A00CEC9)
    )

    val FOREST_GROVE = GameTheme(
        id = "forest_grove",
        displayName = "Forest Grove",
        description = "Earthy greens and warm browns — peaceful woodland",
        emoji = "🌲",
        category = ThemeCategory.FREE,
        backgroundDark = Color(0xFF0D1F0D),
        backgroundLight = Color(0xFFF0F5E8),
        surfaceDark = Color(0xFF1A3318),
        surfaceLight = Color(0xFFF8FCF0),
        tileCorrect = Color(0xFF2ECC40),
        tilePresent = Color(0xFFFF851B),
        tileAbsent = Color(0xFF3D3D3D),
        tileEmpty = Color(0xFF0A1A0A),
        tileFilled = Color(0xFF1A2A1A),
        keyDefault = Color(0xFF5B8C5A),
        keyText = Color(0xFFF0FFE8),
        primaryAccent = Color(0xFF7BC67E),
        musicTrack = "music_forest",
        backgroundPattern = BackgroundPattern.LEAVES,
        patternEmoji = "🌿",
        gradientTop = Color(0x1A2E8B57),
        gradientMid = Color(0x08228B22),
        gradientBottom = Color(0x1A3CB371),
        fontFamily = FontFamily.Serif
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // VIP THEMES (5) — purchasable with diamonds, owned forever
    // ═══════════════════════════════════════════════════════════════════════════

    val NEON_NIGHTS = GameTheme(
        id = "neon_nights",
        displayName = "Neon Nights",
        description = "Cyberpunk glow with electric neon vibes",
        emoji = "🌃",
        category = ThemeCategory.VIP,
        backgroundDark = Color(0xFF0D0221),
        backgroundLight = Color(0xFFF0E6FF),
        surfaceDark = Color(0xFF190535),
        surfaceLight = Color(0xFFF8F0FF),
        tileCorrect = Color(0xFF39FF14),
        tilePresent = Color(0xFFFF00FF),
        tileAbsent = Color(0xFF2A0845),
        tileEmpty = Color(0xFF0A0015),
        tileFilled = Color(0xFF1A0A30),
        keyDefault = Color(0xFF6B2FA0),
        keyText = Color(0xFFE0FFE0),
        primaryAccent = Color(0xFFFF6EC7),
        diamondCost = 50,
        musicTrack = "music_neon",
        backgroundPattern = BackgroundPattern.GRID,
        patternEmoji = "⚡",
        gradientTop = Color(0x1AFF00FF),
        gradientMid = Color(0x0839FF14),
        gradientBottom = Color(0x1AFF6EC7),
        fontFamily = FontFamily.Monospace
    )

    val ROYAL_GOLD = GameTheme(
        id = "royal_gold",
        displayName = "Royal Gold",
        description = "Regal purple and gold — fit for royalty",
        emoji = "👑",
        category = ThemeCategory.VIP,
        backgroundDark = Color(0xFF1A0A2A),
        backgroundLight = Color(0xFFFFF8E1),
        surfaceDark = Color(0xFF2A1545),
        surfaceLight = Color(0xFFFFFBF0),
        tileCorrect = Color(0xFFFFD700),
        tilePresent = Color(0xFFDA70D6),
        tileAbsent = Color(0xFF4A3560),
        tileEmpty = Color(0xFF140820),
        tileFilled = Color(0xFF241535),
        keyDefault = Color(0xFF8B6FC0),
        keyText = Color(0xFFFFE8CC),
        primaryAccent = Color(0xFFFFD700),
        diamondCost = 50,
        musicTrack = "music_royal",
        backgroundPattern = BackgroundPattern.DIAMONDS,
        patternEmoji = "💎",
        gradientTop = Color(0x1AFFD700),
        gradientMid = Color(0x08DA70D6),
        gradientBottom = Color(0x1AFFD700),
        fontFamily = FontFamily.Serif
    )

    val SUNSET_GLOW = GameTheme(
        id = "sunset_glow",
        displayName = "Sunset Glow",
        description = "Warm oranges and pinks — golden hour beauty",
        emoji = "🌅",
        category = ThemeCategory.VIP,
        backgroundDark = Color(0xFF1A0A0A),
        backgroundLight = Color(0xFFFFF0E8),
        surfaceDark = Color(0xFF2A1510),
        surfaceLight = Color(0xFFFFF8F0),
        tileCorrect = Color(0xFFFF6B35),
        tilePresent = Color(0xFFFF1493),
        tileAbsent = Color(0xFF4A2A2A),
        tileEmpty = Color(0xFF140808),
        tileFilled = Color(0xFF2A1515),
        keyDefault = Color(0xFFB8604A),
        keyText = Color(0xFFFFF0E0),
        primaryAccent = Color(0xFFFF8C42),
        diamondCost = 50,
        musicTrack = "music_sunset",
        backgroundPattern = BackgroundPattern.DOTS,
        patternEmoji = "✨",
        gradientTop = Color(0x1AFF6B35),
        gradientMid = Color(0x08FF8C42),
        gradientBottom = Color(0x1AFF1493)
    )

    val ARCTIC_FROST = GameTheme(
        id = "arctic_frost",
        displayName = "Arctic Frost",
        description = "Icy blues and crystalline whites — frozen tundra",
        emoji = "❄️",
        category = ThemeCategory.VIP,
        backgroundDark = Color(0xFF0A1520),
        backgroundLight = Color(0xFFE8F0FA),
        surfaceDark = Color(0xFF152535),
        surfaceLight = Color(0xFFF0F8FF),
        tileCorrect = Color(0xFF4FC3F7),
        tilePresent = Color(0xFFB39DDB),
        tileAbsent = Color(0xFF37474F),
        tileEmpty = Color(0xFF0A1018),
        tileFilled = Color(0xFF1A2530),
        keyDefault = Color(0xFF546E7A),
        keyText = Color(0xFFE0F7FA),
        primaryAccent = Color(0xFF80DEEA),
        diamondCost = 50,
        musicTrack = "music_arctic",
        backgroundPattern = BackgroundPattern.SNOWFLAKES,
        patternEmoji = "❄️",
        gradientTop = Color(0x1A80DEEA),
        gradientMid = Color(0x084FC3F7),
        gradientBottom = Color(0x1AB39DDB)
    )

    val CHERRY_BLOSSOM = GameTheme(
        id = "cherry_blossom",
        displayName = "Cherry Blossom",
        description = "Soft pinks and gentle petals — spring in Japan",
        emoji = "🌸",
        category = ThemeCategory.VIP,
        backgroundDark = Color(0xFF1A0A14),
        backgroundLight = Color(0xFFFFF0F5),
        surfaceDark = Color(0xFF2A1524),
        surfaceLight = Color(0xFFFFF5F8),
        tileCorrect = Color(0xFFFF69B4),
        tilePresent = Color(0xFFDDA0DD),
        tileAbsent = Color(0xFF4A3040),
        tileEmpty = Color(0xFF140810),
        tileFilled = Color(0xFF241520),
        keyDefault = Color(0xFF9E6B8A),
        keyText = Color(0xFFFFF0F5),
        primaryAccent = Color(0xFFFF69B4),
        diamondCost = 50,
        musicTrack = "music_cherry",
        backgroundPattern = BackgroundPattern.HEARTS,
        patternEmoji = "🌸",
        gradientTop = Color(0x1AFF69B4),
        gradientMid = Color(0x08DDA0DD),
        gradientBottom = Color(0x1AFF69B4),
        fontFamily = FontFamily.Cursive
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // SEASONAL THEMES (6) — available during season or purchasable after
    // ═══════════════════════════════════════════════════════════════════════════

    val VALENTINES = GameTheme(
        id = "seasonal_valentines",
        displayName = "Valentine's Day",
        description = "Love is in the air — hearts and roses",
        emoji = "💕",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF2A0A14),
        backgroundLight = Color(0xFFFFF0F3),
        surfaceDark = Color(0xFF3A1525),
        surfaceLight = Color(0xFFFFF5F7),
        tileCorrect = Color(0xFFE91E63),
        tilePresent = Color(0xFFFF80AB),
        tileAbsent = Color(0xFF4A2030),
        tileEmpty = Color(0xFF1A0810),
        tileFilled = Color(0xFF2A1520),
        keyDefault = Color(0xFFC06080),
        keyText = Color(0xFFFFE0E8),
        primaryAccent = Color(0xFFFF4081),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.HEARTS,
        patternEmoji = "💕",
        gradientTop = Color(0x1AE91E63),
        gradientMid = Color(0x08FF80AB),
        gradientBottom = Color(0x1AFF4081)
    )

    val EASTER = GameTheme(
        id = "seasonal_easter",
        displayName = "Easter",
        description = "Pastel eggs and spring joy",
        emoji = "🐣",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF1A1A28),
        backgroundLight = Color(0xFFFFF8F0),
        surfaceDark = Color(0xFF2A2A3D),
        surfaceLight = Color(0xFFFFFCF5),
        tileCorrect = Color(0xFFAED581),
        tilePresent = Color(0xFFCE93D8),
        tileAbsent = Color(0xFF424242),
        tileEmpty = Color(0xFF121218),
        tileFilled = Color(0xFF1E1E28),
        keyDefault = Color(0xFF7986CB),
        keyText = Color(0xFFF8FFF0),
        primaryAccent = Color(0xFFE6EE9C),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.DOTS,
        patternEmoji = "🥚",
        gradientTop = Color(0x1AAED581),
        gradientMid = Color(0x08CE93D8),
        gradientBottom = Color(0x1AE6EE9C)
    )

    val SUMMER = GameTheme(
        id = "seasonal_summer",
        displayName = "Summer Fun",
        description = "Beach vibes and tropical colors",
        emoji = "☀️",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF1A1A05),
        backgroundLight = Color(0xFFFFFDE0),
        surfaceDark = Color(0xFF2A2A10),
        surfaceLight = Color(0xFFFFFFF0),
        tileCorrect = Color(0xFFFFEB3B),
        tilePresent = Color(0xFF4DD0E1),
        tileAbsent = Color(0xFF5D4037),
        tileEmpty = Color(0xFF141405),
        tileFilled = Color(0xFF242410),
        keyDefault = Color(0xFFA1887F),
        keyText = Color(0xFFFFFFF0),
        primaryAccent = Color(0xFFFFD54F),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.WAVES,
        patternEmoji = "☀️",
        gradientTop = Color(0x1AFFEB3B),
        gradientMid = Color(0x084DD0E1),
        gradientBottom = Color(0x1AFFD54F)
    )

    val HALLOWEEN = GameTheme(
        id = "seasonal_halloween",
        displayName = "Halloween",
        description = "Spooky season — pumpkins and cobwebs",
        emoji = "🎃",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF0A0A0A),
        backgroundLight = Color(0xFFFFF3E0),
        surfaceDark = Color(0xFF1A1008),
        surfaceLight = Color(0xFFFFF8EE),
        tileCorrect = Color(0xFFFF6D00),
        tilePresent = Color(0xFF9C27B0),
        tileAbsent = Color(0xFF1B1B1B),
        tileEmpty = Color(0xFF050505),
        tileFilled = Color(0xFF151008),
        keyDefault = Color(0xFF6D4C41),
        keyText = Color(0xFFFFE0B2),
        primaryAccent = Color(0xFFFF9800),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.STARS,
        patternEmoji = "🦇",
        gradientTop = Color(0x1AFF6D00),
        gradientMid = Color(0x089C27B0),
        gradientBottom = Color(0x1AFF9800)
    )

    val THANKSGIVING = GameTheme(
        id = "seasonal_thanksgiving",
        displayName = "Thanksgiving",
        description = "Harvest warmth — amber and crimson leaves",
        emoji = "🦃",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF1A0F05),
        backgroundLight = Color(0xFFFFF5E5),
        surfaceDark = Color(0xFF2A1A0A),
        surfaceLight = Color(0xFFFFFAF0),
        tileCorrect = Color(0xFFD4A017),
        tilePresent = Color(0xFFC0392B),
        tileAbsent = Color(0xFF4A3520),
        tileEmpty = Color(0xFF140A05),
        tileFilled = Color(0xFF241510),
        keyDefault = Color(0xFF8D6E63),
        keyText = Color(0xFFFFECD2),
        primaryAccent = Color(0xFFE6A817),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.LEAVES,
        patternEmoji = "🍂",
        gradientTop = Color(0x1AD4A017),
        gradientMid = Color(0x08C0392B),
        gradientBottom = Color(0x1AE6A817)
    )

    val CHRISTMAS = GameTheme(
        id = "seasonal_christmas",
        displayName = "Christmas",
        description = "Festive reds, greens, and holiday cheer",
        emoji = "🎄",
        category = ThemeCategory.SEASONAL,
        backgroundDark = Color(0xFF0A1A0A),
        backgroundLight = Color(0xFFF5FFF0),
        surfaceDark = Color(0xFF152A15),
        surfaceLight = Color(0xFFF8FFF5),
        tileCorrect = Color(0xFFC62828),
        tilePresent = Color(0xFF2E7D32),
        tileAbsent = Color(0xFF37474F),
        tileEmpty = Color(0xFF0A140A),
        tileFilled = Color(0xFF152015),
        keyDefault = Color(0xFF558B2F),
        keyText = Color(0xFFF0FFF0),
        primaryAccent = Color(0xFFD32F2F),
        diamondCost = 30,
        backgroundPattern = BackgroundPattern.SNOWFLAKES,
        patternEmoji = "🎄",
        gradientTop = Color(0x1AC62828),
        gradientMid = Color(0x082E7D32),
        gradientBottom = Color(0x1AD32F2F)
    )

    /** All available themes, ordered by category. */
    val ALL_THEMES: List<GameTheme> = listOf(
        CLASSIC, OCEAN_BREEZE, FOREST_GROVE,
        NEON_NIGHTS, ROYAL_GOLD, SUNSET_GLOW, ARCTIC_FROST, CHERRY_BLOSSOM,
        VALENTINES, EASTER, SUMMER, HALLOWEEN, THANKSGIVING, CHRISTMAS
    )

    /** Get a theme by its ID. Returns null if not found. */
    fun getThemeById(id: String): GameTheme? {
        return ALL_THEMES.firstOrNull { it.id == id }
    }

    /** Free themes available to all players. */
    val FREE_THEMES = ALL_THEMES.filter { it.category == ThemeCategory.FREE }

    /** VIP-exclusive themes. */
    val VIP_THEMES = ALL_THEMES.filter { it.category == ThemeCategory.VIP }

    /** Seasonal themes. */
    val SEASONAL_THEMES = ALL_THEMES.filter { it.category == ThemeCategory.SEASONAL }
}
