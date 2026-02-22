# Word Journey v2.8.0 Release Notes

## What's New

### 📬 Inbox System
- New Inbox icon (with badge) on the main menu — shows unclaimed item count
- VIP daily rewards and seasonal event rewards now accumulate in your Inbox instead of being applied instantly
- Items stay in Inbox unclaimed until you explicitly tap "Claim" — even if you miss days
- "Claim All" button to collect everything at once
- Clean inbox history showing claimed rewards

### 🔥 Per-Length Daily Challenge Streaks  
- Each challenge length (4, 5, 6 letters) now tracks its **own consecutive-day streak**
- Streak indicator (🔥 N-day streak) shown on each Challenge Card
- Overall streak, per-length streaks, best streaks, and wins counted separately

### ✅ Consecutive-Day Streak Fix
- Daily Challenge streaks now correctly require **consecutive days** — a gap of 2+ days resets to 1
- Playing the same challenge type twice on the same day preserves the streak

### 🔒 VIP-Only Items in Daily Challenge
- Items bar in Daily Challenge is now blurred/locked for non-VIP players
- Overlay shows "VIP Only — Items unavailable in free Daily Challenge"
- VIP players can use all items as before

### 🔔 Notification Toggle Fix
- Re-enabling the "Lives Replenished" notification now works correctly
- Fixed issue where the permission dialog failed silently on Android 13+ when permission was already granted

## Technical Details
- Room database migrated from version 2 → 3 (new `inbox_items` table)
- 508 unit tests, 0 failures
- APK: 7.3 MB | AAB: 6.6 MB
- versionCode: 19 | versionName: 2.8.0
