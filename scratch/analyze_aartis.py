import re, sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Split by "SacredTrack("
tracks = text.split('SacredTrack(')[1:]
for idx, tr in enumerate(tracks):
    key = ""
    title = ""
    lyrics = ""
    for line in tr.split('\n'):
        if 'trackKey =' in line:
            key = line.split('"')[1]
        elif 'titleHindi =' in line:
            title = line.split('"')[1]
    if 'lyricsHindi = """' in tr:
        part = tr.split('lyricsHindi = """')[1]
        lyrics = part.split('""".trimIndent()')[0]
    lines = [l.strip() for l in lyrics.split('\n') if l.strip()]
    if key or title:
        first_line = lines[0] if lines else ""
        last_line = lines[-1] if lines else ""
        print(f"#{idx+1}: [{key}] {title}")
        print(f"    Line count: {len(lines)}, Char count: {len(lyrics)}")
        print(f"    First: {first_line}")
        print(f"    Last:  {last_line}")
        print()
