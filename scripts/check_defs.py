import json
d = json.load(open("app/src/main/assets/words.json", "r", encoding="utf-8"))
for l in ["3","4","5","6","7"]:
    words = d.get(l, [])
    print(f"\n=== {l}-letter words ({len(words)} total) ===")
    for w in words[:5]:
        print(f"  {w['word']}: {w['definition'][:60]}")
    if len(words) > 5:
        print(f"  ... and {len(words)-5} more")
