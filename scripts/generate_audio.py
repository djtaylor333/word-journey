"""Generate simple royalty-free WAV sound effects for Word Journeys."""
import struct, math, os, wave

RAW_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(RAW_DIR, exist_ok=True)
SAMPLE_RATE = 22050

def _write_wav(name, samples, sr=SAMPLE_RATE):
    path = os.path.join(RAW_DIR, f"{name}.wav")
    with wave.open(path, "w") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        for s in samples:
            wf.writeframes(struct.pack("<h", max(-32768, min(32767, int(s)))))
    print(f"  Created {path} ({len(samples)} samples, {len(samples)/sr:.2f}s)")

def sine(freq, duration, volume=0.5, sr=SAMPLE_RATE):
    n = int(sr * duration)
    return [volume * 32767 * math.sin(2 * math.pi * freq * i / sr) for i in range(n)]

def fade(samples, fade_in=0.01, fade_out=0.05, sr=SAMPLE_RATE):
    fi = int(sr * fade_in)
    fo = int(sr * fade_out)
    out = list(samples)
    for i in range(min(fi, len(out))):
        out[i] *= i / fi
    for i in range(min(fo, len(out))):
        out[-(i+1)] *= i / fo
    return out

def mix(*tracks):
    length = max(len(t) for t in tracks)
    result = [0.0] * length
    for t in tracks:
        for i, v in enumerate(t):
            result[i] += v
    mx = max(abs(v) for v in result) or 1
    return [v / mx * 30000 for v in result]

def silence(duration, sr=SAMPLE_RATE):
    return [0.0] * int(sr * duration)

print("Generating Word Journeys audio assets...")

# 1. Key tap - short click
_write_wav("sfx_key_tap", fade(sine(800, 0.04, 0.3) + sine(400, 0.03, 0.2), 0.005, 0.02))

# 2. Tile flip - whoosh
flip = []
for i in range(int(SAMPLE_RATE * 0.25)):
    t = i / SAMPLE_RATE
    freq = 300 + 600 * t
    flip.append(0.4 * 32767 * math.sin(2 * math.pi * freq * t) * max(0, 1 - t * 4))
_write_wav("sfx_tile_flip", flip)

# 3. Invalid word - descending buzz
inv = sine(400, 0.08, 0.4) + sine(300, 0.08, 0.3) + sine(200, 0.12, 0.3)
_write_wav("sfx_invalid_word", fade(inv, 0.01, 0.04))

# 4. Win - ascending arpeggio
win = fade(sine(523, 0.12, 0.5)) + fade(sine(659, 0.12, 0.5)) + fade(sine(784, 0.12, 0.5)) + fade(sine(1047, 0.3, 0.6), 0.01, 0.15)
_write_wav("sfx_win", win)

# 5. Coin earn - bright ding
coin = mix(sine(1200, 0.1, 0.5), sine(1800, 0.08, 0.3))
_write_wav("sfx_coin_earn", fade(coin, 0.005, 0.06))

# 6. Life lost - sad tone
ll = fade(sine(440, 0.15, 0.4)) + fade(sine(350, 0.15, 0.4)) + fade(sine(280, 0.25, 0.3), 0.01, 0.12)
_write_wav("sfx_life_lost", ll)

# 7. Life gained - happy chime
lg = fade(sine(600, 0.1, 0.4)) + fade(sine(800, 0.1, 0.4)) + fade(sine(1000, 0.2, 0.5), 0.01, 0.1)
_write_wav("sfx_life_gained", lg)

# 8. Button click
_write_wav("sfx_button_click", fade(sine(600, 0.03, 0.3), 0.005, 0.02))

# 9. No lives - warning
nl = fade(sine(300, 0.15, 0.4)) + silence(0.05) + fade(sine(300, 0.15, 0.4)) + silence(0.05) + fade(sine(250, 0.2, 0.3), 0.01, 0.1)
_write_wav("sfx_no_lives", nl)

# 10. Background music - simple ambient loop (30 seconds)
print("  Generating music_theme (30s ambient loop)...")
music = []
dur = 30.0
n = int(SAMPLE_RATE * dur)
# Chord progression: Am - F - C - G (each 7.5 seconds)
chords = [
    (220.0, 261.6, 329.6),   # Am
    (174.6, 220.0, 261.6),   # F
    (261.6, 329.6, 392.0),   # C
    (196.0, 246.9, 293.7),   # G
]
chord_len = dur / len(chords)
for i in range(n):
    t = i / SAMPLE_RATE
    ci = int(t / chord_len) % len(chords)
    chord = chords[ci]
    # Slow crossfade between chords
    ct = (t % chord_len) / chord_len
    env = min(ct * 8, 1.0) * min((1 - ct) * 8, 1.0)
    val = 0.0
    for f in chord:
        val += math.sin(2 * math.pi * f * t) * 0.2
        val += math.sin(2 * math.pi * f * 2 * t) * 0.05  # soft octave
    # Gentle tremolo
    trem = 0.85 + 0.15 * math.sin(2 * math.pi * 0.25 * t)
    music.append(val * env * trem * 32767 * 0.35)
# Fade in/out
for i in range(min(int(SAMPLE_RATE * 1.0), len(music))):
    music[i] *= i / (SAMPLE_RATE * 1.0)
for i in range(min(int(SAMPLE_RATE * 1.0), len(music))):
    music[-(i+1)] *= i / (SAMPLE_RATE * 1.0)
_write_wav("music_theme", music)

print("Done! All audio files generated in", RAW_DIR)
