package com.djtaylor.wordjourney.ui.levelselect

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
// Zone Themes — every 10 levels a new visual world
// ═══════════════════════════════════════════════════════════════════════════════

data class ZoneTheme(
    val name: String,
    val emoji: String,
    val bgTop: Color,
    val bgBottom: Color,
    val pathColor: Color,
    val glow: Color,
    val decor: List<String>
)

/** Standard adventure journey zones (Easy / Regular / Hard / VIP) — 50 zones × 10 levels = 500 */
private val zones = listOf(
    // ── World 1: Beginnings (1–100) ─────────────────────────────────────────
    ZoneTheme("Enchanted Meadow", "🌿",   Color(0xFF1A3D2E), Color(0xFF0D2118), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🌸", "🦋", "🌼", "🐝", "🍀")),
    ZoneTheme("Crystal Cavern",  "💎",    Color(0xFF1A1A3D), Color(0xFF0D0D21), Color(0xFF818CF8), Color(0xFF6366F1), listOf("💎", "✨", "🔮", "⚡", "🌟")),
    ZoneTheme("Sunset Desert",   "🏜️",   Color(0xFF3D2A1A), Color(0xFF211A0D), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("🌵", "🦎", "☀️", "🏜️", "🐪")),
    ZoneTheme("Frozen Peaks",    "🏔️",   Color(0xFF1A2D3D), Color(0xFF0D1821), Color(0xFF67E8F9), Color(0xFF06B6D4), listOf("❄️", "🏔️", "🌨️", "⛷️", "🦌")),
    ZoneTheme("Volcanic Core",   "🌋",    Color(0xFF3D1A1A), Color(0xFF210D0D), Color(0xFFF87171), Color(0xFFEF4444), listOf("🌋", "🔥", "💥", "🪨", "⚡")),
    ZoneTheme("Mystic Forest",   "🌲",    Color(0xFF0D3D2A), Color(0xFF082118), Color(0xFF34D399), Color(0xFF10B981), listOf("🌲", "🍄", "🦉", "🌿", "🦊")),
    ZoneTheme("Starlit Sky",     "🌌",    Color(0xFF1A1A4D), Color(0xFF0D0D28), Color(0xFFC084FC), Color(0xFFA855F7), listOf("⭐", "🌙", "🌌", "💫", "🪐")),
    ZoneTheme("Ocean Depths",    "🌊",    Color(0xFF0D2D3D), Color(0xFF081821), Color(0xFF38BDF8), Color(0xFF0EA5E9), listOf("🌊", "🐠", "🐙", "🐚", "🪸")),
    ZoneTheme("Ancient Ruins",   "🏛️",   Color(0xFF2D2A1A), Color(0xFF181710), Color(0xFFFCD34D), Color(0xFFEAB308), listOf("🏛️", "🗿", "📜", "🏺", "⚱️")),
    ZoneTheme("Dragon's Summit", "🐉",    Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFB7185), Color(0xFFF43F5E), listOf("🐉", "👑", "💎", "🗡️", "🏰")),
    // ── World 2: Wild Frontiers (101–200) ────────────────────────────────────
    ZoneTheme("Jungle Canopy",   "🦜",    Color(0xFF0A2D12), Color(0xFF061A0A), Color(0xFF4ADE80), Color(0xFF16A34A), listOf("🌴", "🦜", "🐍", "🌿", "🦋")),
    ZoneTheme("Thunder Plains",  "⚡",    Color(0xFF18181A), Color(0xFF0E0E10), Color(0xFFFDE047), Color(0xFFCA8A04), listOf("⚡", "🌩️", "🌪️", "💨", "🌧️")),
    ZoneTheme("Coral Reef",      "🪸",    Color(0xFF001E30), Color(0xFF001220), Color(0xFFF472B6), Color(0xFFDB2777), listOf("🪸", "🐡", "🦑", "🐚", "🌊")),
    ZoneTheme("Moonlit Marsh",   "🌙",    Color(0xFF0A0A1E), Color(0xFF060610), Color(0xFFC084FC), Color(0xFF9333EA), listOf("🌙", "🐸", "🌾", "🌫️", "🦉")),
    ZoneTheme("Sky Temple",      "☁️",    Color(0xFF0A1E3D), Color(0xFF061220), Color(0xFFE0F2FE), Color(0xFF7DD3FC), listOf("☁️", "🏛️", "🌤️", "✨", "🕊️")),
    ZoneTheme("Ember Fields",    "🔥",    Color(0xFF2E0D00), Color(0xFF1E0800), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🔥", "🌾", "🌅", "🦊", "✨")),
    ZoneTheme("Mushroom Kingdom","🍄",    Color(0xFF1A0A20), Color(0xFF100614), Color(0xFFF9A8D4), Color(0xFFEC4899), listOf("🍄", "🐛", "🌸", "🌿", "⭐")),
    ZoneTheme("Sapphire Lake",   "💙",    Color(0xFF001E3D), Color(0xFF001228), Color(0xFF38BDF8), Color(0xFF0284C7), listOf("💙", "🐦", "🌊", "🪷", "🌿")),
    ZoneTheme("Iron Citadel",    "⚔️",    Color(0xFF141418), Color(0xFF0C0C10), Color(0xFF94A3B8), Color(0xFF64748B), listOf("⚔️", "🛡️", "🏰", "⚙️", "🗡️")),
    ZoneTheme("Rainbow Valley",  "🌈",    Color(0xFF0E0E1E), Color(0xFF080810), Color(0xFFF472B6), Color(0xFFBA38F8), listOf("🌈", "💫", "🌸", "⭐", "🎨")),
    // ── World 3: Ancient Wonders (201–300) ───────────────────────────────────
    ZoneTheme("Sandstone Maze",  "🏺",    Color(0xFF2E1E00), Color(0xFF1E1400), Color(0xFFFCD34D), Color(0xFFB45309), listOf("🏺", "🗿", "🐊", "🌵", "🌅")),
    ZoneTheme("Blizzard Pass",   "❄️",    Color(0xFF082030), Color(0xFF041420), Color(0xFFBAE6FD), Color(0xFF38BDF8), listOf("❄️", "🌨️", "🏔️", "🐻", "🦢")),
    ZoneTheme("Fairy Glen",      "🧚",    Color(0xFF1E041E), Color(0xFF140010), Color(0xFFF0ABFC), Color(0xFFD946EF), listOf("🧚", "🌸", "🌺", "🍄", "✨")),
    ZoneTheme("Shipwreck Cove",  "⚓",    Color(0xFF001830), Color(0xFF001020), Color(0xFF6EE7B7), Color(0xFF059669), listOf("⚓", "🏴‍☠️", "🌊", "🐚", "🦀")),
    ZoneTheme("Clouded Spire",   "🌫️",   Color(0xFF181820), Color(0xFF101014), Color(0xFFCBD5E1), Color(0xFF94A3B8), listOf("🌫️", "⛅", "🏰", "🕊️", "🌤️")),
    ZoneTheme("Gilded Palace",   "👑",    Color(0xFF201800), Color(0xFF140E00), Color(0xFFFDE68A), Color(0xFFD97706), listOf("👑", "💎", "🏰", "✨", "🎭")),
    ZoneTheme("Dark Labyrinth",  "🕯️",   Color(0xFF120808), Color(0xFF0A0404), Color(0xFFFCA5A5), Color(0xFFDC2626), listOf("🕯️", "🦇", "🕷️", "💀", "🌑")),
    ZoneTheme("Cherry Blossom",  "🌸",    Color(0xFF340E2E), Color(0xFF200820), Color(0xFFFDA4AF), Color(0xFFF472B6), listOf("🌸", "🌺", "🦋", "🌷", "🍃")),
    ZoneTheme("Haunted Hollow",  "👻",    Color(0xFF100820), Color(0xFF080414), Color(0xFFC4B5FD), Color(0xFF7C3AED), listOf("👻", "🦇", "🕯️", "🌑", "🕸️")),
    ZoneTheme("Sunken Temple",   "🐚",    Color(0xFF001E30), Color(0xFF001428), Color(0xFF5EEAD4), Color(0xFF0D9488), listOf("🐚", "🐡", "🌊", "🏛️", "🐠")),
    // ── World 4: Elemental Realms (301–400) ──────────────────────────────────
    ZoneTheme("Wisteria Woods",  "🪻",    Color(0xFF1A0A2E), Color(0xFF100620), Color(0xFFD8B4FE), Color(0xFF9333EA), listOf("🪻", "🌸", "🦋", "🌿", "✨")),
    ZoneTheme("Storm Coast",     "⛈️",    Color(0xFF0A1020), Color(0xFF04080E), Color(0xFF60A5FA), Color(0xFF2563EB), listOf("⛈️", "🌊", "⚡", "🌀", "🦅")),
    ZoneTheme("Crystal Tundra",  "🧊",    Color(0xFF06121A), Color(0xFF040C10), Color(0xFFA5F3FC), Color(0xFF06B6D4), listOf("🧊", "❄️", "🏔️", "🐻‍❄️", "🌨️")),
    ZoneTheme("Ember Lair",      "🐲",    Color(0xFF200800), Color(0xFF140400), Color(0xFFFCA5A5), Color(0xFFDC2626), listOf("🐲", "🔥", "💎", "🌋", "⚡")),
    ZoneTheme("Eternal Garden",  "🌺",    Color(0xFF0C2010), Color(0xFF081408), Color(0xFF86EFAC), Color(0xFF16A34A), listOf("🌺", "🌸", "🦋", "🌿", "🌼")),
    ZoneTheme("Space Station",   "🚀",    Color(0xFF020408), Color(0xFF010204), Color(0xFF818CF8), Color(0xFF4F46E5), listOf("🚀", "⭐", "🌌", "🛸", "🪐")),
    ZoneTheme("Floating Islands","🏝️",   Color(0xFF06162E), Color(0xFF040E1E), Color(0xFF34D399), Color(0xFF059669), listOf("🏝️", "☁️", "🌊", "🌴", "🦋")),
    ZoneTheme("Phoenix Peaks",   "🦅",    Color(0xFF2E0A00), Color(0xFF1E0600), Color(0xFFFDBA74), Color(0xFFEA580C), listOf("🦅", "🔥", "🌅", "🏔️", "✨")),
    ZoneTheme("Shadow Realm",    "🌑",    Color(0xFF04040C), Color(0xFF020206), Color(0xFFA78BFA), Color(0xFF7C3AED), listOf("🌑", "💀", "🕷️", "🌑", "⭐")),
    ZoneTheme("Amber Savanna",   "🦁",    Color(0xFF201600), Color(0xFF141000), Color(0xFFFBD38D), Color(0xFFD97706), listOf("🦁", "🦒", "🌅", "🌾", "🦓")),
    // ── World 5: Legendary Endgame (401–500) ─────────────────────────────────
    ZoneTheme("Frost Cathedral", "🕍",    Color(0xFF061020), Color(0xFF040A14), Color(0xFFE0F2FE), Color(0xFF38BDF8), listOf("🕍", "❄️", "✨", "🕊️", "🌨️")),
    ZoneTheme("Hidden Oasis",    "🌴",    Color(0xFF0A1E0A), Color(0xFF061406), Color(0xFF86EFAC), Color(0xFF16A34A), listOf("🌴", "🌺", "💧", "🦋", "🌸")),
    ZoneTheme("Clockwork City",  "⚙️",    Color(0xFF141012), Color(0xFF0C0A0C), Color(0xFFD1FAE5), Color(0xFF6EE7B7), listOf("⚙️", "🔧", "🏙️", "⌚", "🦾")),
    ZoneTheme("Underwater Cave", "🐠",    Color(0xFF001E28), Color(0xFF001418), Color(0xFF67E8F9), Color(0xFF06B6D4), listOf("🐠", "🐡", "🐙", "🌊", "💎")),
    ZoneTheme("Sky Fortress",    "🏰",    Color(0xFF0A0C18), Color(0xFF060810), Color(0xFFC7D2FE), Color(0xFF818CF8), listOf("🏰", "⭐", "☁️", "🌤️", "🕊️")),
    ZoneTheme("Prism Canyon",    "🌈",    Color(0xFF1A0A1E), Color(0xFF100614), Color(0xFFF9A8D4), Color(0xFFDB2777), listOf("🌈", "💎", "✨", "🌸", "🎨")),
    ZoneTheme("Mystic Swamp",    "🐊",    Color(0xFF0A1A08), Color(0xFF060E04), Color(0xFF6EE7B7), Color(0xFF059669), listOf("🐊", "🐸", "🌿", "🍄", "🌾")),
    ZoneTheme("Titan's Peak",    "🗿",    Color(0xFF181012), Color(0xFF100A0C), Color(0xFFCBD5E1), Color(0xFF94A3B8), listOf("🗿", "⚡", "🏔️", "🌩️", "🦅")),
    ZoneTheme("Celestial Gate",  "✨",    Color(0xFF100A20), Color(0xFF080614), Color(0xFFFDE68A), Color(0xFFD97706), listOf("✨", "⭐", "🌌", "💫", "🌙")),
    ZoneTheme("Eternal Summit",  "⛰️",   Color(0xFF060A12), Color(0xFF04060C), Color(0xFFFBBF24), Color(0xFFD97706), listOf("⛰️", "🌅", "🏆", "✨", "🌟"))
)

// ── Seasonal zone themes ──────────────────────────────────────────────────────
// Each season has 10 uniquely-themed zones replacing the standard adventure zones.

private val easterZones = listOf(
    ZoneTheme("Bunny Meadow",      "🐰",  Color(0xFF2E3D1A), Color(0xFF1A2110), Color(0xFFA3E635), Color(0xFF84CC16), listOf("🌷", "🐰", "🌼", "🥚", "🦋")),
    ZoneTheme("Egg Hunt Garden",   "🥚",  Color(0xFF1E3A2A), Color(0xFF112118), Color(0xFF6EE7B7), Color(0xFF34D399), listOf("🥚", "🌿", "🐣", "🌸", "🍃")),
    ZoneTheme("Chick Parade",      "🐣",  Color(0xFF3D3210), Color(0xFF211808), Color(0xFFFDE68A), Color(0xFFFBBF24), listOf("🐣", "🐥", "☀️", "🌻", "🌈")),
    ZoneTheme("Daisy Fields",      "🌸",  Color(0xFF3D1A2E), Color(0xFF210D18), Color(0xFFF9A8D4), Color(0xFFF472B6), listOf("🌸", "🌺", "🦊", "🐇", "💮")),
    ZoneTheme("Rainbow Bridge",    "🌈",  Color(0xFF1A2D3D), Color(0xFF0D1821), Color(0xFF7DD3FC), Color(0xFF38BDF8), listOf("🌈", "☁️", "🌤️", "🕊️", "🎀")),
    ZoneTheme("Clover Hills",      "🍀",  Color(0xFF0D3D1A), Color(0xFF082110), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🍀", "🐝", "🌾", "🦌", "🎍")),
    ZoneTheme("Blossom Cave",      "🌺",  Color(0xFF3D1A1A), Color(0xFF210D0D), Color(0xFFFCA5A5), Color(0xFFF87171), listOf("🌺", "🌹", "🌸", "🌷", "💐")),
    ZoneTheme("Painted Eggs",      "🎨",  Color(0xFF2A1A3D), Color(0xFF180D21), Color(0xFFC084FC), Color(0xFFA855F7), listOf("🎨", "🥚", "🌟", "🐰", "✨")),
    ZoneTheme("Spring Pond",       "🐸",  Color(0xFF0D2D1A), Color(0xFF081810), Color(0xFF86EFAC), Color(0xFF4ADE80), listOf("🐸", "🌊", "🐟", "🌿", "🍃")),
    ZoneTheme("Easter Sunrise",    "🌅",  Color(0xFF3D2A10), Color(0xFF211808), Color(0xFFFDE68A), Color(0xFFF59E0B), listOf("🌅", "🌈", "🕊️", "💛", "🐣"))
)

private val valentinesZones = listOf(
    ZoneTheme("Rose Garden",       "🌹",  Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFDA4AF), Color(0xFFFB7185), listOf("🌹", "💕", "🦢", "🎀", "💐")),
    ZoneTheme("Love Meadow",       "💕",  Color(0xFF3D1A2E), Color(0xFF210D18), Color(0xFFE879F9), Color(0xFFD946EF), listOf("💕", "🌸", "🦋", "🌺", "🎵")),
    ZoneTheme("Candy Hearts",      "🍬",  Color(0xFF2E1A3D), Color(0xFF180D21), Color(0xFFC084FC), Color(0xFFA855F7), listOf("🍬", "💝", "🫧", "✨", "🌟")),
    ZoneTheme("Lovebird Forest",   "🕊️", Color(0xFF0D3D2A), Color(0xFF082118), Color(0xFF6EE7B7), Color(0xFF34D399), listOf("🕊️", "🌿", "🍃", "💚", "🦚")),
    ZoneTheme("Chocolate Hills",   "🍫",  Color(0xFF3D1A10), Color(0xFF210D08), Color(0xFFFCA5A5), Color(0xFFF87171), listOf("🍫", "🎁", "🎀", "💝", "🌟")),
    ZoneTheme("Starry Romance",    "⭐",  Color(0xFF1A1A3D), Color(0xFF0D0D21), Color(0xFF818CF8), Color(0xFF6366F1), listOf("⭐", "💫", "🌙", "💕", "🌌")),
    ZoneTheme("Picnic Bluffs",     "🧺",  Color(0xFF2E3D1A), Color(0xFF1A2110), Color(0xFFA3E635), Color(0xFF84CC16), listOf("🧺", "🌷", "🍓", "🌼", "🎵")),
    ZoneTheme("Petal Cascade",     "🌸",  Color(0xFF3D2A1A), Color(0xFF211810), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("🌸", "🌺", "💮", "🌼", "🎀")),
    ZoneTheme("Heart Cove",        "💖",  Color(0xFF3D0D2D), Color(0xFF210818), Color(0xFFF9A8D4), Color(0xFFF472B6), listOf("💖", "🌊", "🐚", "🌹", "🫧")),
    ZoneTheme("Valentine Peak",    "💝",  Color(0xFF3D1A1A), Color(0xFF210D0D), Color(0xFFFDA4AF), Color(0xFFE11D48), listOf("💝", "🏔️", "⭐", "🌹", "🎀"))
)

private val summerZones = listOf(
    ZoneTheme("Sunny Beach",       "🏖️", Color(0xFF3D2A0D), Color(0xFF211608), Color(0xFFFDE68A), Color(0xFFFBBF24), listOf("🌊", "🏄", "🐠", "🦀", "🌺")),
    ZoneTheme("Coral Reef",        "🐠",  Color(0xFF0D2D3D), Color(0xFF081821), Color(0xFF67E8F9), Color(0xFF06B6D4), listOf("🐠", "🐡", "🐙", "🪸", "🦈")),
    ZoneTheme("Tropical Forest",   "🌴",  Color(0xFF0D3D1A), Color(0xFF082110), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🌴", "🦜", "🍍", "🌺", "🦋")),
    ZoneTheme("Lemonade Stand",    "🍋",  Color(0xFF3D3210), Color(0xFF211808), Color(0xFFFDE68A), Color(0xFFF59E0B), listOf("🍋", "🍹", "☀️", "🌻", "🍧")),
    ZoneTheme("BBQ Grounds",       "🔥",  Color(0xFF3D1A0D), Color(0xFF210D08), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🔥", "🌽", "🍖", "🎉", "🌶️")),
    ZoneTheme("Waterfall Oasis",   "💦",  Color(0xFF0D2D2D), Color(0xFF081818), Color(0xFF34D399), Color(0xFF10B981), listOf("💦", "🌊", "🦋", "🌿", "🐸")),
    ZoneTheme("Sprinkler Park",    "🌈",  Color(0xFF1A1A3D), Color(0xFF0D0D21), Color(0xFF7DD3FC), Color(0xFF38BDF8), listOf("🌈", "💦", "☀️", "🌻", "🎠")),
    ZoneTheme("Ice Cream Hills",   "🍦",  Color(0xFF3D1A2E), Color(0xFF210D18), Color(0xFFF9A8D4), Color(0xFFF472B6), listOf("🍦", "🍧", "🌸", "🌟", "🎀")),
    ZoneTheme("Festival Grounds",  "🎆",  Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFCA5A5), Color(0xFFF43F5E), listOf("🎆", "🎇", "🎉", "🎵", "✨")),
    ZoneTheme("Sunset Horizon",    "🌅",  Color(0xFF3D2810), Color(0xFF211808), Color(0xFFFBBF24), Color(0xFFD97706), listOf("🌅", "🏄", "🌊", "🐬", "🌠"))
)

private val halloweenZones = listOf(
    ZoneTheme("Haunted Forest",    "🌲",  Color(0xFF1A1A0D), Color(0xFF0D0D08), Color(0xFFBEF264), Color(0xFFA3E635), listOf("🕷️", "🕸️", "🌲", "🦇", "💀")),
    ZoneTheme("Pumpkin Patch",     "🎃",  Color(0xFF3D1A0D), Color(0xFF210D08), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🎃", "🌽", "🍂", "🍁", "🦔")),
    ZoneTheme("Ghost Graveyard",   "👻",  Color(0xFF1A1A2D), Color(0xFF0D0D18), Color(0xFF818CF8), Color(0xFF6366F1), listOf("👻", "💀", "🪦", "🕯️", "🦉")),
    ZoneTheme("Witch's Caldron",   "🧙",  Color(0xFF0D1A0D), Color(0xFF081008), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🧙", "🪄", "🔮", "🌙", "✨")),
    ZoneTheme("Vampire Castle",    "🏰",  Color(0xFF2D0D1A), Color(0xFF180810), Color(0xFFFDA4AF), Color(0xFFF43F5E), listOf("🏰", "🦇", "🩸", "🌙", "🕯️")),
    ZoneTheme("Skull Cavern",      "💀",  Color(0xFF1A0D0D), Color(0xFF100808), Color(0xFFF87171), Color(0xFFEF4444), listOf("💀", "🕯️", "🪦", "⛓️", "🔮")),
    ZoneTheme("Candy Trail",       "🍬",  Color(0xFF3D1A3D), Color(0xFF210D21), Color(0xFFC084FC), Color(0xFFA855F7), listOf("🍬", "🍭", "🎃", "🍫", "🌟")),
    ZoneTheme("Spider Bog",        "🕷️", Color(0xFF0D1A0D), Color(0xFF081008), Color(0xFF86EFAC), Color(0xFF4ADE80), listOf("🕷️", "🕸️", "🐸", "🐍", "🌿")),
    ZoneTheme("Shadow Realm",      "🌑",  Color(0xFF0D0D0D), Color(0xFF080808), Color(0xFF94A3B8), Color(0xFF64748B), listOf("🌑", "⭐", "💫", "🔮", "🌌")),
    ZoneTheme("Halloween Peak",    "🎃",  Color(0xFF3D1A08), Color(0xFF210D04), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("🎃", "🏔️", "🌕", "🦇", "⭐"))
)

private val thanksgivingZones = listOf(
    ZoneTheme("Harvest Fields",    "🌾",  Color(0xFF3D2A0D), Color(0xFF211508), Color(0xFFFDE68A), Color(0xFFFBBF24), listOf("🌾", "🍂", "🌽", "🎑", "🦃")),
    ZoneTheme("Apple Orchard",     "🍎",  Color(0xFF3D1A0D), Color(0xFF210D08), Color(0xFFFCA5A5), Color(0xFFF87171), listOf("🍎", "🍏", "🍂", "🍁", "🌳")),
    ZoneTheme("Pilgrim Trail",     "🗺️", Color(0xFF2A2010), Color(0xFF181208), Color(0xFFFCD34D), Color(0xFFEAB308), listOf("🗺️", "🏕️", "🍂", "🌲", "🔥")),
    ZoneTheme("Golden Meadow",     "🍁",  Color(0xFF3D2810), Color(0xFF211808), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🍁", "🍂", "🌾", "🦔", "🐿️")),
    ZoneTheme("Pumpkin Spice",     "☕",  Color(0xFF2D1810), Color(0xFF180E08), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("☕", "🎃", "🍂", "🌟", "🧁")),
    ZoneTheme("Turkey Valley",     "🦃",  Color(0xFF2A3010), Color(0xFF181A08), Color(0xFFA3E635), Color(0xFF84CC16), listOf("🦃", "🌾", "🍗", "🌽", "🍂")),
    ZoneTheme("Cranberry Bogs",    "🫐",  Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFDA4AF), Color(0xFFF43F5E), listOf("🫐", "🌿", "🍁", "🌊", "🦢")),
    ZoneTheme("Cornucopia Cave",   "🌽",  Color(0xFF3D2A10), Color(0xFF211808), Color(0xFFFBBF24), Color(0xFFD97706), listOf("🌽", "🍎", "🥕", "🎃", "🌾")),
    ZoneTheme("Family Hearth",     "🔥",  Color(0xFF3D1A08), Color(0xFF210D04), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🔥", "🕯️", "🏡", "❤️", "🍂")),
    ZoneTheme("Gratitude Summit",  "🌅",  Color(0xFF3D2A18), Color(0xFF211810), Color(0xFFFDE68A), Color(0xFFF59E0B), listOf("🌅", "🦃", "🌾", "⭐", "🏔️"))
)

private val christmasZones = listOf(
    ZoneTheme("Santa's Village",   "🎅",  Color(0xFF3D0D0D), Color(0xFF210808), Color(0xFFFCA5A5), Color(0xFFF87171), listOf("🎅", "🎁", "🛷", "🦌", "⭐")),
    ZoneTheme("Winter Wonderland", "❄️",  Color(0xFF0D2D3D), Color(0xFF081821), Color(0xFF7DD3FC), Color(0xFF38BDF8), listOf("❄️", "⛄", "🌨️", "🎿", "🦌")),
    ZoneTheme("Gift Grotto",       "🎁",  Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFDA4AF), Color(0xFFF472B6), listOf("🎁", "🎀", "✨", "🌟", "🎊")),
    ZoneTheme("Candy Cane Lane",   "🍬",  Color(0xFF3D1A1A), Color(0xFF210D0D), Color(0xFFFCA5A5), Color(0xFFEF4444), listOf("🍬", "🍭", "❄️", "🎄", "⛄")),
    ZoneTheme("Christmas Forest",  "🎄",  Color(0xFF0D2D10), Color(0xFF081808), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🎄", "⭐", "🌟", "🦌", "🎁")),
    ZoneTheme("Elf Workshop",      "🧝",  Color(0xFF0D3D0D), Color(0xFF082108), Color(0xFF86EFAC), Color(0xFF4ADE80), listOf("🧝", "🔨", "🎁", "⭐", "🎄")),
    ZoneTheme("Frozen Lake",       "🏒",  Color(0xFF0D2A3D), Color(0xFF081821), Color(0xFF67E8F9), Color(0xFF06B6D4), listOf("🏒", "⛸️", "❄️", "🌨️", "⛄")),
    ZoneTheme("Reindeer Run",      "🦌",  Color(0xFF2D2010), Color(0xFF181408), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("🦌", "❄️", "🌟", "🛷", "⭐")),
    ZoneTheme("Fireplace Hollow",  "🔥",  Color(0xFF3D1A08), Color(0xFF210D04), Color(0xFFFB923C), Color(0xFFEA580C), listOf("🔥", "🧦", "🎄", "☕", "🕯️")),
    ZoneTheme("North Pole Peak",   "⭐",  Color(0xFF1A1A3D), Color(0xFF0D0D21), Color(0xFF818CF8), Color(0xFF6366F1), listOf("⭐", "🎄", "❄️", "🎅", "🌟"))
)

/** Returns the zone list for a given seasonal pack key (or the default adventures) */
private fun zonesFor(seasonalPackKey: String?): List<ZoneTheme> = when (seasonalPackKey) {
    "easter"       -> easterZones
    "valentines"   -> valentinesZones
    "summer"       -> summerZones
    "halloween"    -> halloweenZones
    "thanksgiving" -> thanksgivingZones
    "christmas"    -> christmasZones
    else           -> zones
}

/** Returns the zone theme for [level] given the optional seasonal context. */
private fun zoneFor(level: Int, seasonalPackKey: String? = null): ZoneTheme {
    val list = zonesFor(seasonalPackKey)
    return list[((level - 1) / 10).coerceIn(0, list.size - 1)]
}

/** Seasonal accent colours for the screen chrome (TopAppBar, button, etc.) */
private fun seasonalAccent(seasonalPackKey: String?): Color? = when (seasonalPackKey) {
    "easter"       -> Color(0xFF84CC16)
    "valentines"   -> Color(0xFFF472B6)
    "summer"       -> Color(0xFFFBBF24)
    "halloween"    -> Color(0xFFFB923C)
    "thanksgiving" -> Color(0xFFF59E0B)
    "christmas"    -> Color(0xFF4ADE80)
    else           -> null
}

// ═══════════════════════════════════════════════════════════════════════════════
// Main Screen
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    difficultyKey: String,
    onNavigateToGame: (String, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val difficulty = state.difficulty
    val theme = LocalGameTheme.current
    // For seasonal packs use a themed accent colour; otherwise use difficulty colour
    val accent = seasonalAccent(state.seasonalPackKey) ?: when (difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
        Difficulty.VIP     -> CoinGold
    }

    // Heart shrink animation
    var showHeartAnim by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnim) 0.5f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heartScale",
        finishedListener = { if (showHeartAnim) showHeartAnim = false }
    )

    var pendingLevel by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.lifeDeducted) {
        if (state.lifeDeducted) {
            showHeartAnim = true
            delay(500)
            viewModel.resetLifeAnimation()
            if (pendingLevel > 0) {
                onNavigateToGame(difficultyKey, pendingLevel)
                pendingLevel = 0
            }
        }
    }

    // Scroll to current level row on load
    val scrollState = rememberScrollState()
    LaunchedEffect(state.currentLevel, state.isLoading) {
        if (!state.isLoading && state.currentLevel > 3) {
            val rowPx = (state.currentLevel - 1) * 320 // approx 120dp * density
            scrollState.animateScrollTo(rowPx.coerceAtLeast(0))
        }
    }

    // Ambient float for decorations
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "floatPhase"
    )

    val curZone = zoneFor(state.currentLevel, state.seasonalPackKey)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.journeyTitle.ifBlank { "${difficulty.displayName} Journey" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        )
                        Text("${curZone.emoji} ${curZone.name}", fontSize = 14.sp, color = curZone.glow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("❤️", fontSize = 32.sp, modifier = Modifier.scale(heartScale))
                            Text("${state.lives}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(y = 1.dp))
                        }
                        if (state.bonusLives > 0) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BonusHeartBlue, modifier = Modifier.padding(horizontal = 2.dp))
                            Box(contentAlignment = Alignment.Center) {
                                Text("💙", fontSize = 32.sp)
                                Text("${state.bonusLives}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(y = 1.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = curZone.bgTop)
            )
        },
        bottomBar = {
            Surface(color = curZone.bgTop, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.timerDisplayMs > 0L && state.lives + state.bonusLives <= 0) {
                        Text("⏱ Next life in ${fmtTimer(state.timerDisplayMs)}", style = MaterialTheme.typography.bodyMedium, color = accent)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            viewModel.playButtonClick()
                            val lvl = state.currentLevel
                            if (viewModel.canStartLevel(lvl) && viewModel.deductLifeForLevel(lvl)) pendingLevel = lvl
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = state.lives + state.bonusLives > 0
                    ) { Text("Continue — Level ${state.currentLevel}", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ThemeBackgroundOverlay(theme = theme, alpha = 0.15f)
            // VIP golden shimmer overlay
            if (difficultyKey == "vip") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.09f),
                                    Color(0xFFD97706).copy(alpha = 0.13f),
                                    Color(0xFFF59E0B).copy(alpha = 0.09f)
                                )
                            )
                        )
                )
            }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                val total = state.totalLevels
                val zoneCount = (total + 9) / 10
                for (zi in 0 until zoneCount) {
                    val start = zi * 10 + 1
                    val end = minOf(start + 9, total)
                    ZoneSection(
                        zone = zoneFor(start, state.seasonalPackKey),
                        zoneIdx = zi,
                        levels = (start..end).toList(),
                        currentLevel = state.currentLevel,
                        accent = accent,
                        floatPhase = floatPhase,
                        difficultyKey = difficultyKey,
                        starRatings = state.starRatings,
                        onLevelClick = { level ->
                            val completed = level < state.currentLevel
                            val current = level == state.currentLevel
                            if (!completed && !current) return@ZoneSection
                            viewModel.playButtonClick()
                            if (completed) {
                                onNavigateToGame(difficultyKey, level)
                            } else if (viewModel.deductLifeForLevel(level)) {
                                pendingLevel = level
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                // "More levels coming soon" banner after the last zone
                MoreLevelsBanner(accent = accent)
                Spacer(Modifier.height(32.dp))
            }
        }
        } // end Box
    }

    // No-lives dialog
    if (state.showNoLivesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoLivesDialog() },
            title = { Text("Out of Lives!", fontSize = 22.sp) },
            text = {
                Column {
                    Text("You need at least 1 life to start a new level.", fontSize = 17.sp)
                    if (state.timerDisplayMs > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Text("⏱ Next life in ${fmtTimer(state.timerDisplayMs)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.dismissNoLivesDialog() }) { Text("OK", fontSize = 17.sp) } }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Zone Section — one themed block of 10 levels
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ZoneSection(
    zone: ZoneTheme,
    zoneIdx: Int,
    levels: List<Int>,
    currentLevel: Int,
    accent: Color,
    floatPhase: Float,
    difficultyKey: String,
    starRatings: Map<Int, Int>,
    onLevelClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(zone.bgTop, zone.bgBottom)))
                drawSparkles(zone, floatPhase, zoneIdx)
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Zone banner
            ZoneBanner(zone, levels.first(), levels.last())

            // Pathway
            Pathway(levels, currentLevel, zone, accent, floatPhase, starRatings, difficultyKey, onLevelClick)
        }
    }
}

