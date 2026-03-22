package com.djtaylor.wordjourney.auth

/**
 * Google Play Games achievement IDs.
 *
 * These IDs are created in Play Console → Play Games Services → Achievements.
 * After running scripts/setup_play_achievements.py the IDs will be filled in automatically,
 * or you can copy them manually from the Play Console.
 *
 * Format: "CgkI<base64>" — 12-character base-64 encoded string from Play Console.
 * Placeholder IDs below must be replaced with real IDs before publishing.
 */
object AchievementIds {

    // ── First steps ───────────────────────────────────────────────────────────
    /** Unlock after winning the very first puzzle */
    const val FIRST_WIN         = "achievement_first_win"

    // ── Win count milestones (incremental) ────────────────────────────────────
    /** Reach 10 total wins */
    const val WIN_10            = "achievement_win_10"
    /** Reach 50 total wins */
    const val WIN_50            = "achievement_win_50"
    /** Reach 100 total wins */
    const val WIN_100           = "achievement_win_100"
    /** Reach 500 total wins */
    const val WIN_500           = "achievement_win_500"

    // ── Skill ─────────────────────────────────────────────────────────────────
    /** Win a puzzle with 1 guess */
    const val FIRST_GUESS_WIN   = "achievement_first_guess_win"
    /** Win a puzzle with exactly 2 guesses */
    const val TWO_GUESS_WIN     = "achievement_two_guess_win"
    /** Win a puzzle using no power-ups */
    const val NO_POWERUP_WIN    = "achievement_no_powerup_win"

    // ── Daily challenge streaks ───────────────────────────────────────────────
    /** 3-day consecutive daily challenge streak */
    const val STREAK_3          = "achievement_streak_3"
    /** 7-day consecutive daily challenge streak */
    const val STREAK_7          = "achievement_streak_7"
    /** 30-day consecutive daily challenge streak */
    const val STREAK_30         = "achievement_streak_30"

    // ── Level pack progress ───────────────────────────────────────────────────
    /** Reach level 10 in any regular pack */
    const val REACH_LEVEL_10    = "achievement_reach_level_10"
    /** Reach level 50 in any regular pack */
    const val REACH_LEVEL_50    = "achievement_reach_level_50"
    /** Complete all 100 levels in any regular pack */
    const val PACK_MASTER       = "achievement_pack_master"

    // ── Daily challenge volume ────────────────────────────────────────────────
    /** Complete your first daily challenge */
    const val FIRST_DAILY       = "achievement_first_daily"
    /** Complete 100 daily challenges (incremental, 100 steps) */
    const val DAILY_100         = "achievement_daily_100"
}
