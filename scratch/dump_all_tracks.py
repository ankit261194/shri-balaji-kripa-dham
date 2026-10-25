import re

kt_file = r"app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt"
with open(kt_file, "r", encoding="utf-8") as f:
    text = f.read()

pattern = re.compile(
    r'SacredTrack\(\s*id\s*=\s*(\d+),\s*trackKey\s*=\s*"([^"]+)",\s*titleHindi\s*=\s*"([^"]+)",\s*titleEnglish\s*=\s*"([^"]+)",\s*subtitleHindi\s*=\s*"([^"]*)",\s*durationText\s*=\s*"([^"]*)",\s*audioUrl\s*=\s*"([^"]*)",\s*lyricsHindi\s*=\s*"""(.*?)"""(?:\.trimIndent\(\))?,\s*isPublished\s*=\s*(true|false),\s*displayOrder\s*=\s*(\d+),\s*youtubeSearchQuery\s*=\s*"([^"]*)"\s*\)',
    re.DOTALL
)

matches = pattern.findall(text)
with open("scratch/dump_tracks.txt", "w", encoding="utf-8") as out:
    for m in matches:
        tid, key, th, te, sub, dur, url, lyr, pub, order, yq = m
        lyr_lines = [l.strip() for l in lyr.strip().split("\n") if l.strip()][:3]
        out.write(f"=== ID {tid}: {th} (Key: {key}) ===\n")
        out.write(f"Subtitle: {sub}\n")
        out.write(f"Audio URL: {url}\n")
        out.write(f"First Lines: {' / '.join(lyr_lines)}\n\n")

print(f"Dumped {len(matches)} tracks.")
