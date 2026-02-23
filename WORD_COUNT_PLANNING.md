# Word Count & Level Planning

_Last updated: v2.11.0 (Feb 23, 2026)_

---

## Adventure Levels — Current Word Pool (`words.json`)

These words power the main adventure pathway (currently 100 levels). Every word at a given
letter-length is a potential level for that difficulty track.

| Word Length | Current Word Count | Possible Levels at that Length |
|-------------|-------------------|-------------------------------|
| 3-letter    | 120               | 120                           |
| 4-letter    | 151               | 151                           |
| 5-letter    | 194               | 194                           |
| 6-letter    | 386               | 386                           |
| 7-letter    | 120               | 120                           |
| **Total**   | **971**           | **971 possible unique levels** |

### Per-difficulty level capacity

| Difficulty | Word Length Used | Max Possible Levels |
|------------|-----------------|---------------------|
| Easy       | 4-letter        | 151                 |
| Regular    | 5-letter        | 194                 |
| Hard       | 6-letter        | 386                 |
| VIP        | 3–7 cycling     | 971 (all combined)  |

**Current cap**: 100 levels for all difficulties.  
**Headroom**: Easy has 51 spare words, Regular 94, Hard 286, VIP 871 before exhausting the pool.

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

| Milestone   | Level Cap | New Words Needed |
|-------------|-----------|-----------------|
| v2.11.0     | 100       | 0 (current)     |
| v3.0.0      | 200       | ~100 adventure words |
| v3.5.0      | 300       | ~100 adventure words |
| v4.0.0      | 500       | ~200 adventure words |
| Seasonal v1 | +300      | ~300 themed seasonal words |

---

## Action Items

- [ ] Add 300 seasonal words to a new `seasonal_words.json` asset (50 per event)
- [ ] Wire `SeasonalLevelRepository` to draw from per-event word lists
- [ ] Expand `words.json` toward 200-level milestone (need ~100 new words per difficulty)
- [ ] Consider expanding 3-letter pool (currently smallest at 120) to at least 200 for VIP diversity
- [ ] Review 7-letter words (120 in adventure, 725 in daily) — add 80+ to adventure list
