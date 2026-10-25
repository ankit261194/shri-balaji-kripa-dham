import re
import json

# 1. Read existing SacredTracksData.kt
with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt", "r", encoding="utf-8") as f:
    orig_kt = f.read()

# 2. Read DurgaSaptashatiData.kt
with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/DurgaSaptashatiData.kt", "r", encoding="utf-8") as f:
    ds_kt = f.read()

# Extract verses from DurgaSaptashatiData.kt
# We want devanagariText for Kavach, Argala, Keelak, Kshama, and Ch 1-13
def extract_verse_text(block_name, verse_num):
    # Regex to find verse block
    pattern = rf'number\s*=\s*{verse_num},.*?devanagariText\s*=\s*"""(.*?)""".*?hindiMeaning\s*=\s*"(.*?)"'
    match = re.search(pattern, ds_kt, re.DOTALL)
    if match:
        dev = match.group(1).strip()
        meaning = match.group(2).strip()
        return dev + "\n\n॥ हिंदी भावार्थ ॥\n" + meaning
    return ""

kavach_text = extract_verse_text("nitya", 2)
argala_text = extract_verse_text("nitya", 3)
keelak_text = extract_verse_text("nitya", 4)
kshama_text = extract_verse_text("nitya", 5)

chapters_text = {}
for ch in range(1, 14):
    chapters_text[ch] = extract_verse_text("sampurna", ch)

print(f"Extracted Kavach: {len(kavach_text)}, Argala: {len(argala_text)}, Keelak: {len(keelak_text)}, Kshama: {len(kshama_text)}")
print(f"Extracted Ch 1: {len(chapters_text[1])}, Ch 13: {len(chapters_text[13])}")
