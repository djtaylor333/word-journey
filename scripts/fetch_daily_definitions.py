#!/usr/bin/env python3
"""
fetch_daily_definitions.py

Generates daily_word_definitions.json for the Bahai Word Journey app.

For every word in the daily challenge pool (valid_words.json for lengths 4/5/6,
MINUS the level words in words.json), this script looks up a plain-English
definition using NLTK WordNet.

Output: app/src/main/assets/daily_word_definitions.json
  { "CARE": "feel concern or interest", "CRANE": "a large wading bird with ...", ... }

Words with no WordNet definition are simply omitted.
The UI is expected to show nothing (not "no definition found") when a word is absent.

Usage:
  pip install nltk
  python scripts/fetch_daily_definitions.py
"""

import json
import os
import sys

# ---------------------------------------------------------------------------
# Paths (relative to repo root)
# ---------------------------------------------------------------------------
SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT    = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR   = os.path.join(REPO_ROOT, "app", "src", "main", "assets")
VALID_WORDS  = os.path.join(ASSETS_DIR, "valid_words.json")
LEVEL_WORDS  = os.path.join(ASSETS_DIR, "words.json")
OUTPUT_FILE  = os.path.join(ASSETS_DIR, "daily_word_definitions.json")

# ---------------------------------------------------------------------------
# NLTK / WordNet setup
# ---------------------------------------------------------------------------
try:
    import nltk
    from nltk.corpus import wordnet
    # Ensure the data is present
    try:
        wordnet.synsets("test")
    except LookupError:
        print("Downloading NLTK WordNet data …")
        nltk.download("wordnet")
        nltk.download("omw-1.4")
        from nltk.corpus import wordnet  # re-import after download
except ImportError:
    sys.exit("ERROR: NLTK is not installed. Run: pip install nltk")

# ---------------------------------------------------------------------------
# Proper-noun / biography filter
# Definitions that start with these phrases are almost certainly proper nouns.
# ---------------------------------------------------------------------------
PROPER_NOUN_PREFIXES = (
    "united states",
    "english ",
    "british ",
    "us ",
    "an american",
    "american ",
    "scottish ",
    "french ",
    "german ",
    "swiss ",
    "italian ",
    "dutch ",
    "spanish ",
    "greek ",
    "roman ",
    "latin ",
    "danish ",
    "irish ",
    "jewish ",
    "a member of ",
    "a native of ",
    "a follower of ",
    "a person who ",
    "a person born",
)


def best_definition(word: str) -> str | None:
    """
    Return the best plain-English definition for *word* (lowercase input).

    Strategy:
    1. Collect all WordNet synsets for the word.
    2. Prefer synsets where this word is a direct lemma (not a redirected match).
    3. Prefer non-proper-noun senses (filter by prefix list above).
    4. Return the first remaining definition, or None if nothing found.
    """
    syns = wordnet.synsets(word)
    if not syns:
        return None

    # Filter to synsets where the word itself is one of the lemma names
    direct = [s for s in syns if word in {l.name().lower() for l in s.lemmas()}]
    candidates = direct if direct else syns

    # Try to find a non-proper-noun definition first
    for s in candidates:
        defn = s.definition()
        if not any(defn.lower().startswith(p) for p in PROPER_NOUN_PREFIXES):
            return defn

    # Fallback: just return the first candidate even if it looks proper-nouny
    return candidates[0].definition()


# ---------------------------------------------------------------------------
# Build the daily word pool (same logic as DailyChallengeRepository.kt)
# ---------------------------------------------------------------------------
def build_daily_pool() -> set[str]:
    print(f"Loading  valid_words.json …  ({VALID_WORDS})")
    with open(VALID_WORDS, encoding="utf-8") as f:
        valid_root = json.load(f)

    print(f"Loading  words.json …  ({LEVEL_WORDS})")
    with open(LEVEL_WORDS, encoding="utf-8-sig") as f:
        level_root = json.load(f)

    # Collect all level-word strings (excluded from daily pool)
    excluded: set[str] = set()
    for items in level_root.values():
        for obj in items:
            excluded.add(obj["word"].upper())

    # Build pool: 4/5/6-letter valid words that are NOT level words
    pool: set[str] = set()
    for length in ("4", "5", "6"):
        arr = valid_root.get(length, [])
        for w in arr:
            uw = w.upper()
            if uw not in excluded:
                pool.add(uw)

    print(f"Daily word pool size: {len(pool):,}")
    return pool


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    pool = build_daily_pool()
    words_sorted = sorted(pool)

    definitions: dict[str, str] = {}
    no_def_count = 0

    total = len(words_sorted)

    for i, word in enumerate(words_sorted, 1):
        if i % 1000 == 0 or i == total:
            print(f"  [{i:>6}/{total}]  coverage so far: {len(definitions):,} definitions", flush=True)

        defn = best_definition(word.lower())
        if defn:
            definitions[word] = defn
        else:
            no_def_count += 1

    # Sort by key for deterministic output
    ordered = dict(sorted(definitions.items()))

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(ordered, f, ensure_ascii=False, indent=None, separators=(",", ":"))
        f.write("\n")

    print()
    print(f"✓ Wrote {len(ordered):,} definitions  →  {OUTPUT_FILE}")
    print(f"  Words without a definition: {no_def_count:,} ({no_def_count/total*100:.1f}%)")


if __name__ == "__main__":
    main()
