## What's New in v2.11.0

### Bug Fix: Daily Challenge Reset
- **Stale save detection** — Daily challenge games saved from a previous day are now discarded on the next day, so you always start fresh with a new word.
- **Date-seeded word selection** — Words are now chosen using a DDMMYYYY numeric seed, ensuring every player worldwide gets the same word on the same day.

### Seasonal Theme Locks
- **Future events** (Coming Soon lock) — Themes for upcoming seasonal events are locked for all users until the event begins.
- **Past events** (VIP Only lock) — Themes from events that have already ended can only be purchased by VIP members.
- **Active events** — Themes for currently running seasonal events are available to all players.

### More Adventures Coming Soon
- A "More Adventures Coming Soon!" banner now appears at the bottom of the level select screen after scrolling past level 100.

### Tests
- 17 new tests in DailyChallengeRepositoryTest (seed logic, stale save detection)
- 18 new tests in SeasonalThemeManagerTest (lock status, season detection)
- 4 new tests in LevelSelectViewModelTest
- 6 new tests in SettingsViewModelTest (seasonal purchase lock rules)
- Total: 582 tests, 0 failures

### Planning
- Added WORD_COUNT_PLANNING.md with full word pool analysis, level capacity, and seasonal content roadmap.
