# Word Journey v2.6.0 — Release Notes

## Bug Fixes

### VIP Mode 7-Letter Words Now Display Correctly
- **Root cause:** `GameGrid.kt` tile size calculation fell through to `else -> 50.dp` for 7-letter words, causing 7 tiles × 50dp + 6 spacers × 6dp = 386dp to overflow the screen.
- **Fix:** Explicit tile and font sizes for all word lengths — 7-letter words now use 44dp tiles and 16sp font, fitting comfortably on all screen sizes.
- VIP levels 5, 10, 15, 20… (7-letter cycle) are now fully playable.

### Tile Colors Animate Smoothly on Theme Change
- Previously, game tiles switched colour instantly when changing themes while the keyboard animated smoothly — creating a jarring mismatch.
- Tile `bgColor` and `borderColor` in `AnimatedTile.kt` are now wrapped in `animateColorAsState(tween(300))`, matching the keyboard's transition behaviour.

## New Features

### Cancel VIP Subscription
- New **VIP Subscription** section in Settings allows players to cancel their VIP membership at any time.
- A confirmation dialog prevents accidental cancellation.
- If a VIP-exclusive theme is active when VIP is cancelled, the theme automatically falls back to Classic.
- Non-VIP users see an informational card explaining VIP benefits.

### Theme Artwork on Every Screen
- Decorative theme backgrounds are now visible on **all screens**: Settings, Statistics, Level Select, and Store (previously only Home and Game).
- `ThemeBackground.kt` completely rewritten with:
  - Filled + stroked shapes (previously stroke-only)
  - Dual gradient layers (vertical + radial) at 3.7× higher opacity
  - Larger, more detailed elements: hearts, snowflakes, stars, leaves, diamonds
  - 60 distributed positions (up from 40)

### VIP Word-Length Badges on Level Select
- VIP level nodes now display a small coloured badge showing the expected word length (e.g. **3L**, **4L**, **5L**, **6L**, **7L**) so players know what to expect before tapping.

### Achievements — Coming Soon
- A new **Achievements** section has been added to the Statistics screen.
- Covered by a "Coming Soon" overlay — full achievement tracking (First Win, Level Master, Daily Streak, Coin Collector, VIP Explorer) is in active development.

## Tests

- **+21 new unit tests:**
  - `DifficultyTest` (16 tests): full VIP word-length cycle validation, difficulty constants
  - `SettingsViewModelTest` — 5 new `cancelVip` tests: isVip reset, VIP-theme fallback, non-VIP theme retention, save verification, idempotent cancel
- **Total: 471 tests, 0 failures** across 16 test suites

## Build

- versionCode: 17
- versionName: 2.6.0
- APK: `word-journey-v2.6.0-release.apk` (7.1 MB)
- AAB: `word-journey-v2.6.0-release.aab` (6.4 MB)