@Composable
private fun ZoneBanner(zone: ZoneTheme, first: Int, last: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = zone.glow.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, zone.glow.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(zone.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(zone.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = zone.glow)
                Text("Levels $first – $last", fontSize = 14.sp, color = zone.glow.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(12.dp))
            Text(zone.emoji, fontSize = 28.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Winding Pathway
// ═══════════════════════════════════════════════════════════════════════════════

private val hPositions = listOf(0.2f, 0.5f, 0.8f, 0.5f) // zigzag x-fractions

@Composable
private fun Pathway(
    levels: List<Int>,
    currentLevel: Int,
    zone: ZoneTheme,
    accent: Color,
    floatPhase: Float,
    starRatings: Map<Int, Int>,
    difficultyKey: String = "",
    onLevelClick: (Int) -> Unit
) {
    val alignments = listOf(Arrangement.Start, Arrangement.Center, Arrangement.End, Arrangement.Center)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawBehind { drawCurvedPath(levels.size, zone, currentLevel, levels.first()) }
    ) {
        for ((idx, level) in levels.withIndex()) {
            val posIdx = idx % alignments.size
            val arr = alignments[posIdx]
            val completed = level < currentLevel
            val current = level == currentLevel
            val locked = level > currentLevel
            val decorEmoji = zone.decor[idx % zone.decor.size]
            val showDecor = idx % 3 == 0

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arr
            ) {
                // Left decor
                if (showDecor && posIdx != 0) {
                    val dy = sin((floatPhase + idx * 0.3f) * 2 * PI.toFloat()) * 6f
                    Text(decorEmoji, fontSize = 22.sp, modifier = Modifier.alpha(0.55f).offset(y = dy.dp))
                    Spacer(Modifier.width(8.dp))
                }
                if (posIdx == 0) Spacer(Modifier.width(24.dp))

                val vipLabel: String? = if (difficultyKey == "vip") {
                    val wl = Difficulty.vipWordLengthForLevel(level)
                    "${wl}L"
                } else null
                LevelNode(level, completed, current, locked, zone, accent, starRatings[level] ?: 0, wordLengthLabel = vipLabel, onLevelClick = { onLevelClick(level) })

                if (posIdx == 2) Spacer(Modifier.width(24.dp))
                // Right decor
                if (showDecor && posIdx != 2) {
                    Spacer(Modifier.width(8.dp))
                    val dy = sin((floatPhase + idx * 0.5f) * 2 * PI.toFloat()) * 6f
                    Text(zone.decor[(idx + 2) % zone.decor.size], fontSize = 22.sp, modifier = Modifier.alpha(0.55f).offset(y = dy.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Level Node
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LevelNode(
    level: Int,
    completed: Boolean,
    current: Boolean,
    locked: Boolean,
    zone: ZoneTheme,
    accent: Color,
    stars: Int,
    wordLengthLabel: String? = null,
    onLevelClick: () -> Unit
) {
    val pulse = if (current) {
        rememberInfiniteTransition(label = "p$level").animateFloat(
            1f, 1.12f,
            infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ps$level"
        ).value
    } else 1f

    val nodeColor = when {
        completed -> zone.glow
        current   -> accent
        else      -> Color.Gray.copy(alpha = 0.35f)
    }
    val glowCol = when {
        current   -> accent.copy(alpha = 0.4f)
        completed -> zone.glow.copy(alpha = 0.2f)
        else      -> Color.Transparent
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(pulse)) {
        // Glow
        if (!locked) {
            Box(
                modifier = Modifier.size(76.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(glowCol, Color.Transparent)))
            )
        }
        // Node circle
        Surface(
            onClick = onLevelClick,
            enabled = !locked,
            shape = CircleShape,
            color = nodeColor,
            border = when {
                current   -> BorderStroke(3.dp, accent)
                completed -> BorderStroke(2.dp, zone.glow.copy(alpha = 0.6f))
                else      -> BorderStroke(2.dp, Color.Gray.copy(alpha = 0.3f))
            },
            shadowElevation = if (current) 8.dp else if (completed) 4.dp else 0.dp,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when {
                        locked -> Text("🔒", fontSize = 20.sp)
                        completed -> {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("$level", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                        else -> Text("$level", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }

        // Star rating for completed levels
        if (completed && stars > 0) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp),
                horizontalArrangement = Arrangement.spacedBy((-2).dp)
            ) {
                for (i in 1..3) {
                    Text(
                        if (i <= stars) "⭐" else "☆",
                        fontSize = if (i <= stars) 11.sp else 10.sp,
                        color = if (i <= stars) Color.Unspecified else Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
        // Current arrow
        if (current) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = if (stars > 0 && completed) 22.dp else 8.dp)) {
                Text("▲", fontSize = 12.sp, color = accent)
            }
        }
        // VIP word length label
        if (wordLengthLabel != null && !locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .background(
                        color = if (current) accent else zone.glow.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = wordLengthLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas draw helpers
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawCurvedPath(
    count: Int,
    zone: ZoneTheme,
    currentLevel: Int,
    firstLevel: Int
) {
    if (count < 2) return
    val rowH = 120.dp.toPx()
    val pts = (0 until count).map { idx ->
        val x = size.width * hPositions[idx % hPositions.size]
        val y = idx * rowH + rowH / 2
        Offset(x, y)
    }

    for (i in 0 until pts.size - 1) {
        val from = pts[i]
        val to = pts[i + 1]
        val level = firstLevel + i
        val isDone = level < currentLevel

        val path = Path().apply {
            moveTo(from.x, from.y)
            val my = (from.y + to.y) / 2
            cubicTo(from.x, my, to.x, my, to.x, to.y)
        }

        if (isDone) {
            // Completed: solid bright + glow
            drawPath(path, zone.pathColor.copy(alpha = 0.6f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, zone.pathColor.copy(alpha = 0.12f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            // Locked: dashed dim
            drawPath(path, zone.pathColor.copy(alpha = 0.15f), style = Stroke(
                width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f))
            ))
        }
    }
}

private fun DrawScope.drawSparkles(zone: ZoneTheme, phase: Float, zoneIdx: Int) {
    for (i in 0 until 14) {
        val seed = (zoneIdx * 100 + i * 37) % 1000
        val bx = (seed * 7 % 1000) / 1000f * size.width
        val by = (seed * 13 % 1000) / 1000f * size.height
        val p = (phase + seed / 1000f) % 1f
        val yo = sin(p * 2 * PI.toFloat()) * 20f
        val a = (sin(p * PI.toFloat()) * 0.3f).coerceIn(0.05f, 0.3f)
        drawCircle(zone.glow.copy(alpha = a), radius = (2 + seed % 3).toFloat().dp.toPx(), center = Offset(bx, by + yo))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════// More Levels Coming Soon banner
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MoreLevelsBanner(accent: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔒", fontSize = 36.sp)
            Text(
                text = "More Adventures Coming Soon!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "You've reached the end of the current journey.\nNew levels are on the way — stay tuned!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Canvas draw helpers
// ══════════════════════════════════════════════════════════════════════════════
private fun fmtTimer(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
