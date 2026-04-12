package com.djtaylor.wordjourney.domain.model

/**
 * A single reward chest in the Star Rewards monthly system.
 *
 * @param id         Unique chest ID used for opened-chest tracking, e.g. "regular_1", "vip_5"
 * @param level      Display level number (1-10)
 * @param starCost   Stars required to open this chest
 * @param coins      Coins awarded on open
 * @param lives      Lives/hearts awarded on open
 * @param addGuessItems  "Add a Guess" items awarded on open
 * @param removeLetterItems "Remove a Letter" items awarded on open
 * @param gems       Diamonds/gems awarded on open
 * @param isVip      Whether this is a VIP-exclusive chest
 */
data class StarChest(
    val id: String,
    val level: Int,
    val starCost: Int,
    val coins: Long = 0L,
    val lives: Int = 0,
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,
    val gems: Int = 0,
    val isVip: Boolean = false
)

/** The reward earned when a chest is opened — mirrors [StarChest] reward fields for UI display. */
data class ChestReward(
    val coins: Long = 0L,
    val lives: Int = 0,
    val addGuessItems: Int = 0,
    val removeLetterItems: Int = 0,
    val gems: Int = 0
) {
    fun isEmpty() = coins == 0L && lives == 0 && addGuessItems == 0 && removeLetterItems == 0 && gems == 0
}

fun StarChest.toReward() = ChestReward(coins, lives, addGuessItems, removeLetterItems, gems)

/**
 * All star chest definitions.
 *
 * Regular chest star costs follow the pattern 3, 8, 15, 24, 35, 48, 63, 80, 99, 120.
 * Differences: +5, +7, +9, +11, +13, +15, +17, +19, +21 — each gap increases by 2.
 *
 * VIP chests share the same star-cost thresholds but offer different (exclusive + bonus) rewards.
 */
object StarChestDefinitions {

    /** Star costs: 3, 8, 15, 24, 35, 48, 63, 80, 99, 120 */
    val STAR_COSTS = listOf(3, 8, 15, 24, 35, 48, 63, 80, 99, 120)

    val regularChests: List<StarChest> = listOf(
        StarChest("regular_1",  level = 1,  starCost = 3,   coins = 100L),
        StarChest("regular_2",  level = 2,  starCost = 8,   coins = 250L, lives = 1),
        StarChest("regular_3",  level = 3,  starCost = 15,  coins = 400L, lives = 1, addGuessItems = 1),
        StarChest("regular_4",  level = 4,  starCost = 24,  coins = 600L, lives = 2, addGuessItems = 1, removeLetterItems = 1),
        StarChest("regular_5",  level = 5,  starCost = 35,  coins = 900L, lives = 2, addGuessItems = 2, removeLetterItems = 1),
        StarChest("regular_6",  level = 6,  starCost = 48,  coins = 1200L, lives = 3, addGuessItems = 2, removeLetterItems = 2, gems = 5),
        StarChest("regular_7",  level = 7,  starCost = 63,  coins = 1800L, lives = 3, addGuessItems = 3, removeLetterItems = 2, gems = 10),
        StarChest("regular_8",  level = 8,  starCost = 80,  coins = 2500L, lives = 5, addGuessItems = 3, removeLetterItems = 3, gems = 15),
        StarChest("regular_9",  level = 9,  starCost = 99,  coins = 3500L, lives = 5, addGuessItems = 4, removeLetterItems = 4, gems = 25),
        StarChest("regular_10", level = 10, starCost = 120, coins = 5000L, lives = 8, addGuessItems = 5, removeLetterItems = 5, gems = 50)
    )

    /**
     * VIP chests: same star-cost tiers but exclusive rewards (gems-forward + larger quantities).
     * These are only openable by VIP subscribers.
     */
    val vipChests: List<StarChest> = listOf(
        StarChest("vip_1",  level = 1,  starCost = 3,   coins = 200L, gems = 5,   isVip = true),
        StarChest("vip_2",  level = 2,  starCost = 8,   coins = 300L, lives = 1,  gems = 10, isVip = true),
        StarChest("vip_3",  level = 3,  starCost = 15,  lives = 2, addGuessItems = 2,  gems = 15, isVip = true),
        StarChest("vip_4",  level = 4,  starCost = 24,  lives = 2, addGuessItems = 3,  removeLetterItems = 2, gems = 20, isVip = true),
        StarChest("vip_5",  level = 5,  starCost = 35,  coins = 500L, lives = 3, addGuessItems = 3,  gems = 30, isVip = true),
        StarChest("vip_6",  level = 6,  starCost = 48,  coins = 1000L, lives = 4, addGuessItems = 3,  removeLetterItems = 3, gems = 50, isVip = true),
        StarChest("vip_7",  level = 7,  starCost = 63,  coins = 1500L, lives = 4, addGuessItems = 5,  removeLetterItems = 4, gems = 75, isVip = true),
        StarChest("vip_8",  level = 8,  starCost = 80,  coins = 2000L, lives = 5, addGuessItems = 5,  removeLetterItems = 5, gems = 100, isVip = true),
        StarChest("vip_9",  level = 9,  starCost = 99,  coins = 3000L, lives = 6, addGuessItems = 7,  removeLetterItems = 7, gems = 150, isVip = true),
        StarChest("vip_10", level = 10, starCost = 120, coins = 5000L, lives = 8, addGuessItems = 10, removeLetterItems = 10, gems = 200, isVip = true)
    )

    val allChests: List<StarChest> get() = regularChests + vipChests

    /** Returns the [StarChest] with the given [id], or null if not found. */
    fun findById(id: String): StarChest? = allChests.firstOrNull { it.id == id }
}

// ── Domain helpers ────────────────────────────────────────────────────────────

/**
 * Compute the available (unspent) stars from a player's progress.
 * Stars roll over month-to-month; only previously-opened chests consume stars.
 */
fun PlayerProgress.availableStars(): Int = maxOf(0, totalStarsEarned - starsSpentOnChests)

/**
 * Returns the set of chest IDs that have been opened in the current month.
 * If [chestResetMonthKey] does not match the current month, returns an empty set
 * (chests have reset even if data has not yet been flushed).
 */
fun PlayerProgress.openedChestsThisMonth(currentMonthKey: String): Set<String> {
    if (chestResetMonthKey != currentMonthKey) return emptySet()
    return openedChestsThisMonthKeys
        .split(",")
        .filter { it.isNotBlank() }
        .toSet()
}

/**
 * Return progress with chest [chestId] marked as opened this month, stars deducted.
 * Also resets the opened-chest list if [currentMonthKey] has changed.
 */
fun PlayerProgress.withChestOpened(chestId: String, starCost: Int, currentMonthKey: String): PlayerProgress {
    val freshOpened = if (chestResetMonthKey != currentMonthKey) emptySet() else {
        openedChestsThisMonthKeys.split(",").filter { it.isNotBlank() }.toSet()
    }
    val updatedKeys = (freshOpened + chestId).joinToString(",")
    return copy(
        starsSpentOnChests = starsSpentOnChests + starCost,
        openedChestsThisMonthKeys = updatedKeys,
        chestResetMonthKey = currentMonthKey
    )
}

/**
 * Streak shield cost formula for the nth use in a month (0-indexed).
 * Pattern: 5, 7, 10, 14, 19, 25, 32 … (differences +2, +3, +4, +5, …)
 * Formula: cost(n) = 4 + (n+1)*(n+2)/2
 */
fun streakShieldCost(usesThisMonth: Int): Int {
    val n = usesThisMonth.coerceAtLeast(0)
    return 4 + (n + 1) * (n + 2) / 2
}
