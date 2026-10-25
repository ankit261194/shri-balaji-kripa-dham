import json

# Read existing SacredTracksData.kt to preserve the original 23 tracks lyrics
with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt", "r", encoding="utf-8") as f:
    kt_content = f.read()

# Read DurgaSaptashatiData.kt to extract text for the 17 chapters
with open("app/src/main/java/com/example/shribalajikripadham/data/sacred/DurgaSaptashatiData.kt", "r", encoding="utf-8") as f:
    saptashati_content = f.read()

print("Files read successfully.")
