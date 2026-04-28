package com.djtaylor.wordjourney.domain.model

/**
 * Hardcoded word pack for VIP levels 1–50 (10 full cycles).
 *
 * Each entry provides a verified word with definition that exactly matches the
 * expected word length for that VIP level. Word lengths cycle as:
 *   (level - 1) % 5 == 0  →  3 letters  (levels 1, 6, 11, 16, …)
 *   (level - 1) % 5 == 1  →  4 letters  (levels 2, 7, 12, 17, …)
 *   (level - 1) % 5 == 2  →  5 letters  (levels 3, 8, 13, 18, …)
 *   (level - 1) % 5 == 3  →  6 letters  (levels 4, 9, 14, 19, …)
 *   (level - 1) % 5 == 4  →  7 letters  (levels 5, 10, 15, 20, …)
 *
 * VIP levels beyond 50 fall back to the regular WordRepository database query.
 */
object VipWordPacks {

    data class VipWord(val word: String, val definition: String)

    /** Returns the target word for [level], or null if beyond the hardcoded range. */
    fun getWord(level: Int): String? = pack[level]?.word

    /** Returns the definition for [level], or null if beyond the hardcoded range. */
    fun getDefinition(level: Int): String? = pack[level]?.definition

    /** True if [level] is covered by this hardcoded pack (levels 1–50). */
    fun hasLevel(level: Int): Boolean = pack.containsKey(level)

    private val pack: Map<Int, VipWord> = mapOf(
        // ── Cycle 1 (levels 1–5) ───────────────────────────────────────────────
        1  to VipWord("CAT",     "A small domesticated carnivorous mammal kept as a pet"),
        2  to VipWord("ABLE",    "Having the power, skill, or means to do something"),
        3  to VipWord("CRANE",   "A large wading bird with a long neck, or a lifting machine"),
        4  to VipWord("BRIDGE",  "A structure built to span a gap or body of water"),
        5  to VipWord("KITCHEN", "A room where food is prepared and cooked"),
        // ── Cycle 2 (levels 6–10) ──────────────────────────────────────────────
        6  to VipWord("SUN",     "The star at the centre of our solar system"),
        7  to VipWord("BOLD",    "Showing a willingness to take risks; confident and fearless"),
        8  to VipWord("BRAVE",   "Ready to face and endure danger or pain without fear"),
        9  to VipWord("CASTLE",  "A large medieval fortified building or the chess rook piece"),
        10 to VipWord("CAPTAIN", "The person who commands a ship, aircraft, or sports team"),
        // ── Cycle 3 (levels 11–15) ─────────────────────────────────────────────
        11 to VipWord("ART",     "The expression of creative skill in a visual or other form"),
        12 to VipWord("CALM",    "Not showing or feeling nervousness, anxiety, or agitation"),
        13 to VipWord("DANCE",   "To move rhythmically to music, typically following a pattern"),
        14 to VipWord("DESERT",  "A barren area of land with very little rainfall or vegetation"),
        15 to VipWord("FREEDOM", "The power or right to act, speak, or think without restraint"),
        // ── Cycle 4 (levels 16–20) ─────────────────────────────────────────────
        16 to VipWord("FLY",     "To move through the air using wings or an engine"),
        17 to VipWord("DARK",    "Having very little or no light; mysterious or sinister"),
        18 to VipWord("EARTH",   "The planet we live on; the substance of the ground"),
        19 to VipWord("FLOWER",  "The seed-bearing part of a plant, often colourful and fragrant"),
        20 to VipWord("JOURNEY", "A long trip or voyage from one place to another"),
        // ── Cycle 5 (levels 21–25) ─────────────────────────────────────────────
        21 to VipWord("NET",     "An open fabric of knotted twine used to catch fish or balls"),
        22 to VipWord("FAST",    "Moving or capable of moving at high speed; to abstain from food"),
        23 to VipWord("FLAME",   "A hot glowing body of burning gas produced by a fire"),
        24 to VipWord("FROZEN",  "Having been turned into ice; unable to move from shock"),
        25 to VipWord("BALANCE", "An even distribution of weight; to keep something steady"),
        // ── Cycle 6 (levels 26–30) ─────────────────────────────────────────────
        26 to VipWord("EGG",     "An oval or round object laid by a female bird or reptile"),
        27 to VipWord("HELP",    "To make it easier for someone to do something; assistance"),
        28 to VipWord("GHOST",   "An apparition or spirit of a dead person; to vanish silently"),
        29 to VipWord("GARDEN",  "A piece of land where plants, flowers, or vegetables are grown"),
        30 to VipWord("BLANKET", "A large piece of soft fabric used for warmth in bed"),
        // ── Cycle 7 (levels 31–35) ─────────────────────────────────────────────
        31 to VipWord("ICE",     "Frozen water; a transparent brittle solid"),
        32 to VipWord("JUMP",    "To push oneself off the ground using the legs; a leap"),
        33 to VipWord("HOUSE",   "A building used as a home; to provide accommodation for"),
        34 to VipWord("ISLAND",  "A piece of land completely surrounded by water"),
        35 to VipWord("CHAPTER", "A main division of a book; a period in a sequence of events"),
        // ── Cycle 8 (levels 36–40) ─────────────────────────────────────────────
        36 to VipWord("MAP",     "A diagrammatic representation of an area of land or sea"),
        37 to VipWord("LAKE",    "A large body of water surrounded by land"),
        38 to VipWord("MONEY",   "A medium of exchange; coins and banknotes collectively"),
        39 to VipWord("JUNGLE",  "A dense tropical forest with lush tangled vegetation"),
        40 to VipWord("CLUSTER", "A group of similar things positioned or occurring closely together"),
        // ── Cycle 9 (levels 41–45) ─────────────────────────────────────────────
        41 to VipWord("OAK",     "A large, long-lived hardwood tree that produces acorns"),
        42 to VipWord("MOON",    "The natural satellite that orbits the Earth"),
        43 to VipWord("OCEAN",   "A vast expanse of salt water covering most of the Earth"),
        44 to VipWord("LADDER",  "A structure with rungs used for climbing up or down"),
        45 to VipWord("COMPASS", "An instrument for determining direction; the scope or range of something"),
        // ── Cycle 10 (levels 46–50) ────────────────────────────────────────────
        46 to VipWord("PIE",     "A baked dish with a pastry crust and sweet or savoury filling"),
        47 to VipWord("NEST",    "A structure built by a bird to hold its eggs and young"),
        48 to VipWord("PRIZE",   "A thing given as a reward for victory or winning a contest"),
        49 to VipWord("MARKET",  "A place where goods are bought and sold; a particular area of trade"),
        50 to VipWord("DIAMOND", "A precious gemstone; an extremely hard carbon crystal")
    )
}
