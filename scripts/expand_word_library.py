"""
expand_word_library.py
======================
Drastically expands words.json and valid_words.json with comprehensive
English word lists (3–7 letters) + definitions from NLTK WordNet.

Usage:
    python scripts/expand_word_library.py

Output:
    app/src/main/assets/words.json        – game words with definitions
    app/src/main/assets/valid_words.json  – all valid guess words (no definition needed)
"""

import json
import os
import sys
import re

ASSETS_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")

# ── Install dependencies if missing ─────────────────────────────────────────
def ensure_deps():
    import subprocess
    for pkg in ["nltk"]:
        try:
            __import__(pkg)
        except ImportError:
            print(f"Installing {pkg}...")
            subprocess.check_call([sys.executable, "-m", "pip", "install", pkg, "-q"])

ensure_deps()

import nltk
# Download required NLTK corpora (silently if already present)
for corpus in ["words", "wordnet", "omw-1.4", "brown"]:
    try:
        nltk.data.find(f"corpora/{corpus}")
    except LookupError:
        print(f"Downloading NLTK corpus: {corpus}")
        nltk.download(corpus, quiet=True)

from nltk.corpus import words as nltk_words
from nltk.corpus import wordnet as wn

# ── Helpers ──────────────────────────────────────────────────────────────────

def clean_word(w: str) -> str:
    return w.strip().upper()

def is_valid_game_word(w: str) -> bool:
    """Only alphabetic, all-caps words of expected length."""
    return bool(re.match(r'^[A-Z]+$', w))

def get_definition(word: str) -> str | None:
    """Return the best single-sentence definition from WordNet."""
    synsets = wn.synsets(word.lower())
    if not synsets:
        return None
    # Prefer noun / verb over adjective/adverb
    for pos_pref in ['n', 'v', 'a', 'r', 's']:
        for s in synsets:
            if s.pos() == pos_pref:
                defn = s.definition()
                if defn:
                    return defn.capitalize().rstrip('. ') + '.'
    return synsets[0].definition().capitalize().rstrip('. ') + '.'

# ── Load NLTK words corpus ────────────────────────────────────────────────────

print("Loading NLTK word corpus...")
all_nltk_words: set[str] = {clean_word(w) for w in nltk_words.words() if w.isalpha()}
print(f"  Total NLTK words loaded: {len(all_nltk_words)}")

# ── Load existing data ────────────────────────────────────────────────────────

words_path = os.path.join(ASSETS_DIR, "words.json")
valid_path = os.path.join(ASSETS_DIR, "valid_words.json")

with open(words_path, "r", encoding="utf-8") as f:
    existing_words: dict = json.load(f)  # {"3": [{"word":..,"definition":..}], ...}

with open(valid_path, "r", encoding="utf-8") as f:
    existing_valid: dict = json.load(f)  # {"3": ["ACE",...], ...}

# Build lookup sets of already-known words keyed by length
existing_game_words: dict[str, set[str]] = {}
for length_key, word_list in existing_words.items():
    existing_game_words[length_key] = {entry["word"].upper() for entry in word_list}

existing_valid_sets: dict[str, set[str]] = {}
for length_key, word_list in existing_valid.items():
    existing_valid_sets[length_key] = {w.upper() for w in word_list}

