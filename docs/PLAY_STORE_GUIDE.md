# Word Journeys — Play Console Release Guide

## Your Play Console
- URL: https://play.google.com/console/u/1/developers/7118341667103181838/app/4972886645696508498/app-dashboard

## AAB File Location
```
word-journey\app\build\outputs\bundle\release\app-release.aab  (6.29 MB)
```

---

## Step-by-Step Checklist

### 1. App Signing (one-time)
Play Console → Setup → App signing
- Choose **"Let Google manage and protect your app signing key"**
- Upload the AAB — Google will extract and manage the signing key
- Your upload key is in `word-journey-release.jks` (keep this safe!)

### 2. Store Listing
Play Console → Grow → Store presence → Main store listing

Copy from `docs/play-store-listing.md`:
- **App name**: Word Journeys
- **Short description**: A Wordle-inspired word puzzle adventure with 700+ levels across 3 difficulties.
- **Full description**: (see play-store-listing.md)

**Graphics you need to create/capture:**
- [ ] **App icon**: 512×512 PNG (export from Android Studio: res → New → Image Asset → Legacy tab → 512×512)
- [ ] **Feature graphic**: 1024×500 PNG (banner image for Play Store)
- [ ] **Phone screenshots**: At least 2 (take from device/emulator, 16:9 ratio)
  - Home screen
  - Gameplay in progress
  - Level select
  - Store screen

### 3. Content Rating
Play Console → Policy → App content → Content rating
- Start questionnaire
- Category: **Games** → **Casual Games**
- All questions about violence, sexuality, language, drugs, gambling: **No**
- User interaction: **No** (single player)
- Generates: **ESRB: Everyone / PEGI 3**

### 4. Target Audience
Play Console → Policy → App content → Target audience and content
- Target age group: **All ages (not specifically for children)**
- NOT a children's app

### 5. Data Safety
Play Console → Policy → App content → Data safety
- Reference: `docs/data-safety-answers.md`
- Does your app collect or share user data? **No**
- Is all data encrypted in transit? **Yes**
- Can users request data deletion? **Yes** (clear app data)

### 6. Privacy Policy
Play Console → Policy → App content → Privacy policy
- URL: `https://djtaylor333.github.io/word-journey/privacy-policy.html`

### 7. App Category & Contact
Play Console → Grow → Store presence → Store settings
- App category: **Games → Word**
- Email: djtaylor333@gmail.com
- Website: https://djtaylor333.github.io/word-journey/

### 8. Create Release
Play Console → Release → Production
1. Click **"Create new release"**
2. Upload: `app\build\outputs\bundle\release\app-release.aab`
3. Release name: `1.3.0`
4. Release notes:
   ```
   Word Journeys v1.3.0

   - 700+ word puzzle levels across Easy, Regular, and Hard modes
   - Earn coins and diamonds as you progress
   - Use power-up items: Add a Guess, Remove a Letter, Definition hint
   - Buy items from the Store or use your inventory
   - Trade diamonds for extra lives
   - Dark mode, customizable audio, and more
   ```
5. Click **"Review release"** then **"Start rollout to Production"** (or Internal Testing first)

### 9. (Recommended) Internal Testing First
Instead of going straight to Production:
1. Play Console → Release → Testing → Internal testing
2. Create a release with the same AAB
3. Add your email as a tester
4. Test the install via Play Store link
5. Once satisfied, promote to Production

---

## Important Notes
- **First review** by Google typically takes 1-3 days
- **Keystore backup**: `word-journey-release.jks` — if you lose this, you can never update the app
- **After first install of v1.3.0**: Clear app data to force word database repopulation
- **App signing**: Once you upload to Play Console, Google manages the app signing key. Your `word-journey-release.jks` becomes the "upload key"
