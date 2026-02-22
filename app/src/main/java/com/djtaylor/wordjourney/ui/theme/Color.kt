package com.djtaylor.wordjourney.ui.theme

import androidx.compose.ui.graphics.Color

// ── Tile states (dark mode) ───────────────────────────────────────────────────
val TileCorrect   = Color(0xFF538D4E)    // green
val TilePresent   = Color(0xFFC9A84C)    // gold/yellow
val TileAbsent    = Color(0xFF555759)    // dark grey
val TileEmpty     = Color(0xFF121213)
val TileFilled    = Color(0xFF1C1B1F)

// ── Tile states (light mode) ─────────────────────────────────────────────────
val TileEmptyLight   = Color(0xFFFFFFFF)    // white
val TileFilledLight  = Color(0xFFF5F0E8)    // warm off-white

// High-contrast alternatives
val TileCorrectHC = Color(0xFFF5793A)   // orange
val TilePresentHC = Color(0xFF85C0F9)   // blue

// Tile borders (dark mode)
val TileBorderEmpty  = Color(0xFF3A3A3C)
val TileBorderFilled = Color(0xFF565758)

// Tile borders (light mode)
val TileBorderEmptyLight  = Color(0xFFD3CFC5)
val TileBorderFilledLight = Color(0xFFA9A49C)

// ── Difficulty accents ────────────────────────────────────────────────────────
val AccentEasy    = Color(0xFF2DD4BF)   // teal
val AccentRegular = Color(0xFFF59E0B)   // warm gold
val AccentHard    = Color(0xFFEF4444)   // crimson

// ── Currency ──────────────────────────────────────────────────────────────────
val CoinGold      = Color(0xFFF59E0B)
val CoinGoldDark  = Color(0xFFB8760A)   // darker for light-mode readability
val DiamondCyan   = Color(0xFF67E8F9)
val DiamondCyanDark = Color(0xFF0891B2) // darker for light-mode readability
val HeartRed      = Color(0xFFEF4444)
val BonusHeartBlue = Color(0xFF3B82F6)

// ── Dark adventure-map theme ──────────────────────────────────────────────────
val BackgroundDark        = Color(0xFF1C1610)
val SurfaceDark           = Color(0xFF2A2117)
val SurfaceVariantDark    = Color(0xFF352B1E)
val OnSurfaceDark         = Color(0xFFE8DCC8)
val OnBackgroundDark      = Color(0xFFF0E8D4)

// ── Light parchment theme ─────────────────────────────────────────────────────
val BackgroundLight       = Color(0xFFFAF0E0)
val SurfaceLight          = Color(0xFFFFF8EC)
val SurfaceVariantLight   = Color(0xFFF0E4C8)
val OnSurfaceLight        = Color(0xFF2A1F0E)
val OnBackgroundLight     = Color(0xFF1C150A)

// ── Keyboard ──────────────────────────────────────────────────────────────────
val KeyDefaultDark   = Color(0xFF818384)
val KeyDefaultLight  = Color(0xFFD3D6DA)
val KeyTextDark      = Color(0xFFFFFFFF)
val KeyTextLight     = Color(0xFF1A1A1B)

// ── Primary ───────────────────────────────────────────────────────────────────
val Primary          = Color(0xFFC9A84C)
val OnPrimary        = Color(0xFF1C1610)
val PrimaryContainer = Color(0xFF352B1E)
val OnPrimaryContainer = Color(0xFFFFE5A0)

val Error         = Color(0xFFEF4444)
val OnError       = Color(0xFFFFFFFF)
