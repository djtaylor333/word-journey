package com.djtaylor.wordjourney.domain.model

/**
 * Hardcoded word pack for VIP levels 1–500 (100 cycles).
 *
 * Each entry provides a verified word with definition that exactly matches the
 * expected word length for that VIP level. Word lengths cycle as:
 *   (level - 1) % 5 == 0  →  3 letters  (levels 1, 6, 11, 16, …)
 *   (level - 1) % 5 == 1  →  4 letters  (levels 2, 7, 12, 17, …)
 *   (level - 1) % 5 == 2  →  5 letters  (levels 3, 8, 13, 18, …)
 *   (level - 1) % 5 == 3  →  6 letters  (levels 4, 9, 14, 19, …)
 *   (level - 1) % 5 == 4  →  7 letters  (levels 5, 10, 15, 20, …)
 *
 * VIP levels beyond 105 fall back to the regular WordRepository database query.
 */
object VipWordPacks {

    data class VipWord(val word: String, val definition: String)

    /** Returns the target word for [level], or null if beyond the hardcoded range. */
    fun getWord(level: Int): String? = pack[level]?.word

    /** Returns the definition for [level], or null if beyond the hardcoded range. */
    fun getDefinition(level: Int): String? = pack[level]?.definition

    /** True if [level] is covered by this hardcoded pack (levels 1–500). */
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
       100 to VipWord("DOLPHIN", "A marine mammal known for its intelligence and friendly behaviour"),
        // ── Cycle 21 (levels 101–105) ──────────────────────────────────────────
       101 to VipWord("ELM",     "A large deciduous tree with broad canopy and serrated leaves"),
       102 to VipWord("GUST",    "A sudden strong rush of wind"),
       103 to VipWord("STOVE",   "A heated appliance used for cooking or warming a room"),
       104 to VipWord("PEPPER",  "A hot spice from dried berries; also a mild hollow vegetable"),
       105 to VipWord("CLIMATE", "The long-term pattern of weather conditions in a particular region"),
        // ── Cycle 22 (levels 106–110) ──────────────────────────────────────────
       106 to VipWord("BOW",     "A weapon for shooting arrows; to bend forward as a greeting"),
       107 to VipWord("ARCH",    "A curved structure spanning an opening; arched or chief"),
       108 to VipWord("ABOVE",   "At a higher level or position than something else"),
       109 to VipWord("ABRUPT",  "Sudden and unexpected; brief to the point of rudeness"),
       110 to VipWord("ABANDON", "To leave behind permanently; to give up completely"),
        // ── Cycle 23 (levels 111–115) ──────────────────────────────────────────
       111 to VipWord("CUP",     "A small bowl-shaped container used for drinking"),
       112 to VipWord("ATOM",    "The smallest unit of a chemical element that can exist"),
       113 to VipWord("ACORN",   "The nut of an oak tree, sitting in a cuplike base"),
       114 to VipWord("ABSENT",  "Not present in a place at the expected or required time"),
       115 to VipWord("ACCOUNT", "A record of financial transactions; a description of events"),
        // ── Cycle 24 (levels 116–120) ──────────────────────────────────────────
       116 to VipWord("DEW",     "Tiny drops of water that form on cool surfaces overnight"),
       117 to VipWord("BALE",    "A large bundle of hay, straw, or paper tied tightly together"),
       118 to VipWord("ADULT",   "A fully grown person or animal; having reached maturity"),
       119 to VipWord("ACTIVE",  "Engaging in action; energetic; in working or operational state"),
       120 to VipWord("ACHIEVE", "To successfully reach a goal through effort or skill"),
        // ── Cycle 25 (levels 121–125) ──────────────────────────────────────────
       121 to VipWord("EEL",     "A long snake-like fish that lives in rivers and the sea"),
       122 to VipWord("BARN",    "A large farm building used for storing crops or housing animals"),
       123 to VipWord("ALARM",   "A warning signal; a device that wakes you up at a set time"),
       124 to VipWord("AFFORD",  "To have enough money or resources to pay for something"),
       125 to VipWord("ANCIENT", "Belonging to the very distant past; extremely old"),
        // ── Cycle 26 (levels 126–130) ──────────────────────────────────────────
       126 to VipWord("FOX",     "A wild animal related to the dog, known for its cunning"),
       127 to VipWord("BEAK",    "The hard pointed mouth of a bird"),
       128 to VipWord("ALBUM",   "A collection of recordings or photographs bound together"),
       129 to VipWord("ARCTIC",  "Relating to the region around the North Pole; extremely cold"),
       130 to VipWord("ANOTHER", "One more of the same kind; a different or additional one"),
        // ── Cycle 27 (levels 131–135) ──────────────────────────────────────────
       131 to VipWord("GEM",     "A precious stone; something considered very fine or excellent"),
       132 to VipWord("BEAN",    "A seed or pod of a climbing plant used as food"),
       133 to VipWord("ANGEL",   "A spiritual being believed to act as a messenger of God"),
       134 to VipWord("AUTUMN",  "The season between summer and winter; fall"),
       135 to VipWord("ATTEMPT", "An effort to achieve something; to try to do something"),
        // ── Cycle 28 (levels 136–140) ──────────────────────────────────────────
       136 to VipWord("HOP",     "To jump on one foot; a small jump; the plant used in brewing"),
       137 to VipWord("BEAR",    "A large heavy mammal; to carry or endure something"),
       138 to VipWord("APPLE",   "A round fruit with crisp flesh, grown on trees in orchards"),
       139 to VipWord("BATTLE",  "A fight between large armed forces; to struggle with"),
       140 to VipWord("BATTERY", "A device that stores electrical energy; unlawful use of force"),
        // ── Cycle 29 (levels 141–145) ──────────────────────────────────────────
       141 to VipWord("JET",     "A stream of liquid or gas shot at high speed; a very dark black"),
       142 to VipWord("BEAT",    "To strike repeatedly; to defeat; the rhythm of music"),
       143 to VipWord("ARROW",   "A slender pointed stick shot from a bow; a direction symbol"),
       144 to VipWord("BEACON",  "A light or fire used as a signal or warning to others"),
       145 to VipWord("BENEATH", "At a lower level or layer; unworthy of someone"),
        // ── Cycle 30 (levels 146–150) ──────────────────────────────────────────
       146 to VipWord("KIT",     "A set of tools or equipment kept together for a purpose"),
       147 to VipWord("BILL",    "A proposed law; a list of charges; the beak of a bird"),
       148 to VipWord("ATLAS",   "A book of maps; the top vertebra that supports the skull"),
       149 to VipWord("BEETLE",  "An insect with hardened forewings; to scurry along quickly"),
       150 to VipWord("BREATHE", "To take air into the lungs and then expel it"),
        // ── Cycle 31 (levels 151–155) ──────────────────────────────────────────
       151 to VipWord("MUD",     "Wet soft earth; to make dirty or unclear"),
       152 to VipWord("BIRD",    "A feathered vertebrate animal with wings and a beak"),
       153 to VipWord("ATTIC",   "A space or room inside the roof of a building"),
       154 to VipWord("BELONG",  "To be the rightful property of; to be a member of a group"),
       155 to VipWord("CABINET", "A piece of furniture with drawers or doors; a group of ministers"),
        // ── Cycle 32 (levels 156–160) ──────────────────────────────────────────
       156 to VipWord("RYE",     "A hardy cereal grain used for bread and whiskey"),
       157 to VipWord("BLOW",    "To move air with force; a hard hit; a sudden shock"),
       158 to VipWord("AUDIO",   "Relating to sound, especially recorded or transmitted sound"),
       159 to VipWord("BISHOP",  "A senior member of the Christian clergy; a chess piece"),
       160 to VipWord("CAPABLE", "Having the ability or qualities needed to do something"),
        // ── Cycle 33 (levels 161–165) ──────────────────────────────────────────
       161 to VipWord("SAP",     "The watery fluid that circulates through a plant"),
       162 to VipWord("BOLT",    "A metal bar for fastening a door; a lightning flash; to run"),
       163 to VipWord("AVOID",   "To keep away from or stop oneself from doing something"),
       164 to VipWord("BITTER",  "Having a sharp pungent taste; feeling resentment or hurt"),
       165 to VipWord("CARTOON", "A humorous drawing or animated film; a preliminary sketch"),
        // ── Cycle 34 (levels 166–170) ──────────────────────────────────────────
       166 to VipWord("TAR",     "A dark thick flammable liquid made by heating coal or wood"),
       167 to VipWord("BOND",    "A thing used to tie or fasten; a close connection; a debt"),
       168 to VipWord("AWAKE",   "Not asleep; alert and fully conscious"),
       169 to VipWord("BOTTLE",  "A container with a narrow neck for storing liquids"),
       170 to VipWord("CEILING", "The upper interior surface of a room; an upper limit"),
        // ── Cycle 35 (levels 171–175) ──────────────────────────────────────────
       171 to VipWord("VAT",     "A large tank or tub used for storing or processing liquids"),
       172 to VipWord("BONE",    "A rigid piece of hard tissue that forms the skeleton"),
       173 to VipWord("AWARE",   "Having knowledge or perception of a situation or fact"),
       174 to VipWord("BOUNCE",  "To spring back after hitting a surface; a lively quality"),
       175 to VipWord("CENTURY", "A period of one hundred years"),
        // ── Cycle 36 (levels 176–180) ──────────────────────────────────────────
       176 to VipWord("WAX",     "A soft substance made by bees or from petroleum; to grow"),
       177 to VipWord("BOOK",    "A written or printed work bound between covers"),
       178 to VipWord("BEACH",   "A pebbly or sandy shore beside a body of water"),
       179 to VipWord("BRANCH",  "A part of a tree growing from the trunk; a division of something"),
       180 to VipWord("CERTAIN", "Known for sure; particular but not specified; confident"),
        // ── Cycle 37 (levels 181–185) ──────────────────────────────────────────
       181 to VipWord("YAM",     "A large starchy tuber similar to a sweet potato"),
       182 to VipWord("BOOT",    "A sturdy shoe covering the foot and ankle or leg"),
       183 to VipWord("BIRTH",   "The start of life as a physically separate being"),
       184 to VipWord("BREATH",  "The air taken into or expelled from the lungs"),
       185 to VipWord("CHANNEL", "A natural or artificial waterway; a TV or radio frequency"),
        // ── Cycle 38 (levels 186–190) ──────────────────────────────────────────
       186 to VipWord("ACE",     "A playing card with a single pip; someone who excels"),
       187 to VipWord("BOWL",    "A round deep dish for food or liquid; to roll a ball"),
       188 to VipWord("BLAST",   "A powerful explosion or strong gust of wind"),
       189 to VipWord("BRONZE",  "A brownish-gold alloy of copper and tin; a reddish-brown colour"),
       190 to VipWord("CHARITY", "The voluntary giving of help or money to those in need"),
        // ── Cycle 39 (levels 191–195) ──────────────────────────────────────────
       191 to VipWord("AID",     "Help or support given to someone in need; to assist"),
       192 to VipWord("CAGE",    "A structure of bars for confining animals or people"),
       193 to VipWord("BLEND",   "To mix ingredients smoothly together; a mixture"),
       194 to VipWord("BUBBLE",  "A thin sphere of liquid enclosing air or gas"),
       195 to VipWord("CHICKEN", "A domesticated bird kept for eggs or meat; a coward"),
        // ── Cycle 40 (levels 196–200) ──────────────────────────────────────────
       196 to VipWord("ANT",     "A small insect that lives in organised colonies underground"),
       197 to VipWord("CAKE",    "A sweet baked food made from flour, eggs, and sugar"),
       198 to VipWord("BLISS",   "Perfect happiness; great joy"),
       199 to VipWord("BUCKET",  "A cylindrical open container with a handle for carrying liquid"),
       200 to VipWord("CHIMNEY", "A vertical channel for smoke in a building; a tall smokestack"),
        // ── Cycle 41 (levels 201–205) ──────────────────────────────────────────
       201 to VipWord("APE",     "A large tailless primate such as a gorilla or chimpanzee"),
       202 to VipWord("CAMP",    "A temporary outdoor shelter; a group sharing a common goal"),
       203 to VipWord("BLOCK",   "A solid mass; to obstruct or prevent passage"),
       204 to VipWord("BUDGET",  "An estimate of income and expenditure; a plan to control spending"),
       205 to VipWord("CITIZEN", "A legally recognised subject or national of a country"),
        // ── Cycle 42 (levels 206–210) ──────────────────────────────────────────
       206 to VipWord("ASH",     "The powdery residue left after burning; a type of deciduous tree"),
       207 to VipWord("CAPE",    "A sleeveless cloak; a piece of land jutting into the sea"),
       208 to VipWord("BLOOD",   "The red liquid that circulates in the arteries and veins"),
       209 to VipWord("BURROW",  "A hole dug by a small animal for shelter; to dig or tunnel"),
       210 to VipWord("CLASSIC", "Judged over time to be of the highest quality; typical"),
        // ── Cycle 43 (levels 211–215) ──────────────────────────────────────────
       211 to VipWord("AXE",     "A tool with a heavy blade used for chopping wood"),
       212 to VipWord("CARD",    "A thin flat piece of paper or plastic used for writing or paying"),
       213 to VipWord("BLOOM",   "The flower of a plant; to flourish or come into full beauty"),
       214 to VipWord("BUTTON",  "A small fastener sewn to clothing; a small disc for pressing"),
       215 to VipWord("COLLECT", "To gather things together; to accumulate over time"),
        // ── Cycle 44 (levels 216–220) ──────────────────────────────────────────
       216 to VipWord("BAY",     "A broad inlet of the sea; a recess in a wall; a laurel tree"),
       217 to VipWord("CAVE",    "A natural underground hollow in rock or a hillside"),
       218 to VipWord("BOARD",   "A flat piece of wood; a committee; to get onto a vehicle"),
       219 to VipWord("CANDLE",  "A cylindrical block of wax with a wick, burned for light"),
       220 to VipWord("COMMAND", "An authoritative order; to be in charge of; a military unit"),
        // ── Cycle 45 (levels 221–225) ──────────────────────────────────────────
       221 to VipWord("BEE",     "A stinging insect that collects nectar and makes honey"),
       222 to VipWord("CLAY",    "A stiff soft fine-grained earth, used in making ceramics"),
       223 to VipWord("BRACE",   "A device that holds or fastens; to prepare oneself; a pair"),
       224 to VipWord("CANYON",  "A deep gorge, typically with a river flowing through it"),
       225 to VipWord("COMPACT", "Closely and neatly packed together; a small flat cosmetics case"),
        // ── Cycle 46 (levels 226–230) ──────────────────────────────────────────
       226 to VipWord("BOG",     "Wet muddy ground that is too soft to support heavy weight"),
       227 to VipWord("CLIP",    "A fastener; to cut; to attach; a short section of film"),
       228 to VipWord("BRAIN",   "The organ in the skull that controls the nervous system; intellect"),
       229 to VipWord("CARBON",  "A chemical element present in all organic compounds; coal or soot"),
       230 to VipWord("CONCERT", "A musical performance given in public; agreement in action"),
        // ── Cycle 47 (levels 231–235) ──────────────────────────────────────────
       231 to VipWord("BOX",     "A container with flat sides; to fight with the fists"),
       232 to VipWord("CLUB",    "A stick used as a weapon; an organisation; a suit of cards"),
       233 to VipWord("BRAND",   "A type of product made by a company; a mark burned on skin"),
       234 to VipWord("CARPET",  "A thick fabric floor covering; to reprimand someone"),
       235 to VipWord("CONDUCT", "The manner in which one behaves; to lead or direct"),
        // ── Cycle 48 (levels 236–240) ──────────────────────────────────────────
       236 to VipWord("BUD",     "A compact knob on a plant that will open into a leaf or flower"),
       237 to VipWord("COAT",    "An outer garment with sleeves; a layer covering a surface"),
       238 to VipWord("BREAK",   "To separate into pieces; a pause or interval; an opportunity"),
       239 to VipWord("CARROT",  "An orange root vegetable tapered at the end"),
       240 to VipWord("CONNECT", "To join or link together; to establish communication"),
        // ── Cycle 49 (levels 241–245) ──────────────────────────────────────────
       241 to VipWord("BUS",     "A large motor vehicle that carries passengers on a fixed route"),
       242 to VipWord("COLD",    "At or below a low temperature; lacking warmth or feeling"),
       243 to VipWord("BRINE",   "Water saturated with salt; pickle liquid"),
       244 to VipWord("CATTLE",  "Bovine animals raised for meat or dairy on a farm"),
       245 to VipWord("CONTROL", "The power to influence or direct; to regulate or manage"),
        // ── Cycle 50 (levels 246–250) ──────────────────────────────────────────
       246 to VipWord("COD",     "A large edible sea fish found in the North Atlantic"),
       247 to VipWord("CONE",    "A solid shape with a circular base and a pointed top"),
       248 to VipWord("BRISK",   "Active and energetic; pleasantly cold and fresh"),
       249 to VipWord("CAVERN",  "A large dark cave; an underground hollow space"),
       250 to VipWord("CONVERT", "To change in form, character, or function; to adopt a new belief"),
        // ── Cycle 51 (levels 251–255) ──────────────────────────────────────────
       251 to VipWord("COW",     "A fully grown female domestic bovine kept for milk or meat"),
       252 to VipWord("CORD",    "A thin flexible string or rope; an electrical cable"),
       253 to VipWord("BROOK",   "A small stream; to tolerate or endure"),
       254 to VipWord("CEMENT",  "A binding substance that hardens to join bricks or stones"),
       255 to VipWord("CORRECT", "Free from error; to mark errors in or put right"),
        // ── Cycle 52 (levels 256–260) ──────────────────────────────────────────
       256 to VipWord("CUB",     "A young carnivorous mammal such as a bear, fox, or lion"),
       257 to VipWord("CORN",    "A cereal plant; a hard painful area on the foot"),
       258 to VipWord("BROTH",   "A thin soup made by simmering meat, bones, or vegetables"),
       259 to VipWord("CHERRY",  "A small round red or dark fruit; a bright deep red colour"),
       260 to VipWord("COTTAGE", "A small house, typically in the countryside"),
        // ── Cycle 53 (levels 261–265) ──────────────────────────────────────────
       261 to VipWord("DAM",     "A barrier built across a river to hold back water"),
       262 to VipWord("CROP",    "A cultivated plant grown on a large scale; to cut short"),
       263 to VipWord("BROWN",   "A colour produced by mixing red, yellow, and black"),
       264 to VipWord("CIRCLE",  "A round plane figure whose boundary is equidistant from the centre"),
       265 to VipWord("COUNTRY", "A nation with its own government; the rural areas outside cities"),
        // ── Cycle 54 (levels 266–270) ──────────────────────────────────────────
       266 to VipWord("DOE",     "A female deer, hare, or rabbit"),
       267 to VipWord("CROW",    "A large black bird; to make the cry of a cock; to boast"),
       268 to VipWord("BUDDY",   "A close friend or companion; to pair up with someone"),
       269 to VipWord("CIRCUS",  "A travelling company of acrobats, clowns, and trained animals"),
       270 to VipWord("CULTURE", "The arts and social customs of a society; growing microorganisms"),
        // ── Cycle 55 (levels 271–275) ──────────────────────────────────────────
       271 to VipWord("ELK",     "A large deer with big antlers, found in North America and Asia"),
       272 to VipWord("CURE",    "To heal an illness; a substance used to heal or preserve"),
       273 to VipWord("BULGE",   "A rounded swelling or protrusion; to swell outwards"),
       274 to VipWord("CITRUS",  "A tree bearing sharp-tasting fruit such as lemons or oranges"),
       275 to VipWord("CURIOUS", "Eager to know or learn something; strange or unusual"),
        // ── Cycle 56 (levels 276–280) ──────────────────────────────────────────
       276 to VipWord("EMU",     "A large flightless Australian bird with long legs"),
       277 to VipWord("CURL",    "A coil or spiral shape, especially in hair; to form curls"),
       278 to VipWord("CANAL",   "An artificial waterway for boats or irrigation"),
       279 to VipWord("COFFEE",  "A hot drink made from roasted ground beans"),
       280 to VipWord("CURRENT", "A body of water or air moving in a direction; the present time"),
        // ── Cycle 57 (levels 281–285) ──────────────────────────────────────────
       281 to VipWord("FIN",     "A flat appendage on a fish used for propelling and steering"),
       282 to VipWord("DAWN",    "The first appearance of light in the sky; the beginning of something"),
       283 to VipWord("CARGO",   "Goods carried by a ship, aircraft, or motor vehicle"),
       284 to VipWord("COLONY",  "A country or area under political control of another nation"),
       285 to VipWord("CURTAIN", "A piece of fabric hung to block light or provide privacy"),
        // ── Cycle 58 (levels 286–290) ──────────────────────────────────────────
       286 to VipWord("FIR",     "An evergreen tree with needle-shaped leaves and upright cones"),
       287 to VipWord("DEAL",    "An agreement; to distribute or trade; a large amount of something"),
       288 to VipWord("CARRY",   "To support and move from one place to another; to hold"),
       289 to VipWord("COMBAT",  "Armed fighting; to take action to reduce or prevent something"),
       290 to VipWord("DECLARE", "To make a formal announcement; to state clearly and officially"),
        // ── Cycle 59 (levels 291–295) ──────────────────────────────────────────
       291 to VipWord("FUN",     "Enjoyment; light-hearted pleasure; amusing or entertaining"),
       292 to VipWord("DECK",    "A floor of a ship; a pack of cards; to hit hard; to decorate"),
       293 to VipWord("CEDAR",   "A large fragrant coniferous tree with durable reddish wood"),
       294 to VipWord("COTTON",  "A soft white fibrous substance from the cotton plant; a fabric"),
       295 to VipWord("DELIVER", "To bring and hand over to the recipient; to rescue or save"),
        // ── Cycle 60 (levels 296–300) ──────────────────────────────────────────
       296 to VipWord("GAP",     "A break or hole in a surface or object; a space between two things"),
       297 to VipWord("DEER",    "A hoofed grazing animal with branched antlers"),
       298 to VipWord("CHAIN",   "A series of connected metal links; a restraint; to fasten"),
       299 to VipWord("CRATER",  "A large bowl-shaped cavity made by an explosion or meteorite"),
       300 to VipWord("DESERVE", "To be worthy of or have a right to something"),
        // ── Cycle 61 (levels 301–305) ──────────────────────────────────────────
       301 to VipWord("GAS",     "A substance that expands freely in air; petrol; to supply gas"),
       302 to VipWord("DICE",    "Small cubes marked with numbers, used in games; to cut food small"),
       303 to VipWord("CHALK",   "A soft white limestone used for writing or drawing"),
       304 to VipWord("CRAYON",  "A stick of coloured wax or chalk used for drawing"),
       305 to VipWord("DESTINY", "The events that will necessarily happen to a particular person"),
        // ── Cycle 62 (levels 306–310) ──────────────────────────────────────────
       306 to VipWord("GUN",     "A weapon that fires projectiles using an explosive charge"),
       307 to VipWord("DISH",    "A flat container for food; a particular type of prepared food"),
       308 to VipWord("CHARM",   "The power of delighting people; a lucky trinket"),
       309 to VipWord("CREDIT",  "Acknowledgment of achievement; money borrowed or trustworthiness"),
       310 to VipWord("DEVELOP", "To grow or cause to grow and become more elaborate"),
        // ── Cycle 63 (levels 311–315) ──────────────────────────────────────────
       311 to VipWord("HAM",     "Meat from the upper part of a pig's leg; to overact"),
       312 to VipWord("DOME",    "A rounded vault forming the roof of a building or structure"),
       313 to VipWord("CHASE",   "To pursue in order to catch or harm; a vigorous pursuit"),
       314 to VipWord("CRISIS",  "A time of intense difficulty or danger; a turning point"),
       315 to VipWord("DISTANT", "Far away in space or time; aloof or reserved in manner"),
        // ── Cycle 64 (levels 316–320) ──────────────────────────────────────────
       316 to VipWord("IVY",     "A climbing evergreen plant with dark glossy leaves"),
       317 to VipWord("DOOR",    "A hinged or sliding barrier used to close an opening"),
       318 to VipWord("CHESS",   "A board game for two players using sixteen pieces each"),
       319 to VipWord("CUSTOM",  "A traditional practice; made to individual order"),
       320 to VipWord("DIVERSE", "Showing a great deal of variety; very different from each other"),
        // ── Cycle 65 (levels 321–325) ──────────────────────────────────────────
       321 to VipWord("OAR",     "A pole with a flat blade used for rowing a boat"),
       322 to VipWord("DOWN",    "Towards or in a lower position; the soft feathers of a bird"),
       323 to VipWord("CHEST",   "A large strong box for storing items; the front of the body"),
       324 to VipWord("DAGGER",  "A short pointed weapon with a sharp edge used for stabbing"),
       325 to VipWord("DYNASTY", "A line of hereditary rulers of a country; a powerful family"),
        // ── Cycle 66 (levels 326–330) ──────────────────────────────────────────
       326 to VipWord("ORB",     "A spherical object; a jewelled globe held by a monarch"),
       327 to VipWord("DRAG",    "To pull along with force; to move slowly; air resistance"),
       328 to VipWord("CHIEF",   "A leader or ruler; most important or prominent"),
       329 to VipWord("DAMAGE",  "Physical harm that reduces value or function; to cause harm"),
       330 to VipWord("ECLIPSE", "The blocking of sunlight by the moon; to surpass or outshine"),
        // ── Cycle 67 (levels 331–335) ──────────────────────────────────────────
       331 to VipWord("ORE",     "A naturally occurring solid from which metal is extracted"),
       332 to VipWord("DRAW",    "To produce a picture; to pull; a tie in a game"),
       333 to VipWord("CHILD",   "A young human being below the age of puberty"),
       334 to VipWord("DANGER",  "The possibility of suffering harm or injury"),
       335 to VipWord("EDUCATE", "To give intellectual and moral instruction to; to teach"),
        // ── Cycle 68 (levels 336–340) ──────────────────────────────────────────
       336 to VipWord("PAW",     "The foot of an animal with claws; to scrape at with a paw"),
       337 to VipWord("DROP",    "A small round mass of liquid; to fall or let fall"),
       338 to VipWord("CHILL",   "A coldness in the air; to make cold; to relax"),
       339 to VipWord("DEBATE",  "A formal discussion; to consider a matter; to argue"),
       340 to VipWord("ELEGANT", "Graceful and stylish in appearance or manner"),
        // ── Cycle 69 (levels 341–345) ──────────────────────────────────────────
       341 to VipWord("PEW",     "A long bench seat in a church for the congregation"),
       342 to VipWord("DRUM",    "A percussion instrument struck with sticks; a cylindrical container"),
       343 to VipWord("CHIVE",   "A herb with long thin green leaves used for flavouring food"),
       344 to VipWord("DECADE",  "A period of ten years"),
       345 to VipWord("EMPEROR", "The sovereign ruler of an empire"),
        // ── Cycle 70 (levels 346–350) ──────────────────────────────────────────
       346 to VipWord("POD",     "A long seed case of a pea or bean plant"),
       347 to VipWord("DUCK",    "A waterbird with a broad flat bill; to lower the head quickly"),
       348 to VipWord("CHORD",   "A group of musical notes sounded together harmoniously"),
       349 to VipWord("DEFINE",  "To state the exact meaning of; to describe the nature of"),
       350 to VipWord("ENCHANT", "To fill with delight; to cast a magic spell upon"),
        // ── Cycle 71 (levels 351–355) ──────────────────────────────────────────
       351 to VipWord("POT",     "A deep round container used for cooking or storing"),
       352 to VipWord("DUNE",    "A mound or ridge of sand formed by the wind"),
       353 to VipWord("CLAIM",   "To state a fact; to demand as one's own; an assertion of truth"),
       354 to VipWord("DEGREE",  "A unit of measurement; an academic qualification; a stage"),
       355 to VipWord("ENDLESS", "Having or seeming to have no end; countless"),
        // ── Cycle 72 (levels 356–360) ──────────────────────────────────────────
       356 to VipWord("PUB",     "A place licensed to sell alcoholic drinks; a public house"),
       357 to VipWord("DUSK",    "The darker stage of twilight at the end of the day"),
       358 to VipWord("CLAMP",   "A device for holding things tightly together; to fasten firmly"),
       359 to VipWord("DEPLOY",  "To move troops or equipment into position for action"),
       360 to VipWord("ENFORCE", "To compel observance of a law or rule; to impose"),
        // ── Cycle 73 (levels 361–365) ──────────────────────────────────────────
       361 to VipWord("PUP",     "A young dog; a young seal or shark"),
       362 to VipWord("DUST",    "Fine dry powder made up of tiny particles of earth or matter"),
       363 to VipWord("CLANG",   "A loud resonant metallic sound; to make such a sound"),
       364 to VipWord("DEPUTY",  "A person appointed to act as a substitute for another"),
       365 to VipWord("ENHANCE", "To intensify, increase, or further improve the quality of"),
        // ── Cycle 74 (levels 366–370) ──────────────────────────────────────────
       366 to VipWord("RAW",     "Uncooked; not processed; sore from abrasion; cold and wet"),
       367 to VipWord("EDGE",    "The outside limit of an area; to move gradually; a cutting side"),
       368 to VipWord("CLEAN",   "Free from dirt, marks, or mess; to make free from dirt"),
       369 to VipWord("DESIGN",  "A plan or drawing; to decide upon the form of; a decorative pattern"),
       370 to VipWord("EVENING", "The period of time at the end of the day"),
        // ── Cycle 75 (levels 371–375) ──────────────────────────────────────────
       371 to VipWord("RIM",     "The upper or outer edge of something circular"),
       372 to VipWord("EPIC",    "A long narrative poem or film; heroic and impressively great"),
       373 to VipWord("CLEAR",   "Easy to understand; transparent; free of obstruction"),
       374 to VipWord("DETAIL",  "A small individual item; to describe item by item; to assign"),
       375 to VipWord("EXAMINE", "To inspect closely and carefully; to test knowledge with questions"),
        // ── Cycle 76 (levels 376–380) ──────────────────────────────────────────
       376 to VipWord("ROW",     "A number of things in a line; to propel a boat; a noisy argument"),
       377 to VipWord("FACE",    "The front of the head; to look towards; courage"),
       378 to VipWord("CLERK",   "A person who keeps records and accounts in an office"),
       379 to VipWord("DEVOUR",  "To eat something eagerly; to take in greedily"),
       380 to VipWord("EXAMPLE", "A thing that serves as an illustration of a type or rule"),
        // ── Cycle 77 (levels 381–385) ──────────────────────────────────────────
       381 to VipWord("RUG",     "A small carpet; a thick woollen blanket used as a covering"),
       382 to VipWord("FARM",    "An area of land used for growing crops or raising livestock"),
       383 to VipWord("CLICK",   "A short sharp sound; to press a mouse button; to suddenly understand"),
       384 to VipWord("DIFFER",  "To be unlike or dissimilar; to have a different opinion"),
       385 to VipWord("EXPLAIN", "To make something clear by describing it in detail"),
        // ── Cycle 78 (levels 386–390) ──────────────────────────────────────────
       386 to VipWord("SAW",     "A tool with a serrated blade for cutting; past tense of see"),
       387 to VipWord("FERN",    "A flowerless plant with feathery green fronds"),
       388 to VipWord("CLIMB",   "To go upward using the hands and feet; an upward movement"),
       389 to VipWord("DIRECT",  "Going straight to a point without deviation; to aim or guide"),
       390 to VipWord("EXPLORE", "To travel through an unfamiliar area to learn about it"),
        // ── Cycle 79 (levels 391–395) ──────────────────────────────────────────
       391 to VipWord("SEA",     "The expanse of salt water that covers most of the Earth"),
       392 to VipWord("FILM",    "A thin layer; a movie; to record moving images on camera"),
       393 to VipWord("CLING",   "To hold on tightly to something; to remain close or attached"),
       394 to VipWord("DIVIDE",  "To separate into parts; to share out; a difference or gap"),
       395 to VipWord("EXTREME", "Reaching a high degree; furthest from the centre; radical"),
        // ── Cycle 80 (levels 396–400) ──────────────────────────────────────────
       396 to VipWord("SKI",     "A long narrow strip used for gliding over snow; to ski"),
       397 to VipWord("FISH",    "A cold-blooded vertebrate living in water; to catch fish"),
       398 to VipWord("CLOAK",   "A sleeveless outer garment; to hide or cover something"),
       399 to VipWord("DONKEY",  "A domesticated hoofed animal related to the horse"),
       400 to VipWord("FASHION", "A popular trend or style; to make into a particular form"),
        // ── Cycle 81 (levels 401–405) ──────────────────────────────────────────
       401 to VipWord("SOD",     "A piece of turf with grass growing on the surface"),
       402 to VipWord("FLAG",    "A piece of cloth used as a symbol; to mark; to become tired"),
       403 to VipWord("CLONE",   "An identical copy; to produce a genetically identical organism"),
       404 to VipWord("DRIVEN",  "Having determination; motivated; propelled by force"),
       405 to VipWord("FEATURE", "A distinctive aspect; an article; to have as a component"),
        // ── Cycle 82 (levels 406–410) ──────────────────────────────────────────
       406 to VipWord("SON",     "A male child in relation to his parents"),
       407 to VipWord("FLAT",    "Having a level horizontal surface; an apartment; not sharp"),
       408 to VipWord("CLOTH",   "Woven or felted fabric made from wool, cotton, or other fibres"),
       409 to VipWord("EFFECT",  "A change produced by a cause; to bring about; impression"),
       410 to VipWord("FERTILE", "Capable of producing crops or offspring abundantly; creative"),
        // ── Cycle 83 (levels 411–415) ──────────────────────────────────────────
       411 to VipWord("SOW",     "An adult female pig; to plant seeds in the ground"),
       412 to VipWord("FLOW",    "To move steadily and continuously; a steady stream"),
       413 to VipWord("CLOWN",   "A comic entertainer in a circus; to behave in a silly way"),
       414 to VipWord("EITHER",  "Used before the first of two alternatives; both of two"),
       415 to VipWord("FICTION", "Literature that describes imaginary events; invented stories"),
        // ── Cycle 84 (levels 416–420) ──────────────────────────────────────────
       416 to VipWord("SPA",     "A place with mineral springs; a commercial health resort"),
       417 to VipWord("FOAM",    "A mass of small bubbles on the surface of liquid; frothy material"),
       418 to VipWord("COACH",   "A horse-drawn carriage; a railway carriage; a sports trainer"),
       419 to VipWord("ENABLE",  "To give someone the authority or means to do something"),
       420 to VipWord("FINALLY", "At the end; after a long period; used to introduce a last point"),
        // ── Cycle 85 (levels 421–425) ──────────────────────────────────────────
       421 to VipWord("SUM",     "The total amount; to briefly review the key points"),
       422 to VipWord("FOLD",    "To bend over on itself; a line made by folding; a group of sheep"),
       423 to VipWord("CORAL",   "A hard substance formed by tiny marine animals; pinkish-orange colour"),
       424 to VipWord("ENCODE",  "To convert information into a particular code or form"),
       425 to VipWord("FISHING", "The activity of catching fish for food or sport"),
        // ── Cycle 86 (levels 426–430) ──────────────────────────────────────────
       426 to VipWord("TAB",     "A small flap for gripping or identifying something; a bill"),
       427 to VipWord("FOOD",    "Nutritious substances consumed to maintain life and growth"),
       428 to VipWord("CRAFT",   "A skill; an activity involving making things by hand; a vessel"),
       429 to VipWord("ENDURE",  "To suffer patiently; to remain in existence; to last"),
       430 to VipWord("FLATTEN", "To make or become flat; to knock down; to reduce"),
        // ── Cycle 87 (levels 431–435) ──────────────────────────────────────────
       431 to VipWord("TAP",     "A device for controlling the flow of liquid; to knock gently"),
       432 to VipWord("FORD",    "A shallow place in a river or stream; to cross at such a place"),
       433 to VipWord("CRASH",   "A violent collision; a loud noise; a sudden financial failure"),
       434 to VipWord("ENERGY",  "The power and ability to be active; a source of usable power"),
       435 to VipWord("FLAVOUR", "The distinctive taste of food or drink; a particular quality"),
        // ── Cycle 88 (levels 436–440) ──────────────────────────────────────────
       436 to VipWord("TAX",     "A compulsory contribution to state revenue; to put a strain on"),
       437 to VipWord("FORM",    "The visible shape or arrangement; a document to fill in"),
       438 to VipWord("CRAWL",   "To move on hands and knees; to move very slowly"),
       439 to VipWord("ENGAGE",  "To occupy or attract; to participate; to hire"),
       440 to VipWord("FOREVER", "For all time; used for emphasis to indicate a very long time"),
        // ── Cycle 89 (levels 441–445) ──────────────────────────────────────────
       441 to VipWord("TIN",     "A silvery metal used to coat other metals; a metal container"),
       442 to VipWord("FORT",    "A fortified military building or strong point"),
       443 to VipWord("CREAK",   "A harsh scraping or squeaking sound; to make such a sound"),
       444 to VipWord("ENGINE",  "A machine that converts energy into mechanical force; a locomotive"),
       445 to VipWord("FORTUNE", "Chance or luck; a large amount of money or wealth"),
        // ── Cycle 90 (levels 446–450) ──────────────────────────────────────────
       446 to VipWord("TOE",     "Any of the digits at the end of the human foot"),
       447 to VipWord("FOUR",    "The number equivalent to the product of two and two"),
       448 to VipWord("CREEK",   "A small stream or waterway; a narrow inlet"),
       449 to VipWord("ENTIRE",  "With no part left out; whole and complete"),
       450 to VipWord("FORWARD", "In the direction one is facing; towards the front; advancing"),
        // ── Cycle 91 (levels 451–455) ──────────────────────────────────────────
       451 to VipWord("TON",     "A unit of weight equal to 2,000 or 2,240 pounds; a large amount"),
       452 to VipWord("FROG",    "A tailless amphibian with a short squat body and long hind legs"),
       453 to VipWord("CREEP",   "To move slowly and carefully; to develop gradually; a crawl"),
       454 to VipWord("ESCAPE",  "To break free from confinement; to avoid danger"),
       455 to VipWord("FRAGILE", "Easily broken or damaged; delicate; not strong"),
        // ── Cycle 92 (levels 456–460) ──────────────────────────────────────────
       456 to VipWord("TOP",     "The highest or most important point; the leading position"),
       457 to VipWord("FUEL",    "Material burned to provide heat or power; to supply with fuel"),
       458 to VipWord("CRIME",   "An action that is punishable by law; a shameful act"),
       459 to VipWord("EVOLVE",  "To develop gradually over time; to change through evolution"),
       460 to VipWord("FRIENDS", "People whom one knows, likes, and trusts; companions"),
        // ── Cycle 93 (levels 461–465) ──────────────────────────────────────────
       461 to VipWord("TOW",     "To pull a vehicle or boat along; a coarse fibre of flax"),
       462 to VipWord("FULL",    "Containing as much or as many as possible; complete"),
       463 to VipWord("CRISP",   "Firm and brittle; pleasantly cold and fresh; clear and sharp"),
       464 to VipWord("EXCITE",  "To cause feelings of enthusiasm and eagerness; to stir up"),
       465 to VipWord("FURNACE", "A chamber in which fuel is burned; a very hot place"),
        // ── Cycle 94 (levels 466–470) ──────────────────────────────────────────
       466 to VipWord("TOY",     "An object for children to play with; to treat without seriousness"),
       467 to VipWord("GAME",    "An activity providing entertainment; a wild animal hunted for sport"),
       468 to VipWord("CRUST",   "The outer layer of bread or pastry; the surface layer of the Earth"),
       469 to VipWord("EXHALE",  "To breathe out; to give off or emit a gas"),
       470 to VipWord("GALLEON", "A large multi-decked sailing ship used in the 15th to 17th centuries"),
        // ── Cycle 95 (levels 471–475) ──────────────────────────────────────────
       471 to VipWord("TUG",     "To pull hard at something; a small powerful boat for towing"),
       472 to VipWord("GATE",    "A hinged barrier in a wall or fence; an exit or entrance"),
       473 to VipWord("CURVE",   "A line that bends smoothly; to bend in such a line"),
       474 to VipWord("EXPAND",  "To become or make larger; to spread out; to elaborate on"),
       475 to VipWord("GATEWAY", "An opening that can be closed with a gate; a means of access"),
        // ── Cycle 96 (levels 476–480) ──────────────────────────────────────────
       476 to VipWord("WEB",     "A network of fine threads made by a spider; an interconnected system"),
       477 to VipWord("GEAR",    "Toothed wheels that transfer mechanical force; equipment"),
       478 to VipWord("CYCLE",   "A series of events regularly repeated; a bicycle; to ride a bike"),
       479 to VipWord("EXPERT",  "A person with special knowledge or skill in a subject"),
       480 to VipWord("GLACIER", "A slowly moving mass of ice formed from compacted snow"),
        // ── Cycle 97 (levels 481–485) ──────────────────────────────────────────
       481 to VipWord("WIG",     "A head covering made of real or artificial hair"),
       482 to VipWord("GLOW",    "A steady radiance of light or heat; to give out a warm light"),
       483 to VipWord("DAISY",   "A wildflower with white petals and a yellow centre"),
       484 to VipWord("FABRIC",  "Cloth or other material produced by weaving or knitting"),
       485 to VipWord("GORILLA", "The largest living primate, native to the forests of Africa"),
        // ── Cycle 98 (levels 486–490) ──────────────────────────────────────────
       486 to VipWord("WIN",     "To be successful or victorious; to achieve success"),
       487 to VipWord("GLUE",    "A sticky substance used for joining things together"),
       488 to VipWord("DECAY",   "The process of rotting or decomposition; to rot gradually"),
       489 to VipWord("FALCON",  "A fast-flying bird of prey with long pointed wings"),
       490 to VipWord("GRADUAL", "Taking place or changing slowly over time; step by step"),
        // ── Cycle 99 (levels 491–495) ──────────────────────────────────────────
       491 to VipWord("WIT",     "Mental sharpness; the ability to use words and ideas cleverly"),
       492 to VipWord("GOAL",    "The object of a person's ambition; a point scored in sport"),
       493 to VipWord("DEPOT",   "A place for storing goods; a headquarters; a bus or train station"),
       494 to VipWord("FAMINE",  "Extreme scarcity of food in a region or country"),
       495 to VipWord("GRANITE", "A hard grey igneous rock composed mainly of quartz and feldspar"),
        // ── Cycle 100 (levels 496–500) ─────────────────────────────────────────
       496 to VipWord("WOK",     "A bowl-shaped frying pan used in Chinese and Asian cooking"),
       497 to VipWord("GOLD",    "A yellow precious metal used in jewellery and as currency"),
       498 to VipWord("DIZZY",   "Having a sensation of whirling and a tendency to fall"),
       499 to VipWord("FAMOUS",  "Known about by many people; widely acclaimed"),
       500 to VipWord("HABITAT", "The natural home or environment of an animal or plant")
    )
}
