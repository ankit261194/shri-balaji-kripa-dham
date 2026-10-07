import re, sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    text = f.read()

tracks = text.split('SacredTrack(')[1:]
for idx, tr in enumerate(tracks):
    key, title, audio = "", "", ""
    for line in tr.split('\n'):
        if 'trackKey =' in line:
            key = line.split('"')[1]
        elif 'titleHindi =' in line:
            title = line.split('"')[1]
        elif 'audioUrl =' in line:
            audio = line.split('"')[1]
    if key:
        print(f"{idx+1}. [{key}] {title} -> {audio}")
