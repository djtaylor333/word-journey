package com.djtaylor.wordjourney.audio

data class AudioSettings(
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.7f,
    val sfxEnabled: Boolean = true,
    val sfxVolume: Float = 0.8f
)

/** All sound effect identifiers used throughout the game. */
enum class SfxSound(val resName: String) {
    KEY_TAP("sfx_key_tap"),
    TILE_FLIP("sfx_tile_flip"),
    INVALID_WORD("sfx_invalid_word"),
    WIN("sfx_win"),
    COIN_EARN("sfx_coin_earn"),
    LIFE_LOST("sfx_life_lost"),
    LIFE_GAINED("sfx_life_gained"),
    BUTTON_CLICK("sfx_button_click"),
    NO_LIVES("sfx_no_lives")
}
