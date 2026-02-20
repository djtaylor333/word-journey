# Word Journeys v1.0.0 — Initial Release

## What is Word Journeys?
A level-based word puzzle game for Android inspired by Wordle. Guess the hidden word within 6 attempts using color-coded feedback.

## Features

### Gameplay
- 3 difficulty modes: **Easy** (4 letters), **Regular** (5 letters), **Hard** (6 letters)
- Level-based progression — each difficulty tracks its own level
- Word is **never revealed** unless guessed correctly — no giving up!
- Word definitions shown on the win screen

### Lives System
- Start with **10 lives**; 1 consumed per level entry
- Lives regenerate 1 every 10 minutes (capped at 10 via regen)
- Bonus lives from level completion can exceed the cap
- Trade 1,000 coins or 3 diamonds for 1 extra life

### Currencies & Items
- **Coins** earned on every win (100 base + 10 × remaining guesses)
- **Diamonds** — premium currency (start with 5)
- **Add a Guess** item (200 coins) — adds an extra guess row
- **Remove a Letter** item (150 coins) — eliminates an incorrect letter

### Store
- In-app purchase stubs (simulated, ready for Google Play Billing integration)
- Buy coins, diamonds, lives, and items

### Audio & Settings
- Background music and sound effects with independent volume controls
- Push notification when lives reach full while app is closed
- Dark mode and high contrast accessibility options

### Technical
- Built with Kotlin + Jetpack Compose (Material 3)
- MVVM architecture, Hilt DI, Room database, DataStore persistence
- Google Play Games cloud save support (ready for production keys)
- Min SDK: Android 10 (API 29)
- Target SDK: Android 15 (API 35)

## Installation
1. Download `word-journeys-v1.0.0-release.apk`
2. On your Android device, enable "Install from unknown sources" if prompted
3. Open the APK file to install
4. Launch Word Journeys and start playing!

## Build Info
- **Version**: 1.0.0
- **Version Code**: 1
- **Build Date**: 2026-02-21
- **APK Size**: ~46 MB
- **Signed**: Debug keystore (sideload-ready)
