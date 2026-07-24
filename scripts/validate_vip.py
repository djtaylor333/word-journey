import re

f = open(r'app/src/main/java/com/djtaylor/wordjourney/domain/model/VipWordPacks.kt', encoding='utf-8').read()
pattern = re.compile(r'\s+(\d+)\s+to VipWord\("([A-Z]+)"')
errors = []
seen_words = {}
for m in pattern.finditer(f):
    lvl = int(m.group(1))
    word = m.group(2)
    expected_len = [3,4,5,6,7][(lvl - 1) % 5]
    if len(word) != expected_len:
        errors.append(f'Level {lvl}: word={word} len={len(word)} expected={expected_len}')
    if word in seen_words:
        errors.append(f'DUPLICATE: Level {lvl} and Level {seen_words[word]} both use {word}')
    seen_words[word] = lvl

if errors:
    print('ERRORS:')
    for e in errors: print(' ', e)
else:
    print(f'All {len(seen_words)} levels have correct word lengths and no duplicates!')
