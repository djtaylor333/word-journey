package com.djtaylor.wordjourney.domain.model

/**
 * Curated word lists for seasonal themed level packs.
 * 100 levels per season, ordered easy to hard.
 * All words are exactly 5 letters, real English words verified against major word lists.
 * All 600 words are unique across the entire file.
 */
object SeasonalWordPacks {

    /**
     * Gets the word for a given season pack and level (1-based).
     * Falls back to wrapping if level > pack size.
     */
    fun getWord(seasonKey: String, level: Int): String {
        val pack = PACKS[seasonKey] ?: return "BLOOM"
        return pack[(level - 1) % pack.size]
    }

    /** Number of levels in a given season pack (always 100). */
    fun packSize(seasonKey: String): Int = PACKS[seasonKey]?.size ?: 100

    /** All season keys with available packs. */
    val availableSeasonKeys: Set<String> get() = PACKS.keys

    private val PACKS: Map<String, List<String>> by lazy {
        mapOf(
            "easter"       to EASTER_WORDS,
            "valentines"   to VALENTINES_WORDS,
            "summer"       to SUMMER_WORDS,
            "halloween"    to HALLOWEEN_WORDS,
            "thanksgiving" to THANKSGIVING_WORDS,
            "christmas"    to CHRISTMAS_WORDS
        )
    }

    // ── Easter & Spring (Mar 15 – Apr 20) ────────────────────────────────────
    private val EASTER_WORDS = listOf(
        // easy / common
        "BLOOM", "CHICK", "BUNNY", "TULIP", "ROBIN",
        "DOVES", "LAMBS", "LILAC", "PETAL", "SEEDS",
        "FRESH", "GREEN", "SUNNY", "PANSY", "DAISY",
        "VINES", "STEMS", "BIRDS", "WORMS", "RAINY",
        "WINDY", "CLOUD", "ARISE", "RENEW", "ALIVE",
        "LIGHT", "GRACE", "RISEN", "CROSS", "ANGEL",
        // medium
        "CHOIR", "PSALM", "FAITH", "BLESS", "NESTS",
        "GROVE", "CREEK", "VIVID", "BRISK", "MUDDY",
        "MISTY", "FLORA", "FRUIT", "PREEN", "PERCH",
        "TWEET", "CHEEP", "BLEAT", "GRAZE", "AZURE",
        "BALMY", "SPRIG", "BOWER", "ARBOR", "THAWS",
        "RIPEN", "SPAWN", "GLORY", "HYMNS", "SPRAY",
        "PLUME", "SPORE", "THORN", "TWIGS", "WISPY",
        "WRENS", "YOLKS", "CROAK", "FROGS", "SWANS",
        "GEESE", "HATCH", "HARES", "HERBS", "HILLS",
        "PLOWS", "PONDS", "POOLS", "PATCH", "PALMS",
        // harder / more obscure
        "VIGIL", "TEPID", "SOGGY", "FROND", "EAGER",
        "CREED", "DEITY", "FOALS", "PRAYS", "VERSE",
        "VETCH", "MOSSY", "FILLY", "LARKS", "FINCH",
        "BOUGH", "GLENS", "TERNS", "CRESS", "CALLA"
    )

    // ── Valentine's Day (Feb 1 – 14) ─────────────────────────────────────────
    private val VALENTINES_WORDS = listOf(
        // easy / common
        "HEART", "LOVED", "ROSES", "SWEET", "CANDY",
        "LOVER", "BLUSH", "SMILE", "CHARM", "GIFTS",
        "DATES", "CUPID", "ADORE", "WINKS", "FLIRT",
        "HONEY", "FANCY", "CHEEK", "FLUSH", "GIDDY",
        "PLUSH", "SIGHS", "ROUGE", "TEDDY", "BOXES",
        "ARDOR", "TRYST", "SWOON", "DOTED", "MOONY",
        // medium
        "CRUSH", "AMOUR", "OGLED", "WOOED", "PINED",
        "CLUNG", "CARES", "TOAST", "SATIN", "SILKY",
        "BEADS", "LACES", "BOWED", "BRAID", "TIARA",
        "JEWEL", "POSED", "DOWRY", "TROTH", "SWEAR",
        "VOWED", "LOYAL", "TRULY", "DEEDS", "POEMS",
        "PROSE", "LUTES", "HARPS", "SONGS", "WALTZ",
        "DANCE", "TANGO", "SAMBA", "MUSIC", "TUNES",
        "IDYLL", "ELOPE", "YEARN", "SWAIN", "BELLE",
        "BUXOM", "SPURN", "JILTS", "POUTS", "TIFFS",
        "SULKS", "SOPPY", "MUSHY", "GUSHY", "TACKY",
        // harder / more obscure
        "GAUDY", "DOTES", "LEMAN", "COURT", "CAMEO",
        "BLISS", "ELATE", "MIRTH", "LYRIC", "CHORD",
        "DITTY", "RHYME", "METER", "FETCH", "RINGS",
        "OATHS", "BONDS", "VYING", "CRAVE", "BESOT"
    )

