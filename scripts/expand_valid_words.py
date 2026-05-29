"""
expand_valid_words.py
Downloads the ENABLE1 comprehensive English word list and merges it into
app/src/main/assets/valid_words.json (3-7 letter words, all uppercase).
Run from the word-journey project root:
    py -3.10 scripts/expand_valid_words.py
"""
import json
import pathlib
import urllib.request
import sys

ASSETS_DIR = pathlib.Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
VALID_WORDS_JSON = ASSETS_DIR / "valid_words.json"

# Reliable mirrors of the ENABLE1 word list (~172,000 common English words)
WORD_LIST_URLS = [
    "https://raw.githubusercontent.com/dolph/dictionary/master/enable1.txt",
    "https://raw.githubusercontent.com/first20hours/google-10000-english/master/20k.txt",
]

def download_word_list(url: str) -> list[str]:
    print(f"  Downloading: {url}")
    with urllib.request.urlopen(url, timeout=30) as resp:
        text = resp.read().decode("utf-8")
    return [w.strip().upper() for w in text.splitlines() if w.strip().isalpha()]

def main():
    # ── Load existing valid_words.json ────────────────────────────────────────
    print("Loading current valid_words.json …")
    with open(VALID_WORDS_JSON, encoding="utf-8") as f:
        existing: dict[str, list[str]] = json.load(f)

    sets: dict[str, set[str]] = {k: set(v) for k, v in existing.items()}
    before = {k: len(v) for k, v in sets.items()}
    print(f"  Before: {before}")

    # ── Download comprehensive word list ──────────────────────────────────────
    all_new_words: list[str] = []
    for url in WORD_LIST_URLS:
        try:
            words = download_word_list(url)
            all_new_words.extend(words)
            print(f"  Got {len(words)} words from {url.split('/')[-1]}")
        except Exception as e:
            print(f"  Warning: failed to download {url}: {e}")

    if not all_new_words:
        print("ERROR: Could not download any word list. Check your internet connection.")
        sys.exit(1)

    # ── Merge into sets by length ─────────────────────────────────────────────
    accepted = 0
    for word in all_new_words:
        length = str(len(word))
        if length in sets and 3 <= len(word) <= 7:
            if word not in sets[length]:
                sets[length].add(word)
                accepted += 1

    after = {k: len(v) for k, v in sets.items()}
    print(f"  After:  {after}")
    print(f"  Total new words added: {accepted}")

    # Spot-check for important words
    for word in ["SEAGULL", "BEACON", "ANCHOR", "PLANET", "MIRROR", "DOLPHIN"]:
        length = str(len(word))
        status = "✓" if word in sets.get(length, set()) else "✗ MISSING"
        print(f"  {word}: {status}")

    # ── Save updated valid_words.json ─────────────────────────────────────────
    output: dict[str, list[str]] = {
        k: sorted(sets[k]) for k in sorted(sets.keys(), key=int)
    }
    with open(VALID_WORDS_JSON, "w", encoding="utf-8") as f:
        json.dump(output, f, separators=(",", ":"))

    print(f"\nSaved updated valid_words.json to {VALID_WORDS_JSON}")

if __name__ == "__main__":
    main()
