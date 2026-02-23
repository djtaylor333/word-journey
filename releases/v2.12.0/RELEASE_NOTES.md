# Word Journeys v2.12.0 Release Notes

**Release Date:** June 2025  
**Version Code:** 23  
**APK:** `word-journey-v2.12.0-release.apk`  
**AAB:** `word-journey-v2.12.0-release.aab`

---

## What's New

### 🔔 Notification Fix (Critical)
- **Fixed**: Notifications were silently failing on all Android 8+ devices.
- **Root Cause**: WorkManager was using its default factory, which cannot inject Hilt dependencies into `@HiltWorker` classes — so workers would construct but fail to access `PlayerDataStore`.
- **Fix**: `WordJourneysApplication` now implements `Configuration.Provider` and provides a `HiltWorkerFactory`, with WorkManager auto-init disabled in the manifest.
- Both the "Lives Full" and new "Daily Challenge" notifications will now reliably fire.

### 🔔 Daily Challenge Noon Reminder
- New notification fires at **12:00 noon** device time if the daily challenge hasn't been completed.
- Shows streak count: *"You're on a 7-day streak — don't break it now!"*
- Respects the **Daily Challenge Reminder** toggle in Settings → Notifications.
- Auto-reschedules for the following noon after each fire.

### 📖 250+ Words Per Adventure Mode
- Word pools expanded ready for 250+ levels per difficulty:
  - 3-letter: 120 → **250** words
  - 4-letter: 151 → **251** words
  - 5-letter: 194 → **328** words
  - 6-letter: 386 (already sufficient)
  - 7-letter: 120 → **233** words
- **No gameplay change** — levels are still capped at 100 per difficulty. The expanded pool is ready to activate when level cap is raised in a future update.

### 🛠 Dev Mode (Easter Egg)
- **Enable**: Tap the version string in **Settings → About** 10 times consecutively.
- **Disable**: Tap 3 more times when dev mode is active.
- A `[DEV]` badge appears in the About dialog when active.
- **Dev tools** appear in Settings panel and near lives on the Home screen:
  - **Reset Daily Challenges** — clears all daily saves so they can be replayed.
  - **Reset Statistics** — zeroes all cumulative stats (levels, wins, guesses, streaks).
  - **Test Lives Notification** — triggers the lives-full notification immediately.
  - **Test Daily Notification** — triggers the daily challenge notification with a 2-second delay.
- Dev mode state is persisted across sessions.

### 📦 APK Naming Fix
- Release artifacts are now correctly named `word-journey-vX.X.X-release.apk/.aab` instead of the incorrect `bahai-resource-library` prefix.

---

## Testing
- 603 unit tests, **0 failures**.
- New TDD tests added:
  - `DailyChallengeReminderWorkerTest` (7 tests) — noon scheduling invariants
  - `SettingsViewModelTest` +11 tests — daily notification toggle, dev mode enable/disable, dev reset functions
  - `HomeViewModelTest` +3 tests — dev mode state reflection

---

## Technical Changes

| File | Change |
|------|--------|
| `WordJourneysApplication.kt` | Implements `Configuration.Provider`, injects `HiltWorkerFactory` |
| `AndroidManifest.xml` | Removes WorkManager auto-init provider |
| `NotificationChannels.kt` | Added `CHANNEL_DAILY_REMINDER` |
| `DailyChallengeReminderWorker.kt` | **New** — noon daily challenge worker |
| `LivesFullNotificationWorker.kt` | PRIORITY_HIGH, BigTextStyle, vibration |
| `HomeViewModel.kt` | Dev trigger functions, daily reminder scheduling |
| `HomeScreen.kt` | Dev notification test buttons near HeartsBar |
| `SettingsViewModel.kt` | `setNotifyDailyChallenge()`, `setDevModeEnabled()`, dev resets |
| `SettingsScreen.kt` | Daily reminder toggle, 10-tap dev easter egg, DevModePanel |
| `PlayerDataStore.kt` / `SavedGameState.kt` | `notifyDailyChallenge`, `devModeEnabled` fields |
| `PlayerRepository.kt` | `devResetDailyChallenges()`, `devResetStatistics()` |
| `words.json` | Expanded from 971 → 1,448 words |
| `app/build.gradle` | `versionCode 23`, `versionName "2.12.0"`, `archivesBaseName 'word-journey'` |
