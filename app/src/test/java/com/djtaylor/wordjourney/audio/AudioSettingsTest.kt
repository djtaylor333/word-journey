package com.djtaylor.wordjourney.audio

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for [AudioSettings] and [SfxSound].
 */
class AudioSettingsTest {

    // ── Default values ────────────────────────────────────────────────────────

    @Test
    fun `default AudioSettings has music enabled`() {
        assertTrue(AudioSettings().musicEnabled)
    }

    @Test
    fun `default AudioSettings has sfx enabled`() {
        assertTrue(AudioSettings().sfxEnabled)
    }

    @Test
    fun `default music volume is 0_7`() {
        assertEquals(0.7f, AudioSettings().musicVolume, 0.001f)
    }

    @Test
    fun `default sfx volume is 0_8`() {
        assertEquals(0.8f, AudioSettings().sfxVolume, 0.001f)
    }

    // ── Immutable copy ────────────────────────────────────────────────────────

    @Test
    fun `copy with musicEnabled false disables music`() {
        val settings = AudioSettings().copy(musicEnabled = false)
        assertFalse(settings.musicEnabled)
        assertTrue(settings.sfxEnabled)  // sfx unchanged
    }

    @Test
    fun `copy with sfxVolume updates only sfx volume`() {
        val settings = AudioSettings().copy(sfxVolume = 0.5f)
        assertEquals(0.5f, settings.sfxVolume, 0.001f)
        assertEquals(0.7f, settings.musicVolume, 0.001f) // music unchanged
    }

    @Test
    fun `copy with zero volumes retains enabled flags`() {
        val settings = AudioSettings().copy(musicVolume = 0f, sfxVolume = 0f)
        assertTrue(settings.musicEnabled)
        assertTrue(settings.sfxEnabled)
        assertEquals(0f, settings.musicVolume, 0.001f)
        assertEquals(0f, settings.sfxVolume, 0.001f)
    }

    @Test
    fun `two AudioSettings with same values are equal`() {
        val a = AudioSettings(musicEnabled = true, musicVolume = 0.5f, sfxEnabled = false, sfxVolume = 0.3f)
        val b = AudioSettings(musicEnabled = true, musicVolume = 0.5f, sfxEnabled = false, sfxVolume = 0.3f)
        assertEquals(a, b)
    }

    @Test
    fun `two AudioSettings with different values are not equal`() {
        val a = AudioSettings()
        val b = AudioSettings().copy(sfxEnabled = false)
        assertNotEquals(a, b)
    }

    // ── SfxSound enum ─────────────────────────────────────────────────────────

    @Test
    fun `all SfxSound entries have non-empty resName`() {
        for (sound in SfxSound.entries) {
            assertTrue("${sound.name} has empty resName", sound.resName.isNotEmpty())
        }
    }

    @Test
    fun `all SfxSound resNames start with sfx_`() {
        for (sound in SfxSound.entries) {
            assertTrue(
                "${sound.name} resName '${sound.resName}' does not start with 'sfx_'",
                sound.resName.startsWith("sfx_")
            )
        }
    }

    @Test
    fun `SfxSound resNames are unique`() {
        val names = SfxSound.entries.map { it.resName }
        assertEquals("Duplicate resNames found", names.size, names.distinct().size)
    }

    @Test
    fun `SfxSound has expected key entries`() {
        val names = SfxSound.entries.map { it.name }
        assertTrue(names.contains("WIN"))
        assertTrue(names.contains("TILE_FLIP"))
        assertTrue(names.contains("KEY_TAP"))
        assertTrue(names.contains("INVALID_WORD"))
        assertTrue(names.contains("COIN_EARN"))
    }
}
