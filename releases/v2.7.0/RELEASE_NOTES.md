# Word Journey v2.7.0 — Release Notes

## New Features & Fixes

### 1. Achievements "Coming Soon" Overlay (Home Screen)
- The Achievements tile on the main menu now shows a proper dark overlay with "🏆 Coming Soon! / Track wins & badges" — matching the style used on the Statistics screen.

### 2. VIP Word Pool De-duplication
- VIP levels no longer share words with standard difficulty levels for the same word length.
- **Partition splits:**
  - 4-letter words: standard uses words 1–76, VIP uses words 77–151
  - 5-letter words: standard uses words 1–97, VIP uses words 98–194
  - 6-letter words: standard uses words 1–193, VIP uses words 194–386
  - 3-letter and 7-letter words: VIP-exclusive (no overlap possible)

### 3. VIP Gold Tint Background
- VIP Game Screen and VIP Level Select Screen now show a subtle golden shimmer overlay distinguishing VIP from standard difficulties.

### 4. Definition Item — Disabled When No Definition
- The 📖 Define item button is now disabled (and labelled "N/A") when the current word has no definition, preventing wasted coins/items.
- A "View 📖" subtitle still appears if the definition was already used this session.

### 5. TDD Test Coverage — 484 Tests, 0 Failures
- **New WordRepository tests:** VIP pool partitioning disjointness (4L, 5L, 6L), full-list behaviour for 3L/7L VIP, `hasDefinition` true/false/override.
- **New GameViewModel tests:** `wordHasDefinition` set correctly for normal game, blank definition, daily challenge, VIP game.
- Updated VIP word cycling test to use a full-sized dataset compatible with partition logic.

## Technical Details
- versionCode: 18
- versionName: 2.7.0
- APK: `word-journey-v2.7.0-release.apk` (7.1 MB)
- AAB: `word-journey-v2.7.0-release.aab` (6.4 MB)
- Min SDK: 26 | Target SDK: 35
- Build: AGP 8.7.0 / Kotlin 2.0.21
