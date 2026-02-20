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
        val KEY_MUSIC_ENABLED                  = booleanPreferencesKey("music_enabled")
        val KEY_MUSIC_VOLUME                   = floatPreferencesKey("music_volume")
        val KEY_SFX_ENABLED                    = booleanPreferencesKey("sfx_enabled")
        val KEY_SFX_VOLUME                     = floatPreferencesKey("sfx_volume")
        val KEY_NOTIFY_LIVES_FULL              = booleanPreferencesKey("notify_lives_full")
        val KEY_HIGH_CONTRAST                  = booleanPreferencesKey("high_contrast")
        val KEY_DARK_MODE                      = booleanPreferencesKey("dark_mode")
        val KEY_PLAY_GAMES_SIGNED_IN           = booleanPreferencesKey("play_games_signed_in")
        val KEY_FIRST_LAUNCH                   = booleanPreferencesKey("first_launch")
        // In-progress serialized game states
        val KEY_GAME_STATE_EASY                = stringPreferencesKey("game_state_easy")
        val KEY_GAME_STATE_REGULAR             = stringPreferencesKey("game_state_regular")
        val KEY_GAME_STATE_HARD                = stringPreferencesKey("game_state_hard")
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
                musicEnabled      = prefs[KEY_MUSIC_ENABLED] ?: true,
                musicVolume       = prefs[KEY_MUSIC_VOLUME] ?: 0.7f,
                sfxEnabled        = prefs[KEY_SFX_ENABLED] ?: true,
                sfxVolume         = prefs[KEY_SFX_VOLUME] ?: 0.8f,
                notifyLivesFull   = prefs[KEY_NOTIFY_LIVES_FULL] ?: true,
                highContrast      = prefs[KEY_HIGH_CONTRAST] ?: false,
                darkMode          = prefs[KEY_DARK_MODE] ?: true,
                playGamesSignedIn = prefs[KEY_PLAY_GAMES_SIGNED_IN] ?: false
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
            prefs[KEY_MUSIC_ENABLED]            = progress.musicEnabled
            prefs[KEY_MUSIC_VOLUME]             = progress.musicVolume
            prefs[KEY_SFX_ENABLED]              = progress.sfxEnabled
            prefs[KEY_SFX_VOLUME]               = progress.sfxVolume
            prefs[KEY_NOTIFY_LIVES_FULL]        = progress.notifyLivesFull
            prefs[KEY_HIGH_CONTRAST]            = progress.highContrast
            prefs[KEY_DARK_MODE]                = progress.darkMode
            prefs[KEY_PLAY_GAMES_SIGNED_IN]     = progress.playGamesSignedIn
        }
    }

    suspend fun saveInProgressGame(state: SavedGameState) {
        val json = Json.encodeToString(state)
        val key = when (state.difficultyKey) {
            "easy"    -> KEY_GAME_STATE_EASY
            "regular" -> KEY_GAME_STATE_REGULAR
            else      -> KEY_GAME_STATE_HARD
        }
        ds.edit { prefs -> prefs[key] = json }
    }

    suspend fun clearInProgressGame(difficultyKey: String) {
        val key = when (difficultyKey) {
            "easy"    -> KEY_GAME_STATE_EASY
            "regular" -> KEY_GAME_STATE_REGULAR
            else      -> KEY_GAME_STATE_HARD
        }
        ds.edit { prefs -> prefs.remove(key) }
    }

    suspend fun loadInProgressGame(difficultyKey: String): SavedGameState? {
        val key = when (difficultyKey) {
            "easy"    -> KEY_GAME_STATE_EASY
            "regular" -> KEY_GAME_STATE_REGULAR
            else      -> KEY_GAME_STATE_HARD
        }
        val json = ds.data.map { it[key] }.catch { emit(null) }
            .let { flow ->
                var result: String? = null
                // Collect single value
                flow.collect { result = it }
                result
            }
        return json?.let { runCatching { Json.decodeFromString<SavedGameState>(it) }.getOrNull() }
    }

    suspend fun markFirstLaunchDone() {
        ds.edit { prefs -> prefs[KEY_FIRST_LAUNCH] = false }
    }
}
