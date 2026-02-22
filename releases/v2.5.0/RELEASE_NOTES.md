# Word Journey v2.5.0 — Release Notes

## Bug Fixes

### 3-Letter & 7-Letter Word Validation Fixed
- **Root cause:** `valid_words.json` stores 3-letter and 7-letter words in lowercase (`"fob"`, `"abandon"`), while 4/5/6-letter words are uppercase. The dictionary parser was storing them as-is, causing lookups to fail.
- **Fix:** Added `.uppercase()` normalization in `parseWordSet()` so all words are stored consistently in uppercase regardless of source format.
- Words like `FOB`, `ACE`, `CAT` (3-letter) and `ABANDON`, `KITCHEN` (7-letter) now validate correctly.

## New Features

### Show Letter Item — Letters Now Appear In The Grid
- Revealed letters are now displayed **directly in the active row tiles** as teal/cyan **HINT** tiles, from left to right across non-green positions.
- If you use 5 Show Letter items on a 5-letter word with no correct letters, all tiles fill in and the word can be submitted immediately (no typing required).
- The old text hint strip below the grid has been removed.
- The leftmost available position is always revealed (deterministic, not random).
- Revealed positions are saved and restored across app restarts.

## Technical

- Added `TileState.HINT` with teal visual styling (dark: `#00838F`, light: `#B3EBF2`, border: `#00BCD4`).
- `GameEngine.prefillPosition()` integrates Show Letter reveals into the game logic — the engine builds the full guess by merging pre-filled positions with user-typed letters.
- `GameEngine.displayInput` provides a positional view of the current row for rendering.
- `SavedGameState` now persists `revealedLetters` for session continuity.
- Added `WordRepository.setWordSetsForTesting()` for clean unit test injection.

## Tests

- **+18 new unit tests** across `GameEngineTest` and `WordRepositoryTest`:
  - `prefillPosition` correctness, `freePositionCount`, `isInputFull`, `displayInput` merging
  - Full word submission from prefilled positions (no typing required)
  - 3/7-letter game win via prefill
  - `isValidWord` case-insensitivity and lowercase normalization (3, 5, 7-letter words)
- Total test count: **450+ tests, 0 failures**
- Updated `GameViewModelTest` for new Show Letter behaviour (no position snackbar)

## Build

- versionCode: 16
- versionName: 2.5.0
- APK: `word-journey-v2.5.0-release.apk` (7.5 MB)
- AAB: `word-journey-v2.5.0-release.aab` (6.7 MB)
