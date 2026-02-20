# Word Journeys

A level-based word puzzle game for Android, inspired by Wordle. Guess the hidden word within a limited number of attempts across three difficulty modes.

## Features

- **Three Difficulty Modes**: Easy (4 letters), Regular (5 letters), Hard (6 letters)
- **Level-Based Progression**: Advance through increasingly challenging levels
- **Lives System**: Start with 10 lives; regenerate 1 life every 10 minutes (cap 10 via timer, bonus lives from level completion can exceed cap)
- **Dual Currency**: Earn coins from winning levels; collect diamonds for premium purchases
- **In-Game Items**: "Add a Guess" (200 coins) and "Remove a Letter" (150 coins)
- **Word Definitions**: Learn the meaning of each word upon solving it
- **No Giving Up**: The word is never revealed until you guess it correctly
- **Audio**: Background music and sound effects with full volume controls
- **Notifications**: Get notified when your lives are fully replenished
- **Cloud Saves**: Google Play Games integration (coming soon)
- **Material 3 Design**: Dark/light mode, high contrast option

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Repository pattern
- **DI**: Hilt
- **Database**: Room (pre-populated word dictionary)
- **Persistence**: DataStore Preferences
- **Build**: Gradle 8.10.2, AGP 8.7.0
- **Min SDK**: 29 (Android 10)

## Building

### Prerequisites
- Android Studio Ladybug or newer
- JDK 21
- Android SDK 35

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK
```bash
./gradlew assembleRelease
```

## Release

Run the release script to bump version, tag, and push:

```powershell
.\build-release.ps1 -BumpType patch   # or minor, major
```

This triggers GitHub Actions to build the APK and create a GitHub Release automatically.

## Project Structure

```
app/src/main/java/com/djtaylor/wordjourney/
├── domain/          # Models, use cases
├── data/            # Room DB, DataStore, repositories
├── ui/              # Compose screens (home, game, store, settings)
├── audio/           # Music & SFX management
├── auth/            # Play Games authentication
├── billing/         # IAP stubs (swap-in ready)
├── notifications/   # Lives-full notification worker
└── di/              # Hilt modules
```

## License

Private project - All rights reserved.
