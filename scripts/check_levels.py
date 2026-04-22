#!/usr/bin/env python3
"""
Check VIP and seasonal level packs for issues:
- Word counts per pack
- Duplicates within and across packs
- VIP level 7 word data
- VIP word pool sizes and cycle correctness
"""
import re
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── 1. Check seasonal word packs ─────────────────────────────────────────────
print("=" * 60)
print("SEASONAL WORD PACKS")
print("=" * 60)

seasonal_path = os.path.join(
    BASE, "app", "src", "main", "java",
    "com", "djtaylor", "wordjourney", "domain", "model",
    "SeasonalWordPacks.kt"
)
content = open(seasonal_path, encoding="utf-8").read()

# Extract each private val XXX_WORDS = listOf(...)
pack_sections = re.findall(
    r'private val (\w+_WORDS)\s*=\s*listOf\((.*?)\)',
    content, re.DOTALL
)

all_seasonal_words = []
for pack_name, words_str in pack_sections:
    words = re.findall(r'"([A-Z]{5})"', words_str)
    dupes = sorted(set(w for w in words if words.count(w) > 1))
    status = "OK" if len(words) == 100 and not dupes else "ISSUE"
    print(f"  {status}  {pack_name}: {len(words)} words", end="")
    if len(words) != 100:
        print(f" (expected 100!)", end="")
    if dupes:
        print(f" | internal duplicates: {dupes}", end="")
    print()
    all_seasonal_words.extend(words)

# Cross-pack duplicates
seen = {}
cross_dupes = []
for pack_name, words_str in pack_sections:
    words = re.findall(r'"([A-Z]{5})"', words_str)
    for w in words:
        if w in seen and seen[w] != pack_name:
            cross_dupes.append((w, seen[w], pack_name))
        seen.setdefault(w, pack_name)

if cross_dupes:
    print(f"\n  CROSS-PACK DUPLICATES ({len(cross_dupes)}):")
    for word, p1, p2 in cross_dupes:
        print(f"    {word}: {p1} & {p2}")
else:
    print("\n  No cross-pack duplicates.")

# ── 2. Check VIP word pools ───────────────────────────────────────────────────
print()
print("=" * 60)
print("VIP WORD POOLS (words.json)")
print("=" * 60)

words_json_path = os.path.join(BASE, "app", "src", "main", "assets", "words.json")
with open(words_json_path, encoding="utf-8") as f:
    words_data = json.load(f)

VIP_POOL_START = {4: 480, 5: 480, 6: 480}
LENGTHS = [3, 4, 5, 6, 7]

for wlen in LENGTHS:
    key = str(wlen)
    all_words = words_data.get(key, [])
    start = VIP_POOL_START.get(wlen, 0)  # 3 and 7 are VIP-exclusive (start=0)
    vip_pool = all_words[start:]
    print(f"  Length {wlen}: total={len(all_words)}, VIP pool start={start}, VIP pool size={len(vip_pool)}")
    if len(vip_pool) == 0:
        print(f"    !! EMPTY VIP POOL for length {wlen} !!")

# ── 3. Check VIP levels 1-10 (two full cycles) ───────────────────────────────
print()
print("=" * 60)
print("VIP LEVELS 1-10 (word assignment check)")
print("=" * 60)

def vip_word_length(level):
    lengths = [3, 4, 5, 6, 7]
    return lengths[(level - 1) % 5]

for level in range(1, 11):
    wlen = vip_word_length(level)
    key = str(wlen)
    all_words = words_data.get(key, [])
    start = VIP_POOL_START.get(wlen, 0)
    vip_pool = all_words[start:]
    if not vip_pool:
        print(f"  Level {level:2d}: length={wlen} -- !! NO WORDS IN VIP POOL !!")
        continue
    idx = (level - 1) % len(vip_pool)
    word = vip_pool[idx]
    word_str = word if isinstance(word, str) else word.get("word", str(word))
    has_def = isinstance(word, dict) and bool(word.get("definition"))
    print(f"  Level {level:2d}: length={wlen}, pool={len(vip_pool)}, idx={idx}, word={word_str}, has_def={has_def}")

# ── 4. Check VIP levels 1-500 for any pool index issues ──────────────────────
print()
print("=" * 60)
print("VIP LEVELS 1-500 (checking for any empty/missing words)")
print("=" * 60)

issues = []
for level in range(1, 501):
    wlen = vip_word_length(level)
    key = str(wlen)
    all_words = words_data.get(key, [])
    start = VIP_POOL_START.get(wlen, 0)
    vip_pool = all_words[start:]
    if not vip_pool:
        issues.append(f"Level {level}: length={wlen} has EMPTY pool")
        continue
    idx = (level - 1) % len(vip_pool)
    word = vip_pool[idx]
    word_str = word if isinstance(word, str) else word.get("word", str(word))
    if not word_str or len(word_str) != wlen:
        issues.append(f"Level {level}: expected length {wlen}, got '{word_str}'")
    if isinstance(word, dict) and not word.get("definition"):
        issues.append(f"Level {level}: word '{word_str}' missing definition")

if issues:
    print(f"  Found {len(issues)} issues:")
    for issue in issues[:20]:
        print(f"    {issue}")
    if len(issues) > 20:
        print(f"    ... and {len(issues) - 20} more")
else:
    print("  All 500 VIP levels OK.")

print()
print("Done.")
