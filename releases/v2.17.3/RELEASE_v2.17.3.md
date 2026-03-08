# Word Journey v2.17.3 — Release Notes

## What's New

### Daily Challenge: Word Definitions on Win Screen  
After completing a Daily Challenge, you'll now see the **definition of the word** you just solved — right on the win screen. Learn as you play!

### Smarter Daily Word Pool  
The Daily Challenge word pool has been refined to only include words that have verified English dictionary definitions. This means:
- Every Daily Challenge word you encounter has a real, meaningful definition ready to display
- No more obscure or invalid entries — only genuine English vocabulary
- The pool still contains thousands of words across 4, 5, and 6-letter challenges

---

## Google Play Store Release Notes (copy-paste ready)

**Short description (80 chars max):**
```
Learn words as you play — definitions now shown after Daily Challenges!
```

**Full release notes (500 chars max):**
```
What's New in v2.17.3:

📖 Daily Challenge definitions — After completing a Daily Challenge, discover the meaning of the word you just solved, right on the win screen!

✅ Verified word pool — Daily Challenge words are now filtered to only use vocabulary with confirmed English dictionary definitions.

Whether you knew the word or had to guess, you'll always learn something new. Enjoy!
```

---

## Technical Summary

| Item | Details |
|------|---------|
| Version Name | 2.17.3 |
| Version Code | 39 |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |
| Build Date | 2026-03-08 |

### Changes

- **`DailyChallengeRepository.kt`** — `buildWordList()` now filters out any word not present in `daily_word_definitions.json`, guaranteeing every daily challenge word has a definition to show after winning
- **`GameViewModelTest.kt`** — 3 new tests: win sets `winDefinition`, blank when no definition, `wordHasDefinition` stays `false` during gameplay  
- **`DailyChallengeRepositoryTest.kt`** — 2 new invariant tests: definition-filtered pool always yields a defined word, seed uniqueness across 365 days

### Daily pool coverage after filtering

| Length | Before | After (with defs) |
|--------|--------|-------------------|
| 4-letter | 7,346 words | 3,174 words |
| 5-letter | 16,473 words | 6,678 words |
| 6-letter | 30,646 words | 11,174 words |
| **Total** | **54,465** | **21,026** |

> The pool still has 21,000+ unique words — enough for 57+ years of daily challenges before repeating!

---

## Architecture Note

The definition display was already wired up end-to-end in prior versions:
- `GameViewModel.handleWin()` calls `DailyChallengeRepository.getDefinitionForDailyWord()`
- Sets `winDefinition` in `GameUiState`  
- `WinDialog` renders the definition block when `definition.isNotBlank()`

This release ensures the data layer (word pool filtering) delivers on the UI's promise.
