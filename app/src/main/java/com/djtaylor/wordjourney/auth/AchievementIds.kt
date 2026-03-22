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
    /** Unlock after winning the very first puzzle */
    const val FIRST_WIN             = "achievement_first_win"

    // ── Win count milestones (incremental) ────────────────────────────────────
    /** Reach 10 total wins */
    const val WIN_10                = "achievement_win_10"
    /** Reach 50 total wins */
    const val WIN_50                = "achievement_win_50"
    /** Reach 100 total wins */
    const val WIN_100               = "achievement_win_100"
    /** Reach 250 total wins */
    const val WIN_250               = "achievement_win_250"
    /** Reach 500 total wins */
    const val WIN_500               = "achievement_win_500"

    // ── Skill ─────────────────────────────────────────────────────────────────
    /** Win a puzzle with only 1 guess */
    const val FIRST_GUESS_WIN       = "achievement_first_guess_win"
    /** Win a puzzle using 2 or fewer guesses */
    const val TWO_GUESS_WIN         = "achievement_two_guess_win"
    /** Win a puzzle without using any power-up items */
    const val NO_POWERUP_WIN        = "achievement_no_powerup_win"
    /** Win a puzzle using the very last available guess */
    const val LAST_GUESS_WIN        = "achievement_last_guess_win"

    // ── Daily challenge streaks ───────────────────────────────────────────────
    /** 3-day consecutive daily challenge streak */
    const val STREAK_3              = "achievement_streak_3"
    /** 7-day consecutive daily challenge streak */
    const val STREAK_7              = "achievement_streak_7"
    /** 14-day consecutive daily challenge streak */
    const val STREAK_14             = "achievement_streak_14"
    /** 30-day consecutive daily challenge streak */
    const val STREAK_30             = "achievement_streak_30"

    // ── Login streaks ─────────────────────────────────────────────────────────
    /** Log in 7 days in a row */
    const val LOGIN_STREAK_7        = "achievement_login_streak_7"
    /** Log in 30 days in a row */
    const val LOGIN_STREAK_30       = "achievement_login_streak_30"

    // ── Level pack progress ───────────────────────────────────────────────────
    /** Reach level 10 in any regular pack */
    const val REACH_LEVEL_10        = "achievement_reach_level_10"
    /** Reach level 25 in any regular pack */
    const val REACH_LEVEL_25        = "achievement_reach_level_25"
    /** Reach level 50 in any regular pack */
    const val REACH_LEVEL_50        = "achievement_reach_level_50"
    /** Complete all 100 levels in any regular pack */
    const val PACK_MASTER           = "achievement_pack_master"

    // ── Seasonal packs ────────────────────────────────────────────────────────
    /** Win your first level from any seasonal word pack */
    const val SEASONAL_CHAMPION     = "achievement_seasonal_champion"

    // ── Daily challenge volume ────────────────────────────────────────────────
    /** Complete your first daily challenge */
    const val FIRST_DAILY           = "achievement_first_daily"
    /** Complete 10 daily challenges (incremental, 10 steps) */
    const val DAILY_10              = "achievement_daily_10"
    /** Complete 100 daily challenges (incremental, 100 steps) */
    const val DAILY_100             = "achievement_daily_100"

    // ── Power-up / item usage ─────────────────────────────────────────────────
    /** Use a power-up item for the first time */
    const val FIRST_ITEM_USED       = "achievement_first_item_used"
    /** Use 50 power-up items total (incremental, 50 steps) */
    const val ITEMS_USED_50         = "achievement_items_used_50"

    // ── Economy ──────────────────────────────────────────────────────────────
    /** Earn 10,000 total coins (incremental, 10000 steps) */
    const val COIN_EARNER_10000     = "achievement_coin_earner_10000"

    // ── Ad rewards ───────────────────────────────────────────────────────────
    /** Watch a rewarded ad for the first time */
    const val FIRST_AD_WATCHED      = "achievement_first_ad_watched"

    // ── In-app purchases ─────────────────────────────────────────────────────
    /** Make any in-app purchase for the first time */
    const val FIRST_PURCHASE        = "achievement_first_purchase"
    /** Purchase any bundle pack (Starter, Adventurer, or Champion) */
    const val BUNDLE_BUYER          = "achievement_bundle_buyer"

    // ── VIP ───────────────────────────────────────────────────────────────────
    /** Activate any VIP subscription */
    const val VIP_SUBSCRIBER        = "achievement_vip_subscriber"
}