# ── Common high-quality 3-letter words extra list (hardcoded supplement) ─────
# These are common, unambiguous English words that WordNet might miss.
EXTRA_3_LETTER = """
ACE ACT ADD AGE AID AIM AIR ALL ARC ARE ARK ARM ART ASH ASK AWE AXE
BAD BAG BAR BAT BAY BED BEG BET BOB BOG BOW BOX BOY BUB BUD BUG BUN
BUS BUT BUY
CAB CAD CAN CAP CAR CAT COB COD COG CON COP COT COW CRY CUB CUD CUP
CUT CAW CUE
DAB DAM DAY DEN DEW DIG DIM DIN DIP DOC DOE DOG DON DOT DRY DUB DUD
DUG DYE
EAR EAT EEL ELK ELM EMU END ERA ERR EVE EWE EYE
FAD FAR FAT FAX FED FIG FIN FIT FIX FLY FOB FOE FOP FOX FRY FUN FUR
GAG GAP GAS GAY GEL GEM GIG GNU GOD GUM GUN GUT GUY GIN GOB GOT GEE
HAD HAG HAP HAS HOP HAT HAW HAY HID HIM HIT HOB HOD HOG HOT HOW HUB
HUE HUM HEX HEN HEM
ICE ILL IMP INK INN ION
JAB JAG JAM JAR JAW JAY JET JIG JOB JOT JOY JUG JUT
KEG KEN KEY KID KIT KOI
LAD LAP LAW LAY LEA LEG LET LID LIP LIT LOB LOG LOT LOW LUG
MAD MAP MAR MOB MOD MOP MOW MOM MUD MUG MUN MAX MOO
NAB NAG NAP NET NOB NOD NUN NUT NIL NIX
OAF OAK OAR OAT ODD ODE OFF OFT OHM OIL OLD ORB ORE OWE OWL OWN OPT
PAD PAL PAN PAP PAR PAT PAW PAY PEA PEG PEN PEP PEW PIE PIG PIN PIT
PLY POD POP POT POX PRY PUB PUG PUN PUP PUS PEW PIT POI PEA
RAG RAM RAP RAT RAW RAY REB RID RIG RIM ROB ROD ROE ROT ROW RUB RUG
RUM RUN RUT RIN RIG RYE
SAC SAG SAP SAT SAW SAX SAY SET SEW SOB SOD SOT SOW SOX SOY SUB SUN
SUP SAG SIN SIP SIR SIT SIX SKY SLY SOB SON SOX SPY STY
TAB TAG TAN TAP TAR TAT TEA TEN TIP TOE TON TOO TOP TOT TOW TOY TUG
TUN TUB TAX TEN TOE TIP TAN TAD TAB TOT
UDO UGH UMP URN USE
VAN VAT VAW VIE VOW VAN VET
WAD WAG WAS WAX WAY WEB WED WIG WIT WOE WOK WAS WIT WIG WEE
YAK YAM YAP YAW YEP YEW YUP
ZAP ZED ZIG ZIP ZIT ZAG
""".split()

EXTRA_3_LETTER_WORDS: set[str] = {w.upper() for w in EXTRA_3_LETTER if w.isalpha()}

# ── Build valid_words for each length ────────────────────────────────────────

LENGTHS = [3, 4, 5, 6, 7]

print("\nBuilding valid word sets...")
new_valid: dict[str, list[str]] = {}
word_counts: dict[str, int] = {}

for length in LENGTHS:
    key = str(length)
    # Start from existing valid set
    current: set[str] = existing_valid_sets.get(key, set()).copy()
    # Add NLTK words
    nltk_at_length = {w for w in all_nltk_words if len(w) == length and is_valid_game_word(w)}
    added = nltk_at_length - current
    current |= nltk_at_length
    # For 3-letter: also add curated supplement
    if length == 3:
        extra = EXTRA_3_LETTER_WORDS - current
        current |= EXTRA_3_LETTER_WORDS
        print(f"  3-letter: +{len(EXTRA_3_LETTER_WORDS)} curated words ({len(extra)} new)")
    sorted_words = sorted(current)
    new_valid[key] = sorted_words
    prev = len(existing_valid_sets.get(key, set()))
    word_counts[key] = len(sorted_words)
    print(f"  {length}-letter valid: {prev} → {len(sorted_words)} (+{len(sorted_words)-prev})")

# ── Build game words (with definitions) for each length ──────────────────────

print("\nBuilding game word definitions (this may take a few minutes)...")

MAX_NEW_GAME_WORDS: dict[int, int] = {3: 500, 4: 1500, 5: 2000, 6: 1500, 7: 1000}

new_words: dict[str, list[dict]] = {}

for length in LENGTHS:
    key = str(length)
    current_game: list[dict] = list(existing_words.get(key, []))
    known: set[str] = {e["word"].upper() for e in current_game}

    candidates = sorted(new_valid[key])
    added = 0
    target = MAX_NEW_GAME_WORDS[length]
    
    for word in candidates:
        if word in known:
            continue
        if len(current_game) >= target:
            break
        defn = get_definition(word)
        if defn and len(defn) > 10:  # Skip very short/empty definitions
            current_game.append({"word": word, "definition": defn})
            known.add(word)
            added += 1
    
    new_words[key] = current_game
    prev = len(existing_words.get(key, []))
    print(f"  {length}-letter game words: {prev} → {len(current_game)} (+{added})")

# ── Write output files ────────────────────────────────────────────────────────

print("\nWriting words.json ...")
with open(words_path, "w", encoding="utf-8") as f:
    json.dump(new_words, f, ensure_ascii=True, indent=None, separators=(',', ':'))

print("Writing valid_words.json ...")
with open(valid_path, "w", encoding="utf-8") as f:
    json.dump(new_valid, f, ensure_ascii=True, indent=None, separators=(',', ':'))

print("\n✅ Word library expansion complete!")
for length in LENGTHS:
    key = str(length)
    print(f"  {length}-letter: {len(new_valid[key]):,} valid | {len(new_words[key]):,} game words")
