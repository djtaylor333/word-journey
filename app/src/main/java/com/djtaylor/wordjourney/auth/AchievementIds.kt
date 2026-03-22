package com.djtaylor.wordjourney.auth

/**
 * Google Play Games achievement IDs.
 *
 * These IDs are created in Play Console → Play Games Services → Achievements.
 * After running scripts/setup_play_achievements.py the IDs will be filled in automatically,
 * or you can copy them manually from the Play Console.
 *
 * Format: "CgkI<base64>" — base-64 encoded string from Play Console.
 * Placeholder IDs (starting with "achievement_") are silently skipped at runtime
 * until replaced with real Play Console IDs.
 *
 * Total achievements: 31 (covers all major game areas)
 */
object AchievementIds {

    // ── First steps ───────────────────────────────────────────────────────────
    const val FIRST_WIN             = "CgkIlIWlqvkfEAIQAg"

    // ── Win count milestones (incremental) ────────────────────────────────────
    const val WIN_10                = "CgkIlIWlqvkfEAIQAw"
    const val WIN_50                = "CgkIlIWlqvkfEAIQDw"
    const val WIN_100               = "CgkIlIWlqvkfEAIQEA"
    const val WIN_250               = "CgkIlIWlqvkfEAIQEQ"
    const val WIN_500               = "CgkIlIWlqvkfEAIQEg"

    // ── Skill ─────────────────────────────────────────────────────────────────
    const val FIRST_GUESS_WIN       = "CgkIlIWlqvkfEAIQFA"
    const val TWO_GUESS_WIN         = "CgkIlIWlqvkfEAIQFQ"
    const val NO_POWERUP_WIN        = "CgkIlIWlqvkfEAIQFg"
    const val LAST_GUESS_WIN        = "CgkIlIWlqvkfEAIQFw"

    // ── Daily challenge streaks ───────────────────────────────────────────────
    const val STREAK_3              = "CgkIlIWlqvkfEAIQBA"
    const val STREAK_7              = "CgkIlIWlqvkfEAIQGA"
    const val STREAK_14             = "CgkIlIWlqvkfEAIQGQ"
    const val STREAK_30             = "CgkIlIWlqvkfEAIQGg"

    // ── Login streaks ─────────────────────────────────────────────────────────
    const val LOGIN_STREAK_7        = "CgkIlIWlqvkfEAIQGw"
    const val LOGIN_STREAK_30       = "CgkIlIWlqvkfEAIQHA"

    // ── Level pack progress ───────────────────────────────────────────────────
    const val REACH_LEVEL_10        = "CgkIlIWlqvkfEAIQBQ"
    const val REACH_LEVEL_25        = "CgkIlIWlqvkfEAIQHQ"
    const val REACH_LEVEL_50        = "CgkIlIWlqvkfEAIQHg"
    const val PACK_MASTER           = "CgkIlIWlqvkfEAIQHw"

    // ── Seasonal packs ────────────────────────────────────────────────────────
    const val SEASONAL_CHAMPION     = "CgkIlIWlqvkfEAIQIA"

    // ── Daily challenge volume ────────────────────────────────────────────────
    const val FIRST_DAILY           = "CgkIlIWlqvkfEAIQBg"
    const val DAILY_10              = "CgkIlIWlqvkfEAIQIQ"
    const val DAILY_100             = "CgkIlIWlqvkfEAIQIg"

    // ── Power-up / item usage ─────────────────────────────────────────────────
    const val FIRST_ITEM_USED       = "CgkIlIWlqvkfEAIQBw"
    const val ITEMS_USED_50         = "CgkIlIWlqvkfEAIQIw"

    // ── Economy ──────────────────────────────────────────────────────────────
    const val COIN_EARNER_10000     = "CgkIlIWlqvkfEAIQJA"

    // ── Ad rewards ───────────────────────────────────────────────────────────
    const val FIRST_AD_WATCHED      = "CgkIlIWlqvkfEAIQCA"

    // ── In-app purchases ─────────────────────────────────────────────────────
    const val FIRST_PURCHASE        = "CgkIlIWlqvkfEAIQCQ"
    const val BUNDLE_BUYER          = "CgkIlIWlqvkfEAIQJQ"

    // ── VIP ───────────────────────────────────────────────────────────────────
    const val VIP_SUBSCRIBER        = "CgkIlIWlqvkfEAIQJg"
}
