"""
expand_words_json.py
Expands app/src/main/assets/words.json with new target words + definitions
sourced from the Free Dictionary API (api.dictionaryapi.dev).

Candidates are drawn from the Google 20k common-English list so that only
recognizable everyday words get added (not obscure Scrabble words).

Run from the word-journey project root:
    py -3.10 scripts/expand_words_json.py
"""
import json
import pathlib
import time
import urllib.request
import urllib.error

ASSETS_DIR = pathlib.Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
WORDS_JSON  = ASSETS_DIR / "words.json"

GOOGLE_20K_URL = (
    "https://raw.githubusercontent.com/first20hours/"
    "google-10000-english/master/20k.txt"
)

MAX_PER_LENGTH = 150   # new words to add per letter-length
MAX_CANDIDATES = 1000  # candidates to try before giving up on a length
DELAY          = 0.08  # seconds between API calls (respectful rate)
DEF_MAX_CHARS  = 130   # max definition length

# ── helpers ───────────────────────────────────────────────────────────────────

def fetch_definition(word: str) -> str | None:
    url = f"https://api.dictionaryapi.dev/api/v2/entries/en/{word.lower()}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=6) as resp:
            data = json.loads(resp.read())
            meanings = data[0].get("meanings", [])
            for meaning in meanings:
                defs = meaning.get("definitions", [])
                if defs:
                    raw = defs[0].get("definition", "").strip()
                    if raw:
                        # Capitalise first letter, strip trailing period, cap length
                        raw = raw[0].upper() + raw[1:]
                        raw = raw.rstrip(".")
                        return raw[:DEF_MAX_CHARS]
    except (urllib.error.HTTPError, urllib.error.URLError, json.JSONDecodeError,
            IndexError, KeyError, TimeoutError):
        pass
    return None


# ── main ──────────────────────────────────────────────────────────────────────

def main():
    # Load current words.json
    print("Loading current words.json …")
    with open(WORDS_JSON, encoding="utf-8") as f:
        words_data: dict[str, list[dict]] = json.load(f)

    before = {k: len(v) for k, v in words_data.items()}
    print(f"  Before: {before}")

    # Build set of existing words per length (uppercase)
    existing: dict[str, set[str]] = {
        k: {entry["word"].upper() for entry in v}
        for k, v in words_data.items()
    }

    # Download Google 20k common-word list
    print(f"\nDownloading common-word candidates …")
    with urllib.request.urlopen(GOOGLE_20K_URL, timeout=30) as resp:
        raw_list = resp.read().decode("utf-8").splitlines()

    candidates_by_len: dict[str, list[str]] = {k: [] for k in "34567"}
    for w in raw_list:
        w = w.strip()
        if not w.isalpha():
            continue
        upper = w.upper()
        length = str(len(upper))
        if length in candidates_by_len:
            candidates_by_len[length].append(upper)

    for length, words in candidates_by_len.items():
        print(f"  {length}-letter candidates: {len(words)}")

    # For each length, try candidates until MAX_PER_LENGTH found or MAX_CANDIDATES tried
    total_added = 0
    for length in ["3", "4", "5", "6", "7"]:
        added = 0
        tried = 0
        candidates = candidates_by_len[length]
        print(f"\n{length}-letter words (target +{MAX_PER_LENGTH}) …")

        for word in candidates:
            if tried >= MAX_CANDIDATES or added >= MAX_PER_LENGTH:
                break
            if word in existing.get(length, set()):
                continue

            tried += 1
            definition = fetch_definition(word)
            if definition:
                words_data[length].append({"word": word, "definition": definition})
                existing[length].add(word)
                added += 1
                if added % 25 == 0:
                    print(f"  … {added} added ({tried} tried)")
                time.sleep(DELAY)

        print(f"  Added {added} new {length}-letter words (tried {tried} candidates)")
        total_added += added

    after = {k: len(v) for k, v in words_data.items()}
    print(f"\nBefore: {before}")
    print(f"After:  {after}")
    print(f"Total added: {total_added}")

    # Save
    with open(WORDS_JSON, "w", encoding="utf-8") as f:
        json.dump(words_data, f, separators=(",", ":"), ensure_ascii=False)
    print(f"\nSaved updated words.json → {WORDS_JSON}")


if __name__ == "__main__":
    main()