    // ── Summer (Jun 1 – Aug 31) ───────────────────────────────────────────────
    private val SUMMER_WORDS = listOf(
        // easy / common
        "BEACH", "OCEAN", "WAVES", "SHORE", "SANDS",
        "TIDAL", "CORAL", "REEFS", "CRABS", "CLAMS",
        "KAYAK", "CANOE", "YACHT", "SAILS", "OZONE",
        "HUMID", "SWEAT", "MUGGY", "BLAZE", "GLARE",
        "DRIED", "DUSTY", "BAKED", "PATIO", "DECKS",
        "TONGS", "GRILL", "FLAME", "SMOKE", "COALS",
        "MELON", "MANGO", "PEACH", "PLUMS", "BERRY",
        "LEMON", "LIMES", "JUICY", "TOWEL", "VISOR",
        // medium
        "SALTY", "SANDY", "BUOYS", "TANKS", "BURNT",
        "SOLAR", "SWIMS", "DIVES", "FLOAT", "FLIPS",
        "BOARD", "STING", "JELLY", "SHOAL", "ATOLL",
        "COVES", "INLET", "DELTA", "FLATS", "DUNES",
        "BLUFF", "RIDGE", "TREKS", "CAMPS", "TENTS",
        "MOTHS", "GNATS", "FLIES", "BIKES", "SKATE",
        "BRINE", "ALGAE", "SURGE", "SWIRL", "FROTH",
        "SPUME", "SCUBA", "FLUKE", "BREAM", "TROUT",
        // harder / more obscure
        "BASIL", "CHIVE", "THYME", "CUMIN", "ANISE",
        "ZESTY", "TANGY", "OASIS", "GLINT", "SHINY",
        "FRIZZ", "GAUZE", "BRINY", "WRACK", "REEDY",
        "BROOK", "GUSTS", "CRISP", "TRAIL", "HIKES"
    )

    // ── Halloween (Oct 1 – 31) ────────────────────────────────────────────────
    private val HALLOWEEN_WORDS = listOf(
        // easy / common
        "GHOST", "WITCH", "SPOOK", "SCARY", "MASKS",
        "CAPES", "FANGS", "SKULL", "GRAVE", "CRYPT",
        "TOMBS", "MOANS", "HOWLS", "NIGHT", "GLOOM",
        "DREAD", "EERIE", "CREEP", "CRAWL", "SCARE",
        "HAUNT", "CURSE", "SPELL", "HEXED", "BREWS",
        "FOGGY", "MISTS", "TROLL", "DEMON", "DEVIL",
        // medium
        "FIEND", "BEAST", "DIRGE", "KNELL", "TOLLS",
        "ELEGY", "COVEN", "RUNES", "SIGIL", "GLYPH",
        "BANES", "WRATH", "SPITE", "VENOM", "MURKY",
        "DREGS", "TALON", "CLAWS", "SNARE", "PROWL",
        "STALK", "LURKS", "HIDES", "GROWL", "SNARL",
        "HOUND", "RAVEN", "CROWS", "BLACK", "EBONY",
        "ASHEN", "GHOUL", "WAILS", "GROAN", "DUSKY",
        "UMBRA", "SHADE", "MANOR", "RUINS", "DECAY",
        "MOLDS", "FUNGI", "SLIME", "OOZES", "MUSTY",
        "ACRID", "FETID", "BOGEY", "GRIMY", "BLEAK",
        // harder / more obscure
        "TAINT", "DOOMS", "HEXES", "PAGAN", "ABYSS",
        "CHASM", "OGRES", "DROSS", "SABLE", "LURID",
        "LIVID", "GAUNT", "WEIRD", "STARK", "STIFF",
        "RIGOR", "GLOAM", "TOILS", "VOIDS", "DREAR"
    )

