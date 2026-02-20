package com.djtaylor.wordjourney.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages background music (MediaPlayer) and sound effects (SoundPool).
 *
 * Audio resource files are loaded with a safe fallback — if a raw resource
 * does not exist in res/raw/, the corresponding sound is silently skipped.
 * This allows the app to compile and run before real audio assets are added.
 *
 * To add real audio:  place MP3/OGG files in app/src/main/res/raw/
 * matching the names in [SfxSound.resName] and "music_theme" for BGM.
 */
@Singleton
class WordJourneysAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Background music ──────────────────────────────────────────────────────
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicPrepared = false

    // ── Sound effects ─────────────────────────────────────────────────────────
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SfxSound, Int>()

    // ── Settings state ────────────────────────────────────────────────────────
    var settings: AudioSettings = AudioSettings()
        private set

    init {
        initSoundPool()
    }

    fun updateSettings(new: AudioSettings) {
        settings = new
        mediaPlayer?.setVolume(
            if (new.musicEnabled) new.musicVolume else 0f,
            if (new.musicEnabled) new.musicVolume else 0f
        )
    }

    // ── Music controls ────────────────────────────────────────────────────────
    fun startMusic() {
        if (!settings.musicEnabled) return
        if (mediaPlayer == null) {
            mediaPlayer = createMusicPlayer() ?: return
        }
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }

    fun pauseMusic() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isMusicPrepared = false
    }

    fun setMusicEnabled(enabled: Boolean) {
        settings = settings.copy(musicEnabled = enabled)
        if (enabled) startMusic() else pauseMusic()
    }

    fun setMusicVolume(volume: Float) {
        settings = settings.copy(musicVolume = volume)
        if (settings.musicEnabled) {
            mediaPlayer?.setVolume(volume, volume)
        }
    }

    // ── SFX controls ──────────────────────────────────────────────────────────
    fun playSfx(sound: SfxSound) {
        if (!settings.sfxEnabled) return
        val id = soundIds[sound] ?: return
        if (id == 0) return
        soundPool?.play(id, settings.sfxVolume, settings.sfxVolume, 1, 0, 1f)
    }

    fun setSfxEnabled(enabled: Boolean) {
        settings = settings.copy(sfxEnabled = enabled)
    }

    fun setSfxVolume(volume: Float) {
        settings = settings.copy(sfxVolume = volume)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    fun onForeground() {
        if (settings.musicEnabled) startMusic()
    }

    fun onBackground() {
        pauseMusic()
    }

    fun release() {
        scope.cancel()
        stopMusic()
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private fun createMusicPlayer(): MediaPlayer? {
        val resId = context.resources.getIdentifier("music_theme", "raw", context.packageName)
        if (resId == 0) return null  // audio file not yet added — skip gracefully
        return try {
            MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(settings.musicVolume, settings.musicVolume)
            }
        } catch (_: Exception) { null }
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        for (sound in SfxSound.entries) {
            val resId = context.resources.getIdentifier(sound.resName, "raw", context.packageName)
            if (resId != 0) {
                val id = soundPool!!.load(context, resId, 1)
                soundIds[sound] = id
            }
            // If resource doesn't exist yet, entry is simply absent from the map
        }
    }
}
