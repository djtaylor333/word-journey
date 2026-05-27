package com.djtaylor.wordjourney.domain.model

/**
 * Hardcoded word pack for VIP levels 1–100 (20 full cycles).
 *
 * Each entry provides a verified word with definition that exactly matches the
 * expected word length for that VIP level. Word lengths cycle as:
 *   (level - 1) % 5 == 0  →  3 letters  (levels 1, 6, 11, 16, …)
 *   (level - 1) % 5 == 1  →  4 letters  (levels 2, 7, 12, 17, …)
 *   (level - 1) % 5 == 2  →  5 letters  (levels 3, 8, 13, 18, …)
 *   (level - 1) % 5 == 3  →  6 letters  (levels 4, 9, 14, 19, …)
 *   (level - 1) % 5 == 4  →  7 letters  (levels 5, 10, 15, 20, …)
 *
 * VIP levels beyond 100 fall back to the regular WordRepository database query.
 */
object VipWordPacks {

    data class VipWord(val word: String, val definition: String)

    /** Returns the target word for [level], or null if beyond the hardcoded range. */
    fun getWord(level: Int): String? = pack[level]?.word

    /** Returns the definition for [level], or null if beyond the hardcoded range. */
    fun getDefinition(level: Int): String? = pack[level]?.definition

    /** True if [level] is covered by this hardcoded pack (levels 1–100). */
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
        50 to VipWord("DIAMOND", "A precious gemstone; an extremely hard carbon crystal"),
        // ── Cycle 11 (levels 51–55) ────────────────────────────────────────────
        51 to VipWord("PEA",     "A small spherical green seed eaten as a vegetable"),
        52 to VipWord("RING",    "A circular band worn as jewellery; a sound made by a bell"),
        53 to VipWord("STORM",   "A violent disturbance of the atmosphere with strong winds"),
        54 to VipWord("ANCHOR",  "A heavy object used to moor a vessel to the seabed"),
        55 to VipWord("PENGUIN", "A flightless seabird found mainly in the southern hemisphere"),
        // ── Cycle 12 (levels 56–60) ────────────────────────────────────────────
        56 to VipWord("OWL",     "A nocturnal bird of prey with large forward-facing eyes"),
        57 to VipWord("OPEN",    "Not closed; available for entry, use, or view"),
        58 to VipWord("LIGHT",   "Electromagnetic radiation visible to the eye; not heavy"),
        59 to VipWord("FOREST",  "A large area densely covered with trees and undergrowth"),
        60 to VipWord("PRESENT", "Something given as a gift; existing or occurring now"),
        // ── Cycle 13 (levels 61–65) ────────────────────────────────────────────
        61 to VipWord("RIB",     "One of the curved bones forming the chest cavity"),
        62 to VipWord("WAVE",    "A ridge of water moving across the sea; to move the hand in greeting"),
        63 to VipWord("BRUSH",   "An implement with bristles for cleaning or painting"),
        64 to VipWord("SILVER",  "A shiny greyish-white precious metal; the colour of this metal"),
        65 to VipWord("MORNING", "The period from dawn until midday"),
        // ── Cycle 14 (levels 66–70) ────────────────────────────────────────────
        66 to VipWord("SKY",     "The atmosphere and outer space as seen from the Earth"),
        67 to VipWord("POND",    "A small area of still water, typically artificial"),
        68 to VipWord("BREAD",   "A baked food made from flour, water, and yeast"),
        69 to VipWord("BUTTER",  "A pale yellow fatty substance made from churned cream"),
        70 to VipWord("VOLCANO", "A mountain with a vent through which lava erupts"),
        // ── Cycle 15 (levels 71–75) ────────────────────────────────────────────
        71 to VipWord("FIG",     "A soft pear-shaped fruit with a sweet dark flesh"),
        72 to VipWord("BELL",    "A hollow metal object that makes a ringing sound when struck"),
        73 to VipWord("CLOUD",   "A visible mass of condensed water vapour floating in the sky"),
        74 to VipWord("STREAM",  "A small narrow river; to flow continuously in a current"),
        75 to VipWord("PYRAMID", "A monumental structure with a square base and triangular faces"),
        // ── Cycle 16 (levels 76–80) ────────────────────────────────────────────
        76 to VipWord("JAM",     "A sweet spread made from fruit boiled with sugar"),
        77 to VipWord("FORK",    "A utensil with prongs for lifting food; a split in a road"),
        78 to VipWord("SWEET",   "Having the taste of sugar; pleasant or endearing"),
        79 to VipWord("PLANET",  "A large body orbiting a star in a fixed path"),
        80 to VipWord("LANTERN", "A lamp with a transparent case protecting the flame"),
        // ── Cycle 17 (levels 81–85) ────────────────────────────────────────────
        81 to VipWord("LOG",     "A length of wood cut from a tree trunk; a record of events"),
        82 to VipWord("COAL",    "A black rock used as a fuel when burnt"),
        83 to VipWord("SWIFT",   "Moving very fast; a fast-flying migratory bird"),
        84 to VipWord("GRAVEL",  "Small stones used for paths and as a building material"),
        85 to VipWord("HARVEST", "The process of gathering a ripened crop from the fields"),
        // ── Cycle 18 (levels 86–90) ────────────────────────────────────────────
        86 to VipWord("HEN",     "A female chicken; the female of any domestic fowl"),
        87 to VipWord("SWIM",    "To propel the body through water using the limbs"),
        88 to VipWord("FENCE",   "A barrier of posts and wire used to enclose an area"),
        89 to VipWord("COBALT",  "A hard silvery-white metal; a deep blue pigment"),
        90 to VipWord("COURAGE", "The ability to do something frightening; bravery"),
        // ── Cycle 19 (levels 91–95) ────────────────────────────────────────────
        91 to VipWord("DEN",     "A wild animal's hidden shelter; a private room for work"),
        92 to VipWord("TIDE",    "The regular rise and fall of the sea; a trend or tendency"),
        93 to VipWord("PEARL",   "A hard lustrous gem formed inside a mollusc shell"),
        94 to VipWord("TIMBER",  "Wood prepared for use in building or carpentry"),
        95 to VipWord("PILGRIM", "A person who travels to a sacred place for religious reasons"),
        // ── Cycle 20 (levels 96–100) ───────────────────────────────────────────
        96 to VipWord("RAM",     "A male sheep; to strike with great force"),
        97 to VipWord("BARK",    "The tough outer covering of a tree; a sharp sound made by a dog"),
        98 to VipWord("CLIFF",   "A steep rock face at the edge of the sea or a drop"),
        99 to VipWord("MIRROR",  "A surface that reflects a clear image; to imitate something"),
       100 to VipWord("DOLPHIN", "A marine mammal known for its intelligence and friendly behaviour")
    )
}
