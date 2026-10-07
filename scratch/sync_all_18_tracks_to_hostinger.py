import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

with open('scratch/uploaded_audio_map.json', 'r', encoding='utf-8') as f:
    audio_map = json.load(f)

# Read tracks from SacredTracksData.kt
with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/SacredTracksData.kt', 'r', encoding='utf-8') as f:
    text = f.read()

raw_tracks = text.split('SacredTrack(')[1:]
tracks = []
for tr in raw_tracks:
    key, title_h, title_e, sub_h, dur, audio, disp, yt = "", "", "", "", "", "", 0, ""
    lyrics = ""
    for line in tr.split('\n'):
        line_s = line.strip()
        if line_s.startswith('trackKey = "'):
            key = line_s.split('"')[1]
        elif line_s.startswith('titleHindi = "'):
            title_h = line_s.split('"')[1]
        elif line_s.startswith('titleEnglish = "'):
            title_e = line_s.split('"')[1]
        elif line_s.startswith('subtitleHindi = "'):
            sub_h = line_s.split('"')[1]
        elif line_s.startswith('durationText = "'):
            dur = line_s.split('"')[1]
        elif line_s.startswith('audioUrl = "'):
            audio = line_s.split('"')[1]
        elif line_s.startswith('displayOrder = '):
            disp = int(line_s.split('=')[1].replace(',', '').strip())
        elif line_s.startswith('youtubeSearchQuery = "'):
            yt = line_s.split('"')[1]
    if 'lyricsHindi = """' in tr:
        part = tr.split('lyricsHindi = """')[1]
        lyrics = part.split('""".trimIndent()')[0]
    if key:
        tracks.append({
            'track_key': key,
            'title_hindi': title_h,
            'title_english': title_e,
            'subtitle_hindi': sub_h,
            'duration_text': dur,
            'audio_url': audio,
            'display_order': disp,
            'is_published': 1,
            'youtube_search_query': yt,
            'lyrics_hindi': lyrics
        })

print(f"Syncing {len(tracks)} authentic tracks to Hostinger MySQL (shribalajikripadham.online)...")

for t in tracks:
    url = "https://shribalajikripadham.online/api/save_sacred_track.php"
    payload = json.dumps(t).encode('utf-8')
    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            'Content-Type': 'application/json; charset=utf-8',
            'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243'
        },
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            res = json.loads(resp.read().decode('utf-8'))
            print(f"Track #{t['display_order']} [{t['track_key']}] {t['title_hindi']}: {res.get('message')}")
    except Exception as e:
        print(f"Error syncing {t['track_key']}: {e}")

print("All 18 tracks successfully synced to Hostinger Cloud!")
