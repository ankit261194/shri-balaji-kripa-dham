import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

url = 'https://shribalajikripadham.online/api/get_sacred_tracks.php'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print('Success:', data.get('success'), 'Count:', len(data.get('tracks', [])))
        for t in data.get('tracks', []):
            lyrics = t.get('lyrics_hindi', '')
            print(f"ID: {t.get('id')}, Key: {t.get('track_key')}, Title: {t.get('title_hindi')}, Lyrics len: {len(lyrics)}, Lyrics: {lyrics[:100]}...")
except Exception as e:
    print('Error:', e)
