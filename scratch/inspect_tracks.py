import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', encoding='utf-8') as f:
    text = f.read()

tracks = text.split("SacredTrack(")[1:]
print(f"Total track blocks: {len(tracks)}")
for idx, t in enumerate(tracks, 1):
    key_match = re.search(r'trackKey\s*=\s*"([^"]+)"', t)
    title_match = re.search(r'titleHindi\s*=\s*"([^"]+)"', t)
    audio_match = re.search(r'audioUrl\s*=\s*"([^"]+)"', t)
    dur_match = re.search(r'durationText\s*=\s*"([^"]+)"', t)
    k = key_match.group(1) if key_match else "unknown"
    tit = title_match.group(1) if title_match else "unknown"
    aud = audio_match.group(1) if audio_match else ""
    dur = dur_match.group(1) if dur_match else ""
    print(f"{idx}. [{k}] {tit} ({dur}) => {aud}")