    // ── Thanksgiving (Nov 1 – 28) ─────────────────────────────────────────────
    private val THANKSGIVING_WORDS = listOf(
        // easy / common
        "FEAST", "GRAVY", "ROAST", "BREAD", "ROLLS",
        "CREAM", "SAUCE", "BROWN", "CLOVE", "MAIZE",
        "GOURD", "WHEAT", "LADLE", "CARVE", "SLICE",
        "WEDGE", "SERVE", "BROTH", "STOCK", "AMBER",
        "OCHRE", "TAWNY", "RUDDY", "STOVE", "ONION",
        "LEEKS", "APPLE", "NUTTY", "SPICE", "MAPLE",
        // medium
        "ACORN", "PECAN", "GRAIN", "STRAW", "SHEAF",
        "CROPS", "LADEN", "UMBER", "SEPIA", "TAUPE",
        "CRIMP", "CRUST", "FLAKY", "KNEAD", "PROOF",
        "KNOBS", "CHURN", "GRIND", "MILLS", "PRESS",
        "PLUCK", "DRESS", "STUFF", "TRUSS", "CURED",
        "JERKY", "BASTE", "GLEAN", "STORE", "CACHE",
        "HOARD", "SPELT", "EMMER", "OFFAL", "TITHE",
        "LIVER", "VITAL", "ORGAN", "TRIPE", "BEETS",
        "KALES", "SQUAB", "BROIL", "SAUTE", "POACH",
        "CRUMB", "GRATE", "SIEVE", "BLEND", "WHISK",
        // harder / more obscure
        "MIXER", "PUREE", "SMOKY", "SPICY", "UMAMI",
        "YUMMY", "TASTY", "LUMPY", "GORGE", "SATED",
        "PLUMP", "AMPLE", "GIVEN", "THANK", "SHARE",
        "TRIBE", "FOLKS", "UNION", "HAPPY", "CHORE"
    )

    // ── Christmas & Winter (Dec 1 – 31) ──────────────────────────────────────
    private val CHRISTMAS_WORDS = listOf(
        // easy / common
        "HOLLY", "BELLS", "SNOWY", "ELVES", "STARS",
        "PEACE", "MERRY", "JOLLY", "CHEER", "CHILL",
        "FROST", "FLAKE", "DRIFT", "SLEET", "SLEDS",
        "CAROL", "TREES", "PINES", "CEDAR", "CABIN",
        "LODGE", "EMBER", "GLOWS", "GLEAM", "COCOA",
        "CIDER", "ICING", "SUGAR", "FUDGE", "MINTS",
        // medium
        "TAFFY", "SCARF", "MITTS", "BOOTS", "CLOAK",
        "SHAWL", "WOOLY", "PLAID", "TWEED", "STOLE",
        "DRAPE", "SWAGS", "POLAR", "BEARS", "BELOW",
        "NIPPY", "FIGGY", "HALOS", "CROWN", "SAINT",
        "MYRRH", "SHINE", "TORCH", "TAPER", "WICKS",
        "PUNCH", "MULLS", "CHIME", "PEALS", "PARTY",
        "REVEL", "WRAPS", "TWINE", "SOCKS", "VISIT",
        "GREET", "NOTES", "TOWER", "NUMBS", "HOARY",
        "RIMED", "GELID", "PARKA", "MUFFS", "GLOVE",
        "CACAO", "KNOLL", "GABLE", "PLUMB", "TROVE",
        // harder / more obscure
        "HOUSE", "STOKE", "ALOFT", "ABOVE", "BLEST",
        "MERCY", "SHONE", "GLITZ", "ROUND", "STOUT",
        "ROBES", "LINEN", "COMET", "VIXEN", "POLKA",
        "GAUZY", "LUNAR", "NORTH", "JOLTS", "TRICE"
    )
}
