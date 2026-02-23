package com.djtaylor.wordjourney.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import com.djtaylor.wordjourney.domain.model.SavedGameState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "word_journeys_prefs")

@Singleton
class PlayerDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val ds = context.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    companion object {
        val KEY_COINS                          = longPreferencesKey("coins")
        val KEY_DIAMONDS                       = intPreferencesKey("diamonds")
        val KEY_LIVES                          = intPreferencesKey("lives")
        val KEY_LAST_REGEN_TS                  = longPreferencesKey("last_regen_ts")
        val KEY_EASY_LEVEL                     = intPreferencesKey("easy_level")
        val KEY_REGULAR_LEVEL                  = intPreferencesKey("regular_level")
        val KEY_HARD_LEVEL                     = intPreferencesKey("hard_level")
        val KEY_EASY_BONUS_COUNTER             = intPreferencesKey("easy_bonus_counter")
        val KEY_REGULAR_BONUS_COUNTER          = intPreferencesKey("regular_bonus_counter")
        val KEY_HARD_BONUS_COUNTER             = intPreferencesKey("hard_bonus_counter")
        val KEY_ADD_GUESS_ITEMS                = intPreferencesKey("add_guess_items")
        val KEY_REMOVE_LETTER_ITEMS            = intPreferencesKey("remove_letter_items")
        val KEY_DEFINITION_ITEMS               = intPreferencesKey("definition_items")
        val KEY_SHOW_LETTER_ITEMS              = intPreferencesKey("show_letter_items")
        // Streaks
        val KEY_DAILY_CHALLENGE_STREAK         = intPreferencesKey("daily_challenge_streak")
        val KEY_DAILY_CHALLENGE_BEST_STREAK    = intPreferencesKey("daily_challenge_best_streak")
        val KEY_DAILY_CHALLENGE_LAST_DATE      = stringPreferencesKey("daily_challenge_last_date")
        val KEY_LOGIN_STREAK                   = intPreferencesKey("login_streak")
        val KEY_LOGIN_BEST_STREAK              = intPreferencesKey("login_best_streak")
        val KEY_LAST_LOGIN_DATE                = stringPreferencesKey("last_login_date")
        // Per-length daily challenge streaks
        val KEY_DAILY_STREAK_4                 = intPreferencesKey("daily_streak_4")
        val KEY_DAILY_STREAK_5                 = intPreferencesKey("daily_streak_5")
        val KEY_DAILY_STREAK_6                 = intPreferencesKey("daily_streak_6")
        val KEY_DAILY_BEST_STREAK_4            = intPreferencesKey("daily_best_streak_4")
        val KEY_DAILY_BEST_STREAK_5            = intPreferencesKey("daily_best_streak_5")
        val KEY_DAILY_BEST_STREAK_6            = intPreferencesKey("daily_best_streak_6")
        val KEY_DAILY_LAST_DATE_4              = stringPreferencesKey("daily_last_date_4")
        val KEY_DAILY_LAST_DATE_5              = stringPreferencesKey("daily_last_date_5")
        val KEY_DAILY_LAST_DATE_6              = stringPreferencesKey("daily_last_date_6")
        val KEY_DAILY_WINS_4                   = intPreferencesKey("daily_wins_4")
        val KEY_DAILY_WINS_5                   = intPreferencesKey("daily_wins_5")
        val KEY_DAILY_WINS_6                   = intPreferencesKey("daily_wins_6")
        // Statistics
        val KEY_TOTAL_COINS_EARNED             = longPreferencesKey("total_coins_earned")
        val KEY_TOTAL_LEVELS_COMPLETED         = intPreferencesKey("total_levels_completed")
        val KEY_TOTAL_GUESSES                  = intPreferencesKey("total_guesses")
        val KEY_TOTAL_WINS                     = intPreferencesKey("total_wins")
        val KEY_TOTAL_ITEMS_USED               = intPreferencesKey("total_items_used")
        val KEY_TOTAL_DAILY_COMPLETED          = intPreferencesKey("total_daily_completed")
        val KEY_TOTAL_DAILY_PLAYED             = intPreferencesKey("total_daily_played")
        // Time played tracking (per category, milliseconds)
        val KEY_TOTAL_TIME_PLAYED_MS           = longPreferencesKey("total_time_played_ms")
        val KEY_EASY_TIME_PLAYED_MS            = longPreferencesKey("easy_time_played_ms")
        val KEY_REGULAR_TIME_PLAYED_MS         = longPreferencesKey("regular_time_played_ms")
        val KEY_HARD_TIME_PLAYED_MS            = longPreferencesKey("hard_time_played_ms")
        val KEY_VIP_TIME_PLAYED_MS             = longPreferencesKey("vip_time_played_ms")
        val KEY_DAILY_TIME_PLAYED_MS           = longPreferencesKey("daily_time_played_ms")
        val KEY_TIMER_TIME_PLAYED_MS           = longPreferencesKey("timer_time_played_ms")
        // Timer Mode best records
        val KEY_TIMER_BEST_LEVELS_EASY         = intPreferencesKey("timer_best_levels_easy")
        val KEY_TIMER_BEST_LEVELS_REGULAR      = intPreferencesKey("timer_best_levels_regular")
        val KEY_TIMER_BEST_LEVELS_HARD         = intPreferencesKey("timer_best_levels_hard")
        val KEY_TIMER_BEST_TIME_EASY           = intPreferencesKey("timer_best_time_easy")
        val KEY_TIMER_BEST_TIME_REGULAR        = intPreferencesKey("timer_best_time_regular")
        val KEY_TIMER_BEST_TIME_HARD           = intPreferencesKey("timer_best_time_hard")
        // VIP
        val KEY_IS_VIP                         = booleanPreferencesKey("is_vip")
        val KEY_VIP_EXPIRY                     = longPreferencesKey("vip_expiry")
        val KEY_LAST_VIP_REWARD_DATE           = stringPreferencesKey("last_vip_reward_date")
        // New player bonus
        val KEY_HAS_RECEIVED_NEW_PLAYER_BONUS  = booleanPreferencesKey("has_received_new_player_bonus")
        // VIP Level pack
        val KEY_VIP_LEVEL                      = intPreferencesKey("vip_level")
        val KEY_VIP_BONUS_COUNTER              = intPreferencesKey("vip_bonus_counter")
        // Settings
        val KEY_MUSIC_ENABLED                  = booleanPreferencesKey("music_enabled")
        val KEY_MUSIC_VOLUME                   = floatPreferencesKey("music_volume")
        val KEY_SFX_ENABLED                    = booleanPreferencesKey("sfx_enabled")
        val KEY_SFX_VOLUME                     = floatPreferencesKey("sfx_volume")
        val KEY_NOTIFY_LIVES_FULL              = booleanPreferencesKey("notify_lives_full")
        val KEY_NOTIFY_DAILY_CHALLENGE         = booleanPreferencesKey("notify_daily_challenge")
        val KEY_DEV_MODE_ENABLED               = booleanPreferencesKey("dev_mode_enabled")
        val KEY_HIGH_CONTRAST                  = booleanPreferencesKey("high_contrast")
        val KEY_DARK_MODE                      = booleanPreferencesKey("dark_mode")
        val KEY_COLORBLIND_MODE                = stringPreferencesKey("colorblind_mode")
        val KEY_TEXT_SCALE_FACTOR              = floatPreferencesKey("text_scale_factor")
        val KEY_PLAY_GAMES_SIGNED_IN           = booleanPreferencesKey("play_games_signed_in")
        // Cosmetics
        val KEY_SELECTED_TILE_THEME            = stringPreferencesKey("selected_tile_theme")
        val KEY_SELECTED_KEYBOARD_THEME        = stringPreferencesKey("selected_keyboard_theme")
        val KEY_OWNED_TILE_THEMES              = stringPreferencesKey("owned_tile_themes")
        val KEY_OWNED_KEYBOARD_THEMES          = stringPreferencesKey("owned_keyboard_themes")
        // Theme system
        val KEY_SELECTED_THEME                 = stringPreferencesKey("selected_theme")
        val KEY_OWNED_THEMES                   = stringPreferencesKey("owned_themes")
        // Onboarding
        val KEY_HAS_COMPLETED_ONBOARDING       = booleanPreferencesKey("has_completed_onboarding")
        val KEY_FIRST_LAUNCH                   = booleanPreferencesKey("first_launch")
        // In-progress serialized game states
        val KEY_GAME_STATE_EASY                = stringPreferencesKey("game_state_easy")
        val KEY_GAME_STATE_REGULAR             = stringPreferencesKey("game_state_regular")
        val KEY_GAME_STATE_HARD                = stringPreferencesKey("game_state_hard")
        val KEY_GAME_STATE_DAILY               = stringPreferencesKey("game_state_daily")
        val KEY_GAME_STATE_DAILY_4             = stringPreferencesKey("game_state_daily_4")
        val KEY_GAME_STATE_DAILY_5             = stringPreferencesKey("game_state_daily_5")
        val KEY_GAME_STATE_DAILY_6             = stringPreferencesKey("game_state_daily_6")
    }

    // ── Flows ─────────────────────────────────────────────────────────────────
    val playerProgressFlow: Flow<PlayerProgress> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            PlayerProgress(
                coins             = prefs[KEY_COINS] ?: 0L,
                diamonds          = prefs[KEY_DIAMONDS] ?: 5,
                lives             = prefs[KEY_LIVES] ?: 10,
                lastLifeRegenTimestamp = prefs[KEY_LAST_REGEN_TS] ?: 0L,
                easyLevel         = prefs[KEY_EASY_LEVEL] ?: 1,
                regularLevel      = prefs[KEY_REGULAR_LEVEL] ?: 1,
                hardLevel         = prefs[KEY_HARD_LEVEL] ?: 1,
                easyLevelsCompletedSinceBonusLife    = prefs[KEY_EASY_BONUS_COUNTER] ?: 0,
                regularLevelsCompletedSinceBonusLife = prefs[KEY_REGULAR_BONUS_COUNTER] ?: 0,
                hardLevelsCompletedSinceBonusLife    = prefs[KEY_HARD_BONUS_COUNTER] ?: 0,
                addGuessItems     = prefs[KEY_ADD_GUESS_ITEMS] ?: 0,
                removeLetterItems = prefs[KEY_REMOVE_LETTER_ITEMS] ?: 0,
                definitionItems   = prefs[KEY_DEFINITION_ITEMS] ?: 0,
                showLetterItems   = prefs[KEY_SHOW_LETTER_ITEMS] ?: 0,
                dailyChallengeStreak     = prefs[KEY_DAILY_CHALLENGE_STREAK] ?: 0,
                dailyChallengeBestStreak = prefs[KEY_DAILY_CHALLENGE_BEST_STREAK] ?: 0,
                dailyChallengeLastDate   = prefs[KEY_DAILY_CHALLENGE_LAST_DATE] ?: "",
                loginStreak       = prefs[KEY_LOGIN_STREAK] ?: 0,
                loginBestStreak   = prefs[KEY_LOGIN_BEST_STREAK] ?: 0,
                lastLoginDate     = prefs[KEY_LAST_LOGIN_DATE] ?: "",
                dailyStreak4      = prefs[KEY_DAILY_STREAK_4] ?: 0,
                dailyStreak5      = prefs[KEY_DAILY_STREAK_5] ?: 0,
                dailyStreak6      = prefs[KEY_DAILY_STREAK_6] ?: 0,
                dailyBestStreak4  = prefs[KEY_DAILY_BEST_STREAK_4] ?: 0,
                dailyBestStreak5  = prefs[KEY_DAILY_BEST_STREAK_5] ?: 0,
                dailyBestStreak6  = prefs[KEY_DAILY_BEST_STREAK_6] ?: 0,
                dailyLastDate4    = prefs[KEY_DAILY_LAST_DATE_4] ?: "",
                dailyLastDate5    = prefs[KEY_DAILY_LAST_DATE_5] ?: "",
                dailyLastDate6    = prefs[KEY_DAILY_LAST_DATE_6] ?: "",
                dailyWins4        = prefs[KEY_DAILY_WINS_4] ?: 0,
                dailyWins5        = prefs[KEY_DAILY_WINS_5] ?: 0,
                dailyWins6        = prefs[KEY_DAILY_WINS_6] ?: 0,
                totalCoinsEarned  = prefs[KEY_TOTAL_COINS_EARNED] ?: 0L,
                totalLevelsCompleted = prefs[KEY_TOTAL_LEVELS_COMPLETED] ?: 0,
                totalGuesses      = prefs[KEY_TOTAL_GUESSES] ?: 0,
                totalWins         = prefs[KEY_TOTAL_WINS] ?: 0,
                totalItemsUsed    = prefs[KEY_TOTAL_ITEMS_USED] ?: 0,
                totalDailyChallengesCompleted = prefs[KEY_TOTAL_DAILY_COMPLETED] ?: 0,
                totalDailyChallengesPlayed    = prefs[KEY_TOTAL_DAILY_PLAYED] ?: 0,
                totalTimePlayedMs      = prefs[KEY_TOTAL_TIME_PLAYED_MS] ?: 0L,
                easyTimePlayedMs       = prefs[KEY_EASY_TIME_PLAYED_MS] ?: 0L,
                regularTimePlayedMs    = prefs[KEY_REGULAR_TIME_PLAYED_MS] ?: 0L,
                hardTimePlayedMs       = prefs[KEY_HARD_TIME_PLAYED_MS] ?: 0L,
                vipTimePlayedMs        = prefs[KEY_VIP_TIME_PLAYED_MS] ?: 0L,
                dailyTimePlayedMs      = prefs[KEY_DAILY_TIME_PLAYED_MS] ?: 0L,
                timerTimePlayedMs      = prefs[KEY_TIMER_TIME_PLAYED_MS] ?: 0L,
                timerBestLevelsEasy    = prefs[KEY_TIMER_BEST_LEVELS_EASY] ?: 0,
                timerBestLevelsRegular = prefs[KEY_TIMER_BEST_LEVELS_REGULAR] ?: 0,
                timerBestLevelsHard    = prefs[KEY_TIMER_BEST_LEVELS_HARD] ?: 0,
                timerBestTimeSecsEasy    = prefs[KEY_TIMER_BEST_TIME_EASY] ?: 0,
                timerBestTimeSecsRegular = prefs[KEY_TIMER_BEST_TIME_REGULAR] ?: 0,
                timerBestTimeSecsHard    = prefs[KEY_TIMER_BEST_TIME_HARD] ?: 0,
                isVip             = prefs[KEY_IS_VIP] ?: false,
                vipExpiryTimestamp = prefs[KEY_VIP_EXPIRY] ?: 0L,
                lastVipRewardDate = prefs[KEY_LAST_VIP_REWARD_DATE] ?: "",
                hasReceivedNewPlayerBonus = prefs[KEY_HAS_RECEIVED_NEW_PLAYER_BONUS] ?: false,
                musicEnabled      = prefs[KEY_MUSIC_ENABLED] ?: true,
                musicVolume       = prefs[KEY_MUSIC_VOLUME] ?: 0.7f,
                sfxEnabled        = prefs[KEY_SFX_ENABLED] ?: true,
                sfxVolume         = prefs[KEY_SFX_VOLUME] ?: 0.8f,
                notifyLivesFull          = prefs[KEY_NOTIFY_LIVES_FULL] ?: true,
                notifyDailyChallenge     = prefs[KEY_NOTIFY_DAILY_CHALLENGE] ?: true,
                highContrast      = prefs[KEY_HIGH_CONTRAST] ?: false,
                darkMode          = prefs[KEY_DARK_MODE] ?: true,
                colorblindMode    = prefs[KEY_COLORBLIND_MODE] ?: "none",
                textScaleFactor   = prefs[KEY_TEXT_SCALE_FACTOR] ?: 1.0f,
                playGamesSignedIn = prefs[KEY_PLAY_GAMES_SIGNED_IN] ?: false,
                selectedTheme         = prefs[KEY_SELECTED_THEME] ?: "classic",
                ownedThemes           = prefs[KEY_OWNED_THEMES] ?: "classic,ocean_breeze,forest_grove",
                selectedTileTheme     = prefs[KEY_SELECTED_TILE_THEME] ?: "default",
                selectedKeyboardTheme = prefs[KEY_SELECTED_KEYBOARD_THEME] ?: "default",
                ownedTileThemes       = prefs[KEY_OWNED_TILE_THEMES] ?: "default",
                ownedKeyboardThemes   = prefs[KEY_OWNED_KEYBOARD_THEMES] ?: "default",
                hasCompletedOnboarding = prefs[KEY_HAS_COMPLETED_ONBOARDING] ?: false,
                vipLevel              = prefs[KEY_VIP_LEVEL] ?: 1,
                vipLevelsCompletedSinceBonusLife = prefs[KEY_VIP_BONUS_COUNTER] ?: 0,
                devModeEnabled        = prefs[KEY_DEV_MODE_ENABLED] ?: false
            )
        }

    val isFirstLaunch: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_FIRST_LAUNCH] ?: true }

    // ── Update helpers ────────────────────────────────────────────────────────
    suspend fun savePlayerProgress(progress: PlayerProgress) {
        ds.edit { prefs ->
            prefs[KEY_COINS]                    = progress.coins
            prefs[KEY_DIAMONDS]                 = progress.diamonds
            prefs[KEY_LIVES]                    = progress.lives
            prefs[KEY_LAST_REGEN_TS]            = progress.lastLifeRegenTimestamp
            prefs[KEY_EASY_LEVEL]               = progress.easyLevel
            prefs[KEY_REGULAR_LEVEL]            = progress.regularLevel
            prefs[KEY_HARD_LEVEL]               = progress.hardLevel
            prefs[KEY_EASY_BONUS_COUNTER]       = progress.easyLevelsCompletedSinceBonusLife
            prefs[KEY_REGULAR_BONUS_COUNTER]    = progress.regularLevelsCompletedSinceBonusLife
            prefs[KEY_HARD_BONUS_COUNTER]       = progress.hardLevelsCompletedSinceBonusLife
            prefs[KEY_ADD_GUESS_ITEMS]          = progress.addGuessItems
            prefs[KEY_REMOVE_LETTER_ITEMS]      = progress.removeLetterItems
            prefs[KEY_DEFINITION_ITEMS]         = progress.definitionItems
            prefs[KEY_SHOW_LETTER_ITEMS]        = progress.showLetterItems
            prefs[KEY_DAILY_CHALLENGE_STREAK]   = progress.dailyChallengeStreak
            prefs[KEY_DAILY_CHALLENGE_BEST_STREAK] = progress.dailyChallengeBestStreak
            prefs[KEY_DAILY_CHALLENGE_LAST_DATE]   = progress.dailyChallengeLastDate
            prefs[KEY_LOGIN_STREAK]             = progress.loginStreak
            prefs[KEY_LOGIN_BEST_STREAK]        = progress.loginBestStreak
            prefs[KEY_LAST_LOGIN_DATE]          = progress.lastLoginDate
            prefs[KEY_DAILY_STREAK_4]           = progress.dailyStreak4
            prefs[KEY_DAILY_STREAK_5]           = progress.dailyStreak5
            prefs[KEY_DAILY_STREAK_6]           = progress.dailyStreak6
            prefs[KEY_DAILY_BEST_STREAK_4]      = progress.dailyBestStreak4
            prefs[KEY_DAILY_BEST_STREAK_5]      = progress.dailyBestStreak5
            prefs[KEY_DAILY_BEST_STREAK_6]      = progress.dailyBestStreak6
            prefs[KEY_DAILY_LAST_DATE_4]        = progress.dailyLastDate4
            prefs[KEY_DAILY_LAST_DATE_5]        = progress.dailyLastDate5
            prefs[KEY_DAILY_LAST_DATE_6]        = progress.dailyLastDate6
            prefs[KEY_DAILY_WINS_4]             = progress.dailyWins4
            prefs[KEY_DAILY_WINS_5]             = progress.dailyWins5
            prefs[KEY_DAILY_WINS_6]             = progress.dailyWins6
            prefs[KEY_TOTAL_COINS_EARNED]       = progress.totalCoinsEarned
            prefs[KEY_TOTAL_LEVELS_COMPLETED]   = progress.totalLevelsCompleted
            prefs[KEY_TOTAL_GUESSES]            = progress.totalGuesses
            prefs[KEY_TOTAL_WINS]               = progress.totalWins
            prefs[KEY_TOTAL_ITEMS_USED]         = progress.totalItemsUsed
            prefs[KEY_TOTAL_DAILY_COMPLETED]    = progress.totalDailyChallengesCompleted
            prefs[KEY_TOTAL_DAILY_PLAYED]       = progress.totalDailyChallengesPlayed
            prefs[KEY_TOTAL_TIME_PLAYED_MS]     = progress.totalTimePlayedMs
            prefs[KEY_EASY_TIME_PLAYED_MS]       = progress.easyTimePlayedMs
            prefs[KEY_REGULAR_TIME_PLAYED_MS]    = progress.regularTimePlayedMs
            prefs[KEY_HARD_TIME_PLAYED_MS]       = progress.hardTimePlayedMs
            prefs[KEY_VIP_TIME_PLAYED_MS]        = progress.vipTimePlayedMs
            prefs[KEY_DAILY_TIME_PLAYED_MS]      = progress.dailyTimePlayedMs
            prefs[KEY_TIMER_TIME_PLAYED_MS]      = progress.timerTimePlayedMs
            prefs[KEY_TIMER_BEST_LEVELS_EASY]   = progress.timerBestLevelsEasy
            prefs[KEY_TIMER_BEST_LEVELS_REGULAR]= progress.timerBestLevelsRegular
            prefs[KEY_TIMER_BEST_LEVELS_HARD]   = progress.timerBestLevelsHard
            prefs[KEY_TIMER_BEST_TIME_EASY]     = progress.timerBestTimeSecsEasy
            prefs[KEY_TIMER_BEST_TIME_REGULAR]  = progress.timerBestTimeSecsRegular
            prefs[KEY_TIMER_BEST_TIME_HARD]     = progress.timerBestTimeSecsHard
            prefs[KEY_IS_VIP]                   = progress.isVip
            prefs[KEY_VIP_EXPIRY]               = progress.vipExpiryTimestamp
            prefs[KEY_LAST_VIP_REWARD_DATE]     = progress.lastVipRewardDate
            prefs[KEY_HAS_RECEIVED_NEW_PLAYER_BONUS] = progress.hasReceivedNewPlayerBonus
            prefs[KEY_MUSIC_ENABLED]            = progress.musicEnabled
            prefs[KEY_MUSIC_VOLUME]             = progress.musicVolume
            prefs[KEY_SFX_ENABLED]              = progress.sfxEnabled
            prefs[KEY_SFX_VOLUME]               = progress.sfxVolume
            prefs[KEY_NOTIFY_LIVES_FULL]        = progress.notifyLivesFull
            prefs[KEY_HIGH_CONTRAST]            = progress.highContrast
            prefs[KEY_DARK_MODE]                = progress.darkMode
            prefs[KEY_COLORBLIND_MODE]          = progress.colorblindMode
            prefs[KEY_TEXT_SCALE_FACTOR]        = progress.textScaleFactor
            prefs[KEY_PLAY_GAMES_SIGNED_IN]     = progress.playGamesSignedIn
            prefs[KEY_SELECTED_THEME]           = progress.selectedTheme
            prefs[KEY_OWNED_THEMES]             = progress.ownedThemes
            prefs[KEY_SELECTED_TILE_THEME]      = progress.selectedTileTheme
            prefs[KEY_SELECTED_KEYBOARD_THEME]  = progress.selectedKeyboardTheme
            prefs[KEY_OWNED_TILE_THEMES]        = progress.ownedTileThemes
            prefs[KEY_OWNED_KEYBOARD_THEMES]    = progress.ownedKeyboardThemes
            prefs[KEY_HAS_COMPLETED_ONBOARDING] = progress.hasCompletedOnboarding
            prefs[KEY_NOTIFY_DAILY_CHALLENGE]   = progress.notifyDailyChallenge
            prefs[KEY_DEV_MODE_ENABLED]         = progress.devModeEnabled
            prefs[KEY_VIP_LEVEL]                = progress.vipLevel
            prefs[KEY_VIP_BONUS_COUNTER]        = progress.vipLevelsCompletedSinceBonusLife
        }
    }

    suspend fun saveInProgressGame(state: SavedGameState) {
        val json = Json.encodeToString(state)
        val key = when {
            state.difficultyKey == "easy"    -> KEY_GAME_STATE_EASY
            state.difficultyKey == "regular" -> KEY_GAME_STATE_REGULAR
            state.difficultyKey == "daily_4" -> KEY_GAME_STATE_DAILY_4
            state.difficultyKey == "daily_5" -> KEY_GAME_STATE_DAILY_5
            state.difficultyKey == "daily_6" -> KEY_GAME_STATE_DAILY_6
            state.difficultyKey.startsWith("daily") -> KEY_GAME_STATE_DAILY
            else      -> KEY_GAME_STATE_HARD
        }
        ds.edit { prefs -> prefs[key] = json }
    }

    suspend fun clearInProgressGame(difficultyKey: String) {
        val key = when {
            difficultyKey == "easy"    -> KEY_GAME_STATE_EASY
            difficultyKey == "regular" -> KEY_GAME_STATE_REGULAR
            difficultyKey == "daily_4" -> KEY_GAME_STATE_DAILY_4
            difficultyKey == "daily_5" -> KEY_GAME_STATE_DAILY_5
            difficultyKey == "daily_6" -> KEY_GAME_STATE_DAILY_6
            difficultyKey.startsWith("daily") -> KEY_GAME_STATE_DAILY
            else      -> KEY_GAME_STATE_HARD
        }
        ds.edit { prefs -> prefs.remove(key) }
    }

    suspend fun loadInProgressGame(difficultyKey: String): SavedGameState? {
        val key = when {
            difficultyKey == "easy"    -> KEY_GAME_STATE_EASY
            difficultyKey == "regular" -> KEY_GAME_STATE_REGULAR
            difficultyKey == "daily_4" -> KEY_GAME_STATE_DAILY_4
            difficultyKey == "daily_5" -> KEY_GAME_STATE_DAILY_5
            difficultyKey == "daily_6" -> KEY_GAME_STATE_DAILY_6
            difficultyKey.startsWith("daily") -> KEY_GAME_STATE_DAILY
            else      -> KEY_GAME_STATE_HARD
        }
        val json = ds.data.map { it[key] }.catch { emit(null) }.first()
        return json?.let { runCatching { Json.decodeFromString<SavedGameState>(it) }.getOrNull() }
    }

    suspend fun markFirstLaunchDone() {
        ds.edit { prefs -> prefs[KEY_FIRST_LAUNCH] = false }
    }

    suspend fun markOnboardingDone() {
        ds.edit { prefs -> prefs[KEY_HAS_COMPLETED_ONBOARDING] = true }
    }
}
