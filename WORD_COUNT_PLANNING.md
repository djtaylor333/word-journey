# Word Count & Level Planning

_Last updated: v2.12.0 (June 2025)_

---

## Adventure Levels — Current Word Pool (`words.json`)

These words power the main adventure pathway (currently capped at 100 levels per difficulty, but
the pool is ready to support 250+ levels once the cap is raised). Every word at a given
letter-length is a potential level for that difficulty track.

| Word Length | Word Count | Possible Levels | Status |
|-------------|-----------|-----------------|--------|
| 3-letter    | 250       | 250             | ✅ 250+ ready |
| 4-letter    | 251       | 251             | ✅ 250+ ready |
| 5-letter    | 328       | 328             | ✅ 250+ ready |
| 6-letter    | 386       | 386             | ✅ 250+ ready |
| 7-letter    | 233       | 233             | ✅ 250+ ready |
| **Total**   | **1,448** | **1,448 possible unique levels** | |

### Per-difficulty level capacity

| Difficulty | Word Length Used | Max Possible Levels | Current Cap |
|------------|-----------------|---------------------|-------------|
| Easy       | 4-letter        | 251                 | 100         |
| Regular    | 5-letter        | 328                 | 100         |
| Hard       | 6-letter        | 386                 | 100         |
| VIP        | 3–7 cycling     | 1,448 (all combined) | 100        |

**Current cap**: 100 levels for all difficulties (unchanged in v2.12.0).  
**Headroom**: Easy 151 spare, Regular 228, Hard 286, VIP 1,348 — all pools ready for 250+ levels.

### How to enable 250+ levels

Raise `totalLevels` in `LevelSelectViewModel.kt` (currently `100`) to the desired ceiling:

```kotlin
// LevelSelectViewModel.kt — currently:
private val totalLevels = 100

// Change to enable 250 levels:
private val totalLevels = 250
```

The word pools are large enough to support this today.

---

## Daily Challenge — Word Pool (`valid_words.json`)

The daily challenge draws from a broader, curated word-validity list shared across all players.
Every person playing on the same calendar day sees the same word (DDMMYYYY deterministic seed).

| Word Length | Pool Size |
|-------------|-----------|
| 3-letter    | 392       |
| 4-letter    | 7,597     |
| 5-letter    | 16,801    |
| 6-letter    | 31,032    |
| 7-letter    | 725       |
| **Total**   | **56,547**|

With date-seeded selection there is no risk of repeating a word for hundreds of years for the
larger pools; 3-letter and 7-letter pools cycle roughly every 1–2 years.

---

## Seasonal Events — Planned Word Pool

Each seasonal event gets **50 levels** (planned). Words must be thematically relevant to each
event and span multiple difficulty lengths.

### Planned seasonal level sets

| Season        | Dates (approx)       | Target Levels | Words Needed (4+5+6-letter mix) |
|---------------|----------------------|---------------|---------------------------------|
| Valentine's   | Feb 1 – Feb 28       | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| Easter        | Mar 20 – Apr 30      | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| Summer        | Jun 1 – Aug 31       | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| Halloween     | Oct 1 – Oct 31       | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| Thanksgiving  | Nov 1 – Nov 30       | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| Christmas     | Dec 1 – Dec 31       | 50            | ~17 × 4-letter, ~17 × 5-letter, ~16 × 6-letter |
| **Total**     |                      | **300**       | **~300 themed words**           |

### Seasonal word sourcing plan

For each seasonal set of 50 levels, source thematically appropriate words:

- **Valentine's**: love, rose, heart, kiss, date, dove, ring, vow, poem, bliss, candy, cupid, flame, grace, adore, tulip, angel, match, bloom, sweet …
- **Easter**: lamb, nest, eggs, hunt, lily, hare, chick, bunny, spring, bloom, risen, cross, faith, grace, candy, pastel, feast, Robin, basket …
- **Summer**: wave, swim, sand, heat, surf, lime, dock, sail, grill, melon, coral, glare, ocean, shell, shore, towel, breeze, lemon, mango …
- **Halloween**: ghost, witch, haunt, mask, trick, spell, grave, potion, cobweb, lair, skull, candy, creep, storm, lantern, shadow, grim …
- **Thanksgiving**: feast, grain, corn, pear, pie, yam, cider, gravy, squash, bless, table, pilgrim, turkey, cranberry …
- **Christmas**: snow, gift, star, bell, dove, wrap, elves, carol, holly, wreath, tinsel, frost, glow, angel, reindeer, sleigh …

---

## Growth Roadmap

| Milestone   | Level Cap | New Words Needed | Status |
|-------------|-----------|-----------------|--------|
| v2.11.0     | 100       | 0 (original 971 words) | ✅ shipped |
| **v2.12.0** | **100**   | **+477 words (pools ready for 250+)** | ✅ shipped |
| v3.0.0      | 250       | 0 (pool already ready!) | Ready to enable |
| v3.5.0      | 300       | ~50 more per length | Planned |
| v4.0.0      | 500       | ~200+ more per length | Planned |
| Seasonal v1 | +300      | ~300 themed seasonal words | Planned |

---

## Action Items

- [x] ~~Expand 3-letter pool to 250+~~ (done in v2.12.0 — now 250)
- [x] ~~Expand 4-letter pool to 250+~~ (done in v2.12.0 — now 251)
- [x] ~~Expand 5-letter pool to 250+~~ (done in v2.12.0 — now 328)
- [x] ~~Expand 7-letter pool toward 250+~~ (done in v2.12.0 — now 233, approaching 250)
- [ ] **Raise `totalLevels` to 250** in `LevelSelectViewModel.kt` when ready to unlock (no new words needed!)
- [ ] Add 17 more 7-letter words to reach 250 (low priority)
- [ ] Add 300 seasonal words to a new `seasonal_words.json` asset (50 per event)
- [ ] Wire `SeasonalLevelRepository` to draw from per-event word lists
