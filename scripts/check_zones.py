#!/usr/bin/env python3
"""Check zone decor lists for emoji issues."""
import re, os

BASE = r"c:\Users\david\OneDrive\Documents\projects\AI Projects\word-journey"
content = open(os.path.join(BASE, "app", "src", "main", "java", "com", "djtaylor",
    "wordjourney", "ui", "levelselect", "LevelSelectScreen.kt"), encoding="utf-8").read()

zones = re.findall(r'ZoneTheme\("(.*?)".*?listOf\((.*?)\)\)', content, re.DOTALL)
problems = []
for name, decor_str in zones:
    decors = re.findall(r'"(.*?)"', decor_str)
    for d in decors:
        blen = len(d.encode("utf-8"))
        if blen > 12:
            problems.append((name, d, blen, repr(d)))

print(f"Total zones: {len(zones)}")
if problems:
    print("Suspicious decor entries (>12 bytes — possible double emoji):")
    for name, d, blen, r in problems:
        print(f"  Zone '{name}': {r}  ({blen} bytes utf-8)")
else:
    print("All decor entries look clean.")
