# v2.9.0 Release Notes

**Release Date:** 2026-02-22  
**Version Code:** 20  
**Version Name:** 2.9.0

---

## New Features

### ⏱️ Timer Mode
A brand-new game mode where you race against the clock!

- **Three difficulty levels:**
  - 🟢 Easy — 4-letter words, 3-minute start
  - 🟡 Regular — 5-letter words, 4-minute start
  - 🔴 Hard — 6-letter words, 5-minute start
- **+30 seconds** added for every correct word
- **No hearts required** to play
- **Life rewards:** +1 life every 5 correct words (VIP: +2 extra bonus every 10 correct)
- **Items:** Add Guess / Remove Letter / Show Letter use inventory; **Define Word is FREE**
- **Recap screen** with full session stats and best-score comparison
- **Best scores** tracked separately per difficulty in player profile

---

## Improvements

### 👑 VIP Purchase → Immediate Inbox Reward
Previously, VIP daily inbox rewards only appeared on the next app launch. Now they are delivered **immediately** when you purchase a VIP subscription.

### 🏆 Double/Triple Streak Rewards
Daily Challenge streak milestone rewards have been significantly increased:

| Streak | Normal (2×) | VIP (3×) |
|--------|-------------|----------|
| 3 days | 200 coins | 300 coins |
| 7 days | 1,000 coins + 2💎 | 1,500 coins + 3💎 |
| 14 days | 2,000 coins + 6💎 | 3,000 coins + 9💎 |
| 30 days | 4,000 coins + 10💎 + 2❤️ | 6,000 coins + 15💎 + 3❤️ |

A message now shows both normal and VIP reward amounts when a milestone is reached.

### 📊 Win Rate Fix
Win Rate in Statistics now correctly uses **total games played** as the denominator (including daily challenge losses), instead of wins only. The old calculation always showed 100% — this has been fixed.

### 🎮 Menu Renames
- "Themed Level Packs" → **"VIP Levels"**
- "Themed Packs" → **"Theme Level Packs"**

---

## Technical
- versionCode: 19 → 20
- 476 unit tests, all passing
- New: `TimerModeViewModel`, `TimerModeScreen`
- New DataStore keys: `totalDailyChallengesPlayed`, timer best-score records (6 fields)
- `StoreViewModel` now seeds VIP inbox reward immediately on purchase
